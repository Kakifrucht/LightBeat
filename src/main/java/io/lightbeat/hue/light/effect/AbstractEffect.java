package io.lightbeat.hue.light.effect;

import io.lightbeat.LightBeat;
import io.lightbeat.hue.light.LightUpdate;

import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Abstract implementation of {@link LightEffect}.
 * Implementations must override {@link #executeEffect()} and can access the current
 * {@link LightUpdate} via field.
 */
abstract class AbstractEffect implements LightEffect {

    final Random rnd = new Random();
    final ScheduledExecutorService executorService = LightBeat.getComponentHolder().getExecutorService();

    LightUpdate lightUpdate;


    @Override
    public void beatReceived(LightUpdate lightUpdate) {
        this.lightUpdate = lightUpdate;
        executeEffect();
    }

    abstract void executeEffect();

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
