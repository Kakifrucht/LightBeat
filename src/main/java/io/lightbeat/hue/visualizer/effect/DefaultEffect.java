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

    private Color lastFadeColor;


    public DefaultEffect(ComponentHolder componentHolder) {
        super(componentHolder);
    }

    @Override
    void execute() {

        if (lightUpdate.isBrightnessChange()) {
            updateBrightness(lightUpdate);
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

        Color color = colorSet.getNextColor();
        for (Light light : lightsToChange) {
            light.getColorController().setColor(this, color);
        }
    }

    @Override
    public void noBeatReceived(LightUpdate lightUpdate) {
        updateBrightness(lightUpdate);
    }

    private void updateBrightness(LightUpdate lightUpdate) {

        boolean brightnessWasIncreased = false;
        for (Light light : lightUpdate.getLights()) {
            light.getBrightnessController().setBrightness(lightUpdate.getBrightness(), lightUpdate.getBrightnessFade());
            brightnessWasIncreased = light.getBrightnessController().isBrightnessWasIncreased();
        }

        // update fade color if brightness was increased
        if (brightnessWasIncreased) {
            lastFadeColor = colorSet.getNextColor(lastFadeColor);
            for (Light light : lightUpdate.getLights()) {
                light.getColorController().setFadeColor(this, lastFadeColor);
            }
        }
    }
}