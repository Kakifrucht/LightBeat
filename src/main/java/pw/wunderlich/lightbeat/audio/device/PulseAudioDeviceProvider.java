package pw.wunderlich.lightbeat.audio.device;

import org.jitsi.impl.neomedia.device.AudioSystem;
import org.jitsi.impl.neomedia.device.CaptureDeviceInfo2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pw.wunderlich.lightbeat.AppTaskOrchestrator;

import javax.media.protocol.PullBufferDataSource;
import javax.media.protocol.PullBufferStream;

/**
 * Provides {@link AudioDevice}'s for PulseAudio devices on Linux.
 * This provider discovers both standard capture devices (microphones)
 * and loopback devices (monitors of output sinks).
 */
public class PulseAudioDeviceProvider extends BaseJmfDeviceProvider {

    private static final Logger logger = LoggerFactory.getLogger(PulseAudioDeviceProvider.class);

    public static boolean isLinux() {
        String os = System.getProperty("os.name").toLowerCase();
        return (os.contains("nix") || os.contains("nux") || os.contains("aix"));
    }


    public PulseAudioDeviceProvider(AppTaskOrchestrator taskOrchestrator) {
        super(taskOrchestrator);
        if (!isLinux()) {
            throw new IllegalStateException("PulseAudio can only be used on Linux");
        }
    }

    @Override
    protected String getAudioSystemProtocol() {
        return AudioSystem.LOCATOR_PROTOCOL_PULSEAUDIO;
    }

    @Override
    protected String getAudioSystemName() {
        return "PulseAudio";
    }

    @Override
    protected AudioDevice createAudioDevice(CaptureDeviceInfo2 deviceInfo) {
        var deviceName = deviceInfo.getName();
        var finalName = deviceName.startsWith("Monitor of ") ?
                deviceInfo.getName().replace("Monitor of ", "Loopback: ")
                : deviceInfo.getName();
        return new PulseAudioDevice(deviceInfo, finalName);
    }

    /**
     * An AudioDevice implementation that uses the JMF "pull" model, suitable
     * for libjitsi's PulseAudio DataSource. It extends the BaseJmfAudioDevice
     * to inherit the common JMF lifecycle management.
     */
    class PulseAudioDevice extends BaseJmfAudioDevice {
        private PullBufferStream stream;
        private volatile boolean isRunning = false;

        public PulseAudioDevice(CaptureDeviceInfo2 deviceInfo, String deviceName) {
            super(deviceInfo, deviceName);
        }

        @Override
        protected void startReading() {
            if (!(dataSource instanceof PullBufferDataSource pullDataSource)) {
                throw new IllegalStateException("DataSource is not a PullBufferDataSource");
            }
            this.stream = pullDataSource.getStreams()[0];
            this.isRunning = true;
            scheduleRead();
        }

        @Override
        protected void stopReading() {
            this.isRunning = false;
        }

        @Override
        public boolean isOpen() {
            return super.isOpen() && stream != null;
        }

        private void scheduleRead() {
            if (isRunning) {
                taskOrchestrator.dispatch(this::performRead);
            }
        }

        private void performRead() {
            if (!isRunning) {
                return;
            }

            try {
                stream.read(jmfBuffer);
                notifyListener();
                scheduleRead();
            } catch (Exception e) {
                if (isRunning) {
                    logger.warn("Error reading from PulseAudio stream for device '{}'. Stopping device.", getName(), e);
                    stopReading();
                }
            }
        }
    }
}
