package io.lightbeat.audio;

/**
 * Event called by a {@link BeatEventManager} to be passed to all {@link BeatObserver}'s.
 */
public class BeatEvent {

    private final double triggeringAmplitude;
    private final double average;


    // silence
    BeatEvent() {
        this(0d, 0d);
    }

    // no beat
    BeatEvent(double average) {
        this(0d, average);
    }

    BeatEvent(double triggeringAmplitude, double average) {
        this.triggeringAmplitude = triggeringAmplitude;
        this.average = average;
    }

    public double getTriggeringAmplitude() {
        return triggeringAmplitude;
    }

    double getAverage() {
        return average;
    }
}
