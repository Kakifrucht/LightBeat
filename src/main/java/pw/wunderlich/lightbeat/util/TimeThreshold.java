package pw.wunderlich.lightbeat.util;

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

    /**
     * Initialize with threshold.
     * @param initMillis time in millis until {@link #isMet()} will return true
     */
    public TimeThreshold(long initMillis) {
        currentThreshold = System.currentTimeMillis() + initMillis;
        isEnabled = true;
    }

    /**
     * @param thresholdMillis time in millis until {@link #isMet()} will return true
     */
    public void setCurrentThreshold(long thresholdMillis) {

        if (thresholdMillis < 0) {
            throw new IllegalArgumentException("Threshold must be greater than 0");
        }

        long newThreshold = System.currentTimeMillis() + thresholdMillis;
        this.currentThreshold = newThreshold < System.currentTimeMillis() ? Long.MAX_VALUE : newThreshold;
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
