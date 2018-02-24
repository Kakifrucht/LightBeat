package io.lightbeat.audio;

import io.lightbeat.config.Config;
import org.jtransforms.fft.DoubleFFT_1D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Default {@link AudioReader} implementation, that also serves as an {@link BeatEventManager}.
 */
public class LBAudioReader implements BeatEventManager, AudioReader {

    private static final double MINIMUM_AMPLITUDE = 0.005d;

    private static final Logger logger = LoggerFactory.getLogger(LBAudioReader.class);

    private final Config config;
    private final ScheduledExecutorService executorService;
    private final AudioFormat format = new AudioFormat(44100f, 16, 1, true, true);
    private final Line.Info lineInfo = new Line.Info(TargetDataLine.class);
    private final Set<BeatObserver> beatEventObservers = new HashSet<>();

    private volatile TargetDataLine dataLine;
    private BeatInterpreter beatInterpreter;
    private ScheduledFuture future;


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

        future = executorService.scheduleWithFixedDelay(new Runnable() {

            private final int frameSize = 512;
            private final byte[] audioInputBuffer = new byte[frameSize];


            @Override
            public void run() {

                if (!isOpen()) {
                    logger.error("Selected audio stream is no longer available");
                    stop();
                    return;
                }

                double highestAmplitude = -1d;
                while (dataLine.available() >= frameSize && dataLine.read(audioInputBuffer, 0, frameSize) > 0) {

                    // convert to normalized values (2 bytes per sample)
                    double[] normalizedAudioBuffer = new double[frameSize / 2];
                    for (int i = 0, s = 0; s < frameSize; i++) {
                        short sample = 0;

                        sample |= audioInputBuffer[s++] << 8;
                        sample |= audioInputBuffer[s++] & 0xFF;

                        normalizedAudioBuffer[i] = sample / (double) Short.MAX_VALUE;
                    }

                    DoubleFFT_1D fft = new DoubleFFT_1D(frameSize / 2);
                    fft.realForward(normalizedAudioBuffer);

                    // filter frequencies
                    for (int i = 4; i < normalizedAudioBuffer.length; i++) {
                        normalizedAudioBuffer[i] = 0.0d;
                    }

                    // there is most likely a more efficient way than converting via fft -> removing values -> inverse fft -> rms
                    fft.realInverse(normalizedAudioBuffer, true);

                    // calculate root mean square and use value as amplitude
                    double average = 0d;
                    for (int i = 0; i < normalizedAudioBuffer.length; i += 2) {
                        average += normalizedAudioBuffer[i];
                    }
                    average /= normalizedAudioBuffer.length;

                    double averageMeanSquare = 0;
                    for (int i = 0; i < normalizedAudioBuffer.length; i += 2) {
                        averageMeanSquare += Math.pow(normalizedAudioBuffer[i] - average, 2d);
                    }
                    averageMeanSquare /= normalizedAudioBuffer.length;
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
                    if (event != null) {
                        if (event.getAverage() == 0.0d) {
                            beatEventObservers.forEach(BeatObserver::silenceDetected);
                        } else if (event.getTriggeringAmplitude() == 0.0d) {
                            beatEventObservers.forEach(BeatObserver::noBeatReceived);
                        } else {
                            beatEventObservers.forEach(toNotify -> toNotify.beatReceived(event));
                        }
                    }
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
