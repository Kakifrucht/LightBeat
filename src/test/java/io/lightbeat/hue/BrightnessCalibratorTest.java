package io.lightbeat.hue;

import io.lightbeat.config.Config;
import io.lightbeat.config.ConfigNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BrightnessCalibratorTest {

    private static final int MAX_BRIGHTNESS = 254;
    private static final int MEDIAN_BRIGHTNESS = MAX_BRIGHTNESS / 2;

    private BrightnessCalibrator calibrator;

    @BeforeEach
    void setUp() {
        Config config = mock(Config.class);
        when(config.getInt(ConfigNode.BRIGHTNESS_MIN)).thenReturn(0);
        when(config.getInt(ConfigNode.BRIGHTNESS_MAX)).thenReturn(254);
        when(config.getInt(ConfigNode.BRIGHTNESS_SENSITIVITY)).thenReturn(20);
        calibrator = new BrightnessCalibrator(config);
    }

    @Test
    void getBrightness() {

        BrightnessCalibrator.BrightnessData data;

        // test calibration phase
        for (int i = 0; i < 9; i++) {
            data = calibrator.getBrightness(i < 8 ? 0d : 1d);
            assertEquals(MEDIAN_BRIGHTNESS, data.getBrightness());
            assertEquals(MEDIAN_BRIGHTNESS / 2, data.getBrightnessLow());
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
        assertEquals(MAX_BRIGHTNESS / 2, data.getBrightnessLow());

        // can't directly change back to low, due to time threshold
        data = calibrator.getBrightness(-1d);
        assertEquals(MAX_BRIGHTNESS, data.getBrightness());
    }

    @Test
    void getLowestBrightnessData() {
        BrightnessCalibrator.BrightnessData data = calibrator.getLowestBrightnessData();
        assertAll("lowestData",
                () -> assertEquals(false, data.isBrightnessChange()),
                () -> assertEquals(0, data.getBrightness())
        );

        calibrator.getBrightness(1d);
        assertEquals(true, calibrator.getLowestBrightnessData().isBrightnessChange());
    }
}
