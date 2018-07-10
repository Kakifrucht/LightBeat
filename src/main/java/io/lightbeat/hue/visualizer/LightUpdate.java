package io.lightbeat.hue.visualizer;

import io.lightbeat.config.Config;
import io.lightbeat.config.ConfigNode;
import io.lightbeat.hue.bridge.light.Light;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores the current beats light update information while passing through effects.
 * Get the lights to update via {@link #getLights()} and {@link #getLightsTurnedOn()} to change
 * their settings. The updates can then be applied via {@link #doLightUpdates()}.
 */
public class LightUpdate {

    private final List<Light> lights;
    private final List<Light> lightsTurnedOn;
    private final List<Light> mainLights;

    private final int brightness;
    private final int brightnessFade;
    private final double brightnessPercentage;
    private final boolean doBrightnessChange;
    private final long timeSinceLastBeat;


    LightUpdate(Config config, List<Light> lights, BrightnessCalibrator.BrightnessData brightnessData, long timeSinceLastBeat) {

        this.lights = lights;
        this.lightsTurnedOn = new ArrayList<>(lights);
        this.lightsTurnedOn.removeIf(light -> !light.isOn());

        this.mainLights = new ArrayList<>();
        mainLights.add(lights.get(0));

        double randomThreshold = (double) config.getInt(ConfigNode.LIGHT_AMOUNT_PROBABILITY) / 10d;
        for (int i = 1; i < lights.size() && Math.random() < randomThreshold; i++) {
            mainLights.add(lights.get(i));
        }

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

    /**
     * @return list consisting of the main lights for this light update, will contain at least one light, or more
     */
    public List<Light> getMainLights() {
        return mainLights;
    }

    public List<Light> getLightsTurnedOn() {
        return lightsTurnedOn;
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
