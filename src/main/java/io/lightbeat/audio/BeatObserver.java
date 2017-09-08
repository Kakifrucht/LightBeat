package io.lightbeat.audio;

/**
 * Implementing class receives callbacks whenever a peak in the audio data was read.
 * Additional methods {@link #noBeatReceived()} and {@link #silenceDetected()} are issued when there
 * was no peak or there was no audible audio data anymore.
 */
public interface BeatObserver {

    /**
     * Called when a beat was received.
     *
     * @param event containing data about the current beat
     */
    void beatReceived(BeatEvent event);

    /**
     * Called when no beat was received for a certain amount of times.
     */
    void noBeatReceived();

    /**
     * Called when silence in the audio data was detected.
     */
    void silenceDetected();
}
