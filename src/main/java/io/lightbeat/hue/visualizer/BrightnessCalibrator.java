package io.lightbeat.hue.visualizer;

import io.lightbeat.config.Config;
import io.lightbeat.config.ConfigNode;
import io.lightbeat.util.TimeThreshold;
import io.lightbeat.util.DoubleAverageBuffer;

/**
 * Dynamically calibrates the brightness level after receiving amplitudes, based on the highest
 * amplitude received. Calling {@link #getBrightness(double)} returns a {@link BrightnessData}
 * object, which contains the relevant information for the next light update, and if a brightness
 * change is needed in the first place. The first call to the method will always return {@link BrightnessData}
 * that sets the brightness to 50%, and keeps sending the same amount. Brightness only changes if difference
 * in percentage since last brightness is higher than {@link #BRIGHTNESS_CHANGE_MINIMUM_PERCENTAGE}. The last
 * brightness change must also have ocurred at least {@link #BRIGHTNESS_REDUCTION_MIN_DELAY_MILLIS} milliseconds
 * apart.
 */
class BrightnessCalibrator {

    static final double BRIGHTNESS_DIFFERENCE_PERCENTAGE_BASE = 0.055d;
    private static final double BRIGHTNESS_CHANGE_MINIMUM_PERCENTAGE = 0.2d;

    static final int CALIBRATION_SIZE = 30;
    private static final long BRIGHTNESS_REDUCTION_MIN_DELAY_MILLIS = 5000L;
    private static final int BUFFER_SIZE = 150;

    private final int brightnessMin;
    private final int brightnessRange;
    private final double brightnessFadeDifference;
    private final double brightnessFadeAndBeatThreshold;

    private double currentBrightness = 0d;

    private final TimeThreshold brightnessReductionThreshold = new TimeThreshold(0L);
    private final DoubleAverageBuffer amplitudeDifferenceHistory = new DoubleAverageBuffer(BUFFER_SIZE);


    BrightnessCalibrator(Config config) {
        this.brightnessMin = config.getInt(ConfigNode.BRIGHTNESS_MIN);
        this.brightnessRange = config.getInt(ConfigNode.BRIGHTNESS_MAX) - brightnessMin;

        double configBrightnessFadeDifference = (double) config.getInt(ConfigNode.BRIGHTNESS_FADE_DIFFERENCE);
        this.brightnessFadeDifference = configBrightnessFadeDifference * BRIGHTNESS_DIFFERENCE_PERCENTAGE_BASE;
        this.brightnessFadeAndBeatThreshold = brightnessFadeDifference * 2;
    }

    /**
     * Get the brightness for the given amplitude difference from the average amplitude.
     *
     * @param amplitudeDifference difference from average amplitude (between -1 and 1)
     * @return BrightnessData object containing all information about the brightness
     */
    BrightnessData getBrightness(double amplitudeDifference) {

        amplitudeDifferenceHistory.add(amplitudeDifference);

        // calibration phase
        if (amplitudeDifferenceHistory.size() < CALIBRATION_SIZE) {
            return getBrightnessData(0.5d);
        }

        // multiplier is calibrated in regards to the currently highest amplitude, which will set brightness to max if received
        double brightnessMultiplier = 1 / amplitudeDifferenceHistory.getMaxValue();
        double brightnessPercentage = Math.max(Math.min(amplitudeDifference * brightnessMultiplier, 1d), -1d);
        brightnessPercentage = (brightnessPercentage + 1d) / 2d;

        return getBrightnessData(brightnessPercentage);
    }

    BrightnessData getLowestBrightnessData() {
        brightnessReductionThreshold.setCurrentThreshold(0L);
        return getBrightnessData(0d);
    }

    private BrightnessData getBrightnessData(double brightnessPercentage) {

        double brightnessDifference = brightnessPercentage - currentBrightness;

        // if ceilings are reached always do brightness update if necessary
        // if reducing brightness ensure that reduction threshold is met
        boolean doBrightnessChange = (Math.abs(brightnessDifference) > BRIGHTNESS_CHANGE_MINIMUM_PERCENTAGE
                || (brightnessPercentage == 1d && currentBrightness < 1d)
                || (brightnessPercentage == 0d && currentBrightness > 0d))
                && (brightnessPercentage > currentBrightness || brightnessReductionThreshold.isMet());

        if (doBrightnessChange) {
            brightnessReductionThreshold.setCurrentThreshold(BRIGHTNESS_REDUCTION_MIN_DELAY_MILLIS);
            currentBrightness = brightnessPercentage;
        }

        return new BrightnessData(currentBrightness, doBrightnessChange);
    }


    class BrightnessData {

        private final double brightnessPercentage;
        private final boolean doBrightnessChange;

        private final int brightnessFade;
        private final int brightness;


        private BrightnessData(double brightnessPercentage, boolean doBrightnessChange) {

            this.brightnessPercentage = brightnessPercentage;
            this.doBrightnessChange = doBrightnessChange;

            double brightnessPercentageLow = Math.max(brightnessPercentage - brightnessFadeDifference, 0d);
            double brightnessPercentageHigh = Math.min(brightnessPercentage + brightnessFadeDifference, 1d);

            brightnessPercentageLow = Math.min(brightnessPercentageLow, 1d - brightnessFadeAndBeatThreshold);
            brightnessPercentageHigh = Math.max(brightnessPercentageHigh, brightnessFadeAndBeatThreshold);

            this.brightnessFade = (int) Math.round(brightnessRange * brightnessPercentageLow) + brightnessMin;
            this.brightness = (int) Math.round(brightnessRange * brightnessPercentageHigh) + brightnessMin;
        }

        double getBrightnessPercentage() {
            return brightnessPercentage;
        }

        boolean isBrightnessChange() {
            return doBrightnessChange;
        }

        int getBrightnessFade() {
            return brightnessFade;
        }

        int getBrightness() {
            return brightness;
        }
    }
}
