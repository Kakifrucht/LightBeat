package io.lightbeat.hue.light.effect;

import com.philips.lighting.model.PHLight;
import io.lightbeat.hue.light.LightStateBuilder;
import io.lightbeat.hue.light.color.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * Passes one color to all lights one by one and selects a new one after all have the new color.
 * Keeps the order of the lights and always overwrites other color based effects.
 */
public class ColorChainEffect extends AbstractThresholdEffect {

    private List<PHLight> lightsInOrder;

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

        // initialize to one color
        lightUpdate.copyBuilderToAll(LightStateBuilder.create().setColor(currentColor));
    }

    @Override
    void executeEffect() {
        if (currentIndex++ >= lightsInOrder.size() - 1) {
            Color nextColor = lightUpdate.getColorSet().getNextColor();
            if (nextColor.equals(currentColor)) {
                nextColor = lightUpdate.getColorSet().getNextColor();
            }

            currentColor = nextColor;
            currentIndex = 0;
        }

        PHLight nextLight = lightsInOrder.get(currentIndex);
        lightUpdate.getBuilder(nextLight).setColor(currentColor);

        // undo updates to other lights
        for (PHLight phLight : lightUpdate.getLights()) {
            if (!nextLight.equals(phLight)) {
                lightUpdate.getBuilder(phLight).setColor(null);
            }
        }
    }
}
