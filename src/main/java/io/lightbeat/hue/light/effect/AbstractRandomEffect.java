package io.lightbeat.hue.light.effect;

/**
 * Adds a random probability parameter to an effect that will only be checked
 * if the {@link AbstractThresholdEffect} is not active at the moment.
 */
public abstract class AbstractRandomEffect extends AbstractThresholdEffect {

    private final float randomProbability;


    AbstractRandomEffect(float brightnessThreshold, float activationProbability, float randomProbability) {
        super(brightnessThreshold, activationProbability);
        this.randomProbability = randomProbability;
    }

    @Override
    public void beatReceived(LightUpdate lightUpdate) {
        boolean isActive = super.isActive;
        super.beatReceived(lightUpdate);
        if (!isActive) {
            if (rnd.nextDouble() < randomProbability) {
                executeEffectOnceRandomly();
            }
        }
    }

    abstract void executeEffectOnceRandomly();
}
