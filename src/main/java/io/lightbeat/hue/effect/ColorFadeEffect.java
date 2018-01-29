package io.lightbeat.hue.effect;

import io.lightbeat.config.Config;
import io.lightbeat.hue.color.Color;
import io.lightbeat.hue.color.ColorSet;

/**
 * Sends the same fade color to all lights to cause a continous light update fade effect.
 */
public class ColorFadeEffect extends AbstractThresholdEffect {

    private Color lastFadeColor;


    public ColorFadeEffect(Config config, ColorSet colorSet, double brightnessThreshold, double activationProbability) {
        super(config, colorSet, brightnessThreshold, activationProbability);
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
