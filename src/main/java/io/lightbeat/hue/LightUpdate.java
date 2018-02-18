package io.lightbeat.hue;

import io.lightbeat.hue.light.Light;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores the current beats light update information while passing through effects.
 * Get the lights to update via {@link #getLights()} and {@link #getLightsTurnedOn()} to change
 * their settings. The updates can then be applied via {@link #doLightUpdates()}.
 */
public class LightUpdate {

    private final List<Light> lights;
    private final List<Light> activeLights;

    private final int brightness;
    private final int brightnessFade;
    private final double brightnessPercentage;
    private final boolean doBrightnessChange;
    private final long timeSinceLastBeat;


    LightUpdate(List<Light> lights, BrightnessCalibrator.BrightnessData brightnessData, long timeSinceLastBeat) {

        this.lights = lights;
        this.activeLights = new ArrayList<>(lights);

        this.brightness = brightnessData.getBrightness();
        this.brightnessFade = brightnessData.getBrightnessFade();
        this.brightnessPercentage = brightnessData.getBrightnessPercentage();
        this.doBrightnessChange = brightnessData.isBrightnessChange();
        this.timeSinceLastBeat = timeSinceLastBeat;
    }

    void doLightUpdates() {
        for (Light light : lights) {
            light.doLightUpdate(true);
        }
    }

    public List<Light> getLights() {
        return lights;
    }

    public List<Light> getLightsTurnedOn() {
        activeLights.removeIf(light -> !light.isOn());
        return activeLights;
    }

    public int getBrightness() {
        return brightness;
    }

    public int getBrightnessFade() {
        return brightnessFade;
    }

    public double getBrightnessPercentage() {
        return brightnessPercentage;
    }

    public boolean isBrightnessChange() {
        return doBrightnessChange;
    }

    public long getTimeSinceLastBeat() {
        return timeSinceLastBeat;
    }
}
