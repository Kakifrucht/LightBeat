package io.lightbeat.hue.effect;

import io.lightbeat.hue.LightUpdate;

/**
 * Implementing classes contain custom light change effects and patterns when beats were received.
 */
public interface LightEffect {

    /**
     * To be called whenever a beat was received.
     *
     * @param lightUpdate current state of the beat that will be modified
     */
    void beatReceived(LightUpdate lightUpdate);

    /**
     * To be called when no beat was received.
     *
     * @param lightUpdate current state of the beat that will be modified
     */
    void noBeatReceived(LightUpdate lightUpdate);
}
