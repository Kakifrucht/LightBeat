package pw.wunderlich.lightbeat.hue.visualizer;

import pw.wunderlich.lightbeat.config.Config;
import pw.wunderlich.lightbeat.config.ConfigNode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransitionTimeCalibratorTest {

    @Test
    void getTransitionTime() {

        Config config = Mockito.mock(Config.class);
        Mockito.when(config.getInt(ConfigNode.BRIGHTNESS_FADE_MAX_TIME)).thenReturn(5);

        int maxTransitionTime = 5;
        TransitionTimeCalibrator transitionTimeCalibrator = new TransitionTimeCalibrator(config);

        long timeSinceLastBeat = 10L;

        // test calibration phase
        for (int i = 0; i < TransitionTimeCalibrator.CALIBRATION_SIZE; i++) {
            assertEquals(maxTransitionTime / 2, transitionTimeCalibrator.getTransitionTime(timeSinceLastBeat));
        }

        // average in calibrator now at timeSinceLastBeat
        assertEquals(getTransitionTimeForAverage(maxTransitionTime),  transitionTimeCalibrator.getTransitionTime(timeSinceLastBeat));
        assertEquals(maxTransitionTime, transitionTimeCalibrator.getTransitionTime((timeSinceLastBeat * 2)));
        assertEquals(TransitionTimeCalibrator.MIN_TRANSITION_TIME, transitionTimeCalibrator.getTransitionTime(0L));

        // average still at timeSinceLastBeat
        for (int i = 0; i < TransitionTimeCalibrator.HISTORY_SIZE; i++) {
            assertEquals(getTransitionTimeForAverage(maxTransitionTime), transitionTimeCalibrator.getTransitionTime(timeSinceLastBeat));
        }
    }

    private double getTransitionTimeForAverage(int maxTransitionTime) {
        return Math.round(.5d * maxTransitionTime);
    }
}
