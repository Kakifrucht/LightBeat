package pw.wunderlich.lightbeat.audio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pw.wunderlich.lightbeat.config.Config;
import pw.wunderlich.lightbeat.config.ConfigNode;
import pw.wunderlich.lightbeat.util.DoubleAverageBuffer;
import pw.wunderlich.lightbeat.util.TimeThreshold;

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

    private final DoubleAverageBuffer amplitudeHistory;
    private boolean isSilent = true;

    private final TimeThreshold noBeatThreshold = new TimeThreshold();
    private final TimeThreshold silenceThreshold = new TimeThreshold();

    private double peakGateThreshold = 0d;
    private long lastUpdateTime = 0L;


    BeatInterpreter(Config config, int updatesPerSecond) {
        this.config = config;
        this.amplitudeHistory = new DoubleAverageBuffer((int) (AVERAGE_WINDOW_MS / 1000 * updatesPerSecond), false);
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

        amplitudeHistory.add(amplitude);
        double average = amplitudeHistory.getCurrentAverage();

        double normalizedSensitivity = (config.getInt(ConfigNode.BEAT_SENSITIVITY) - 1) / 9d;
        double beatMultiplier = MAX_MULTIPLIER - (normalizedSensitivity * (MAX_MULTIPLIER - MIN_MULTIPLIER));
        double dynamicThreshold = average * beatMultiplier;

        if (amplitude > dynamicThreshold && amplitude > peakGateThreshold) {
            noBeatThreshold.setCurrentThreshold(NO_BEAT_RECEIVED_MILLIS);
            disableSilenceThreshold();

            peakGateThreshold = amplitude * PEAK_DECAY_MULTIPLIER;
            logger.info("Beat detected at {} (avg {}, dynThresh: {}, peakThresh: {})",
                    fD(amplitude), fD(average), fD(dynamicThreshold), fD(peakGateThreshold));
            return new BeatEvent(amplitude, average);
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

    private void disableSilenceThreshold() {
        silenceThreshold.disable();
        isSilent = false;
    }

    private static String fD(double value) {
        return String.format("%.4f", value);
    }
}
