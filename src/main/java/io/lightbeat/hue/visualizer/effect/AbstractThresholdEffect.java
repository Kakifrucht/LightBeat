package io.lightbeat.hue.visualizer.effect;

import io.lightbeat.ComponentHolder;
import io.lightbeat.hue.visualizer.LightUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds custom brightness threshold and, if met, activation probability parameters
 * to effects that shouldn't always be run. They will stop running once the current
 * brightness falls below the given threshold. Will also
 * disactivate the effect if no beat was received for a while. Calls {@link #executionDone(LightUpdate)}
 * to allow effects to clean up.
 */
public abstract class AbstractThresholdEffect extends AbstractEffect {

    private static final Logger logger = LoggerFactory.getLogger(AbstractThresholdEffect.class);

    private final double brightnessThreshold;
    private final double activationProbability;

    private double brightnessDeactivationThreshold;
    boolean isActive = false;


    AbstractThresholdEffect(ComponentHolder componentHolder, double brightnessThreshold, double activationProbability) {
        super(componentHolder);
        this.brightnessThreshold = brightnessThreshold;
        this.activationProbability = activationProbability;
        this.brightnessDeactivationThreshold = brightnessThreshold;
    }

    @Override
    public void beatReceived(LightUpdate lightUpdate) {

        if (isActive) {
            if (lightUpdate.isBrightnessChange() && lightUpdate.getBrightnessPercentage() < brightnessDeactivationThreshold) {
                setActive(false, lightUpdate);
            } else {
                execute(lightUpdate);
            }
        } else {
            if (lightUpdate.isBrightnessChange()
                    && lightUpdate.getBrightnessPercentage() > brightnessThreshold
                    && rnd.nextDouble() < activationProbability) {
                setActive(true, lightUpdate);
            }
        }
    }

    @Override
    public void noBeatReceived(LightUpdate lightUpdate) {
        if (isActive) {
            setActive(false, lightUpdate);
        }
    }

    void setBrightnessDeactivationThreshold(double newThreshold) {
        this.brightnessDeactivationThreshold = newThreshold;
    }

    abstract void initialize(LightUpdate lightUpdate);

    void executionDone(LightUpdate lightUpdate) {
        // don't force overwrites by subclasses
    }

    private void setActive(boolean active, LightUpdate lightUpdate) {
        this.isActive = active;
        if (active) {
            logger.info("{} was started", this);
            initialize(lightUpdate);
            execute(lightUpdate);
        } else {
            executionDone(lightUpdate);
            logger.info("{} was stopped", this);
        }
    }
}
