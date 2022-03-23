package io.lightbeat.hue.bridge.light.controller;

import io.lightbeat.hue.visualizer.effect.LightEffect;
import io.lightbeat.hue.bridge.light.Light;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Controls the lights strobing ability.
 */
public class StrobeController extends AbstractController {

    private final ScheduledExecutorService executorService;

    private volatile ScheduledFuture<?> currentStrobe;

    private volatile Long strobeDelay;
    private volatile Boolean setOn;


    public StrobeController(Light currentLight, ScheduledExecutorService executorService) {
        super(currentLight);
        this.executorService = executorService;
    }

    @Override
    public boolean unsetControllingEffect(LightEffect effect) {

        boolean wasUnset = super.unsetControllingEffect(effect);

        if (wasUnset) {
            interruptStrobe();

            if (!controlledLight.isOn()) {
                this.setOn = true;
            }
        }

        return wasUnset;
    }

    public void applyUpdates() {

        if (setOn != null) {
            controlledLight.setOn(setOn);
            setOn = null;
        }

        if (strobeDelay != null) {

            boolean onAfterStrobe = controlledLight.isOn();
            controlledLight.setOn(!onAfterStrobe);

            currentStrobe = executorService.schedule(() -> {

                controlledLight.setOn(onAfterStrobe);
                controlledLight.doLightUpdate(1);

            }, strobeDelay, TimeUnit.MILLISECONDS);

            strobeDelay = null;
        }
    }

    public boolean isStrobing() {
        return currentStrobe != null && !currentStrobe.isDone();
    }

    /**
     * Mark this light to be strobed. Will only work if calling effect can control this light.
     *
     * @param effect calling light effect
     * @param timeSinceLastBeat time in milliseconds since the last beat was received, will be used
     *                          to determine for strobe duration
     */
    public void doStrobe(LightEffect effect, long timeSinceLastBeat) {

        if (!canControl(effect)) {
            return;
        }

        if (isStrobing()) {
            interruptStrobe();
        }

        long strobeDelay = timeSinceLastBeat;
        // strobe on beat, at least for 250 ms and at max for 500 ms
        while (strobeDelay > 500L) {
            strobeDelay /= 2;
        }

        this.strobeDelay = Math.max(strobeDelay, 250L);
    }

    public void cancelStrobe() {
        if (isStrobing()) {
            currentStrobe.cancel(false);
        }
    }

    public void setOn(boolean setOn) {
        if (this.setOn != null && setOn != this.setOn) {
            this.setOn = null;
        } else {
            this.setOn = setOn;
        }
    }

    /**
     * Interrupts the current strobe while resetting its state if the strobe wasn't done yet after cancelling.
     */
    private void interruptStrobe() {
        if (isStrobing()) {
            currentStrobe.cancel(false);
            if (!currentStrobe.isDone()) {
                controlledLight.setOn(!controlledLight.isOn());
            }
        }
    }
}
