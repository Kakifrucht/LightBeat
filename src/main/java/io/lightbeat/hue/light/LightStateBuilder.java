package io.lightbeat.hue.light;

import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;
import io.lightbeat.hue.light.color.Color;
import io.lightbeat.hue.light.color.ColorSet;

/**
 * Builder class to create {@link PHLightState}'s.
 * Can also copy from other builders via {@link #copyFromBuilder(LightStateBuilder)}.
 */
@SuppressWarnings("UnusedReturnValue")
public class LightStateBuilder {

    public static LightStateBuilder create() {
        return new LightStateBuilder();
    }

    private int transitionTime = Integer.MIN_VALUE;
    private int brightness = Integer.MIN_VALUE;
    private Color color = null;
    private Boolean setOn;
    private PHLight.PHLightAlertMode alert;


    private LightStateBuilder() {}

    public LightStateBuilder setTransitionTime(int transitionTime) {
        this.transitionTime = transitionTime;
        return this;
    }

    public LightStateBuilder setBrightness(int brightness) {
        this.brightness = brightness;
        return this;
    }

    public LightStateBuilder setColor(Color color) {
        this.color = color;
        return this;
    }

    public LightStateBuilder setRandomHue(ColorSet colorSet) {
        color = colorSet.getNextColor();
        return this;
    }

    public LightStateBuilder setAlertMode(PHLight.PHLightAlertMode alert) {
        this.alert = alert;
        return this;
    }

    LightStateBuilder setOn(boolean setOn) {
        this.setOn = setOn;
        return this;
    }

    void copyFromBuilder(LightStateBuilder copyFrom) {

        if (copyFrom != null && (!copyFrom.isDefault() || copyFrom.transitionTime != Integer.MIN_VALUE)) {

            if (copyFrom.transitionTime != Integer.MIN_VALUE) {
                this.transitionTime = copyFrom.transitionTime;
            }

            if (copyFrom.brightness != Integer.MIN_VALUE) {
                this.brightness = copyFrom.brightness;
            }

            if (copyFrom.color != null) {
                this.color = copyFrom.color;
            }

            if (copyFrom.setOn != null) {
                this.setOn = copyFrom.setOn;
            }

            if (copyFrom.alert != null) {
                this.alert = copyFrom.alert;
            }
        }
    }

    PHLightState getLightState() {

        if (isDefault()) {
            return null;
        }

        PHLightState newLightState = new PHLightState();
        newLightState.setTransitionTime(transitionTime != Integer.MIN_VALUE ? transitionTime : 0);

        if (brightness >= 0) {
            newLightState.setBrightness(brightness);
        }

        if (color != null) {
            newLightState.setHue((int) (color.getHue() * 65535));
            newLightState.setSaturation((int) (color.getSaturation() * 254));
        }

        if (setOn != null) {
            newLightState.setOn(setOn);
        }

        if (alert != null) {
            newLightState.setAlertMode(alert);
        }

        return newLightState;
    }

    private boolean isDefault() {
        return brightness == Integer.MIN_VALUE
                && color == null
                && setOn == null
                && alert == null;
    }
}
