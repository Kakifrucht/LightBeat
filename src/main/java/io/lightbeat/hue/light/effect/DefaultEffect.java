package io.lightbeat.hue.light.effect;

import io.lightbeat.hue.light.Light;
import io.lightbeat.hue.light.LightStateBuilder;
import io.lightbeat.hue.light.LightUpdate;

import java.util.ArrayList;
import java.util.List;

/**
 * Default effect that updates the color of a random amount of lights.
 */
public class DefaultEffect extends AbstractEffect {

    @Override
    void executeEffect() {

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
            light.getStateBuilder().setRandomHue(lightUpdate.getColorSet());
        }
    }

    @Override
    public void noBeatReceived(LightUpdate lightUpdate) {

        lightUpdate.copyBuilderToAll(LightStateBuilder.create()
                .setTransitionTime(lightUpdate.getTransitionTime() + 3)
                .setBrightness(lightUpdate.getBrightness())
        );
    }
}
