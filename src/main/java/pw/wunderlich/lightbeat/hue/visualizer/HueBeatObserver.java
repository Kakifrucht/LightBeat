package pw.wunderlich.lightbeat.hue.visualizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pw.wunderlich.lightbeat.audio.BeatEvent;
import pw.wunderlich.lightbeat.audio.BeatObserver;
import pw.wunderlich.lightbeat.config.Config;
import pw.wunderlich.lightbeat.config.ConfigNode;
import pw.wunderlich.lightbeat.hue.bridge.color.ColorSet;
import pw.wunderlich.lightbeat.hue.bridge.color.CustomColorSet;
import pw.wunderlich.lightbeat.hue.bridge.color.RandomColorSet;
import pw.wunderlich.lightbeat.hue.bridge.light.Light;
import pw.wunderlich.lightbeat.hue.visualizer.effect.*;
import pw.wunderlich.lightbeat.util.DoubleAverageBuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Receives {@link BeatEvent}'s dispatched by the audio module.
 * Determines brightness changes and passes the data through its
 * effect pipe, which will then update selected lights accordingly.
 */
public class HueBeatObserver implements BeatObserver {

    private static final Logger logger = LoggerFactory.getLogger(HueBeatObserver.class);
    private static final int AMPLITUDE_HISTORY_SIZE = 75;

    private final Config config;
    private ColorSet colorSet;
    private String colorSetString = "";

    private final List<Light> lights;
    private final List<LightEffect> effectPipe;

    private final BrightnessCalibrator brightnessCalibrator;
    private final TransitionTimeCalibrator transitionTimeCalibrator;

    private final DoubleAverageBuffer amplitudeHistory = new DoubleAverageBuffer(AMPLITUDE_HISTORY_SIZE, false);

    private long lastBeatTimeStamp = System.currentTimeMillis();


    public HueBeatObserver(Config config, ScheduledExecutorService scheduledExecutorService, List<Light> lights) {

        this.config = config;
        this.lights = lights;
        this.lights.forEach(Light::storeState);

        this.brightnessCalibrator = new BrightnessCalibrator(config);
        this.transitionTimeCalibrator = new TransitionTimeCalibrator(config);

        // effects at the end of pipe have the highest priority
        effectPipe = new ArrayList<>();
        effectPipe.add(new DefaultEffect());

        if (config.getBoolean(ConfigNode.EFFECT_ALERT)) {
            effectPipe.add(new AlertEffect(0.8d, 0.4d, 0.05d));
        }

        if (config.getBoolean(ConfigNode.EFFECT_COLOR_STROBE)) {
            effectPipe.add(new ColorStrobeEffect(scheduledExecutorService, 0.8d, 0.15d));
        }

        effectPipe.add(new ColorFlipEffect(0.7d, 0.15d));
        effectPipe.add(new ColorFadeEffect(0.6d, 0.2d));
        effectPipe.add(new ColorChainEffect(0.5d, 0.1d));

        if (config.getBoolean(ConfigNode.EFFECT_STROBE)) {
            effectPipe.add(new StrobeEffect(0.95d, 0.4d, 0.02d));
            effectPipe.add(new StrobeChainEffect(0.8d, 0.1d));
        }
    }

    @Override
    public void beatReceived(BeatEvent event) {

        amplitudeHistory.add(event.triggeringAmplitude());

        double amplitudeDifference = event.triggeringAmplitude() - amplitudeHistory.getCurrentAverage();
        BrightnessCalibrator.BrightnessData data = brightnessCalibrator.getBrightness(amplitudeDifference);

        passDataToEffectPipe(data, true);
        lastBeatTimeStamp = System.currentTimeMillis();
    }

    @Override
    public void noBeatReceived() {
        passDataToEffectPipe(brightnessCalibrator.getLowestBrightnessData(), false);
    }

    @Override
    public void silenceDetected() {
        noBeatReceived();
        amplitudeHistory.clear();
    }

    @Override
    public void audioReaderStopped(StopStatus status) {
        // gracefully disable effects that may still be running scheduler threads
        noBeatReceived();
        lights.forEach(Light::restoreState);
    }

    private void passDataToEffectPipe(BrightnessCalibrator.BrightnessData data, boolean receivedBeat) {

        List<Light> shuffledLights = new ArrayList<>(lights);
        Collections.shuffle(shuffledLights);

        ColorSet colorSet = updateColorSet();
        long timeSinceLastBeat = System.currentTimeMillis() - lastBeatTimeStamp;
        int transitionTime = transitionTimeCalibrator.getTransitionTime(timeSinceLastBeat);

        LightUpdate lightUpdate = new LightUpdate(
                config, shuffledLights, colorSet, data, timeSinceLastBeat, transitionTime
        );

        try {
            effectPipe.forEach(effect -> {
                if (receivedBeat) {
                    effect.beatReceived(lightUpdate);
                } else {
                    effect.noBeatReceived(lightUpdate);
                }
            });
            lightUpdate.execute();
        } catch (Exception e) {
            logger.error("Exception during light update effect loop", e);
        }
    }

    private ColorSet updateColorSet() {
        String selectedColorSet = config.get(ConfigNode.COLOR_SET_SELECTED);
        if (!Objects.equals(this.colorSetString, selectedColorSet)) {
            this.colorSetString = selectedColorSet;
            if (selectedColorSet == null || selectedColorSet.equals("Random")) {
                colorSet = new RandomColorSet();
            } else {
                colorSet = new CustomColorSet(config, selectedColorSet);
            }
        }
        return colorSet;
    }
}
