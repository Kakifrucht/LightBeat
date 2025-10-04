package pw.wunderlich.lightbeat.hue.visualizer;

import pw.wunderlich.lightbeat.config.Config;
import pw.wunderlich.lightbeat.config.ConfigNode;
import pw.wunderlich.lightbeat.util.DoubleAverageBuffer;

/**
 * Dynamically calibrates the brightness level after receiving amplitudes, based on the highest
 * amplitude received. Calling {@link #getBrightness(double)} returns a {@link BrightnessData}
 * object, which contains the relevant information for the next light update, and if a brightness
 * change is needed in the first place. The first call to the method will always return {@link BrightnessData}
 * that sets the brightness to 50%, and keeps sending the same amount. Brightness only changes if difference
 * in percentage since last brightness is higher than {@link #BRIGHTNESS_CHANGE_MINIMUM_PERCENTAGE}.
 */
class BrightnessCalibrator {

    static final double BRIGHTNESS_DIFFERENCE_PERCENTAGE_BASE = 0.04d;
    /**
     * Determined brightness values will only change if the new brightness value is at least this amount higher/lower
     * than the given minimum percentage difference.
     */
    private static final double BRIGHTNESS_CHANGE_MINIMUM_PERCENTAGE = 0.2d;

    static final int CALIBRATION_SIZE = 30;
    /**
     * The buffer size should keep data that is around one and two minutes old, at avg 125 bpm.
     */
    private static final int BUFFER_SIZE = 150;

    private final Config config;

    private int brightnessMin = -1;
    private int brightnessRange = -1;

    private double brightnessFadeDifference = -1d;
    private double brightnessHighestFade;
    private double brightnessLowestBeat;

    private double currentBrightnessPercentage = 0d;

    private final DoubleAverageBuffer amplitudeDifferenceHistory = new DoubleAverageBuffer(BUFFER_SIZE);


    BrightnessCalibrator(Config config) {
        this.config = config;
        updateConfigValues();
    }

    private boolean updateConfigValues() {
        int newBrightnessMin = config.getInt(ConfigNode.BRIGHTNESS_MIN);
        int newBrightnessMax = config.getInt(ConfigNode.BRIGHTNESS_MAX);
        int newFadeDifference = config.getInt(ConfigNode.BRIGHTNESS_FADE_DIFFERENCE);
        int newBrightnessRange = newBrightnessMax - newBrightnessMin;
        double newBrightnessFadeDifference = newFadeDifference * BRIGHTNESS_DIFFERENCE_PERCENTAGE_BASE;
        // Check if any of the configuration values have changed
        if (this.brightnessMin != newBrightnessMin
                || this.brightnessRange != newBrightnessRange
                || this.brightnessFadeDifference != newBrightnessFadeDifference) {
            this.brightnessMin = newBrightnessMin;
            this.brightnessRange = newBrightnessRange;
            this.brightnessFadeDifference = newBrightnessFadeDifference;
            this.brightnessLowestBeat = newBrightnessFadeDifference * 2;
            this.brightnessHighestFade = 1d - brightnessLowestBeat;
            return true;
        }
        return false;
    }

    /**
     * Get the brightness for the given amplitude difference from the average amplitude.
     *
     * @param amplitudeDifference difference from average amplitude (between -1 and 1)
     * @return BrightnessData object containing all information about the brightness
     */
    BrightnessData getBrightness(double amplitudeDifference) {

        // if config was changed, determine new values and force a brightness change
        boolean forceBrightnessChange = updateConfigValues();

        amplitudeDifferenceHistory.add(amplitudeDifference);

        // calibration phase
        if (amplitudeDifferenceHistory.size() < CALIBRATION_SIZE) {
            return getBrightnessData(0.5d, forceBrightnessChange);
        }

        // brightness percentage is determined in regard to the max value in history, if current difference
        // is as high as the max value set brightness to 100%
        double brightnessMultiplier = 1 / amplitudeDifferenceHistory.getMaxValue();
        double brightnessPercentage = Math.max(Math.min(amplitudeDifference * brightnessMultiplier, 1d), -1d);
        // normalize value between -1 and 1 to 0 and 1
        brightnessPercentage = (brightnessPercentage + 1d) / 2d;

        return getBrightnessData(brightnessPercentage, forceBrightnessChange);
    }

    BrightnessData getLowestBrightnessData() {
        return getBrightnessData(0d, false);
    }

    void clearHistory() {
        this.amplitudeDifferenceHistory.clear();
    }

    private BrightnessData getBrightnessData(double brightnessPercentage, boolean forceBrightnessChange) {

        double brightnessDifference = brightnessPercentage - currentBrightnessPercentage;

        // if ceilings are reached always do brightness update if necessary,
        // if reducing brightness ensure that reduction threshold is met
        boolean doBrightnessChange = forceBrightnessChange
                || (Math.abs(brightnessDifference) > BRIGHTNESS_CHANGE_MINIMUM_PERCENTAGE
                || (brightnessPercentage == 1d && currentBrightnessPercentage < 1d)
                || (brightnessPercentage == 0d && currentBrightnessPercentage > 0d));

        if (doBrightnessChange) {
            currentBrightnessPercentage = brightnessPercentage;
        }

        return new BrightnessData(currentBrightnessPercentage, doBrightnessChange);
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
            brightnessPercentageLow = Math.min(brightnessPercentageLow, brightnessHighestFade);
            this.brightnessFade = (int) Math.round(brightnessRange * brightnessPercentageLow) + brightnessMin;

            double brightnessPercentageHigh = Math.min(brightnessPercentage + brightnessFadeDifference, 1d);
            brightnessPercentageHigh = Math.max(brightnessPercentageHigh, brightnessLowestBeat);
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
