package io.lightbeat.hue.light.effect;

import io.lightbeat.hue.light.Light;
import io.lightbeat.hue.light.color.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * Passes one color to all lights one by one and selects a new one after all have the new color.
 * Keeps the order of the lights and always overwrites other color based effects.
 */
public class ColorChainEffect extends AbstractThresholdEffect {

    private List<Light> lightsInOrder;

    private Color currentColor;
    private int currentIndex;


    public ColorChainEffect(float brightnessThreshold, float activationProbability) {
        super(brightnessThreshold, activationProbability);
    }

    @Override
    void initializeEffect() {
        lightsInOrder = new ArrayList<>(lightUpdate.getLights());

        currentColor = lightUpdate.getColorSet().getNextColor();
        currentIndex = -1;
    }

    @Override
    void executeEffect() {

        if (currentIndex++ >= lightsInOrder.size() - 1) {
            currentColor = lightUpdate.getColorSet().getNextColor(currentColor);
            currentIndex = 0;
        }

        Light nextLight = lightsInOrder.get(currentIndex);
        nextLight.getStateBuilder().setColor(currentColor);

        // undo color updates to other lights
        for (Light light : lightUpdate.getLightsTurnedOn()) {
            if (!nextLight.equals(light)) {
                light.getStateBuilder().setColor(null);
            }
        }
    }
}
