package io.lightbeat.hue.light.effect;

import io.lightbeat.hue.light.LightStateBuilder;
import io.lightbeat.hue.light.color.Color;

/**
 * Sends same color to all lights.
 */
public class SameColorEffect extends AbstractThresholdEffect {


    public SameColorEffect(float brightnessThreshold, float activationProbability) {
        super(brightnessThreshold, activationProbability);
    }

    @Override
    public void executeEffect() {
        Color color = lightUpdate.getColorSet().getNextColor();
        lightUpdate.copyBuilderToAll(LightStateBuilder.create().setColor(color));
    }

    @Override
    void initializeEffect() {}
}
