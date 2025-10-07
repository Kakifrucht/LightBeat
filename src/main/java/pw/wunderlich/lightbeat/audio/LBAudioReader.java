package pw.wunderlich.lightbeat.audio;

import org.jtransforms.fft.DoubleFFT_1D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pw.wunderlich.lightbeat.AppTaskOrchestrator;
import pw.wunderlich.lightbeat.audio.device.*;
import pw.wunderlich.lightbeat.config.Config;
import pw.wunderlich.lightbeat.config.ConfigNode;
import pw.wunderlich.lightbeat.util.TimeThreshold;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Default {@link AudioReader} implementation that also serves as an {@link BeatEventManager}.
 * This implementation uses a listener-based approach to process audio data as it becomes available.
 * It can filter frequencies for bass detection and notifies registered {@link BeatObserver}s when a beat is detected.
 */
public class LBAudioReader implements BeatEventManager, AudioReader {

    private static final int AMPLITUDES_PER_SECOND = 50;
    private static final double BASS_CUTOFF_HZ = 200.0;
    private static final double MINIMUM_AMPLITUDE = 0.005d;

    private static final Logger logger = LoggerFactory.getLogger(LBAudioReader.class);

    private final Config config;
    private final AppTaskOrchestrator taskOrchestrator;

    private final List<DeviceProvider> deviceProviders;

    private final List<BeatObserver> beatEventObservers = new ArrayList<>();

    private AudioDevice audioDevice;
    private BeatInterpreter beatInterpreter;
    private TimeThreshold nextBeatThreshold;
    private ScheduledFuture<?> healthCheckFuture;

    private ByteBuffer remainderBuffer;
    private LBAudioFormat audioFormat;
    private int bytesPerChunk;
    private int samplesPerChunk;


    public LBAudioReader(Config config, AppTaskOrchestrator taskOrchestrator) {
        this.config = config;
        this.taskOrchestrator = taskOrchestrator;

        this.deviceProviders = new ArrayList<>();
        if (WASAPIDeviceProvider.isWindows()) {
            deviceProviders.add(new WASAPIDeviceProvider(taskOrchestrator));
        } else if (CoreAudioDeviceProvider.isMac()) {
            deviceProviders.add(new CoreAudioDeviceProvider(taskOrchestrator));
        } else if (PulseAudioDeviceProvider.isLinux()) {
            deviceProviders.add(new PulseAudioDeviceProvider(taskOrchestrator));
        }
        // fallbacks, first port audio (also libjitsi wrapped), then java audio
        deviceProviders.add(new PortAudioDeviceProvider(taskOrchestrator));
        deviceProviders.add(new JavaAudioDeviceProvider(taskOrchestrator));
    }

    @Override
    public List<AudioDevice> getSupportedDevices() {
        List<AudioDevice> devices = new ArrayList<>();
        for (DeviceProvider deviceProvider : deviceProviders) {
            devices.addAll(deviceProvider.getAudioDevices());
            if (!devices.isEmpty() && !DUMP_ALL_DEVICES) {
                break;
            }
        }
        return devices;
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

        this.audioDevice = audioDevice;
        this.audioDevice.setAudioListener(this::onDataAvailable);

        boolean started = audioDevice.start();
        if (!started) {
            logger.warn("Couldn't start selected audio device {}", audioDevice.getName());
            this.audioDevice.setAudioListener(null);
            this.audioDevice = null;
            return false;
        }

        this.beatInterpreter = new BeatInterpreter(config, AMPLITUDES_PER_SECOND);
        this.nextBeatThreshold = new TimeThreshold(TimeUnit.SECONDS.toMillis(1));
        this.audioFormat = audioDevice.getAudioFormat();

        int bytesPerSecond = (int) (audioFormat.sampleRate() * audioFormat.getBytesPerFrame());
        this.bytesPerChunk = bytesPerSecond / AMPLITUDES_PER_SECOND;
        this.samplesPerChunk = bytesPerChunk / audioFormat.getBytesPerFrame();

        // Initialize a buffer to hold unprocessed data between listener calls.
        // Its size is exactly one chunk, as it will only hold trailing data smaller than a chunk.
        this.remainderBuffer = ByteBuffer.allocate(bytesPerChunk);
        this.remainderBuffer.order(audioFormat.littleEndian() ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);

        // Start a health check to ensure the device remains open.
        healthCheckFuture = taskOrchestrator.schedulePeriodicTask(() -> {
            if (audioDevice.isOpen()) {
                return;
            }
            logger.error("Audio stream '{}' is no longer available. Stopping reader.", audioDevice.getName());
            stop();
        }, 1, 1, TimeUnit.SECONDS);

        logger.info("Now listening to audio input from device {} ({})", audioDevice.getName(), audioFormat);
        return true;
    }

    private synchronized void onDataAvailable(byte[] data, int length) {
        if (!isOpen()) {
            return;
        }

        // Create a read-only buffer for the new data to process it without copying everything.
        ByteBuffer newData = ByteBuffer.wrap(data, 0, length).order(remainderBuffer.order());

        byte[] chunkData = new byte[bytesPerChunk];
        BeatEvent beatEvent = null;

        // Process chunks as long as we have enough combined data (remainder and new data).
        while (remainderBuffer.position() + newData.remaining() >= bytesPerChunk) {
            int bytesFromRemainder = remainderBuffer.position();
            int bytesFromNewData = bytesPerChunk - bytesFromRemainder;

            // Read the old remainder first.
            if (bytesFromRemainder > 0) {
                remainderBuffer.flip();
                remainderBuffer.get(chunkData, 0, bytesFromRemainder);
                remainderBuffer.clear();
            }

            newData.get(chunkData, bytesFromRemainder, bytesFromNewData);

            ByteBuffer chunkByteBuffer = ByteBuffer.wrap(chunkData)
                    .order(audioFormat.littleEndian() ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);

            double[] normalizedAudioBuffer = new double[samplesPerChunk];
            for (int i = 0; i < normalizedAudioBuffer.length; i++) {
                int bytePosition = i * audioFormat.bytesPerSample();
                if (audioFormat.bytesPerSample() == 2) {
                    normalizedAudioBuffer[i] = chunkByteBuffer.getShort(bytePosition) / (double) Short.MAX_VALUE;
                } else {
                    normalizedAudioBuffer[i] = chunkByteBuffer.get(bytePosition) / (double) Byte.MAX_VALUE;
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

            var beatEventInner = beatInterpreter.interpretValue(rms >= MINIMUM_AMPLITUDE ? rms : 0d);
            if (beatEventInner != null) {
                beatEvent = beatEventInner;
            }
        }

        if (newData.hasRemaining()) {
            remainderBuffer.put(newData);
        }

        if (beatEvent != null) {
            notifyObservers(beatEvent);
        }
    }

    /**
     * Notifies registered observers about a detected beat event.
     * This is dispatched on the task orchestrator to avoid blocking the audio thread.
     */
    private void notifyObservers(final BeatEvent beatEvent) {
        taskOrchestrator.dispatch(() -> {
            if (beatEvent.isSilence()) {
                beatEventObservers.forEach(BeatObserver::silenceDetected);
            } else if (beatEvent.isNoBeat()) {
                beatEventObservers.forEach(BeatObserver::noBeatReceived);
            } else if (nextBeatThreshold.isMet()) {
                nextBeatThreshold.setCurrentThreshold(config.getInt(ConfigNode.BEAT_MIN_TIME_BETWEEN));
                beatEventObservers.forEach(toNotify -> toNotify.beatReceived(beatEvent));
            } else {
                logger.info("Beat received, but it was skipped due to BEAT_MIN_TIME_BETWEEN");
            }
        });
    }

    /**
     * Applies a low-pass filter using FFT, cutting off frequencies above BASS_CUTOFF_HZ.
     */
    private void lowPassFilter(double[] normalizedSampleArray, LBAudioFormat format) {
        int sampleCount = normalizedSampleArray.length;
        if (sampleCount == 0) return;

        DoubleFFT_1D fft = new DoubleFFT_1D(sampleCount);
        fft.realForward(normalizedSampleArray);

        double freqPerBin = format.sampleRate() / sampleCount;
        int cutoffBin = (int) (BASS_CUTOFF_HZ / freqPerBin);

        // Zero out high-frequency components
        for (int i = cutoffBin * 2; i < sampleCount; i++) {
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
        if (audioDevice == null) {
            return;
        }

        if (healthCheckFuture != null) {
            healthCheckFuture.cancel(true);
            healthCheckFuture = null;
        }

        BeatObserver.StopStatus status = audioDevice.isOpen() ? BeatObserver.StopStatus.USER : BeatObserver.StopStatus.ERROR;

        audioDevice.setAudioListener(null);
        audioDevice.stop();
        audioDevice = null;
        remainderBuffer = null;

        // Dispatch the final notification to observers to ensure thread safety
        taskOrchestrator.dispatch(() -> {
            beatEventObservers.forEach(beatObserver -> beatObserver.audioReaderStopped(status));
            beatEventObservers.clear();
        });
        logger.info("No longer listening to audio input");
    }

    @Override
    public void registerBeatObserver(BeatObserver beatObserver) {
        beatEventObservers.add(beatObserver);
    }
}
