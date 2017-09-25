package io.lightbeat.hue.light.effect;

import io.lightbeat.hue.light.LightStateBuilder;
import io.lightbeat.hue.light.color.Color;

/**
 * Sends same color to all lights.
 */
public class SameColorEffect extends AbstractThresholdEffect {

    private Color lastColor;


    public SameColorEffect(float brightnessThreshold, float activationProbability) {
        super(brightnessThreshold, activationProbability);
    }

    @Override
    void initializeEffect() {
        lastColor = null;
    }

    @Override
    public void executeEffect() {
        lastColor = lightUpdate.getColorSet().getNextColor(lastColor);
        lightUpdate.copyBuilderToAll(LightStateBuilder.create().setColor(lastColor));
    }
}
