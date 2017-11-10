package io.lightbeat.hue.light.controller;

import com.philips.lighting.model.PHLightState;
import io.lightbeat.hue.light.Light;
import io.lightbeat.hue.light.LightStateBuilder;

/**
 * Controls the lights brightness and fade brightness.
 */
public class BrightnessController extends AbstractController {

    private volatile int brightness;
    private volatile int fadeBrightness;

    private volatile boolean brightnessWasUpdated;
    private volatile boolean brightnessWasIncreased;


    public BrightnessController(Light controlledLight) {
        super(controlledLight);
    }

    public void applyUpdates() {
        if (controlledLight.getLastKnownLightState().getBrightness() != brightness || brightnessWasIncreased) {

            controlledLight.getStateBuilder().setBrightness(brightness);
            if (brightnessWasIncreased) {
                brightnessWasIncreased = false;
            }
        }
    }

    @Override
    public void applyFadeUpdatesExecute(LightStateBuilder stateBuilder, PHLightState lastUpdate) {
        if (brightnessWasUpdated) {
            stateBuilder.setBrightness(fadeBrightness);
        } else if (lastUpdate != null) {
            // don't set if brightness is already at fade brightness level
            if (lastUpdate.getBrightness() != null && lastUpdate.getBrightness() != fadeBrightness) {
                stateBuilder.setBrightness(fadeBrightness);
            }
        }

        if (brightnessWasUpdated) {
            brightnessWasUpdated = false;
        }
    }

    /**
     * Defines this {@link Light}'s current max and fade brightness.
     *
     * @param brightness current brightness on beat
     * @param fadeBrightness brightness used for fading effect
     */
    public void setBrightness(int brightness, int fadeBrightness) {
        brightnessWasUpdated = brightness != this.brightness;
        brightnessWasIncreased = brightness > this.brightness;
        this.brightness = brightness;
        this.fadeBrightness = fadeBrightness;
    }

    public boolean isBrightnessWasIncreased() {
        return brightnessWasIncreased;
    }
}
