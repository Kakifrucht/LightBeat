package io.lightbeat.hue.light;

import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;
import io.lightbeat.hue.bridge.LightQueue;
import io.lightbeat.hue.light.controller.BrightnessController;
import io.lightbeat.hue.light.controller.ColorController;
import io.lightbeat.hue.light.controller.StrobeController;

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
    public PHLightState getLastKnownLightState() {
        return light.getLastKnownLightState();
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
        if (brightnessController.isBrightnessWasIncreased()) {
            brightnessController.applyUpdates();
        }

        PHLightState lastLightStateUpdate = null;
        if (!currentBuilder.isDefault()) {

            // brightness updates only need to be applied if color changed/strobing and if it wasn't yet applied
            if (!brightnessController.isBrightnessWasIncreased()) {
                brightnessController.applyUpdates();
            }

            lastLightStateUpdate = currentBuilder.getLightState();
            lightQueue.addUpdate(light, currentBuilder.getLightState());
        }

        this.currentBuilder = LightStateBuilder.create();

        if (doFade) {
            LightStateBuilder fadeBuilder = LightStateBuilder.create().setTransitionTime(fadeTime);

            colorController.applyFadeUpdates(fadeBuilder, lastLightStateUpdate);
            brightnessController.applyFadeUpdates(fadeBuilder, lastLightStateUpdate);

            if (!fadeBuilder.isDefault()) {
                lightQueue.addUpdate(light, fadeBuilder.getLightState());
            }
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
