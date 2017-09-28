package io.lightbeat.hue.light.effect;

import com.philips.lighting.model.PHLight;
import io.lightbeat.hue.light.Light;

import java.util.ArrayList;
import java.util.List;

/**
 * Effect to add a strobing, while turning all but one lights off and strobing the other ones (amount
 * dependant on light configuration). The strobe will be synchronized to the beat. May also randomly
 * turn one light off and on.
 */
public class StrobeEffect extends AbstractRandomEffect {

    private volatile Light activeLight;
    private int nextLightInBeats;


    public StrobeEffect(float brightnessThreshold, float activationProbability, float randomProbability) {
        super(brightnessThreshold, activationProbability, randomProbability);
        setBrightnessDeactivationThreshold(0.7f);
    }

    @Override
    void initializeEffect() {
        activeLight = null;
        nextLightInBeats = 0;
    }

    @Override
    public void executeEffect() {

        List<Light> controllableLights = new ArrayList<>();
        for (Light light : lightUpdate.getLights()) {
            if (light.canDoStrobe(this)) {
                controllableLights.add(light);
            }
        }

        if (controllableLights.isEmpty()) {
            return;
        }

        int brightness = lightUpdate.getBrightness();
        if (nextLightInBeats-- <= 0) {

            if (activeLight != null) {
                // turn currently active light off
                activeLight.setOn(false);
            } else {
                // turn all lights off at the beginning and take control
                for (Light controllableLight : controllableLights) {
                    controllableLight.setOn(false);
                    controllableLight.setStrobeController(this);
                }
            }

            for (Light light : controllableLights) {
                if (!light.equals(activeLight) && !light.isOn()) {
                    activeLight = light;
                    activeLight.setOn(true);

                    if (activeLight.getLastKnownLightState().getBrightness() != brightness) {
                        // due to turning all lights off at the beginning brightness must eventually be set again
                        activeLight.getStateBuilder().setBrightness(brightness);
                    }

                    break;
                }
            }

            nextLightInBeats = 5 + rnd.nextInt(6);

        } else {

            // add alert to main light
            activeLight.getStateBuilder().setAlertMode(PHLight.PHLightAlertMode.ALERT_SELECT);

            // strobe random lights, depending on how many lights are in the configuration (minimum 1)
            int amountToStrobe = Math.max((controllableLights.size() - 1) / 2, 1);
            for (Light light : controllableLights) {

                if (!light.equals(this.activeLight) && !light.isOn()) {

                    if (light.getLastKnownLightState().getBrightness() != brightness) {
                        light.getStateBuilder().setBrightness(brightness);
                    }

                    light.doStrobe(this, lightUpdate.getTimeSinceLastBeat());

                    if (--amountToStrobe == 0) {
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void executionDone() {
        lightUpdate.getLights().forEach(l -> l.unsetStrobeController(this));
    }

    @Override
    void executeEffectOnceRandomly() {

        if (lightUpdate.isBrightnessChange()) {
            return;
        }

        // strobe all strobable lights but one
        List<Light> lights = lightUpdate.getLightsTurnedOn();
        for (int i = 1; i < lights.size(); i++) {
            lights.get(i).doStrobe(this, lightUpdate.getTimeSinceLastBeat(), lightUpdate.getBrightness());
        }
    }
}
