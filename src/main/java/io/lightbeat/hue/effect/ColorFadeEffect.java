package io.lightbeat.hue.effect;

import io.lightbeat.hue.color.Color;
import io.lightbeat.hue.color.ColorSet;
import io.lightbeat.hue.light.Light;

import java.util.List;

/**
 * Sends the same fade color to all lights, while still updating one random light normally.
 */
public class ColorFadeEffect extends AbstractThresholdEffect {

    private Color lastFadeColor;


    public ColorFadeEffect(ColorSet colorSet, float brightnessThreshold, float activationProbability) {
        super(colorSet, brightnessThreshold, activationProbability);
    }

    @Override
    void initialize() {
        lastFadeColor = null;
    }

    @Override
    public void execute() {
        Color singleColor = colorSet.getNextColor(lastFadeColor);
        lastFadeColor = colorSet.getNextColor(singleColor);

        List<Light> lightsTurnedOn = lightUpdate.getLightsTurnedOn();
        if (!lightsTurnedOn.isEmpty()) {
            lightsTurnedOn.get(0).getColorController().setColor(this, singleColor);
            lightUpdate.setFadeColorForAll(this, colorSet.getNextColor(lastFadeColor));
        }
    }
}
