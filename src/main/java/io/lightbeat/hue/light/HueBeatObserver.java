package io.lightbeat.hue.light;

import io.lightbeat.audio.BeatEvent;
import io.lightbeat.audio.BeatObserver;
import io.lightbeat.config.Config;
import io.lightbeat.config.ConfigNode;
import io.lightbeat.hue.bridge.HueManager;
import io.lightbeat.hue.light.color.ColorSet;
import io.lightbeat.hue.light.effect.*;
import io.lightbeat.util.DoubleAverageBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Receives {@link BeatEvent}'s dispatched by the audio module.
 * Determines brightness changes and passes the data through it's
 * effect pipe, which will then update selected lights accordingly.
 * Brightness is dependant on previously received music amplitudes.
 */
public class HueBeatObserver implements BeatObserver {

    private static final Logger logger = LoggerFactory.getLogger(HueBeatObserver.class);

    private final HueManager hueManager;
    private final BrightnessCalibrator brightnessCalibrator;
    private final List<LightEffect> effectPipe;
    private final ColorSet colorSet;

    private final DoubleAverageBuffer amplitudeHistory = new DoubleAverageBuffer(75, false);
    private long lastBeatTimeStamp = System.currentTimeMillis();


    public HueBeatObserver(HueManager hueManager, Config config) {
        this.hueManager = hueManager;
        this.brightnessCalibrator = new BrightnessCalibrator(config);

        // effects at the end of pipe have highest priority
        effectPipe = new ArrayList<>();
        effectPipe.add(new DefaultEffect());
        if (config.getBoolean(ConfigNode.BRIGHTNESS_GLOW)) {
            effectPipe.add(new AlertEffect(0.8f, 0.4f, 0.05f));
        }

        effectPipe.add(new SameColorEffect(0.5f, 0.15f));
        effectPipe.add(new ColorFlipEffect(0.4f, 0.125f));
        effectPipe.add(new ColorChainEffect(0.6f, 0.1f));

        if (config.getBoolean(ConfigNode.BRIGHTNESS_STROBE)) {
            effectPipe.add(new StrobeEffect(0.9f, 1f, 0.05f));
            effectPipe.add(new StrobeChainEffect(0.7f, 0.15f));
        }

        colorSet = hueManager.getColorSet();
    }

    @Override
    public void beatReceived(BeatEvent event) {

        amplitudeHistory.add(event.getTriggeringAmplitude());

        double amplitudeDifference = event.getTriggeringAmplitude() - amplitudeHistory.getCurrentAverage();
        BrightnessCalibrator.BrightnessData data = brightnessCalibrator.getBrightness(amplitudeDifference);

        doLightUpdate(data, true);
        lastBeatTimeStamp = System.currentTimeMillis();
    }

    @Override
    public void noBeatReceived() {
        doLightUpdate(brightnessCalibrator.getLowestBrightnessData(), false);
    }

    @Override
    public void silenceDetected() {
        noBeatReceived();
        brightnessCalibrator.clear();
        amplitudeHistory.clear();
    }

    private void doLightUpdate(BrightnessCalibrator.BrightnessData data, boolean receivedBeat) {
        LightUpdate lightUpdate = new LightUpdate(hueManager.getSelectedLights(), colorSet, data, getTimeSinceLastBeat());
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
