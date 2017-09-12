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

    private static final Logger logger = LoggerFactory.getLogger(LBAudioReader.class);

    private final Config config;
    private final ScheduledExecutorService executorService;
    private final AudioFormat format = new AudioFormat(44100f, 16, 1, true, true);
    private final Line.Info lineInfo = new Line.Info(TargetDataLine.class);
    private final Set<BeatObserver> beatEventObservers = new HashSet<>();

    private volatile TargetDataLine dataLine;
    private CaptureInterpreter captureInterpreter;
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

        if (dataLine != null && dataLine.isOpen()) {
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

        captureInterpreter = new CaptureInterpreter(config);

        if (future != null) {
            future.cancel(true);
        }


        future = executorService.scheduleAtFixedRate(new Runnable() {

            private final int frameSize = 512;
            private final byte[] audioInputBuffer = new byte[frameSize];


            @Override
            public void run() {

                while (dataLine.read(audioInputBuffer, 0, frameSize) > 0) {

                    // convert to normalized values (2 bytes per sample)
                    double[] complexAudioBuffer = new double[frameSize];
                    for (int i = 0, s = 0; s < frameSize; i++) {
                        short sample = 0;

                        sample |= audioInputBuffer[s++] << 8;
                        sample |= audioInputBuffer[s++] & 0xFF;

                        complexAudioBuffer[i] = sample / (double) Short.MAX_VALUE;
                    }

                    DoubleFFT_1D fft = new DoubleFFT_1D(frameSize / 2);
                    fft.realForwardFull(complexAudioBuffer);

                    for (int i = 4; i < complexAudioBuffer.length; i++) {
                        // remove frequencies above threshold
                        complexAudioBuffer[i] = 0.0d;
                    }

                    // there is most likely a more efficient way than converting via fft -> removing values -> inverse fft -> rms
                    fft.complexInverse(complexAudioBuffer, true);

                    // calculate root mean square and use value as amplitude
                    double sum = 0;
                    for (int i = 0; i < frameSize / 2; i++) {
                        sum += Math.pow(complexAudioBuffer[i * 2], 2);
                    }

                    sum /= frameSize;
                    double amplitude = Math.sqrt(sum);

                    if (amplitude < 0.005d) {
                        amplitude = 0d;
                    }

                    BeatEvent event = captureInterpreter.interpretValue(amplitude);
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

        return true;
    }

    @Override
    public void stop() {
        if (dataLine != null) {
            dataLine.stop();
            dataLine.close();
        }
    }

    @Override
    public void registerBeatObserver(BeatObserver beatObserver) {
        beatEventObservers.add(beatObserver);
    }

    @Override
    public void unregisterBeatObserver(BeatObserver beatObserver) {
        beatEventObservers.remove(beatObserver);
    }
}
