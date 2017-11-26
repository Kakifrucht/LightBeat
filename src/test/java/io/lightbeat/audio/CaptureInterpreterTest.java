package io.lightbeat.audio;

import io.lightbeat.config.Config;
import io.lightbeat.config.ConfigNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CaptureInterpreterTest {

    @Test
    void interpretValue() {

        Config config = mock(Config.class);
        when(config.getInt(ConfigNode.BEAT_SENSITIVITY)).thenReturn(5);
        when(config.getInt(ConfigNode.BEAT_MIN_TIME_BETWEEN)).thenReturn(200);

        CaptureInterpreter captureInterpreter = new CaptureInterpreter(config);

        BeatEvent event = captureInterpreter.interpretValue(0d);
        assertNull(event);
        event = captureInterpreter.interpretValue(0.1d);
        assertNull(event); // calibration phase
        try {
            // pass calibration phase
            Thread.sleep(1000L);
        } catch (InterruptedException ignored) {}

        BeatEvent event2 = captureInterpreter.interpretValue(1d);
        assertAll("captureInterpreter",
                () -> assertNotNull(event2),
                () -> assertEquals(1d, event2.getTriggeringAmplitude()));

        BeatEvent event3 = captureInterpreter.interpretValue(0.3d);
        assertNull(event3);
    }
}
