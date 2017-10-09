package io.lightbeat.hue;

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
    private static final double BRIGHTNESS_CHANGE_PERCENTAGE = 0.25d;
    private static final long BRIGHTNESS_REDUCTION_MIN_DELAY_MILLIS = 5000L;

    private final int minBrightness;
    private final int brightnessRange;

    private final double sensitivityMultiplier;

    private double lastBrightness = 0d;

    private final TimeThreshold brightnessReductionThreshold = new TimeThreshold(0L);
    private final DoubleAverageBuffer amplitudeDifferenceHistory = new DoubleAverageBuffer(75);


    BrightnessCalibrator(Config config) {
        this.minBrightness = config.getInt(ConfigNode.BRIGHTNESS_MIN);
        int maxBrightness = config.getInt(ConfigNode.BRIGHTNESS_MAX);
        this.brightnessRange = maxBrightness - minBrightness;

        this.sensitivityMultiplier = 1.0d - (config.getInt(ConfigNode.BRIGHTNESS_SENSITIVITY) / 200.0d);

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
        double brightnessPercentage = Math.max(Math.min(amplitudeDifference * brightnessMultiplier, 1d), -1d);
        brightnessPercentage = (brightnessPercentage + 1d) / 2d;
        double brightnessDifference = brightnessPercentage - lastBrightness;

        // if ceilings are reached always do brightness update if necessary
        boolean doBrightnessChange = Math.abs(brightnessDifference) > BRIGHTNESS_CHANGE_PERCENTAGE
                || (brightnessPercentage == 1d && lastBrightness < 1d)
                || (brightnessPercentage == 0d && lastBrightness > 0d);

        if (doBrightnessChange) {
            // brightnessReductionThreshold reduces unnecessary fluctuations and thus reduces latency
            if (brightnessPercentage < lastBrightness && !brightnessReductionThreshold.isMet()) {
                brightnessPercentage = lastBrightness;
                doBrightnessChange = false;
            } else {
                setLastBrightness(brightnessPercentage);
            }
        } else {
            brightnessPercentage = lastBrightness;
        }

        return new BrightnessData(brightnessPercentage, doBrightnessChange);
    }

    BrightnessData getLowestBrightnessData() {
        setLastBrightness(0d);
        return new BrightnessData(0d, true);
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
        private final boolean doBrightnessChange;

        private final int brightness;
        private final int brightnessLow;


        private BrightnessData(double brightnessPercentage, boolean doBrightnessChange) {

            this.brightnessPercentage = brightnessPercentage;
            this.doBrightnessChange = doBrightnessChange;

            this.brightness = (int) (brightnessPercentage * brightnessRange) + minBrightness;
            this.brightnessLow = ((brightness - minBrightness) / 2) + minBrightness;
        }

        double getBrightnessPercentage() {
            return brightnessPercentage;
        }

        boolean isBrightnessChange() {
            return doBrightnessChange;
        }

        int getBrightness() {
            return brightness;
        }

        int getBrightnessLow() {
            return brightnessLow;
        }
    }
}
