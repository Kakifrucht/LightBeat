package io.lightbeat.hue.light.effect;

import io.lightbeat.hue.light.LightStateBuilder;

/**
 * Sends same color to all lights.
 */
public class SameColorEffect extends AbstractThresholdEffect {


    public SameColorEffect() {
        super(0.5f, 0.4f);
    }

    @Override
    public void executeEffect() {
        int randomHue = rnd.nextInt(65535);
        lightUpdate.copyBuilderToAll(LightStateBuilder.create().setHue(randomHue));
    }

    @Override
    void initializeEffect() {
    }
}
