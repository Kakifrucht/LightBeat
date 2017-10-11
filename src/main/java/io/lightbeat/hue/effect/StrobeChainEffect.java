package io.lightbeat.hue.effect;

import io.lightbeat.hue.light.Light;
import io.lightbeat.hue.color.ColorSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns all lights off and strobes them one by one in order.
 */
public class StrobeChainEffect extends AbstractThresholdEffect {

    private List<Light> lightsInOrder;
    private int currentIndex;


    public StrobeChainEffect(ColorSet colorSet, double brightnessThreshold, double activationProbability) {
        super(colorSet, brightnessThreshold, activationProbability);
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

        lightsInOrder.get(currentIndex++)
                .getStrobeController()
                .doStrobe(this, lightUpdate.getTimeSinceLastBeat());

        if (currentIndex >= lightsInOrder.size()) {
            currentIndex = 0;
        }
    }

    @Override
    public void executionDone() {
        lightsInOrder.forEach(l -> l.getStrobeController().unsetControllingEffect(this));
    }
}
