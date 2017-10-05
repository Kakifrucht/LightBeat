package io.lightbeat.hue;

import io.lightbeat.audio.BeatEvent;
import io.lightbeat.audio.BeatObserver;
import io.lightbeat.config.Config;
import io.lightbeat.config.ConfigNode;
import io.lightbeat.hue.bridge.HueManager;
import io.lightbeat.hue.effect.*;
import io.lightbeat.hue.color.ColorSet;
import io.lightbeat.util.DoubleAverageBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Receives {@link BeatEvent}'s dispatched by the audio module.
 * Determines brightness changes and passes the data through it's
 * effect pipe, which will then update selected lights accordingly.
 */
public class HueBeatObserver implements BeatObserver {

    private static final Logger logger = LoggerFactory.getLogger(HueBeatObserver.class);

    private final HueManager hueManager;
    private final BrightnessCalibrator brightnessCalibrator;
    private final List<LightEffect> effectPipe;

    private final DoubleAverageBuffer amplitudeHistory = new DoubleAverageBuffer(75, false);
    private long lastBeatTimeStamp = System.currentTimeMillis();


    public HueBeatObserver(HueManager hueManager, Config config) {
        this.hueManager = hueManager;
        this.brightnessCalibrator = new BrightnessCalibrator(config);

        // effects at the end of pipe have highest priority
        ColorSet colorSet = hueManager.getColorSet();
        effectPipe = new ArrayList<>();
        effectPipe.add(new DefaultEffect(colorSet));
        if (config.getBoolean(ConfigNode.BRIGHTNESS_GLOW)) {
            effectPipe.add(new AlertEffect(colorSet,0.8f, 0.4f, 0.05f));
        }

        effectPipe.add(new ColorFadeEffect(colorSet,0.5f, 0.15f));
        effectPipe.add(new ColorFlipEffect(colorSet,0.4f, 0.125f));
        effectPipe.add(new ColorChainEffect(colorSet,0.5f, 0.1f));

        if (config.getBoolean(ConfigNode.BRIGHTNESS_STROBE)) {
            effectPipe.add(new StrobeEffect(colorSet,0.95f, 0.5f, 0.02f));
            effectPipe.add(new StrobeChainEffect(colorSet,0.7f, 0.15f));
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
        brightnessCalibrator.clear();
        amplitudeHistory.clear();
    }

    private void passDataToEffectPipe(BrightnessCalibrator.BrightnessData data, boolean receivedBeat) {
        LightUpdate lightUpdate = new LightUpdate(hueManager.getSelectedLights(), data, getTimeSinceLastBeat());
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
