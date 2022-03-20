package io.lightbeat.audio;

/**
 * Event called by a {@link BeatEventManager} to be passed to all {@link BeatObserver}'s.
 */
public class BeatEvent {

    private final double triggeringAmplitude;
    private final double average;


    /**
     * Construct a BeatEvent consisting of silence.
     * Calling {@link #isSilence()} or {@link #isNoBeat()} will always return true.
     */
    BeatEvent() {
        this(0d, 0d);
    }

    /**
     * Construct a BeatEvent when no beat was detected.
     * Calling {@link #isNoBeat()} will always return true.
     *
     * @param average current amplitude average as normalized double value
     */
    BeatEvent(double average) {
        this(0d, average);
    }

    /**
     * Construct a BeatEvent when a beat was detected.
     *
     * @param triggeringAmplitude beat amplitude as normalized double value
     * @param average amplitude average when the beat was detected as normalized double value
     */
    BeatEvent(double triggeringAmplitude, double average) {
        this.triggeringAmplitude = triggeringAmplitude;
        this.average = average;
    }

    /**
     * @return amplitude as normalized double that triggered the event dispatch
     */
    public double getTriggeringAmplitude() {
        return triggeringAmplitude;
    }

    /**
     * @return average amplitude as normalized double when event was dispatched
     */
    public double getAverage() {
        return average;
    }

    boolean isSilence() {
        return triggeringAmplitude == 0d && average == 0d;
    }

    boolean isNoBeat() {
        return triggeringAmplitude == 0d;
    }
}
