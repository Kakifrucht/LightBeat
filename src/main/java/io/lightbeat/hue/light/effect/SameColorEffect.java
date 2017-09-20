package io.lightbeat.hue.light.effect;

import io.lightbeat.hue.light.LightStateBuilder;

import java.awt.Color;

/**
 * Sends same color to all lights.
 */
public class SameColorEffect extends AbstractThresholdEffect {


    public SameColorEffect(float brightnessThreshold, float activationProbability) {
        super(0.5f, 0.4f);
    }

    @Override
    public void executeEffect() {
        Color color = lightUpdate.getColorSet().getNextColor();
        lightUpdate.copyBuilderToAll(LightStateBuilder.create().setColor(color));
    }

    @Override
    void initializeEffect() {}
}
