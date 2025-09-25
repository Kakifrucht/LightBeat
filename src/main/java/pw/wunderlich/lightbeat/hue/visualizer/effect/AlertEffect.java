package pw.wunderlich.lightbeat.hue.visualizer.effect;

import pw.wunderlich.lightbeat.hue.bridge.light.Light;
import pw.wunderlich.lightbeat.hue.visualizer.LightUpdate;
import pw.wunderlich.lightbeat.util.TimeThreshold;

import java.util.List;

/**
 * Adds the {@link io.github.zeroone3010.yahueapi.AlertType#SHORT_ALERT} effect
 * to all bulbs if {@link #ALERT_THRESHOLD_MILLIS} is met. May add the effect one time randomly.
 */
public class AlertEffect extends AbstractRandomEffect {

    /**
     * Approximated time until alert effect is done.
     */
    private static final long ALERT_THRESHOLD_MILLIS = 500L;

    private TimeThreshold alertThreshold;


    public AlertEffect(double brightnessThreshold, double activationProbability, double randomProbability) {
        super(brightnessThreshold, activationProbability, randomProbability);
    }

    @Override
    void initialize(LightUpdate lightUpdate) {
        alertThreshold = new TimeThreshold(0);
    }

    @Override
    public void execute(LightUpdate lightUpdate) {

        if (alertThreshold.isMet()) {
            lightUpdate.getLights().forEach(l -> l.getBrightnessController().setAlertMode());
            alertThreshold.setCurrentThreshold(ALERT_THRESHOLD_MILLIS);
        }
    }

    @Override
    void executeEffectOnceRandomly(LightUpdate lightUpdate) {
        List<Light> lightsTurnedOn = lightUpdate.getLightsTurnedOn();
        if (!lightsTurnedOn.isEmpty()) {
            lightsTurnedOn
                    .get(0)
                    .getBrightnessController()
                    .setAlertMode();
        }
    }
}
