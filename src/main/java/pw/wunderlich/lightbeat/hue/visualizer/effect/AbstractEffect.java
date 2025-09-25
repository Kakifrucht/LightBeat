package pw.wunderlich.lightbeat.hue.visualizer.effect;

import pw.wunderlich.lightbeat.hue.bridge.light.Light;
import pw.wunderlich.lightbeat.hue.visualizer.LightUpdate;

import java.util.Random;

/**
 * Abstract implementation of {@link LightEffect}.
 * Implementations must override {@link #execute(LightUpdate)} and can access the current
 * {@link LightUpdate} via field.
 */
abstract class AbstractEffect implements LightEffect {

    final Random rnd = new Random();


    @Override
    public void beatReceived(LightUpdate lightUpdate) {
        execute(lightUpdate);
    }

    abstract void execute(LightUpdate lightUpdate);

    void unsetControllingEffect(LightUpdate lightUpdate) {
        for (Light light : lightUpdate.getLights()) {
            light.getColorController().unsetControllingEffect(this);
            light.getStrobeController().unsetControllingEffect(this);
            light.getBrightnessController().unsetControllingEffect(this);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
