package io.lightbeat.hue.light;

import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;
import io.lightbeat.hue.color.Color;

/**
 * Builder class to create {@link PHLightState}'s.
 * Can also copy from other builders via {@link #copyFromBuilder(LightStateBuilder)}.
 */
@SuppressWarnings("UnusedReturnValue")
public class LightStateBuilder {

    public static LightStateBuilder create() {
        return new LightStateBuilder();
    }

    private int transitionTime = 0;
    private int brightness = Integer.MIN_VALUE;
    private Color color = null;
    private Boolean setOn;
    private PHLight.PHLightAlertMode alert;


    private LightStateBuilder() {}

    public void copyFromBuilder(LightStateBuilder copyFrom) {

        if (copyFrom != null && (!copyFrom.isDefault() || copyFrom.transitionTime > 0)) {

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

    public LightStateBuilder addTransitionTime(int transitionTime) {
        this.transitionTime += transitionTime;
        return this;
    }

    public LightStateBuilder setAlertMode(PHLight.PHLightAlertMode alert) {
        this.alert = alert;
        return this;
    }

    public LightStateBuilder setColor(Color color) {
        this.color = color;
        return this;
    }

    public LightStateBuilder setBrightness(int brightness) {
        this.brightness = brightness;
        return this;
    }

    LightStateBuilder setOn(boolean setOn) {
        this.setOn = setOn;
        return this;
    }

    boolean isDefault() {
        return brightness == Integer.MIN_VALUE
                && color == null
                && setOn == null
                && alert == null;
    }

    PHLightState getLightState() {

        if (isDefault()) {
            return null;
        }

        PHLightState newLightState = new PHLightState();
        newLightState.setTransitionTime(transitionTime);

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
}
