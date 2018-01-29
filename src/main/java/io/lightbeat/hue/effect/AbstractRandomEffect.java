package io.lightbeat.hue.effect;

import io.lightbeat.config.Config;
import io.lightbeat.hue.LightUpdate;
import io.lightbeat.hue.color.ColorSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds a random probability parameter to an effect that will only be checked
 * if the {@link AbstractThresholdEffect} is not active at the moment.
 */
public abstract class AbstractRandomEffect extends AbstractThresholdEffect {

    private static final Logger logger = LoggerFactory.getLogger(AbstractRandomEffect.class);

    private final double randomProbability;


    AbstractRandomEffect(Config config, ColorSet colorSet, double brightnessThreshold, double activationProbability, double randomProbability) {
        super(config, colorSet, brightnessThreshold, activationProbability);
        this.randomProbability = randomProbability;
    }

    @Override
    public void beatReceived(LightUpdate lightUpdate) {
        boolean isActive = super.isActive;
        super.beatReceived(lightUpdate);
        if (!isActive) {
            if (rnd.nextDouble() < randomProbability) {
                logger.info("{} was executed once", this);
                executeEffectOnceRandomly();
            }
        }
    }

    abstract void executeEffectOnceRandomly();
}
