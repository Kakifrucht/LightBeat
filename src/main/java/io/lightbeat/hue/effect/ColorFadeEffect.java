package io.lightbeat.hue.effect;

import io.lightbeat.ComponentHolder;
import io.lightbeat.hue.color.Color;
import io.lightbeat.hue.light.Light;

/**
 * Sends the same fade color to all lights to cause a continous light update fade effect.
 */
public class ColorFadeEffect extends AbstractThresholdEffect {

    private Color lastFadeColor;


    public ColorFadeEffect(ComponentHolder componentHolder, double brightnessThreshold, double activationProbability) {
        super(componentHolder, brightnessThreshold, activationProbability);
    }

    @Override
    void initialize() {
        lastFadeColor = null;
    }

    @Override
    public void execute() {
        lastFadeColor = colorSet.getNextColor(lastFadeColor);

        for (Light light : lightUpdate.getLights()) {
            light.getColorController().setFadeColor(this, lastFadeColor);
        }
    }
}
