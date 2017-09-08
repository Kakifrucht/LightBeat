package io.lightbeat.hue.light;

import io.lightbeat.audio.BeatEvent;
import io.lightbeat.audio.BeatObserver;
import io.lightbeat.config.Config;
import io.lightbeat.hue.HueManager;
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

    private final DoubleAverageBuffer amplitudeHistory = new DoubleAverageBuffer(75, false);


    public HueBeatObserver(HueManager hueManager, Config config) {
        this.hueManager = hueManager;
        this.brightnessCalibrator = new BrightnessCalibrator(config);

        effectPipe = new ArrayList<>();
        // lower effects have highest priority
        effectPipe.add(new DefaultEffect());
        effectPipe.add(new AlertEffect());
        effectPipe.add(new SameColorEffect());
        effectPipe.add(new ColorFlipEffect());
        effectPipe.add(new StrobeEffect());
    }

    @Override
    public void beatReceived(BeatEvent event) {

        amplitudeHistory.add(event.getTriggeringAmplitude());

        double amplitudeDifference = event.getTriggeringAmplitude() - amplitudeHistory.getCurrentAverage();
        BrightnessCalibrator.BrightnessData data = brightnessCalibrator.getBrightness(amplitudeDifference);

        LightUpdate lightUpdate = new LightUpdate(hueManager.getLights(true), data);

        effectPipe.forEach(effect -> effect.beatReceived(lightUpdate));
        lightUpdate.doLightUpdates();
    }

    @Override
    public void noBeatReceived() {
        BrightnessCalibrator.BrightnessData data = brightnessCalibrator.getLowestBrightnessData();
        LightUpdate lightUpdate = new LightUpdate(hueManager.getLights(true), data);

        effectPipe.forEach(effect -> effect.noBeatReceived(lightUpdate));
        lightUpdate.doLightUpdates();
    }

    @Override
    public void silenceDetected() {
        noBeatReceived();
        brightnessCalibrator.clearHistory();
        amplitudeHistory.clear();
    }
}
