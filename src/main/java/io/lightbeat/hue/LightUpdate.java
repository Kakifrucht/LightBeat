package io.lightbeat.hue;

import io.lightbeat.hue.color.Color;
import io.lightbeat.hue.effect.LightEffect;
import io.lightbeat.hue.light.Light;
import io.lightbeat.hue.light.LightStateBuilder;

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
    private final int brightnessLow;
    private final double brightnessPercentage;
    private final boolean doBrightnessChange;
    private final long timeSinceLastBeat;


    LightUpdate(List<Light> lights, BrightnessCalibrator.BrightnessData brightnessData, long timeSinceLastBeat) {

        this.lights = lights;
        this.activeLights = new ArrayList<>(lights);

        this.brightness = brightnessData.getBrightness();
        this.brightnessLow = brightnessData.getBrightnessLow();
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

    public int getBrightnessLow() {
        return brightnessLow;
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

    /**
     * Copies the state of the provided {@link LightStateBuilder} to all lights, even if they are exempt.
     *
     * @param builder to copy from
     */
    public void copyBuilderToAll(LightStateBuilder builder) {
        for (Light light : getLights()) {
            light.getStateBuilder().copyFromBuilder(builder);
        }
    }

    public void setFadeColorForAll(LightEffect effect, Color fadeColor) {
        for (Light light : getLights()) {
            light.getColorController().setFadeColor(effect, fadeColor);
        }
    }
}
