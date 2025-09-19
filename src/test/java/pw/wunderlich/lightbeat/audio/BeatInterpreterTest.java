package pw.wunderlich.lightbeat.audio;

import pw.wunderlich.lightbeat.config.Config;
import pw.wunderlich.lightbeat.config.ConfigNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BeatInterpreterTest {

    @Test
    void interpretValue() {

        Config config = mock(Config.class);
        when(config.getInt(ConfigNode.BEAT_SENSITIVITY)).thenReturn(5);
        when(config.getInt(ConfigNode.BEAT_MIN_TIME_BETWEEN)).thenReturn(200);

        BeatInterpreter beatInterpreter = new BeatInterpreter(config);

        BeatEvent event = beatInterpreter.interpretValue(0d);
        assertNull(event);
        event = beatInterpreter.interpretValue(0.1d);
        assertNull(event); // calibration phase
        try {
            // pass calibration phase
            Thread.sleep(1000L);
        } catch (InterruptedException ignored) {}

        BeatEvent event2 = beatInterpreter.interpretValue(1d);
        assertAll("beatInterpreter",
                () -> assertNotNull(event2),
                () -> assertEquals(1d, event2.getTriggeringAmplitude()));

        BeatEvent event3 = beatInterpreter.interpretValue(0.3d);
        assertNull(event3);
    }
}
