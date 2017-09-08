package io.lightbeat.hue.light;

import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.lightbeat.LightBeat;

import java.util.Random;

/**
 * Builder class to create {@link PHLightState}'s. Also used to send built states to the {@link LightQueue}.
 */
public class LightStateBuilder {

    private final static int MAX_HUE = 65535;

    private final static Logger logger = LoggerFactory.getLogger(LightStateBuilder.class);

    public static LightStateBuilder create() {
        return new LightStateBuilder();
    }


    private int transitionTime = Integer.MIN_VALUE;
    private int brightness = Integer.MIN_VALUE;
    private int hue = Integer.MIN_VALUE;
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

    public LightStateBuilder setHue(int hue) {
        this.hue = hue;
        return this;
    }

    public LightStateBuilder setRandomHue() {
        hue = new Random().nextInt(MAX_HUE);
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
            logger.info("Updated light {} to bri {} | hue {}/{} | mode {} | on {}",
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

        if (copyFrom.hue != Integer.MIN_VALUE) {
            this.hue = copyFrom.hue;
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

        if (hue != Integer.MIN_VALUE) {
            newLightState.setSaturation(254);
            newLightState.setHue(hue);
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
                && hue == Integer.MIN_VALUE
                && setOn == null
                && alert == null;
    }
}
