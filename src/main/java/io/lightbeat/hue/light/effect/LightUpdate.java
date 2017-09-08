package io.lightbeat.hue.light.effect;

import com.philips.lighting.model.PHLight;
import io.lightbeat.hue.light.BrightnessCalibrator;
import io.lightbeat.hue.light.LightStateBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores the current beats light update information while passing through effects.
 * Every light gets a custom {@link LightStateBuilder} and lights can be removed from the current update.
 * The updates can then be applied via {@link #doLightUpdates()}.
 */
public class LightUpdate {

    private final List<PHLight> lights;
    private final int brightness;
    private final double brightnessPercentage;
    private final boolean doBrightnessChange;
    private final int transitionTime;

    private final Map<PHLight, LightStateBuilder> builders;


    public LightUpdate(List<PHLight> lights, BrightnessCalibrator.BrightnessData brightnessData) {

        this.lights = lights;
        this.brightness = brightnessData.getBrightness();
        this.brightnessPercentage = brightnessData.getBrightnessPercentage();
        this.doBrightnessChange = brightnessData.isBrightnessChange();
        this.transitionTime = brightnessData.getTransitionTime();

        this.builders = new HashMap<>();
        for (PHLight light : lights) {
            LightStateBuilder newBuilder = LightStateBuilder.create()
                    .setTransitionTime(transitionTime);

            // turn lights on if turned off
            if (!light.getLastKnownLightState().isOn()) {
                newBuilder.setOn(true);
            }

            if (doBrightnessChange) {
                int transitionTime = this.transitionTime + ((brightnessData.getBrightnessDifference() < -60) ? 2 : 1);
                newBuilder.setTransitionTime(transitionTime)
                        .setBrightness(brightness);
            }

            builders.put(light, newBuilder);
        }
    }

    public void doLightUpdates() {
        for (PHLight light : lights) {
            builders.get(light).updateState(light);
        }
    }

    List<PHLight> getLights() {
        return lights;
    }

    int getBrightness() {
        return brightness;
    }

    double getBrightnessPercentage() {
        return brightnessPercentage;
    }

    boolean isBrightnessChange() {
        return doBrightnessChange;
    }

    int getTransitionTime() {
        return transitionTime;
    }

    LightStateBuilder getBuilder(PHLight light) {
        return builders.get(light);
    }

    /**
     * Copies the state of the provided {@link LightStateBuilder} to all lights in current update
     * that weren't removed via {@link #removeLightFromUpdate(PHLight)}.
     *
     * @param builder to copy from
     */
    void copyBuilderToAll(LightStateBuilder builder) {
        for (LightStateBuilder lightStateBuilder : builders.values()) {
            lightStateBuilder.copyFromBuilder(builder);
        }
    }

    void removeLightFromUpdate(PHLight toRemove) {
        lights.remove(toRemove);
        builders.remove(toRemove);
    }
}
