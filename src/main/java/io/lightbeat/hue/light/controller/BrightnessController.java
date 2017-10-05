package io.lightbeat.hue.light.controller;

import io.lightbeat.hue.light.Light;

/**
 * Controls the lights brightness and fade brightness.
 */
public class BrightnessController extends AbstractController {

    private volatile int brightness;
    private volatile int fadeBrightness;

    private volatile boolean brightnessWasUpdated;


    public BrightnessController(Light lightToControl) {
        super(lightToControl);
    }

    public void applyUpdates() {
        lightToControl.getStateBuilder().setBrightness(brightness);
        if (brightnessWasUpdated) {
            brightnessWasUpdated = false;
        }
    }

    public boolean isBrightnessWasUpdated() {
        return brightnessWasUpdated;
    }

    /**
     * Defines this {@link Light}s current max and fade brightness.
     *
     * @param brightness current brightness on beat
     * @param fadeBrightness brightness used for fading effect
     */
    public void setBrightness(int brightness, int fadeBrightness) {
        brightnessWasUpdated = brightness != this.brightness;
        this.brightness = brightness;
        this.fadeBrightness = fadeBrightness;
    }

    public int getBrightness() {
        return brightness;
    }

    public int getFadeBrightness() {
        return fadeBrightness;
    }
}
