package io.lightbeat.hue.visualizer.effect;

import io.lightbeat.ComponentHolder;
import io.lightbeat.hue.visualizer.LightUpdate;
import io.lightbeat.hue.bridge.color.Color;
import io.lightbeat.hue.bridge.light.Light;

import java.util.ArrayList;
import java.util.List;

/**
 * Default effect that updates the color of a random amount of lights.
 */
public class DefaultEffect extends AbstractEffect {

    private Color color;
    private Color fadeColor;


    public DefaultEffect(ComponentHolder componentHolder) {
        super(componentHolder);
    }

    @Override
    void execute(LightUpdate lightUpdate) {

        if (color == null) {
            updateColor(lightUpdate);
        }

        if (lightUpdate.isBrightnessChange()) {
            updateBrightness(lightUpdate);
        }

        // select random light(s) to update
        List<Light> lights = lightUpdate.getLightsTurnedOn();
        if (lights.isEmpty()) {
            return;
        }

        List<Light> lightsToUpdate = new ArrayList<>();
        lightsToUpdate.add(lights.get(0));

        // randomly add more lights, depending on amount of lights in configuration
        int randomThreshold = Math.min(5, (int) Math.round(lights.size() * 0.7d));
        for (int i = 1; i < lights.size() && rnd.nextInt(10) < randomThreshold; i++) {
            lightsToUpdate.add(lights.get(i));
        }

        for (Light light : lightsToUpdate) {
            light.getColorController().setColor(this, color);
        }
    }

    @Override
    public void noBeatReceived(LightUpdate lightUpdate) {
        updateBrightness(lightUpdate);
    }

    private void updateColor(LightUpdate lightUpdate) {
        fadeColor = colorSet.getNextColor(fadeColor);
        color = colorSet.getNextColor(fadeColor);

        for (Light light : lightUpdate.getLights()) {
            light.getColorController().setFadeColor(this, fadeColor);
        }
    }

    private void updateBrightness(LightUpdate lightUpdate) {

        boolean brightnessWasIncreased = false;
        for (Light light : lightUpdate.getLights()) {
            light.getBrightnessController().setBrightness(lightUpdate.getBrightness(), lightUpdate.getBrightnessFade());
            brightnessWasIncreased = light.getBrightnessController().isBrightnessWasIncreased();
        }

        // update colors if brightness was increased
        if (brightnessWasIncreased) {
            updateColor(lightUpdate);
        }
    }
}
