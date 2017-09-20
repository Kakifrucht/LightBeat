package io.lightbeat.hue.light;

import io.lightbeat.audio.BeatEvent;
import io.lightbeat.audio.BeatObserver;
import io.lightbeat.config.Config;
import io.lightbeat.config.ConfigNode;
import io.lightbeat.hue.bridge.HueManager;
import io.lightbeat.hue.light.color.ColorSet;
import io.lightbeat.hue.light.effect.*;
import io.lightbeat.util.DoubleAverageBuffer;

import java.util.ArrayList;
import java.util.List;

/**
 * Receives {@link BeatEvent}'s dispatched by the audio module.
 * Determines brightness changes and passes the data through it's
 * effect pipe, which will then update selected lights accordingly.
 * Brightness is dependant on previously received music amplitudes.
 */
public class HueBeatObserver implements BeatObserver {

    private final HueManager hueManager;
    private final BrightnessCalibrator brightnessCalibrator;
    private final List<LightEffect> effectPipe;
    private final ColorSet colorSet;

    private final DoubleAverageBuffer amplitudeHistory = new DoubleAverageBuffer(75, false);
    private long lastBeatTimeStamp = System.currentTimeMillis();


    public HueBeatObserver(HueManager hueManager, Config config) {
        this.hueManager = hueManager;
        this.brightnessCalibrator = new BrightnessCalibrator(config);

        effectPipe = new ArrayList<>();
        // effects at the end of pipe have highest priority
        effectPipe.add(new DefaultEffect());
        effectPipe.add(new AlertEffect(0.7f, 0.4f, 0.05f));
        effectPipe.add(new SameColorEffect(0.5f, 0.2f));
        effectPipe.add(new ColorFlipEffect(0.6f, 0.25f));
        effectPipe.add(new ColorChainEffect(0.4f, 0.3f));
        if (config.getBoolean(ConfigNode.BRIGHTNESS_STROBE)) {
            effectPipe.add(new StrobeEffect(0.8f, 1.0f, 0.05f));
        }

        colorSet = hueManager.getColorSet();
    }

    @Override
    public void beatReceived(BeatEvent event) {

        amplitudeHistory.add(event.getTriggeringAmplitude());

        double amplitudeDifference = event.getTriggeringAmplitude() - amplitudeHistory.getCurrentAverage();
        BrightnessCalibrator.BrightnessData data = brightnessCalibrator.getBrightness(amplitudeDifference);

        LightUpdate lightUpdate = getNewLightUpdate(data);

        effectPipe.forEach(effect -> effect.beatReceived(lightUpdate));
        lightUpdate.doLightUpdates();

        lastBeatTimeStamp = System.currentTimeMillis();
    }

    @Override
    public void noBeatReceived() {
        LightUpdate lightUpdate = getNewLightUpdate(brightnessCalibrator.getLowestBrightnessData());

        effectPipe.forEach(effect -> effect.noBeatReceived(lightUpdate));
        lightUpdate.doLightUpdates();
    }

    @Override
    public void silenceDetected() {
        noBeatReceived();
        brightnessCalibrator.clearHistory();
        amplitudeHistory.clear();
    }

    private LightUpdate getNewLightUpdate(BrightnessCalibrator.BrightnessData data) {
        return new LightUpdate(hueManager.getLights(true), colorSet, data, getTimeSinceLastBeat());
    }

    private long getTimeSinceLastBeat() {
        return System.currentTimeMillis() - lastBeatTimeStamp;
    }
}
