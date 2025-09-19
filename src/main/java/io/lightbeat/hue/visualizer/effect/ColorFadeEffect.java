package io.lightbeat.hue.visualizer.effect;

import io.lightbeat.ComponentHolder;
import io.lightbeat.hue.bridge.color.Color;
import io.lightbeat.hue.bridge.light.Light;
import io.lightbeat.hue.visualizer.LightUpdate;

/**
 * Sends the same fade color to all lights to cause a continuous color update fade effect.
 * Sets the main lights to the fade color during the beat while re-selecting a new color
 * on every beat.
 */
public class ColorFadeEffect extends AbstractThresholdEffect {

    private Color lastColor;


    public ColorFadeEffect(ComponentHolder componentHolder, double brightnessThreshold, double activationProbability) {
        super(componentHolder, brightnessThreshold, activationProbability);
    }

    @Override
    void initialize(LightUpdate lightUpdate) {
        lastColor = null;
    }

    @Override
    public void execute(LightUpdate lightUpdate) {
        lastColor = lightUpdate.getColorSet().getNextColor(lastColor);

        for (Light light : lightUpdate.getLights()) {
            if (light.getColorController().setControllingEffect(this)) {

                // if it is a main light, do beat
                if (lightUpdate.getMainLights().contains(light)) {
                    light.getColorController().setColor(this, lastColor);
                }

                light.getColorController().setFadeColor(this, lastColor);
            }
        }
    }

    @Override
    void executionDone(LightUpdate lightUpdate) {
        unsetControllingEffect(lightUpdate);
    }
}
