package io.lightbeat.audio;

import io.lightbeat.config.Config;
import io.lightbeat.config.ConfigNode;
import org.jtransforms.fft.DoubleFFT_1D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Default {@link AudioReader} implementation, that also serves as an {@link BeatEventManager}.
 * Get a list of supported mixers with {@link #getSupportedMixers()}, which can be used to start
 * the scheduled beat detection thread via {@link #start(Mixer)}. Can filter out all frequencies
 * but bass frequency via FFT, if config option {@link ConfigNode#BEAT_BASS_ONLY_MODE} is set to
 * true. Classes can register to receive {@link BeatEvent BeatEvents} by calling
 * {@link #registerBeatObserver(BeatObserver)}, that are called whenever a beat was detected.
 */
public class LBAudioReader implements BeatEventManager, AudioReader {

    private static final double MINIMUM_AMPLITUDE = 0.005d;
    private static final int SAMPLE_SIZE = 256;
    private static final int FRAME_SIZE = SAMPLE_SIZE * 2;

    private static final Logger logger = LoggerFactory.getLogger(LBAudioReader.class);

    private final Config config;
    private final ScheduledExecutorService executorService;
    private final AudioFormat format = new AudioFormat(44100f, 16, 1, true, true);
    private final Line.Info lineInfo = new Line.Info(TargetDataLine.class);
    private final List<BeatObserver> beatEventObservers = new ArrayList<>();

    private volatile TargetDataLine dataLine;
    private BeatInterpreter beatInterpreter;
    private ScheduledFuture<?> future;


    public LBAudioReader(Config config, ScheduledExecutorService executorService) {
        this.config = config;
        this.executorService = executorService;
    }

    @Override
    public List<Mixer> getSupportedMixers() {
        List<Mixer> list = new ArrayList<>();

        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            Mixer mixer = AudioSystem.getMixer(info);
            if (mixer.isLineSupported(lineInfo)) {
                list.add(mixer);
            }
        }

        return list;
    }

    @Override
    public Mixer getMixerByName(String name) {
        return getSupportedMixers().stream()
                .filter(mixer -> mixer.getMixerInfo().getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean start(Mixer mixer) {

        if (isOpen()) {
            stop();
        }

        try {
            dataLine = (TargetDataLine) mixer.getLine(lineInfo);
            dataLine.open(format);
            dataLine.start();
        } catch (LineUnavailableException e) {
            dataLine = null;
            logger.warn("Could not start audio capture thread, as selected audio mixer is not supported", e);
            return false;
        }

        beatInterpreter = new BeatInterpreter(config);

        byte[] audioInputBuffer = new byte[FRAME_SIZE];
        future = executorService.scheduleWithFixedDelay(() -> {

            if (!isOpen()) {
                logger.error("Selected audio stream is no longer available");
                stop();
                return;
            }

            double highestAmplitude = -1d;
            while (dataLine.available() >= FRAME_SIZE && dataLine.read(audioInputBuffer, 0, FRAME_SIZE) > 0) {

                // convert to normalized values (2 bytes per sample)
                double[] normalizedAudioBuffer = new double[SAMPLE_SIZE];
                for (int i = 0, s = 0; s < FRAME_SIZE; i++) {
                    short sample = 0;

                    sample |= audioInputBuffer[s++] << 8;
                    sample |= audioInputBuffer[s++] & 0xFF;

                    normalizedAudioBuffer[i] = sample / (double) Short.MAX_VALUE;
                }

                // filter out all but low frequencies
                if (config.getBoolean(ConfigNode.BEAT_BASS_ONLY_MODE)) {

                    DoubleFFT_1D fft = new DoubleFFT_1D(SAMPLE_SIZE);
                    fft.realForward(normalizedAudioBuffer);

                    // filter frequencies
                    for (int i = 4; i < normalizedAudioBuffer.length; i++) {
                        normalizedAudioBuffer[i] = 0d;
                    }

                    fft.realInverse(normalizedAudioBuffer, true);
                }

                // calculate root mean square and use value as amplitude
                double averageMeanSquare = 0;
                for (double sample : normalizedAudioBuffer) {
                    averageMeanSquare += Math.pow(sample, 2d);
                }

                averageMeanSquare /= SAMPLE_SIZE;
                averageMeanSquare = Math.sqrt(averageMeanSquare);

                double amplitude = averageMeanSquare;
                if (amplitude < MINIMUM_AMPLITUDE) {
                    amplitude = 0d;
                }

                if (amplitude > highestAmplitude) {
                    highestAmplitude = amplitude;
                }
            }

            if (highestAmplitude >= 0d) {

                BeatEvent event = beatInterpreter.interpretValue(highestAmplitude);
                if (event == null) {
                    return;
                }

                if (event.isSilence()) {
                    beatEventObservers.forEach(BeatObserver::silenceDetected);
                } else if (event.isNoBeat()) {
                    beatEventObservers.forEach(BeatObserver::noBeatReceived);
                } else {
                    beatEventObservers.forEach(toNotify -> toNotify.beatReceived(event));
                }
            }
        }, 8L, 8L, TimeUnit.MILLISECONDS);

        logger.info("Now listening to audio input from mixer {}", mixer.getMixerInfo().getName());
        return true;
    }

    @Override
    public boolean isOpen() {
        return dataLine != null && dataLine.isOpen();
    }

    @Override
    public void stop() {
        if (dataLine != null && future != null) {

            future.cancel(true);

            for (BeatObserver beatEventObserver : beatEventObservers) {
                beatEventObserver.audioReaderStopped(dataLine.isRunning() ? BeatObserver.StopStatus.USER : BeatObserver.StopStatus.ERROR);
            }

            dataLine.stop();
            dataLine.close();
            dataLine = null;

            beatEventObservers.clear();
            logger.info("No longer listening to audio input");
        }
    }

    @Override
    public void registerBeatObserver(BeatObserver beatObserver) {
        beatEventObservers.add(beatObserver);
    }
}
