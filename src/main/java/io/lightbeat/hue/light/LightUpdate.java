package io.lightbeat.hue.light;

import io.lightbeat.hue.light.color.ColorSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores the current beats light update information while passing through effects.
 * Every light gets a custom {@link LightStateBuilder} and lights can be removed from the current update.
 * The updates can then be applied via {@link #doLightUpdates()}.
 */
public class LightUpdate {

    private final List<Light> lights;
    private final List<Light> activeLights;
    private final ColorSet colorSet;

    private final int brightness;
    private final double brightnessPercentage;
    private final boolean doBrightnessChange;
    private final int transitionTime;
    private final long timeSinceLastBeat;


    LightUpdate(List<Light> lights, ColorSet colorSet,
                       BrightnessCalibrator.BrightnessData brightnessData, long timeSinceLastBeat) {

        this.lights = lights;
        this.activeLights = new ArrayList<>(lights);
        this.activeLights.removeIf(l -> !l.isOn());
        this.colorSet = colorSet;

        this.brightness = brightnessData.getBrightness();
        this.brightnessPercentage = brightnessData.getBrightnessPercentage();
        this.doBrightnessChange = brightnessData.isBrightnessChange();
        this.transitionTime = brightnessData.getTransitionTime();
        this.timeSinceLastBeat = timeSinceLastBeat;

        for (Light light : lights) {

            if (doBrightnessChange) {
                int transitionTime = this.transitionTime + ((brightnessData.getBrightnessDifference() < -60) ? 2 : 1);
                light.getStateBuilder().setTransitionTime(transitionTime)
                        .setBrightness(brightness);
            } else {
                light.getStateBuilder().setTransitionTime(transitionTime);
            }
        }
    }

    void doLightUpdates() {
        lights.forEach(Light::sendUpdate);
    }

    public List<Light> getLights() {
        return lights;
    }

    public List<Light> getLightsTurnedOn() {
        activeLights.removeIf(l -> !l.isOn());
        return activeLights;
    }

    public ColorSet getColorSet() {
        return colorSet;
    }

    public int getBrightness() {
        return brightness;
    }

    public double getBrightnessPercentage() {
        return brightnessPercentage;
    }

    public boolean isBrightnessChange() {
        return doBrightnessChange;
    }

    public int getTransitionTime() {
        return transitionTime;
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
}
