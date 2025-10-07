package pw.wunderlich.lightbeat.hue.visualizer;

import pw.wunderlich.lightbeat.config.Config;
import pw.wunderlich.lightbeat.config.ConfigNode;
import pw.wunderlich.lightbeat.util.DoubleAverageBuffer;

/**
 * Dynamically calibrates the transition time used for the light fade effect for a light.
 * Define the maxTransitionTime through the config, which will be the highest value returned by
 * {@link #getTransitionTime(long)}. It will reach this transition time when the given time is
 * at least twice as long as the average of previously received values
 * (history size defined by {@link #HISTORY_SIZE}).
 */
class TransitionTimeCalibrator {

    static final int HISTORY_SIZE = 25;
    static final int CALIBRATION_SIZE = 10;
    static final int MIN_TRANSITION_TIME = 1;

    private final Config config;

    private final DoubleAverageBuffer buffer;


    TransitionTimeCalibrator(Config config) {
        this.config = config;
        buffer = new DoubleAverageBuffer(HISTORY_SIZE);
    }

    /**
     * Gets the transition time for a given time since last beat occurred.
     * Returned value is higher if the timeSinceLastBeat is higher than average.
     * The first {@link #CALIBRATION_SIZE} entries will return given (maxTransitionTime / 2).
     *
     * @param timeSinceLastBeat time in milliseconds since the last beat occurred
     * @return Integer that is at least {@link #MIN_TRANSITION_TIME} and at max the given maxTransitionTime in
     *          the constructor
     */
    int getTransitionTime(long timeSinceLastBeat) {

        int maxTransitionTime = config.getInt(ConfigNode.BRIGHTNESS_FADE_MAX_TIME);

        buffer.add(timeSinceLastBeat);

        if (buffer.size() <= CALIBRATION_SIZE) {
            return maxTransitionTime / 2;
        }

        double timeToGetMaxTransition = buffer.getCurrentAverage() * 2;

        double percentage = Math.min(timeSinceLastBeat / timeToGetMaxTransition, 1d);
        return Math.max((int) Math.round(percentage * maxTransitionTime), MIN_TRANSITION_TIME);
    }

    void clearHistory() {
        buffer.clear();
    }
}
