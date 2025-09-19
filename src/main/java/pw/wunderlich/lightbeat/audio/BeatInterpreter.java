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
 * The logic is independent of the audio sample size or the frequency of updates.
 * It uses a time-based rolling average to establish a dynamic beat detection threshold.
 */
class BeatInterpreter {

    private static final Logger logger = LoggerFactory.getLogger(BeatInterpreter.class);

    // The time window in milliseconds for calculating the local average amplitude.
    private static final long AVERAGE_WINDOW_MS = 3000L;
    // Base multiplier for how quickly the beat threshold decays towards the average.
    private static final double BEAT_THRESHOLD_DECAY_RATE_BASE = 0.005d;
    // Timeouts for special events.
    private static final long NO_BEAT_RECEIVED_MILLIS = 2000L;
    private static final long SILENCE_MILLIS = 1000L;

    private final Config config;

    private final Queue<AmplitudeSample> amplitudeHistory = new ArrayDeque<>();
    private double beatThreshold;
    private boolean isSilent = true;

    private final TimeThreshold nextBeatThreshold = new TimeThreshold(1000L); // Start with a 1s calibration phase
    private final TimeThreshold noBeatThreshold = new TimeThreshold();
    private final TimeThreshold silenceThreshold = new TimeThreshold();

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

        updateHistory(currentTime, amplitude);
        double average = calculateCurrentAverage();

        if (amplitude > beatThreshold && amplitude > 0d) {

            noBeatThreshold.setCurrentThreshold(NO_BEAT_RECEIVED_MILLIS);
            disableSilenceThreshold();

            beatThreshold = amplitude;

            if (nextBeatThreshold.isMet()) {
                nextBeatThreshold.setCurrentThreshold(config.getInt(ConfigNode.BEAT_MIN_TIME_BETWEEN));
                logger.info("Beat detected at {} (avg: {})", String.format("%.4f", amplitude), String.format("%.4f", average));
                return new BeatEvent(amplitude, average);
            }

        } else {

            if (average < beatThreshold) {
                double difference = beatThreshold - average;
                double beatThresholdReductionMultiplier = config.getInt(ConfigNode.BEAT_SENSITIVITY) * BEAT_THRESHOLD_DECAY_RATE_BASE;
                beatThreshold -= difference * beatThresholdReductionMultiplier;
            }

            if (amplitude == 0d) {
                if (silenceThreshold.isEnabled()) {
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
            } else {
                disableSilenceThreshold();
                if (noBeatThreshold.isMet()) {
                    noBeatThreshold.disable();
                    logger.info("No beat detected");
                    return new BeatEvent(average);
                }
            }
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
}
