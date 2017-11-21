package io.lightbeat.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class TimeThresholdTest {

    private TimeThreshold timeThreshold;


    @BeforeEach
    void setup() {
        timeThreshold = new TimeThreshold(0L);
    }

    @Test
    void setCurrentThresholdIsMet() {
        timeThreshold.setCurrentThreshold(Long.MAX_VALUE);
        assertFalse(timeThreshold.isMet());
        timeThreshold.setCurrentThreshold(0L);
        assertTrue(timeThreshold.isMet());
        try {
            timeThreshold.setCurrentThreshold(-1);
            fail("No exception was thrown");
        } catch (Exception e) {
            assertTrue(e.getClass().equals(IllegalArgumentException.class));
        }
    }

    @Test
    void disableIsEnabled() {
        assertTrue(timeThreshold.isEnabled());
        timeThreshold.disable();
        assertTrue(!timeThreshold.isEnabled());
    }
}
