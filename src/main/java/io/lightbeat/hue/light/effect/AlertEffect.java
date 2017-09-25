package io.lightbeat.hue.light.effect;

import com.philips.lighting.model.PHLight;
import io.lightbeat.hue.light.Light;
import io.lightbeat.hue.light.LightStateBuilder;
import io.lightbeat.util.TimeThreshold;

import java.util.List;

/**
 * Adds the {@link com.philips.lighting.model.PHLight.PHLightAlertMode#ALERT_SELECT} effect
 * to all bulbs if {@link #ALERT_THRESHOLD_MILLIS} is met. May add the effect one time randomly.
 */
public class AlertEffect extends AbstractRandomEffect {

    private static final long ALERT_THRESHOLD_MILLIS = 500L;

    private TimeThreshold alertThreshold;


    public AlertEffect(float brightnessThreshold, float activationProbability, float randomProbability) {
        super(brightnessThreshold, activationProbability, randomProbability);
    }

    @Override
    public void executeEffect() {

        if (alertThreshold.isMet()) {
            lightUpdate.copyBuilderToAll(LightStateBuilder.create().setAlertMode(PHLight.PHLightAlertMode.ALERT_SELECT));
            alertThreshold.setCurrentThreshold(ALERT_THRESHOLD_MILLIS); // ~time until effect is done
        }
    }

    @Override
    void executeEffectOnceRandomly() {
        List<Light> activeLights = lightUpdate.getLightsTurnedOn();
        if (!activeLights.isEmpty()) {
            activeLights
                    .get(0)
                    .getStateBuilder()
                    .setAlertMode(PHLight.PHLightAlertMode.ALERT_SELECT);
        }
    }

    @Override
    void initializeEffect() {
        alertThreshold = new TimeThreshold(0);
    }
}
