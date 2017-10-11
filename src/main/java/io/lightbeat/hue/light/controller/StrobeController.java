package io.lightbeat.hue.light.controller;

import com.philips.lighting.model.PHLightState;
import io.lightbeat.hue.effect.LightEffect;
import io.lightbeat.hue.light.Light;
import io.lightbeat.hue.light.LightStateBuilder;

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
    private volatile Boolean setOn;


    public StrobeController(Light currentLight, ScheduledExecutorService executorService) {
        super(currentLight);
        this.executorService = executorService;
    }

    @Override
    public void unsetControllingEffect(LightEffect effect) {
        if (canControl(effect)) {
            super.unsetControllingEffect(effect);
            interruptStrobe();

            if (!controlledLight.isOn()) {
                this.setOn = true;
            }
        }
    }

    @Override
    public void applyFadeUpdatesExecute(LightStateBuilder stateBuilder, PHLightState lastUpdate) {}

    public void applyUpdates() {

        if (setOn != null) {
            controlledLight.setOn(setOn);
            setOn = null;
        }

        if (strobeDelay != null) {

            // strobe on beat, at least for 250 ms and at max for 500 ms
            while (strobeDelay > 500L) {
                strobeDelay /= 2;
            }
            strobeDelay = Math.max(strobeDelay, 250L);

            boolean onAfterStrobe = controlledLight.isOn();
            controlledLight.setOn(!onAfterStrobe);

            currentStrobe = executorService.schedule(() -> {

                controlledLight.setOn(onAfterStrobe);
                controlledLight.doLightUpdate();

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

    public void setOn(boolean setOn) {
        if (this.setOn != null && setOn != this.setOn) {
            this.setOn = null;
        } else {
            this.setOn = setOn;
        }
    }

    /**
     * Interrupts the current strobe while resetting it's state if the strobe wasn't done yet after cancelling.
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
