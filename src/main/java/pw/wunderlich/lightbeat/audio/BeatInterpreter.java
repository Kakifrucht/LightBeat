package pw.wunderlich.lightbeat.audio;

import pw.wunderlich.lightbeat.config.Config;
import pw.wunderlich.lightbeat.config.ConfigNode;
import pw.wunderlich.lightbeat.util.TimeThreshold;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Interprets a stream of audio amplitudes (RMS) to detect beat events.
 * Uses a hybrid dual-threshold model to improve detection consistency.
 * A beat must be both relatively louder than the average and a significant fraction
 * of the last major peak's amplitude.
 */
class BeatInterpreter {

    private static final Logger logger = LoggerFactory.getLogger(BeatInterpreter.class);

    private static final long AVERAGE_WINDOW_MS = 3000L;
    private static final long NO_BEAT_RECEIVED_MILLIS = 2000L;
    private static final long SILENCE_MILLIS = 1000L;

    private static final double MAX_MULTIPLIER = 1.50; // Corresponds to sensitivity 1
    private static final double MIN_MULTIPLIER = 1.30; // Corresponds to sensitivity 10

    private static final double PEAK_DECAY_RATE_PER_MS = 0.00015;
    private static final double PEAK_DECAY_MULTIPLIER = 1.2d;

    private final Config config;

    private final Queue<AmplitudeSample> amplitudeHistory = new ArrayDeque<>();
    private boolean isSilent = true;

    private final TimeThreshold nextBeatThreshold = new TimeThreshold(1000L); // Start with a 1s calibration phase
    private final TimeThreshold noBeatThreshold = new TimeThreshold();
    private final TimeThreshold silenceThreshold = new TimeThreshold();

    private double peakGateThreshold = 0d;
    private long lastUpdateTime = 0L;

    private record AmplitudeSample(long timestamp, double value) {
    }

    BeatInterpreter(Config config) {
        this.config = config;
    }

    /**
     * Processes a new amplitude value and returns a BeatEvent if a beat, silence,
     * or no-beat timeout is detected.
     *
     * @param amplitude The new RMS amplitude value.
     * @return A BeatEvent if detected, otherwise null.
     */
    BeatEvent interpretValue(double amplitude) {
        long currentTime = System.currentTimeMillis();
        long timeDelta = (lastUpdateTime == 0) ? 0 : currentTime - lastUpdateTime;
        lastUpdateTime = currentTime;

        if (timeDelta > 0) {
            peakGateThreshold = Math.max(0, peakGateThreshold - (PEAK_DECAY_RATE_PER_MS * timeDelta));
        }

        updateHistory(currentTime, amplitude);
        double average = calculateCurrentAverage();

        double normalizedSensitivity = (config.getInt(ConfigNode.BEAT_SENSITIVITY) - 1) / 9d;
        double beatMultiplier = MAX_MULTIPLIER - (normalizedSensitivity * (MAX_MULTIPLIER - MIN_MULTIPLIER));
        double dynamicThreshold = average * beatMultiplier;

        if (amplitude > dynamicThreshold && amplitude > peakGateThreshold) {

            if (nextBeatThreshold.isMet()) {
                nextBeatThreshold.setCurrentThreshold(config.getInt(ConfigNode.BEAT_MIN_TIME_BETWEEN));
                noBeatThreshold.setCurrentThreshold(NO_BEAT_RECEIVED_MILLIS);
                disableSilenceThreshold();

                logger.info("Beat detected at {} (avg {}, dynThresh: {}, peakThresh: {})",
                        fD(amplitude), fD(average), fD(dynamicThreshold), fD(peakGateThreshold));
                return new BeatEvent(amplitude, average);
            }

            peakGateThreshold = amplitude * PEAK_DECAY_MULTIPLIER;
        }

        if (amplitude > 0d) {
            disableSilenceThreshold();
            if (noBeatThreshold.isMet()) {
                noBeatThreshold.disable();
                logger.info("No beat detected (dynThresh: {})", fD(dynamicThreshold));
                return new BeatEvent(average);
            }
        } else if (silenceThreshold.isEnabled()) {
            if (silenceThreshold.isMet()) {
                silenceThreshold.disable();
                noBeatThreshold.disable();
                isSilent = true;
                logger.info("Silence detected");
                return new BeatEvent(); // Silence event
            }
        } else if (!isSilent) {
            silenceThreshold.setCurrentThreshold(SILENCE_MILLIS);
        }

        return null;
    }

    /**
     * Adds the new sample and removes any samples older than AVERAGE_WINDOW_MS.
     */
    private void updateHistory(long currentTime, double amplitude) {
        amplitudeHistory.add(new AmplitudeSample(currentTime, amplitude));
        long cutoffTime = currentTime - AVERAGE_WINDOW_MS;
        // Remove old samples from the front of the queue
        while (!amplitudeHistory.isEmpty() && amplitudeHistory.peek().timestamp < cutoffTime) {
            amplitudeHistory.poll();
        }
    }

    private double calculateCurrentAverage() {
        if (amplitudeHistory.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (AmplitudeSample sample : amplitudeHistory) {
            sum += sample.value;
        }
        return sum / amplitudeHistory.size();
    }

    private void disableSilenceThreshold() {
        silenceThreshold.disable();
        isSilent = false;
    }

    private static String fD(double value) {
        return String.format("%.4f", value);
    }
}
