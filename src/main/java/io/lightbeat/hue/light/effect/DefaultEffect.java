package io.lightbeat.hue.light.effect;

import com.philips.lighting.model.PHLight;
import io.lightbeat.hue.light.LightStateBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Default effect that updates the color of a random amount of lights.
 */
public class DefaultEffect extends AbstractEffect {

    @Override
    void executeEffect() {

        // select random light(s) to update
        List<PHLight> lights = lightUpdate.getLights();
        List<PHLight> lightsToChange = new ArrayList<>();
        lightsToChange.add(lights.get(0));

        // randomly add more lights, depending on amount of lights in configuration
        int randomThreshold = Math.min(5, (int) Math.round(lights.size() * 0.7d));
        for (int i = 1; i < lights.size() && rnd.nextInt(10) < randomThreshold; i++) {
            lightsToChange.add(lights.get(i));
        }

        for (PHLight light : lightsToChange) {
            lightUpdate.getBuilder(light).setRandomHue(lightUpdate.getColorSet());
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
