package io.lightbeat.hue.effect;

import io.lightbeat.ComponentHolder;
import io.lightbeat.LightBeat;
import io.lightbeat.hue.LightUpdate;
import io.lightbeat.hue.color.ColorSet;

import java.util.Random;

/**
 * Abstract implementation of {@link LightEffect}.
 * Implementations must override {@link #execute()} and can access the current
 * {@link LightUpdate} via field.
 */
abstract class AbstractEffect implements LightEffect {

    final ComponentHolder componentHolder;

    final ColorSet colorSet;
    final Random rnd = new Random();

    LightUpdate lightUpdate;


    AbstractEffect(ColorSet colorSet) {
        this.componentHolder = LightBeat.getComponentHolder();
        this.colorSet = colorSet;
    }

    @Override
    public void beatReceived(LightUpdate lightUpdate) {
        this.lightUpdate = lightUpdate;
        execute();
    }

    abstract void execute();

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
