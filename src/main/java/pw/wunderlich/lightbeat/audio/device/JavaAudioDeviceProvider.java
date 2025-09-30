package pw.wunderlich.lightbeat.audio.device;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pw.wunderlich.lightbeat.AppTaskOrchestrator;

import javax.sound.sampled.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Provides {@link AudioDevice}'s via Java audio API.
 */
public class JavaAudioDeviceProvider implements DeviceProvider {

    private static final float SAMPLE_RATE = 44100f;
    private static final int BYTES_PER_SAMPLE = 2;

    private static final Logger logger = LoggerFactory.getLogger(JavaAudioDeviceProvider.class);

    private final AudioFormat format = new AudioFormat(SAMPLE_RATE, BYTES_PER_SAMPLE * 8, 1, true, false);
    private final Line.Info lineInfo = new Line.Info(TargetDataLine.class);
    private final AppTaskOrchestrator taskOrchestrator;


    public JavaAudioDeviceProvider(AppTaskOrchestrator taskOrchestrator) {
        this.taskOrchestrator = taskOrchestrator;
    }

    @Override
    public List<AudioDevice> getAudioDevices() {
        return Arrays.stream(AudioSystem.getMixerInfo())
                .map(AudioSystem::getMixer)
                .filter(mixer -> mixer.isLineSupported(lineInfo))
                .map(JavaAudioDevice::new)
                .collect(Collectors.toList());
    }


    private class JavaAudioDevice implements AudioDevice {

        private static final int POLL_BUFFER = 4096;
        private static final int POLL_INTERVAL_MS = 5;

        private final Mixer mixer;
        private TargetDataLine dataLine;
        private LBAudioFormat audioFormat;
        private AudioDataListener listener;
        private Future<?> captureFuture;
        private volatile boolean isRunning = false;
        private final AtomicBoolean isPolling = new AtomicBoolean(false);
        private final byte[] pollBuffer = new byte[POLL_BUFFER];

        public JavaAudioDevice(Mixer mixer) {
            this.mixer = mixer;
        }

        @Override
        public String getName() {
            return mixer.getMixerInfo().getName();
        }

        @Override
        public LBAudioFormat getAudioFormat() {
            return audioFormat;
        }

        @Override
        public void setAudioListener(AudioDataListener listener) {
            this.listener = listener;
        }

        @Override
        public boolean start() {
            if (isRunning) {
                return false;
            }

            try {
                dataLine = (TargetDataLine) mixer.getLine(lineInfo);
                dataLine.open(format, dataLine.getBufferSize());
                audioFormat = new LBAudioFormat(format);
            } catch (LineUnavailableException e) {
                dataLine = null;
                logger.warn("Could not open audio line for mixer '{}'", getName(), e);
                return false;
            }

            isRunning = true;
            dataLine.start();
            captureFuture = taskOrchestrator.schedulePeriodicTask(this::poll, 0, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);

            logger.info("Started JavaSound device: {}", getName());
            return true;
        }

        private void poll() {
            // Skip if already polling or not running
            if (!isRunning || !isPolling.compareAndSet(false, true)) {
                return;
            }

            try {
                int available = dataLine.available();

                if (available > 0) {
                    int toRead = Math.min(available, pollBuffer.length);
                    int bytesRead = dataLine.read(pollBuffer, 0, toRead);
                    if (bytesRead > 0 && listener != null) {
                        listener.onDataAvailable(pollBuffer, bytesRead);
                    }
                }
            } catch (Exception e) {
                if (isRunning) {
                    logger.error("Error during audio polling for device: {}. Stopping.", getName(), e);
                    stop();
                }
            } finally {
                isPolling.set(false);
            }
        }

        @Override
        public boolean isOpen() {
            return (isRunning && dataLine != null && dataLine.isOpen()) || !captureFuture.isDone();
        }

        @Override
        public boolean stop() {
            if (!isRunning) {
                return false;
            }

            isRunning = false;

            if (captureFuture != null && !captureFuture.isDone()) {
                captureFuture.cancel(true);
            }

            if (dataLine != null) {
                dataLine.stop();
                dataLine.close();
            }

            dataLine = null;
            captureFuture = null;
            logger.info("Stopped JavaSound device: {}", getName());
            return true;
        }
    }
}
