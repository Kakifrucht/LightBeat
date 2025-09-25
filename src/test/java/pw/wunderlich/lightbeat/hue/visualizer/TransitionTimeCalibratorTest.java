package pw.wunderlich.lightbeat.hue.visualizer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import pw.wunderlich.lightbeat.config.Config;
import pw.wunderlich.lightbeat.config.ConfigNode;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransitionTimeCalibratorTest {

    private static final int MAX_TRANSITION_TIME = 5;
    private static final long TIME_SINCE_LAST_BEAT = 10L;

    private TransitionTimeCalibrator transitionTimeCalibrator;

    @BeforeEach
    void setUp() {
        Config config = Mockito.mock(Config.class);
        Mockito.when(config.getInt(ConfigNode.BRIGHTNESS_FADE_MAX_TIME)).thenReturn(MAX_TRANSITION_TIME);
        transitionTimeCalibrator = new TransitionTimeCalibrator(config);
    }

    @Test
    void calibrationPhaseReturnsHalfOfMax() {
        for (int i = 0; i < TransitionTimeCalibrator.CALIBRATION_SIZE; i++) {
            assertEquals(MAX_TRANSITION_TIME / 2, transitionTimeCalibrator.getTransitionTime(TIME_SINCE_LAST_BEAT));
        }
    }

    @Test
    void afterCalibrationAverageReturnsRoundedHalfOfMax() {
        // advance calibrator to fill calibration entries
        for (int i = 0; i < TransitionTimeCalibrator.CALIBRATION_SIZE; i++) {
            transitionTimeCalibrator.getTransitionTime(TIME_SINCE_LAST_BEAT);
        }

        // now average in calibrator equals TIME_SINCE_LAST_BEAT, expected rounded half of max
        assertEquals(getTransitionTimeForAverage(), transitionTimeCalibrator.getTransitionTime(TIME_SINCE_LAST_BEAT));
    }

    @Test
    void returnsMaxWhenTimeIsAtLeastTwiceTheAverage() {
        // fill calibration to reach average
        for (int i = 0; i < TransitionTimeCalibrator.CALIBRATION_SIZE; i++) {
            transitionTimeCalibrator.getTransitionTime(TIME_SINCE_LAST_BEAT);
        }

        // time twice the average should yield max transition time
        assertEquals(MAX_TRANSITION_TIME, transitionTimeCalibrator.getTransitionTime(TIME_SINCE_LAST_BEAT * 2));
    }

    @Test
    void returnsMinimumForZeroTime() {
        // fill calibration phase so we are past the initial returns of max/2
        for (int i = 0; i < TransitionTimeCalibrator.CALIBRATION_SIZE; i++) {
            transitionTimeCalibrator.getTransitionTime(TIME_SINCE_LAST_BEAT);
        }

        assertEquals(TransitionTimeCalibrator.MIN_TRANSITION_TIME, transitionTimeCalibrator.getTransitionTime(0L));
    }

    @Test
    void historyMaintainsAverageAfterFilling() {
        // fill entire history with the same value
        int totalFill = TransitionTimeCalibrator.HISTORY_SIZE;
        for (int i = 0; i < totalFill; i++) {
            transitionTimeCalibrator.getTransitionTime(TIME_SINCE_LAST_BEAT);
        }

        // after history is filled, average should still be TIME_SINCE_LAST_BEAT
        assertEquals(getTransitionTimeForAverage(), transitionTimeCalibrator.getTransitionTime(TIME_SINCE_LAST_BEAT));
    }

    private double getTransitionTimeForAverage() {
        return Math.round(.5d * TransitionTimeCalibratorTest.MAX_TRANSITION_TIME);
    }
}
