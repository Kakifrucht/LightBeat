package io.lightbeat.hue.light;

import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;
import io.lightbeat.hue.light.effect.LightEffect;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Class encapsulating a {@link PHLight}. Update it's state by getting the current builder
 * with {@link #getStateBuilder()} and send it via {@link #sendUpdate()}.
 * It includes strobe control that can be accessed via {@link #doStrobe(LightEffect, long, int)}.
 */
public class Light {

    private final PHLight light;
    private final LightQueue lightQueue;
    private final ScheduledExecutorService executorService;

    private volatile LightStateBuilder currentBuilder;

    private LightEffect strobeController;

    private volatile LightStateBuilder builderToCopyAfterTurningOn;
    private volatile boolean isOn;
    private volatile ScheduledFuture currentStrobe;


    public Light(PHLight light, LightQueue lightQueue, ScheduledExecutorService executorService) {
        this.lightQueue = lightQueue;
        this.light = light;
        this.executorService = executorService;

        this.currentBuilder = LightStateBuilder.create();
        this.builderToCopyAfterTurningOn = LightStateBuilder.create();

        this.isOn = light.getLastKnownLightState().isOn();
    }

    public PHLight getLight() {
        return light;
    }

    public PHLightState getLastKnownLightState() {
        return light.getLastKnownLightState();
    }

    /**
     * Get the lights state builder. If light is currently exempt this method return a builder that will
     * be applied once the exemption has been lifted.
     *
     * @return builder
     */
    public LightStateBuilder getStateBuilder() {
        return isOn ? currentBuilder : builderToCopyAfterTurningOn;
    }

    /**
     * Turn this light on or off.
     *
     * @param on whether to turn the light on or off
     */
    public void setOn(boolean on) {

        if (currentStrobe != null) {
            currentStrobe.cancel(false);
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

    public boolean isOn() {
        return isOn;
    }

    public boolean canDoStrobe(LightEffect effect) {
        return strobeController == null || strobeController.equals(effect);
    }

    public boolean setStrobeController(LightEffect strobeController) {
        if (canDoStrobe(strobeController)) {
            this.strobeController = strobeController;
            return true;
        }

        return false;
    }

    public void unsetStrobeController(LightEffect cancellingEffect) {
        if (cancellingEffect.equals(strobeController)) {
            strobeController = null;
            interruptStrobe();

            if (!isOn) {
                setOn(true);
            }
        }
    }

    public void doStrobe(LightEffect strobeController, long timeSinceLastBeat) {
        doStrobe(strobeController, timeSinceLastBeat, -1);
    }

    public void doStrobe(LightEffect strobeController, long timeSinceLastBeat, int brightness) {

        if (!setStrobeController(strobeController)) {
            return;
        }

        interruptStrobe();

        // strobe on beat, at least for 250 ms and at max for 500 ms
        long strobeDelay = timeSinceLastBeat;
        while (strobeDelay > 500L) {
            strobeDelay /= 2;
        }
        strobeDelay = Math.max(strobeDelay, 250L);

        boolean onAfterStrobe = isOn;
        setOn(!onAfterStrobe);

        currentStrobe = executorService.schedule(() -> {

            setOn(onAfterStrobe);
            if (onAfterStrobe && brightness >= 0) {
                getStateBuilder().setBrightness(brightness);
            }

            sendUpdate();

            if (onAfterStrobe) {
                this.strobeController = null;
            }

            currentStrobe = null;

        }, strobeDelay, TimeUnit.MILLISECONDS);
    }

    private void interruptStrobe() {
        if (currentStrobe != null) {

            currentStrobe.cancel(false);
            if (!currentStrobe.isDone()) {
                setOn(!isOn);
            }
        }
    }

    void sendUpdate() {
        PHLightState lightState = currentBuilder.getLightState();
        if (lightState != null) {
            lightQueue.addUpdate(light, lightState);
        }
        this.currentBuilder = LightStateBuilder.create();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Light && this.light.getUniqueId().equals(((Light) o).light.getUniqueId());
    }

    @Override
    public int hashCode() {
        return light.getUniqueId().hashCode();
    }
}
