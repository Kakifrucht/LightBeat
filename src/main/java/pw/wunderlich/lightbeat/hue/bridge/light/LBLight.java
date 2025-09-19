package pw.wunderlich.lightbeat.hue.bridge.light;

import pw.wunderlich.lightbeat.hue.bridge.LightQueue;
import pw.wunderlich.lightbeat.hue.bridge.light.controller.BrightnessController;
import pw.wunderlich.lightbeat.hue.bridge.light.controller.ColorController;
import pw.wunderlich.lightbeat.hue.bridge.light.controller.StrobeController;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Default and thread safe {@link Light} implementation.
 */
public class LBLight implements Light {

    private final io.github.zeroone3010.yahueapi.Light light;
    private final LightQueue lightQueue;

    private final ColorController colorController;
    private final BrightnessController brightnessController;
    private final StrobeController strobeController;

    private volatile LightStateBuilder currentBuilder;
    private volatile LightStateBuilder builderToCopyAfterTurningOn;
    private volatile boolean isOn;


    public LBLight(io.github.zeroone3010.yahueapi.Light light, LightQueue lightQueue, ScheduledExecutorService executorService) {
        this.light = light;
        this.lightQueue = lightQueue;

        this.colorController = new ColorController(this);
        this.brightnessController = new BrightnessController(this);
        this.strobeController = new StrobeController(this, executorService);

        this.currentBuilder = LightStateBuilder.create();
        this.builderToCopyAfterTurningOn = LightStateBuilder.create();

        this.isOn = light.getState().getOn();
    }

    @Override
    public io.github.zeroone3010.yahueapi.Light getBase() {
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
    public synchronized void doLightUpdate(int transitionTime) {

        strobeController.applyUpdates();
        colorController.applyUpdates();

        if (!currentBuilder.isDefault() || brightnessController.isBrightnessWasIncreased()) {
            brightnessController.applyUpdates();
            lightQueue.addUpdate(this, currentBuilder.getLightState());
        }

        if (transitionTime > 0) {
            currentBuilder = LightStateBuilder.create().setTransitionTime(transitionTime);

            colorController.applyFadeUpdates();
            brightnessController.applyFadeUpdates();

            if (!currentBuilder.isDefault()) {
                lightQueue.addUpdate(this, currentBuilder.getLightState());
            }
        }

        this.currentBuilder = LightStateBuilder.create();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof LBLight && this.light.getId().equals(((LBLight) o).light.getId());
    }

    @Override
    public int hashCode() {
        return light.getId().hashCode();
    }
}
