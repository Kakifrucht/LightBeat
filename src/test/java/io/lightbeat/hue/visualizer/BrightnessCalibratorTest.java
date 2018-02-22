package io.lightbeat.hue.visualizer;

import io.lightbeat.config.Config;
import io.lightbeat.config.ConfigNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BrightnessCalibratorTest {

    private static final int MIN_BRIGHTNESS = 0;
    private static final int MAX_BRIGHTNESS = 254;
    private static final int MEDIAN_BRIGHTNESS = MAX_BRIGHTNESS / 2;
    private static final int FADE_DIFFERENCE_BRIGHTNESS = 5;

    private static int minBeatBrightness;
    private static int maxFadeBrightness;

    private BrightnessCalibrator calibrator;


    @BeforeAll
    static void setUpAll() {
        // determine brightness min/max expected brightness values
        double multiplier = BrightnessCalibrator.BRIGHTNESS_DIFFERENCE_PERCENTAGE_BASE * FADE_DIFFERENCE_BRIGHTNESS * 2;
        minBeatBrightness = (int) Math.round(multiplier * MAX_BRIGHTNESS);
        maxFadeBrightness = (int) (MAX_BRIGHTNESS - Math.round(multiplier * MAX_BRIGHTNESS));
    }

    @BeforeEach
    void setUp() {
        Config config = mock(Config.class);

        when(config.getInt(ConfigNode.BRIGHTNESS_MIN)).thenReturn(MIN_BRIGHTNESS);
        when(config.getInt(ConfigNode.BRIGHTNESS_MAX)).thenReturn(MAX_BRIGHTNESS);
        when(config.getInt(ConfigNode.BRIGHTNESS_FADE_DIFFERENCE)).thenReturn(FADE_DIFFERENCE_BRIGHTNESS);

        calibrator = new BrightnessCalibrator(config);
    }

    @Test
    void getBrightness() {

        BrightnessCalibrator.BrightnessData data;

        // test calibration phase (brightness at 50%)
        for (int i = 0; i < 9; i++) {
            data = calibrator.getBrightness(i < 8 ? 0d : 1d);
            assertEquals(MEDIAN_BRIGHTNESS, getAverageBrightness(data));
        }

        // verify that 0d as difference gives us median brightness
        data = calibrator.getBrightness(0d);
        assertEquals(MEDIAN_BRIGHTNESS, getAverageBrightness(data));

        // test brightness reduction threshold (should not change brightness, not met yet)
        data = calibrator.getBrightness(-1d);
        assertEquals(MEDIAN_BRIGHTNESS, getAverageBrightness(data));

        // verify that brightness doesn't change with insignificant amplitude differences
        data = calibrator.getBrightness(0.1d);
        assertEquals(MEDIAN_BRIGHTNESS, getAverageBrightness(data));

        // brightness corrected upwards, difference significant enough
        data = calibrator.getBrightness(0.5d);
        assertEquals(Math.round(MAX_BRIGHTNESS * 0.749d), getAverageBrightness(data));

        // change brightness to max
        data = calibrator.getBrightness(1d);
        assertEquals(MAX_BRIGHTNESS, data.getBrightness());
        assertEquals(maxFadeBrightness, data.getBrightnessFade());

        // can't directly change back to low, due to time threshold
        data = calibrator.getBrightness(-1d);
        assertEquals(MAX_BRIGHTNESS, data.getBrightness());
    }

    @Test
    void getLowestBrightnessData() {
        BrightnessCalibrator.BrightnessData data = calibrator.getLowestBrightnessData();

        assertAll("lowestData",
                () -> assertEquals(false, data.isBrightnessChange()),
                () -> assertEquals(MIN_BRIGHTNESS, data.getBrightnessFade()),
                () -> assertEquals(minBeatBrightness, data.getBrightness())
        );

        calibrator.getBrightness(1d);
        assertEquals(true, calibrator.getLowestBrightnessData().isBrightnessChange());
    }

    private int getAverageBrightness(BrightnessCalibrator.BrightnessData data) {
        return Math.round((data.getBrightness() + data.getBrightnessFade()) / 2);
    }
}
