package io.lightbeat.hue.visualizer;

import io.lightbeat.config.Config;
import io.lightbeat.config.ConfigNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BrightnessCalibratorTest {

    //TODO fix test
    private static final int MIN_BRIGHTNESS = 0;
    private static final int MAX_BRIGHTNESS = 254;
    private static final int MEDIAN_BRIGHTNESS = MAX_BRIGHTNESS / 2;

    private BrightnessCalibrator calibrator;

    @BeforeEach
    void setUp() {
        Config config = mock(Config.class);
        when(config.getInt(ConfigNode.BRIGHTNESS_MIN)).thenReturn(MIN_BRIGHTNESS);
        when(config.getInt(ConfigNode.BRIGHTNESS_MAX)).thenReturn(MAX_BRIGHTNESS);
        when(config.getInt(ConfigNode.BRIGHTNESS_FADE_DIFFERENCE)).thenReturn(5);
        calibrator = new BrightnessCalibrator(config);
    }

    @Test
    void getBrightness() {

        BrightnessCalibrator.BrightnessData data;

        // test calibration phase
        for (int i = 0; i < 9; i++) {
            data = calibrator.getBrightness(i < 8 ? 0d : 1d);
            assertEquals(MEDIAN_BRIGHTNESS * 1.25d, data.getBrightness());
            assertEquals(MEDIAN_BRIGHTNESS / 2, data.getBrightnessFade());
        }

        // verify that 0d as difference gives us median brightness
        data = calibrator.getBrightness(0d);
        assertEquals(MEDIAN_BRIGHTNESS, data.getBrightness());

        // test brightness reduction threshold
        data = calibrator.getBrightness(-1d);
        assertEquals(MEDIAN_BRIGHTNESS, data.getBrightness());

        // verify that brightness doesn't change with insignificant amplitude differences
        data = calibrator.getBrightness(0.1d);
        assertEquals(MEDIAN_BRIGHTNESS, data.getBrightness());

        // brightness corrected upwards, difference significant enough
        data = calibrator.getBrightness(0.44d);
        assertEquals(182, data.getBrightness());

        // change brightness to max
        data = calibrator.getBrightness(1d);
        assertEquals(MAX_BRIGHTNESS, data.getBrightness());
        assertEquals(MAX_BRIGHTNESS / 2, data.getBrightnessFade());

        // can't directly change back to low, due to time threshold
        data = calibrator.getBrightness(-1d);
        assertEquals(MAX_BRIGHTNESS, data.getBrightness());
    }

    @Test
    void getLowestBrightnessData() {
        BrightnessCalibrator.BrightnessData data = calibrator.getLowestBrightnessData();
        assertAll("lowestData",
                () -> assertEquals(false, data.isBrightnessChange()),
                () -> assertEquals(MIN_BRIGHTNESS, data.getBrightness())
        );

        calibrator.getBrightness(1d);
        assertEquals(true, calibrator.getLowestBrightnessData().isBrightnessChange());
    }
}
