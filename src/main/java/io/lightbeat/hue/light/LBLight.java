package io.lightbeat.hue.light;

import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;
import io.lightbeat.hue.bridge.LightQueue;
import io.lightbeat.hue.light.controller.BrightnessController;
import io.lightbeat.hue.light.controller.ColorController;
import io.lightbeat.hue.light.controller.StrobeController;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Default {@link Light} implementation.
 */
public class LBLight implements Light {

    private final PHLight light;
    private final LightQueue lightQueue;
    private final int fadeTime;

    private final ColorController colorController;
    private final BrightnessController brightnessController;
    private final StrobeController strobeController;

    private volatile LightStateBuilder currentBuilder;
    private volatile LightStateBuilder builderToCopyAfterTurningOn;
    private volatile boolean isOn;

    private PHLightState lastLightStateUpdate;


    public LBLight(PHLight light, LightQueue lightQueue, ScheduledExecutorService executorService, int fadeTime) {
        this.lightQueue = lightQueue;
        this.light = light;
        this.fadeTime = fadeTime;

        this.colorController = new ColorController(this);
        this.brightnessController = new BrightnessController(this);
        this.strobeController = new StrobeController(this, executorService);

        this.currentBuilder = LightStateBuilder.create();
        this.builderToCopyAfterTurningOn = LightStateBuilder.create();

        this.isOn = light.getLastKnownLightState().isOn();
    }

    @Override
    public ColorController getColorController() {
        return colorController;
    }

    @Override
    public BrightnessController getBrightnessController() {
        return brightnessController;
    }

    @Override
    public StrobeController getStrobeController() {
        return strobeController;
    }

    @Override
    public LightStateBuilder getStateBuilder() {
        return isOn ? currentBuilder : builderToCopyAfterTurningOn;
    }

    @Override
    public void setOn(boolean on) {

        if (strobeController.isStrobing()) {
            strobeController.cancelStrobe();
        }

        if (this.isOn == on) {
            return;
        }

        if (on) {
            currentBuilder.copyFromBuilder(builderToCopyAfterTurningOn);
        } else {
            builderToCopyAfterTurningOn = LightStateBuilder.create();
        }

        currentBuilder.setOn(on);
        this.isOn = on;
    }

    @Override
    public boolean isOff() {
        return !isOn;
    }

    @Override
    public void doLightUpdate() {

        strobeController.applyUpdates();
        if (isOn) {
            colorController.applyUpdates();
        }

        // only set brightness if it was updated or other state stuff was updated
        boolean brightnessAlreadySet = false;
        if (brightnessController.isBrightnessWasUpdated()) {
            brightnessController.applyUpdates();
            brightnessAlreadySet = true;
        }

        if (!currentBuilder.isDefault()) {

            if (!brightnessAlreadySet
                    && brightnessController.getBrightness() != light.getLastKnownLightState().getBrightness()) {
                brightnessController.applyUpdates();
            }

            lastLightStateUpdate = currentBuilder.getLightState();
            lightQueue.addUpdate(light, currentBuilder.getLightState());
        }

        this.currentBuilder = LightStateBuilder.create();
    }

    @Override
    public void doLightUpdateFade() {

        if (isOn && !strobeController.isStrobing()) {

            LightStateBuilder fadeBuilder = LightStateBuilder.create();

            if (colorController.isFadeColorWasUpdated()
                    || (lastLightStateUpdate != null && lastLightStateUpdate.getHue() != null)) {
                fadeBuilder.setColor(colorController.getFadeColor());
            }

            if (lastLightStateUpdate != null) {

                Integer lastUpdateBrightness = lastLightStateUpdate.getBrightness();
                int fadeBrightness = brightnessController.getFadeBrightness();
                if (lastLightStateUpdate != null
                        && lastUpdateBrightness != null
                        && lastUpdateBrightness != fadeBrightness) {
                    fadeBuilder.setBrightness(fadeBrightness);
                }
            }


            if (!fadeBuilder.isDefault()) {
                lightQueue.addUpdate(light, fadeBuilder.addTransitionTime(fadeTime).getLightState());
            }
        }

        lastLightStateUpdate = null;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof LBLight && this.light.getUniqueId().equals(((LBLight) o).light.getUniqueId());
    }

    @Override
    public int hashCode() {
        return light.getUniqueId().hashCode();
    }
}
