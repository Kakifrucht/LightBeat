package io.lightbeat.util;

/**
 * Helper class offering methods to compare time easily and check if a given time threshold has been passed.
 * Can be disabled with {@link #disable()}, in which case {@link #isMet()} will always return false.
 * Will be enabled if threshold is set with {@link #setCurrentThreshold(long)} or initialized with threshold.
 */
public class TimeThreshold {

    private long currentThreshold;
    private boolean isEnabled;


    public TimeThreshold() {
        currentThreshold = Long.MAX_VALUE;
        isEnabled = false;
    }

    public TimeThreshold(long init) {
        currentThreshold = System.currentTimeMillis() + init;
        isEnabled = true;
    }

    public void setCurrentThreshold(long currentThreshold) {
        this.currentThreshold = System.currentTimeMillis() + currentThreshold;
        isEnabled = true;
    }

    public void disable() {
        if (isEnabled) {
            isEnabled = false;
        }
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public boolean isMet() {
        return isEnabled() && currentThreshold <= System.currentTimeMillis();
    }
}
