package pw.wunderlich.lightbeat.hue.visualizer.effect;

import pw.wunderlich.lightbeat.ComponentHolder;
import pw.wunderlich.lightbeat.hue.bridge.light.Light;
import pw.wunderlich.lightbeat.hue.visualizer.LightUpdate;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns all lights off and strobes them one by one in order. May strobe multiple lights at once,
 * if {@link LightUpdate#getMainLights()} contains more than one light.
 */
public class StrobeChainEffect extends AbstractThresholdEffect {

    private List<Light> lightsInOrder;
    private int currentIndex;


    public StrobeChainEffect(ComponentHolder componentHolder, double brightnessThreshold, double activationProbability) {
        super(componentHolder, brightnessThreshold, activationProbability);
        setBrightnessDeactivationThreshold(brightnessThreshold - 0.1d);
    }

    @Override
    void initialize(LightUpdate lightUpdate) {
        lightsInOrder = new ArrayList<>();
        currentIndex = 0;
    }

    @Override
    void execute(LightUpdate lightUpdate) {

        if (lightsInOrder.isEmpty()) {
            for (Light light : lightUpdate.getLights()) {
                if (light.getStrobeController().setControllingEffect(this)) {
                    lightsInOrder.add(light);
                    light.getStrobeController().setOn(false);
                }
            }
            return;
        }

        for (int i = 0; i < lightUpdate.getMainLights().size(); i++) {

            lightsInOrder.get(currentIndex++)
                    .getStrobeController()
                    .doStrobe(this, lightUpdate.getTimeSinceLastBeat());

            if (currentIndex >= lightsInOrder.size()) {
                currentIndex = 0;
            }
        }
    }

    @Override
    public void executionDone(LightUpdate lightUpdate) {
        unsetControllingEffect(lightUpdate);
    }
}
