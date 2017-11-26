package io.lightbeat.hue;

import io.lightbeat.config.Config;
import io.lightbeat.config.ConfigNode;
import io.lightbeat.util.TimeThreshold;
import io.lightbeat.util.DoubleAverageBuffer;

/**
 * Dynamically calibrates the brightness level after receiving amplitudes, based on the highest
 * amplitude received. Calling {@link #getBrightness(double)} returns a {@link BrightnessData}
 * object, which contains the relevant information for the next light update, and if a brightness
 * change is needed in the first place. The first call to the method will always return {@link BrightnessData}
 * that sets the brightness to 50%, and keeps sending the same amount.
 */
class BrightnessCalibrator {

    private static final double BRIGHTNESS_CHANGE_MINIMUM_PERCENTAGE = 0.25d;
    private static final long BRIGHTNESS_REDUCTION_MIN_DELAY_MILLIS = 5000L;
    private static final int BUFFER_SIZE = 150;

    private final int minBrightness;
    private final int brightnessRange;

    private final double sensitivityMultiplier;

    private boolean isFirstBeat = true;
    private double previousBrightness = 0d;

    private final TimeThreshold brightnessReductionThreshold = new TimeThreshold(0L);
    private final DoubleAverageBuffer amplitudeDifferenceHistory = new DoubleAverageBuffer(BUFFER_SIZE);


    BrightnessCalibrator(Config config) {
        this.minBrightness = config.getInt(ConfigNode.BRIGHTNESS_MIN);
        int maxBrightness = config.getInt(ConfigNode.BRIGHTNESS_MAX);
        this.brightnessRange = maxBrightness - minBrightness;

        this.sensitivityMultiplier = 1.0d - (config.getInt(ConfigNode.BRIGHTNESS_SENSITIVITY) / 200.0d);
    }

    /**
     * Get the brightness for the given amplitude difference from the average amplitude.
     *
     * @param amplitudeDifference difference from average amplitude (between -1 and 1)
     * @return BrightnessData object containing all information about the brightness
     */
    BrightnessData getBrightness(double amplitudeDifference) {

        // if is first beat set lights to 50%
        if (isFirstBeat) {
            isFirstBeat = false;
            if (amplitudeDifference != 0d) {
                amplitudeDifferenceHistory.add(amplitudeDifference);
            }
            return getBrightnessData(0.5d, true);
        }

        amplitudeDifferenceHistory.add(amplitudeDifference);

        // calibration phase
        if (amplitudeDifferenceHistory.size() < 5) {
            return getBrightnessData(0.5d, false);
        }

        // multiplier is calibrated in regards to the currently highest amplitude, which will set brightness to max if received
        double brightnessMultiplier = 1 / (amplitudeDifferenceHistory.getMaxValue() * sensitivityMultiplier);
        double brightnessPercentage = Math.max(Math.min(amplitudeDifference * brightnessMultiplier, 1d), -1d);
        brightnessPercentage = (brightnessPercentage + 1d) / 2d;
        double brightnessDifference = brightnessPercentage - previousBrightness;

        // if ceilings are reached always do brightness update if necessary
        boolean doBrightnessChange = Math.abs(brightnessDifference) > BRIGHTNESS_CHANGE_MINIMUM_PERCENTAGE
                || (brightnessPercentage == 1d && previousBrightness < 1d)
                || (brightnessPercentage == 0d && previousBrightness > 0d);

        if (doBrightnessChange) {
            // brightnessReductionThreshold reduces unnecessary fluctuations and thus reduces latency
            if (brightnessPercentage < previousBrightness && !brightnessReductionThreshold.isMet()) {
                brightnessPercentage = previousBrightness;
                doBrightnessChange = false;
            }
        } else {
            brightnessPercentage = previousBrightness;
        }

        return getBrightnessData(brightnessPercentage, doBrightnessChange);
    }

    BrightnessData getLowestBrightnessData() {
        return getBrightnessData(0d, true);
    }

    private void setPreviousBrightness(double brightness) {
        brightnessReductionThreshold.setCurrentThreshold(BRIGHTNESS_REDUCTION_MIN_DELAY_MILLIS);
        previousBrightness = brightness;
    }

    private BrightnessData getBrightnessData(double brightnessPercentage, boolean doBrightnessChange) {

        if (doBrightnessChange) {
            setPreviousBrightness(brightnessPercentage);
        }

        return new BrightnessData(brightnessPercentage, doBrightnessChange);
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
