package io.lightbeat.hue.visualizer;

import io.lightbeat.util.DoubleAverageBuffer;

/**
 * Dynamically calibrates the transition time used for the light fade effect for a light.
 * Define the maxTransitionTime in the constructor, which will be the highest value returned by
 * {@link #getTransitionTime(long)}. It will reach this transition time when the given time is
 * at least twice as long than the average of previously received values
 * (history size defined by {@link #HISTORY_SIZE}.
 *
 */
class TransitionTimeCalibrator {

    private static final int HISTORY_SIZE = 25;
    private static final int CALIBRATION_SIZE = 10;
    private static final int MIN_TRANSITION_TIME = 1;

    private final int maxTransitionTime;

    private final DoubleAverageBuffer buffer;


    TransitionTimeCalibrator(int maxTransitionTime) {
        this.maxTransitionTime = Math.max(Math.min(8, maxTransitionTime), 1);
        buffer = new DoubleAverageBuffer(HISTORY_SIZE);
    }

    /**
     * Gets the transition time for a given time since last beat ocurred.
     * Returned value is higher if the timeSinceLastBeat is higher than average.
     * The first {@link #CALIBRATION_SIZE} entries will return given (maxTransitionTime / 2).
     *
     * @param timeSinceLastBeat time in milliseconds since the last beat ocurred
     * @return Integer that is at least {@link #MIN_TRANSITION_TIME} and at max the given maxTransitionTime in
     *          the constructor
     */
    int getTransitionTime(long timeSinceLastBeat) {

        buffer.add(timeSinceLastBeat);

        if (buffer.size() < CALIBRATION_SIZE) {
            return maxTransitionTime / 2;
        }

        double timeToGetMaxTransition = buffer.getCurrentAverage() * 2;

        double percentage = Math.min(timeSinceLastBeat / timeToGetMaxTransition, 1d);
        return Math.max((int) Math.round(percentage * maxTransitionTime), MIN_TRANSITION_TIME);
    }
}
