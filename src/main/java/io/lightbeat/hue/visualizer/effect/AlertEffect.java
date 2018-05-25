package io.lightbeat.hue.visualizer.effect;

import io.lightbeat.ComponentHolder;
import io.lightbeat.hue.bridge.light.Light;
import io.lightbeat.hue.visualizer.LightUpdate;
import io.lightbeat.util.TimeThreshold;

import java.util.List;

/**
 * Adds the {@link com.philips.lighting.model.PHLight.PHLightAlertMode#ALERT_SELECT} effect
 * to all bulbs if {@link #ALERT_THRESHOLD_MILLIS} is met. May add the effect one time randomly.
 */
public class AlertEffect extends AbstractRandomEffect {

    private static final long ALERT_THRESHOLD_MILLIS = 500L;

    private TimeThreshold alertThreshold;


    public AlertEffect(ComponentHolder componentHolder, double brightnessThreshold, double activationProbability, double randomProbability) {
        super(componentHolder, brightnessThreshold, activationProbability, randomProbability);
    }

    @Override
    void initialize() {
        alertThreshold = new TimeThreshold(0);
    }

    @Override
    public void execute(LightUpdate lightUpdate) {

        if (alertThreshold.isMet()) {
            lightUpdate.getLights().forEach(l -> l.getBrightnessController().setAlertMode());
            alertThreshold.setCurrentThreshold(ALERT_THRESHOLD_MILLIS); // ~time until effect is done
        }
    }

    @Override
    void executeEffectOnceRandomly(LightUpdate lightUpdate) {
        List<Light> activeLights = lightUpdate.getLightsTurnedOn();
        if (!activeLights.isEmpty()) {
            activeLights
                    .get(0)
                    .getBrightnessController()
                    .setAlertMode();
        }
    }
}
