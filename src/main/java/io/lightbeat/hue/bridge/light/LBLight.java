package io.lightbeat.hue.bridge.light;

import com.philips.lighting.model.PHLight;
import io.lightbeat.hue.bridge.LightQueue;
import io.lightbeat.hue.bridge.light.controller.BrightnessController;
import io.lightbeat.hue.bridge.light.controller.ColorController;
import io.lightbeat.hue.bridge.light.controller.StrobeController;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Default and thread safe {@link Light} implementation.
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


    public LBLight(PHLight light, LightQueue lightQueue, ScheduledExecutorService executorService, int fadeTime) {
        this.light = light;
        this.lightQueue = lightQueue;
        this.fadeTime = fadeTime;

        this.colorController = new ColorController(this);
        this.brightnessController = new BrightnessController(this);
        this.strobeController = new StrobeController(this, executorService);

        this.currentBuilder = LightStateBuilder.create();
        this.builderToCopyAfterTurningOn = LightStateBuilder.create();

        this.isOn = light.getLastKnownLightState().isOn();
    }

    @Override
    public PHLight getBase() {
        return light;
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
    public boolean isOn() {
        return isOn;
    }

    @Override
    public synchronized void setOn(boolean on) {

        if (strobeController.isStrobing()) {
            strobeController.cancelStrobe();
        }

        if (this.isOn == on) {
            return;
        }

        if (/* turned */ on) {
            currentBuilder.copyFromBuilder(builderToCopyAfterTurningOn);
            brightnessController.forceBrightnessUpdate();
        } else {
            builderToCopyAfterTurningOn = LightStateBuilder.create();
        }

        currentBuilder.setOn(on);
        this.isOn = on;
    }

    @Override
    public synchronized void doLightUpdate(boolean doFade) {

        strobeController.applyUpdates();
        colorController.applyUpdates();

        if (!currentBuilder.isDefault() || brightnessController.isBrightnessWasIncreased()) {
            brightnessController.applyUpdates();
            lightQueue.addUpdate(this, currentBuilder.getLightState());
        }

        if (doFade) {
            currentBuilder = LightStateBuilder.create().setTransitionTime(fadeTime);

            colorController.applyFadeUpdates();
            brightnessController.applyFadeUpdates();

            if (!currentBuilder.isDefault()) {
                lightQueue.addUpdate(this, currentBuilder.getLightState());
            }
        }

        this.currentBuilder = LightStateBuilder.create();
    }

    @Override
    public void recoverFromError(int errorCode) {
        if (errorCode == 201) { // error description: parameter, hue, is not modifiable. Device is set to off.
            if (isOn) {
                isOn = false;
            }
            setOn(true);
        }
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
