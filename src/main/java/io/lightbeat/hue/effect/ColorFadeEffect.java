package io.lightbeat.hue.effect;

import io.lightbeat.ComponentHolder;
import io.lightbeat.hue.color.Color;

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
        Color singleColor = colorSet.getNextColor(lastFadeColor);
        lastFadeColor = colorSet.getNextColor(singleColor);

        lightUpdate.setFadeColorForAll(this, colorSet.getNextColor(lastFadeColor));
    }
}
