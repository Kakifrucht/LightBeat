package pw.wunderlich.lightbeat.hue.bridge.light.controller;

import pw.wunderlich.lightbeat.hue.bridge.light.Light;

/**
 * Controls the lights brightness and fade brightness.
 */
public class BrightnessController extends AbstractController {

    private volatile int brightness;
    private volatile int fadeBrightness;

    private volatile int lastSetBrightness;
    private volatile boolean brightnessWasIncreased;
    private volatile boolean doAlert;


    public BrightnessController(Light controlledLight) {
        super(controlledLight);
    }

    public void applyUpdates() {

        updateBrightness(brightness);
        if (doAlert) {
            controlledLight.getStateBuilder().setAlertMode(true);
            doAlert = false;
        }

        if (brightnessWasIncreased) {
            brightnessWasIncreased = false;
        }
    }

    public void applyFadeUpdates() {
        updateBrightness(fadeBrightness);
    }

    /**
     * Defines this {@link Light}'s current max and fade brightness.
     *
     * @param brightness current brightness on beat
     * @param fadeBrightness brightness used for fading effect
     */
    public void setBrightness(int brightness, int fadeBrightness) {
        brightnessWasIncreased = brightness > this.brightness;
        this.brightness = brightness;
        this.fadeBrightness = fadeBrightness;
    }

    public void setAlertMode() {
        brightnessWasIncreased = true;
        doAlert = true;
    }

    public void forceBrightnessUpdate() {
        brightnessWasIncreased = true;
        lastSetBrightness = -1;
    }

    public boolean isBrightnessWasIncreased() {
        return brightnessWasIncreased;
    }

    private void updateBrightness(int newBrightness) {

        if (newBrightness != lastSetBrightness) {
            controlledLight.getStateBuilder().setBrightness(newBrightness);
            lastSetBrightness = newBrightness;
        }
    }
}
