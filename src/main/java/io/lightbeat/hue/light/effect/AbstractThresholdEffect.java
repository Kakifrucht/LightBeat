package io.lightbeat.hue.light.effect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.lightbeat.util.TimeThreshold;

/**
 * Adds custom brightness threshold and, if met, activation probability parameters
 * to effects that shouldn't always be run. They will stop running once the current
 * brightness falls below the given threshold and are rate limited. Will also
 * disactivate the effect if no beat was received for a while. Calls {@link #executionDone()}
 * to allow effects to clean up.
 */
public abstract class AbstractThresholdEffect extends AbstractEffect {

    private static final Logger logger = LoggerFactory.getLogger(AbstractThresholdEffect.class);

    private static final long MILLIS_BETWEEN_ACTIVATION = 20000L;

    private final TimeThreshold activationThreshold = new TimeThreshold(0);
    private final float brightnessThreshold;
    private final float activationProbability;

    private float brightnessDeactivationThreshold;
    boolean isActive = false;


    AbstractThresholdEffect(float brightnessThreshold, float activationProbability) {
        this.brightnessThreshold = brightnessThreshold;
        this.activationProbability = activationProbability;
        this.brightnessDeactivationThreshold = brightnessThreshold;
    }

    @Override
    public void beatReceived(LightUpdate lightUpdate) {
        this.lightUpdate = lightUpdate;
        if (isActive) {
            if (lightUpdate.isBrightnessChange() && lightUpdate.getBrightnessPercentage() < brightnessDeactivationThreshold) {
                setActive(false);
            } else if (!lightUpdate.getLights().isEmpty()) {
                executeEffect();
            }
        } else {
            if (lightUpdate.isBrightnessChange()
                    && lightUpdate.getBrightnessPercentage() > brightnessThreshold
                    && rnd.nextDouble() < activationProbability
                    && activationThreshold.isMet()) {
                setActive(true);
            }
        }
    }

    @Override
    public void noBeatReceived(LightUpdate lightUpdate) {
        this.lightUpdate = lightUpdate;
        if (isActive) {
            setActive(false);
        }
    }

    void setBrightnessDeactivationThreshold(float newThreshold) {
        this.brightnessDeactivationThreshold = newThreshold;
    }

    abstract void initializeEffect();

    void executionDone() {
        // don't force overwrites by subclasses
    }

    private void setActive(boolean active) {
        this.isActive = active;
        if (active) {
            logger.info("{} was started", this);
            initializeEffect();
            if (!lightUpdate.getLights().isEmpty()) {
                executeEffect();
            }
        } else {
            activationThreshold.setCurrentThreshold(MILLIS_BETWEEN_ACTIVATION);
            executionDone();
            logger.info("{} was stopped", this);
        }
    }
}
