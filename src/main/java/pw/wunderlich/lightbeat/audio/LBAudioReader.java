package pw.wunderlich.lightbeat.audio;

import pw.wunderlich.lightbeat.audio.device.*;
import pw.wunderlich.lightbeat.config.Config;
import pw.wunderlich.lightbeat.config.ConfigNode;
import org.jtransforms.fft.DoubleFFT_1D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Default {@link AudioReader} implementation, that also serves as an {@link BeatEventManager}.
 * Get a list of supported audio devices with {@link #getSupportedDevices()}, which can be used to start
 * the scheduled beat detection thread via {@link #start(AudioDevice)}. Can filter out all frequencies
 * but bass frequency via FFT, if config option {@link ConfigNode#BEAT_BASS_ONLY_MODE} is set to
 * true. Classes can register to receive {@link BeatEvent BeatEvents} by calling
 * {@link #registerBeatObserver(BeatObserver)}, that are called whenever a beat was detected.
 */
public class LBAudioReader implements BeatEventManager, AudioReader {

    private static final int AMPLITUDES_PER_SECOND = 100;
    // Define a specific frequency for the bass cutoff instead of a magic percentage.
    private static final double BASS_CUTOFF_HZ = 200.0;
    private static final double MINIMUM_AMPLITUDE = 0.005d;

    private static final Logger logger = LoggerFactory.getLogger(LBAudioReader.class);

    private final Config config;
    private final ScheduledExecutorService executorService;
    private final List<DeviceProvider> deviceProviders = new ArrayList<>();
    private final List<BeatObserver> beatEventObservers = new ArrayList<>();

    private AudioDevice audioDevice;
    private BeatInterpreter beatInterpreter;
    private ScheduledFuture<?> future;
    private ByteBuffer audioBuffer;


    public LBAudioReader(Config config, ScheduledExecutorService executorService) {
        this.config = config;
        this.executorService = executorService;

        if (WASAPIDeviceProvider.isWindows()) {
            deviceProviders.add(new WASAPIDeviceProvider());
        }
        deviceProviders.add(new JavaAudioDeviceProvider());
    }

    @Override
    public List<AudioDevice> getSupportedDevices() {
        return deviceProviders.stream()
                .flatMap(deviceProvider -> deviceProvider.getAudioDevices().stream())
                .collect(Collectors.toList());
    }

    @Override
    public AudioDevice getDeviceByName(String name) {
        return getSupportedDevices().stream()
                .filter(device -> device.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean start(AudioDevice audioDevice) {
        if (isOpen()) {
            stop();
        }

        boolean started = audioDevice.start();
        if (!started) {
            logger.warn("Couldn't read from selected audio device {}", audioDevice.getName());
            return false;
        }

        this.audioDevice = audioDevice;
        this.beatInterpreter = new BeatInterpreter(config);

        LBAudioFormat audioFormat = audioDevice.getAudioFormat();
        int bytesPerSecond = (int) (audioFormat.sampleRate() * audioFormat.getBytesPerFrame());
        int bytesPerChunk = bytesPerSecond / AMPLITUDES_PER_SECOND;
        int samplesPerChunk = bytesPerChunk / audioFormat.getBytesPerFrame();

        audioBuffer = ByteBuffer.allocate(bytesPerChunk);
        audioBuffer.order(audioFormat.littleEndian() ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);//TODO why does big endian not work

        //TODO investigate amplitude differences between jitsi and java audio api
        long intervalMillis = 1000 / AMPLITUDES_PER_SECOND;

        future = executorService.scheduleAtFixedRate(() -> {
            if (!isOpen()) {
                logger.error("Selected audio stream is no longer available");
                stop();
                return;
            }

            int availableBytes = audioDevice.available();
            if (availableBytes < bytesPerChunk) {
                return;
            }

            int bytesToSkip = availableBytes - bytesPerChunk;
            while (bytesToSkip > 0) {
                int amountToRead = Math.min(bytesToSkip, audioBuffer.array().length);
                int bytesSkipped = audioDevice.read(audioBuffer.array(), amountToRead);

                if (bytesSkipped <= 0) {
                    logger.warn("Audio stream ended while trying to skip stale data.");
                    return;
                }
                bytesToSkip -= bytesSkipped;
            }

            int bytesRead = audioDevice.read(audioBuffer.array(), bytesPerChunk);
            if (bytesRead < bytesPerChunk) {
                logger.warn("Only {} bytes were read, expected {} bytes. Stream may be closing.", bytesRead, bytesPerChunk);
                return;
            }

            // Convert to normalized values
            double[] normalizedAudioBuffer = new double[samplesPerChunk];
            for (int i = 0; i < normalizedAudioBuffer.length; i++) {
                int bytePosition = i * audioFormat.bytesPerSample();

                if (audioFormat.bytesPerSample() == 2) {
                    normalizedAudioBuffer[i] = audioBuffer.getShort(bytePosition) / (double) Short.MAX_VALUE;
                } else {
                    normalizedAudioBuffer[i] = audioBuffer.get(bytePosition) / (double) Byte.MAX_VALUE;
                }
            }

            if (config.getBoolean(ConfigNode.BEAT_BASS_ONLY_MODE)) {
                lowPassFilter(normalizedAudioBuffer, audioFormat);
            }

            double rms = Arrays.stream(normalizedAudioBuffer)
                    .map(val -> val * val)
                    .average()
                    .orElse(0d);
            rms = Math.sqrt(rms);

            BeatEvent event = beatInterpreter.interpretValue(rms >= MINIMUM_AMPLITUDE ? rms : 0d);
            if (event != null) {
                if (event.isSilence()) {
                    beatEventObservers.forEach(BeatObserver::silenceDetected);
                } else if (event.isNoBeat()) {
                    beatEventObservers.forEach(BeatObserver::noBeatReceived);
                } else {
                    beatEventObservers.forEach(toNotify -> toNotify.beatReceived(event));
                }
            }

        }, 0L, intervalMillis, TimeUnit.MILLISECONDS);

        logger.info("Now listening to audio input from device {} ({})", audioDevice.getName(), audioFormat);
        return true;
    }

    /**
     * Applies a low-pass filter using FFT, cutting off frequencies above BASS_CUTOFF_HZ.
     * This implementation is now robust against changes in sample rate or chunk size.
     */
    private void lowPassFilter(double[] normalizedSampleArray, LBAudioFormat format) {
        int sampleCount = normalizedSampleArray.length;
        if (sampleCount == 0) return;

        DoubleFFT_1D fft = new DoubleFFT_1D(sampleCount);
        fft.realForward(normalizedSampleArray);

        // Calculate the frequency represented by each bin in the FFT output.
        double freqPerBin = format.sampleRate() / sampleCount;
        int cutoffBin = (int) (BASS_CUTOFF_HZ / freqPerBin);

        for (int i = cutoffBin; i < sampleCount / 2; i++) {
            normalizedSampleArray[i] = 0d;
        }

        fft.realInverse(normalizedSampleArray, true);
    }

    @Override
    public boolean isOpen() {
        return audioDevice != null && audioDevice.isOpen();
    }

    @Override
    public void stop() {
        if (audioDevice != null && future != null) {
            future.cancel(true);

            BeatObserver.StopStatus status = audioDevice.isOpen() ? BeatObserver.StopStatus.USER : BeatObserver.StopStatus.ERROR;
            beatEventObservers.forEach(beatObserver -> beatObserver.audioReaderStopped(status));

            audioDevice.stop();
            audioDevice = null;
            audioBuffer = null;

            beatEventObservers.clear();
            logger.info("No longer listening to audio input");
        }
    }

    @Override
    public void registerBeatObserver(BeatObserver beatObserver) {
        beatEventObservers.add(beatObserver);
    }
}
