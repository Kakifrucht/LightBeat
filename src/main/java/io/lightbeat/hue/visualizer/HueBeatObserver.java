package io.lightbeat.hue.visualizer;

import io.lightbeat.ComponentHolder;
import io.lightbeat.audio.BeatEvent;
import io.lightbeat.audio.BeatObserver;
import io.lightbeat.config.Config;
import io.lightbeat.config.ConfigNode;
import io.lightbeat.hue.bridge.color.ColorSet;
import io.lightbeat.hue.bridge.light.Light;
import io.lightbeat.hue.visualizer.effect.*;
import io.lightbeat.util.DoubleAverageBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Receives {@link BeatEvent}'s dispatched by the audio module.
 * Determines brightness changes and passes the data through its
 * effect pipe, which will then update selected lights accordingly.
 */
public class HueBeatObserver implements BeatObserver {

    private static final Logger logger = LoggerFactory.getLogger(HueBeatObserver.class);
    private static final int AMPLITUDE_HISTORY_SIZE = 75;

    private final ComponentHolder componentHolder;
    private final List<Light> selectedLights;
    private final List<LightEffect> effectPipe;

    private final BrightnessCalibrator brightnessCalibrator;
    private final TransitionTimeCalibrator transitionTimeCalibrator;

    private final DoubleAverageBuffer amplitudeHistory = new DoubleAverageBuffer(AMPLITUDE_HISTORY_SIZE, false);
    private long lastBeatTimeStamp = System.currentTimeMillis();


    public HueBeatObserver(ComponentHolder componentHolder, List<Light> selectedLights) {

        this.componentHolder = componentHolder;
        this.selectedLights = selectedLights;

        Config config = componentHolder.getConfig();
        this.brightnessCalibrator = new BrightnessCalibrator(config);
        this.transitionTimeCalibrator = new TransitionTimeCalibrator(config);

        // effects at the end of pipe have the highest priority
        effectPipe = new ArrayList<>();
        effectPipe.add(new DefaultEffect(componentHolder));

        if (config.getBoolean(ConfigNode.EFFECT_ALERT)) {
            effectPipe.add(new AlertEffect(componentHolder, 0.8d, 0.4d, 0.05d));
        }

        if (config.getBoolean(ConfigNode.EFFECT_COLOR_STROBE)) {
            effectPipe.add(new ColorStrobeEffect(componentHolder, 0.8d, 0.15d));
        }

        effectPipe.add(new ColorFlipEffect(componentHolder, 0.7d, 0.15d));
        effectPipe.add(new ColorFadeEffect(componentHolder, 0.6d, 0.2d));
        effectPipe.add(new ColorChainEffect(componentHolder, 0.5d, 0.1d));

        if (config.getBoolean(ConfigNode.EFFECT_STROBE)) {
            effectPipe.add(new StrobeEffect(componentHolder, 0.95d, 0.4d, 0.02d));
            effectPipe.add(new StrobeChainEffect(componentHolder, 0.8d, 0.1d));
        }
    }

    @Override
    public void beatReceived(BeatEvent event) {

        amplitudeHistory.add(event.getTriggeringAmplitude());

        double amplitudeDifference = event.getTriggeringAmplitude() - amplitudeHistory.getCurrentAverage();
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

        componentHolder.getHueManager().recoverOriginalState();
    }

    private void passDataToEffectPipe(BrightnessCalibrator.BrightnessData data, boolean receivedBeat) {

        Collections.shuffle(selectedLights);

        ColorSet colorSet = componentHolder.getHueManager().getColorSet();
        long timeSinceLastBeat = getTimeSinceLastBeat();
        int transitionTime = transitionTimeCalibrator.getTransitionTime(timeSinceLastBeat);

        LightUpdate lightUpdate = new LightUpdate(
                componentHolder.getConfig(), selectedLights, colorSet, data, timeSinceLastBeat, transitionTime
        );

        try {
            effectPipe.forEach(effect -> {
                if (receivedBeat) {
                    effect.beatReceived(lightUpdate);
                } else {
                    effect.noBeatReceived(lightUpdate);
                }
            });
            lightUpdate.doLightUpdates();
        } catch (Exception e) {
            logger.error("Exception during light update effect loop", e);
        }
    }

    private long getTimeSinceLastBeat() {
        return System.currentTimeMillis() - lastBeatTimeStamp;
    }
}
