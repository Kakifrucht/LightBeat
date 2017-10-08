package io.lightbeat.hue.light.controller;

import io.lightbeat.hue.effect.LightEffect;
import io.lightbeat.hue.light.Light;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Controls the lights strobing ability.
 */
public class StrobeController extends AbstractController {

    private final ScheduledExecutorService executorService;
    private volatile ScheduledFuture currentStrobe;

    private volatile Long strobeDelay;


    public StrobeController(Light lightToControl, ScheduledExecutorService executorService) {
        super(lightToControl);
        this.executorService = executorService;
    }

    @Override
    public void unsetControllingEffect(LightEffect effect) {
        if (canControl(effect)) {
            super.unsetControllingEffect(effect);
            interruptStrobe();

            if (lightToControl.isOff()) {
                lightToControl.setOn(true);
            }
        }
    }

    public void applyUpdates() {
        if (strobeDelay != null) {

            // strobe on beat, at least for 250 ms and at max for 500 ms
            while (strobeDelay > 500L) {
                strobeDelay /= 2;
            }
            strobeDelay = Math.max(strobeDelay, 250L);

            boolean onAfterStrobe = !lightToControl.isOff();
            lightToControl.setOn(!onAfterStrobe);

            currentStrobe = executorService.schedule(() -> {

                lightToControl.setOn(onAfterStrobe);
                lightToControl.doLightUpdate();

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

        strobeDelay = timeSinceLastBeat;
    }

    public void cancelStrobe() {
        if (isStrobing()) {
            currentStrobe.cancel(false);
        }
    }

    /**
     * Interrupts the current strobe while resetting it's state if the strobe was still active.
     */
    private void interruptStrobe() {
        if (isStrobing()) {

            currentStrobe.cancel(false);
            if (!currentStrobe.isDone()) {
                lightToControl.setOn(lightToControl.isOff());
            }
        }
    }
}
