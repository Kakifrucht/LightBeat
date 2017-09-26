package io.lightbeat.hue.light.effect;

import io.lightbeat.hue.light.Light;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns all lights off and strobes them one by one in order.
 */
public class StrobeChainEffect extends AbstractThresholdEffect {

    private List<Light> lightsInOrder;
    private int currentIndex;


    public StrobeChainEffect(float brightnessThreshold, float activationProbability) {
        super(brightnessThreshold, activationProbability);
        setBrightnessDeactivationThreshold(0.55f);
    }

    @Override
    void initializeEffect() {
        lightsInOrder = new ArrayList<>();
        currentIndex = 0;
    }

    @Override
    void executeEffect() {

        if (lightsInOrder.isEmpty()) {
            for (Light light : lightUpdate.getLights()) {
                if (light.setStrobeController(this)) {
                    lightsInOrder.add(light);
                    light.setOn(false);
                }
            }
            return;
        }

        Light toStrobe = lightsInOrder.get(currentIndex++);
        if (toStrobe.getLastKnownLightState().getBrightness() != lightUpdate.getBrightness()) {
            toStrobe.getStateBuilder().setBrightness(lightUpdate.getBrightness());
        }

        toStrobe.doStrobe(this, lightUpdate.getTimeSinceLastBeat());

        if (currentIndex >= lightsInOrder.size()) {
            currentIndex = 0;
        }
    }

    @Override
    public void executionDone() {
        lightsInOrder.forEach(l -> l.cancelStrobe(this));
    }
}
