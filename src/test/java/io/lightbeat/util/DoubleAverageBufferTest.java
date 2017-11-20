package io.lightbeat.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DoubleAverageBufferTest {

    private static final int DEFAULT_BUFFER_SIZE = 5;
    private static final int FIVE = 5;
    private static final int TWO = 2;

    private DoubleAverageBuffer buffer;

    @BeforeEach
    void setup() {
        buffer = new DoubleAverageBuffer(DEFAULT_BUFFER_SIZE);
    }

    @Test
    void add() {
        buffer.add(FIVE);
        buffer.add(TWO);
        assertEquals(2, buffer.size());
    }

    @Test
    void getCurrentAverage() {
        buffer.add(FIVE);
        buffer.add(TWO);
        assertEquals((FIVE + TWO) / 2d, buffer.getCurrentAverage());
    }

    @Test
    void getMaxValue() {
        DoubleAverageBuffer buffer = new DoubleAverageBuffer(100);
        for (int i = 100; i > 0; i--) {
            buffer.add(i);
        }
        assertEquals(100d, buffer.getMaxValue());
        for (int i = 0; i > -100; i--) {
            buffer.add(i);
            assertEquals(i + 99, buffer.getMaxValue());
        }
    }

    @Test
    void size() {
        for (int i = 0; i < 100; i++) {
            buffer.add(i);
        }
        assertEquals(DEFAULT_BUFFER_SIZE, buffer.size());
    }

    @Test
    void clear() {
        for (int i = 0; i < 1000; i++) {
            buffer.add(i);
        }
        buffer.clear();
        assertEquals(0, buffer.size());
    }
}