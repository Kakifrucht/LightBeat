package io.lightbeat.hue.visualizer.effect;

import io.lightbeat.ComponentHolder;
import io.lightbeat.hue.visualizer.LightUpdate;
import io.lightbeat.hue.bridge.color.ColorSet;

import java.util.Random;

/**
 * Abstract implementation of {@link LightEffect}.
 * Implementations must override {@link #execute(LightUpdate)} and can access the current
 * {@link LightUpdate} via field.
 */
abstract class AbstractEffect implements LightEffect {

    final ComponentHolder componentHolder;

    final ColorSet colorSet;
    final Random rnd = new Random();


    AbstractEffect(ComponentHolder componentHolder) {
        this.componentHolder = componentHolder;
        this.colorSet = componentHolder.getHueManager().getColorSet();
    }

    @Override
    public void beatReceived(LightUpdate lightUpdate) {
        execute(lightUpdate);
    }

    abstract void execute(LightUpdate lightUpdate);

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
