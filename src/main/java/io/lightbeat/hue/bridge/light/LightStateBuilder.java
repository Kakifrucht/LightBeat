package io.lightbeat.hue.bridge.light;

import io.github.zeroone3010.yahueapi.AlertType;
import io.github.zeroone3010.yahueapi.State;
import io.lightbeat.hue.bridge.color.Color;

/**
 * Builder class to create {@link State}'s.
 * Can also copy from other builders via {@link #copyFromBuilder(LightStateBuilder)}.
 */
@SuppressWarnings("UnusedReturnValue")
public class LightStateBuilder {

    static LightStateBuilder create() {
        return new LightStateBuilder();
    }

    private volatile int transitionTime = 0;
    private volatile int brightness = Integer.MIN_VALUE;
    private volatile Color color = null;
    private volatile Boolean setOn;
    private volatile Boolean alert;


    private LightStateBuilder() {}

    void copyFromBuilder(LightStateBuilder copyFrom) {

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

    LightStateBuilder setTransitionTime(int transitionTime) {
        this.transitionTime = transitionTime;
        return this;
    }

    LightStateBuilder setOn(boolean setOn) {
        this.setOn = setOn;
        return this;
    }

    public LightStateBuilder setAlertMode(boolean doAlert) {
        this.alert = doAlert;
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

    boolean isDefault() {
        return brightness < 0
                && color == null
                && setOn == null
                && alert == null;
    }

    State getLightState() {

        if (isDefault()) {
            return null;
        }

        State.Builder newLightState = new State.Builder();
        newLightState.transitionTime(transitionTime);

        if (brightness >= 0) {
            newLightState.brightness(brightness);
        }

        if (color != null) {
            newLightState.hue((int) (color.getHue() * 65535));
            newLightState.saturation((int) (color.getSaturation() * 254));
        }

        if (setOn != null) {
            newLightState.on(setOn);
        }

        if (alert != null && alert) {
            return newLightState.alert(AlertType.SHORT_ALERT);
        } else {
            return newLightState.keepCurrentState();
        }
    }
}
