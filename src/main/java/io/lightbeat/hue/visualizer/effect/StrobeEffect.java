package io.lightbeat.hue.visualizer.effect;

import io.lightbeat.ComponentHolder;
import io.lightbeat.hue.bridge.light.Light;
import io.lightbeat.hue.visualizer.LightUpdate;

import java.util.ArrayList;
import java.util.List;

/**
 * Effect to add a strobing, while turning all but one lights off and strobing the main lights for the current update.
 * The strobe will be synchronized to the beat. May also randomly turn one light off and on.
 */
public class StrobeEffect extends AbstractRandomEffect {

    private volatile Light activeLight;
    private int nextLightInBeats;


    public StrobeEffect(ComponentHolder componentHolder,
                        double brightnessThreshold, double activationProbability, double randomProbability) {
        super(componentHolder, brightnessThreshold, activationProbability, randomProbability);
        setBrightnessDeactivationThreshold(brightnessThreshold - 0.2d);
    }

    @Override
    void initialize() {
        activeLight = null;
        nextLightInBeats = 0;
    }

    @Override
    public void execute(LightUpdate lightUpdate) {

        List<Light> controllableLights = new ArrayList<>();
        for (Light light : lightUpdate.getLights()) {
            if (light.getStrobeController().canControl(this)) {
                controllableLights.add(light);
            }
        }

        if (controllableLights.isEmpty()) {
            return;
        }

        if (nextLightInBeats-- <= 0) {

            if (activeLight != null) {
                // turn currently active light off
                activeLight.getStrobeController().setOn(false);
            } else {
                // turn all lights off at the beginning and take control
                for (Light controllableLight : controllableLights) {
                    controllableLight.getStrobeController().setOn(false);
                    controllableLight.getStrobeController().setControllingEffect(this);
                }
            }

            for (Light light : controllableLights) {
                if (!light.equals(activeLight)) {
                    activeLight = light;
                    activeLight.getStrobeController().setOn(true);
                    break;
                }
            }

            nextLightInBeats = 5 + rnd.nextInt(6);

        } else {

            activeLight.getBrightnessController().setAlertMode();

            // strobe main lights
            for (Light light : lightUpdate.getMainLights()) {
                if (controllableLights.contains(light)
                        && !light.equals(this.activeLight)
                        && !light.getStrobeController().isStrobing()) {
                    light.getStrobeController().doStrobe(this, lightUpdate.getTimeSinceLastBeat());
                }
            }
        }
    }

    @Override
    public void executionDone(LightUpdate lightUpdate) {
        unsetControllingEffect(lightUpdate);
    }

    @Override
    void executeEffectOnceRandomly(LightUpdate lightUpdate) {

        if (lightUpdate.isBrightnessChange() || lightUpdate.getBrightnessPercentage() < 0.5d) {
            return;
        }

        // strobe all strobable lights but one
        List<Light> lights = lightUpdate.getLightsTurnedOn();
        for (int i = 1; i < lights.size(); i++) {
            lights.get(i).getStrobeController().doStrobe(this, lightUpdate.getTimeSinceLastBeat());
        }
    }
}
