package io.lightbeat.hue.effect;

import io.lightbeat.ComponentHolder;
import io.lightbeat.hue.light.Light;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns all lights off and strobes them one by one in order.
 */
public class StrobeChainEffect extends AbstractThresholdEffect {

    private static final double ADDITIONAL_LIGHT_PROBABILITY = 0.2d;

    private List<Light> lightsInOrder;
    private int currentIndex;


    public StrobeChainEffect(ComponentHolder componentHolder, double brightnessThreshold, double activationProbability) {
        super(componentHolder, brightnessThreshold, activationProbability);
        setBrightnessDeactivationThreshold(brightnessThreshold - 0.1d);
    }

    @Override
    void initialize() {
        lightsInOrder = new ArrayList<>();
        currentIndex = 0;
    }

    @Override
    void execute() {

        if (lightsInOrder.isEmpty()) {
            for (Light light : lightUpdate.getLights()) {
                if (light.getStrobeController().setControllingEffect(this)) {
                    lightsInOrder.add(light);
                    light.getStrobeController().setOn(false);
                }
            }
            return;
        }

        boolean isFirstLight = true;
        while (isFirstLight || Math.random() < ADDITIONAL_LIGHT_PROBABILITY) {

            isFirstLight = false;

            lightsInOrder.get(currentIndex++)
                    .getStrobeController()
                    .doStrobe(this, lightUpdate.getTimeSinceLastBeat());

            if (currentIndex >= lightsInOrder.size()) {
                currentIndex = 0;
            }
        }
    }

    @Override
    public void executionDone() {
        lightsInOrder.forEach(l -> l.getStrobeController().unsetControllingEffect(this));
    }
}
