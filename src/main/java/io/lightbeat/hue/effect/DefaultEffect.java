package io.lightbeat.hue.effect;

import io.lightbeat.hue.light.Light;
import io.lightbeat.hue.LightUpdate;
import io.lightbeat.hue.color.ColorSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Default effect that updates the color of a random amount of lights.
 */
public class DefaultEffect extends AbstractEffect {

    public DefaultEffect(ColorSet colorSet) {
        super(colorSet);
    }

    @Override
    void execute() {

        if (lightUpdate.isBrightnessChange()) {

            lightUpdate.setFadeColorForAll(this, colorSet.getNextColor());

            int newBrightness = lightUpdate.getBrightness();
            int newBrightnessLow = lightUpdate.getBrightnessLow();
            for (Light light : lightUpdate.getLights()) {
                light.getBrightnessController().setBrightness(newBrightness, newBrightnessLow);
            }
        }

        // select random light(s) to update
        List<Light> lights = lightUpdate.getLightsTurnedOn();
        if (lights.isEmpty()) {
            return;
        }

        List<Light> lightsToChange = new ArrayList<>();
        lightsToChange.add(lights.get(0));

        // randomly add more lights, depending on amount of lights in configuration
        int randomThreshold = Math.min(5, (int) Math.round(lights.size() * 0.7d));
        for (int i = 1; i < lights.size() && rnd.nextInt(10) < randomThreshold; i++) {
            lightsToChange.add(lights.get(i));
        }

        for (Light light : lightsToChange) {
            light.getColorController().setColor(this, colorSet.getNextColor());
        }
    }

    @Override
    public void noBeatReceived(LightUpdate lightUpdate) {

        for (Light light : lightUpdate.getLights()) {
            light.getBrightnessController().setBrightness(lightUpdate.getBrightness(), lightUpdate.getBrightnessLow());
            light.getStateBuilder().addTransitionTime(3);
        }
    }
}
