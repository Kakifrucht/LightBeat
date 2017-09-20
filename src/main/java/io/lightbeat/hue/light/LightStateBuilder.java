package io.lightbeat.hue.light;

import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;
import io.lightbeat.LightBeat;
import io.lightbeat.hue.light.color.Color;
import io.lightbeat.hue.light.color.ColorSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builder class to create {@link PHLightState}'s. Also used to send built states to the {@link LightQueue}.
 */
@SuppressWarnings("UnusedReturnValue")
public class LightStateBuilder {

    private static final Logger logger = LoggerFactory.getLogger(LightStateBuilder.class);

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

    public LightStateBuilder setOn(boolean setOn) {
        this.setOn = setOn;
        return this;
    }

    public LightStateBuilder setAlertMode(PHLight.PHLightAlertMode alert) {
        this.alert = alert;
        return this;
    }

    public void updateState(PHLight lightToUpdate) {
        PHLightState newState = getLightState();
        if (newState != null) {
            LightBeat.getComponentHolder().getHueManager().getQueue().addUpdate(lightToUpdate, newState);

            String mode = newState.getAlertMode().equals(PHLight.PHLightAlertMode.ALERT_UNKNOWN) ? "null" : newState.getAlertMode().toString();
            logger.info("Updated light {} to bri {} | color {}/{} | mode {} | on {}",
                    lightToUpdate.getName(), newState.getBrightness(), newState.getHue(),
                    newState.getSaturation(), mode, newState.isOn()
            );
        }
    }

    public void copyFromBuilder(LightStateBuilder copyFrom) {

        // is default passes if transition time is set to 0, we still want to copy it in that case
        if (copyFrom.isDefault() || copyFrom.transitionTime == 0) {
            return;
        }

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

    private PHLightState getLightState() {

        // don't update if not necessary
        if (isDefault()) {
            return null;
        }

        PHLightState newLightState = new PHLightState();
        newLightState.setTransitionTime(transitionTime != Integer.MIN_VALUE ? transitionTime : 0);

        if (brightness >= 0) {
            newLightState.setBrightness(brightness);
        }

        if (color != null) {
            newLightState.setHue(color.getHue());
            newLightState.setSaturation(color.getSaturation());
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
        return transitionTime <= 0
                && brightness == Integer.MIN_VALUE
                && color == null
                && setOn == null
                && alert == null;
    }
}
