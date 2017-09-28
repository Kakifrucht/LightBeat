package io.lightbeat.hue.light;

import io.lightbeat.config.Config;
import io.lightbeat.config.ConfigNode;
import io.lightbeat.util.TimeThreshold;
import io.lightbeat.util.DoubleAverageBuffer;

/**
 * Dynamically calibrates the brightness level after receiving amplitudes, based on the highest
 * amplitude received. Calling {@link #getBrightness(double)} returns a {@link BrightnessData}
 * object, which contains the relevant information for the next light update, and if a brightness
 * change is needed in the first place.
 */
class BrightnessCalibrator {

    private static final double HISTORY_STARTING_VALUE = 0.15d;
    private static final long BRIGHTNESS_REDUCTION_MIN_DELAY_MILLIS = 5000L;

    private final int minBrightness;
    private final int brightnessRange;

    private final double sensitivityMultiplier;
    private final int transitionTime;

    private double lastBrightness = 0d;

    private final TimeThreshold brightnessReductionThreshold = new TimeThreshold(0L);
    private final DoubleAverageBuffer amplitudeDifferenceHistory = new DoubleAverageBuffer(75);


    BrightnessCalibrator(Config config) {
        this.minBrightness = config.getInt(ConfigNode.BRIGHTNESS_MIN);
        int maxBrightness = config.getInt(ConfigNode.BRIGHTNESS_MAX);
        this.brightnessRange = maxBrightness - minBrightness;

        this.sensitivityMultiplier = 1.0d - (config.getInt(ConfigNode.BRIGHTNESS_SENSITIVITY) / 200.0d);
        this.transitionTime = config.getInt(ConfigNode.LIGHTS_TRANSITION_TIME);

        this.amplitudeDifferenceHistory.add(HISTORY_STARTING_VALUE);
    }

    /**
     * Get the brightness for the given amplitude difference from the average amplitude.
     *
     * @param amplitudeDifference difference from average amplitude (between -1 and 1)
     * @return BrightnessData object containing all information about the brightness
     */
    BrightnessData getBrightness(double amplitudeDifference) {

        amplitudeDifferenceHistory.add(amplitudeDifference);

        // multiplier is calibrated in regards to the currently highest amplitude, which will set brightness to max if received
        double brightnessMultiplier = 1 / (amplitudeDifferenceHistory.getMaxValue() * sensitivityMultiplier);
        double brightnessPercentage = Math.min(amplitudeDifference * brightnessMultiplier, 1d);
        double brightnessDifference = brightnessPercentage - lastBrightness;

        boolean doBrightnessChange = Math.abs(brightnessDifference) > 0.15d;
        if (doBrightnessChange) {
            // brightnessReductionThreshold reduces unnecessary fluctuations and thus reduces latency
            if (brightnessPercentage < lastBrightness && !brightnessReductionThreshold.isMet()) {
                brightnessPercentage = lastBrightness;
                brightnessDifference = 0d;
                doBrightnessChange = false;
            } else {
                setLastBrightness(brightnessPercentage);
            }
        } else {
            brightnessPercentage = lastBrightness;
            brightnessDifference = 0d;
        }

        return new BrightnessData(brightnessPercentage, brightnessDifference, doBrightnessChange);
    }

    BrightnessData getLowestBrightnessData() {
        double brightnessDifferencePercentage = 0d - lastBrightness;
        setLastBrightness(0d);
        return new BrightnessData(0d, brightnessDifferencePercentage, true);
    }

    void clear() {
        amplitudeDifferenceHistory.clear();
        amplitudeDifferenceHistory.add(HISTORY_STARTING_VALUE);
    }

    private void setLastBrightness(double brightness) {
        brightnessReductionThreshold.setCurrentThreshold(BRIGHTNESS_REDUCTION_MIN_DELAY_MILLIS);
        lastBrightness = brightness;
    }


    class BrightnessData {

        private final double brightnessPercentage;
        private final double brightnessDifferencePercentage;
        private final boolean doBrightnessChange;

        private final int brightness;


        private BrightnessData(double brightnessPercentage, double brightnessDifferencePercentage, boolean doBrightnessChange) {

            this.brightnessPercentage = brightnessPercentage;
            this.brightnessDifferencePercentage = brightnessDifferencePercentage;
            this.doBrightnessChange = doBrightnessChange;

            this.brightness = (int) (brightnessPercentage * brightnessRange) + minBrightness;
        }

        double getBrightnessPercentage() {
            return brightnessPercentage;
        }

        double getBrightnessDifferencePrevious() {
            return brightnessDifferencePercentage;
        }

        boolean isBrightnessChange() {
            return doBrightnessChange;
        }

        int getBrightness() {
            return brightness;
        }

        int getTransitionTime() {
            return transitionTime;
        }
    }
}
