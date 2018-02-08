package io.lightbeat.hue.effect;

import io.lightbeat.ComponentHolder;
import io.lightbeat.hue.color.Color;
import io.lightbeat.hue.light.Light;

import java.util.HashMap;
import java.util.Map;

/**
 * Flips selected lights between two colors and switches colors every couple beats.
 */
public class ColorFlipEffect extends AbstractThresholdEffect {

    /**
     * String is light identifier, if boolean is true set to first hue of cycle, else second one
     */
    private Map<Light, Boolean> lightFlipDirection;

    private int nextColorsInBeats;

    private Color color1;
    private Color color2;


    public ColorFlipEffect(ComponentHolder componentHolder, double brightnessThreshold, double activationProbability) {
        super(componentHolder, brightnessThreshold, activationProbability);
    }

    @Override
    void initialize() {

        color1 = colorSet.getNextColor();
        color2 = colorSet.getNextColor(color1);

        lightFlipDirection = new HashMap<>();
        nextColorsInBeats = 0;
    }

    @Override
    public void execute() {

        if (lightFlipDirection.isEmpty()) {

            for (Light light : lightUpdate.getLights()) {
                if (light.getColorController().setControllingEffect(this)) {
                    lightFlipDirection.put(light, rnd.nextBoolean());
                }
            }

            if (lightFlipDirection.isEmpty()) {
                return;
            }
        }

        if (--nextColorsInBeats <= 0) {

            nextColorsInBeats = 4 + rnd.nextInt(4);

            color1 = color2;
            color2 = colorSet.getNextColor(color1);

            for (Light light : lightFlipDirection.keySet()) {
                flipLightColor(light, rnd.nextBoolean());
            }

        } else {
            for (Light light : lightFlipDirection.keySet()) {
                flipLightColor(light, lightFlipDirection.get(light));
            }
        }
    }

    private void flipLightColor(Light light, boolean useColor1) {
        lightFlipDirection.put(light, !useColor1);
        light.getColorController().setColor(this, useColor1 ? color1 : color2);
        light.getColorController().setFadeColor(this, useColor1 ? color2 : color1);
        light.getBrightnessController().forceBrightnessUpdate();
    }

    @Override
    public void executionDone() {
        for (Light light : lightFlipDirection.keySet()) {
            light.getColorController().setFadeColor(this, color1);
            light.getColorController().unsetControllingEffect(this);
        }
    }
}
