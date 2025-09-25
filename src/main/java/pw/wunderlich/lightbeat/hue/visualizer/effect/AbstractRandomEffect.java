package pw.wunderlich.lightbeat.hue.visualizer.effect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pw.wunderlich.lightbeat.hue.visualizer.LightUpdate;

/**
 * Adds a random probability parameter to an effect that will only be checked
 * if the {@link AbstractThresholdEffect} is not active at the moment.
 */
public abstract class AbstractRandomEffect extends AbstractThresholdEffect {

    private static final Logger logger = LoggerFactory.getLogger(AbstractRandomEffect.class);

    private final double randomProbability;


    AbstractRandomEffect(double brightnessThreshold, double activationProbability, double randomProbability) {
        super(brightnessThreshold, activationProbability);
        this.randomProbability = randomProbability;
    }

    @Override
    public void beatReceived(LightUpdate lightUpdate) {
        boolean isActive = super.isActive;
        super.beatReceived(lightUpdate);
        if (!isActive) {
            if (rnd.nextDouble() < randomProbability) {
                logger.info("{} was executed once", this);
                executeEffectOnceRandomly(lightUpdate);
            }
        }
    }

    abstract void executeEffectOnceRandomly(LightUpdate lightUpdate);
}
