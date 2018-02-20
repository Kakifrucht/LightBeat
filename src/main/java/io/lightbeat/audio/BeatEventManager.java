package io.lightbeat.audio;

/**
 * Implementing class manages all registered {@link BeatObserver}'s,
 * issuing the callbacks defined in said interface.
 */
public interface BeatEventManager {

    /**
     * Registers a beat observer to receive callbacks.
     *
     * @param beatObserver to register
     */
    void registerBeatObserver(BeatObserver beatObserver);
}
