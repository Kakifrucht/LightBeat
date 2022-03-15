package io.lightbeat.util;

import java.util.Arrays;

/**
 * Efficient ring buffer that calculates the average directly (O(1)) and can
 * determine the max element in the buffer.
 */
public class DoubleAverageBuffer {

    private final double[] ringBuffer;
    private final boolean determineMax;

    private int headIndex;
    private int size;

    private double currentTotal;
    private double currentMax = -Double.MAX_VALUE;
    private int currentMaxIndex = -1;


    public DoubleAverageBuffer(int size) {
        this(size, true);
    }

    public DoubleAverageBuffer(int size, boolean determineMax) {
        ringBuffer = new double[size];
        this.determineMax = determineMax;
    }

    public void add(double toAdd) {

        double toRemove = ringBuffer[headIndex];
        currentTotal -= toRemove;

        ringBuffer[headIndex] = toAdd;
        currentTotal += toAdd;

        if (determineMax) {
            if (headIndex == currentMaxIndex) {
                currentMax = -Double.MAX_VALUE;
                for (int i = 0; i < ringBuffer.length; i++) {
                    if (ringBuffer[i] > currentMax) {
                        currentMax = ringBuffer[i];
                        currentMaxIndex = i;
                    }
                }
            }

            if (toAdd > currentMax) {
                currentMax = toAdd;
                currentMaxIndex = headIndex;
            }
        }

        if (size < ringBuffer.length) {
            size++;
        }

        if (++headIndex >= ringBuffer.length) {
            headIndex = 0;
        }
    }

    public double[] getBuffer() {
        return ringBuffer;
    }

    public double getCurrentAverage() {
        return currentTotal / size;
    }

    public double getMaxValue() {
        if (!determineMax) {
            throw new UnsupportedOperationException("Determination of max value is set disabled");
        }
        return currentMax;
    }

    public int size() {
        return size;
    }

    public boolean isFull() {
        return size == ringBuffer.length;
    }

    public void clear() {
        Arrays.fill(ringBuffer, 0.0d);
        headIndex = 0;
        size = 0;
        currentTotal = 0d;
        currentMax = -Double.MAX_VALUE;
        currentMaxIndex = -1;
    }
}
