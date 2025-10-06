package pw.wunderlich.lightbeat.audio.device;

import org.jitsi.impl.neomedia.device.AudioSystem;
import org.jitsi.impl.neomedia.device.CaptureDeviceInfo2;
import org.jitsi.service.libjitsi.LibJitsi;
import org.jitsi.utils.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import pw.wunderlich.lightbeat.AppTaskOrchestrator;

import javax.media.Buffer;
import javax.media.Manager;
import javax.media.format.AudioFormat;
import javax.media.protocol.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * An abstract base class for DeviceProviders that use libjitsi.
 * This class handles the common initialization logic and provides a reusable
 * JMF-based AudioDevice implementation.
 */
public abstract class BaseJmfDeviceProvider implements DeviceProvider {

    private static final Logger logger = LoggerFactory.getLogger(BaseJmfDeviceProvider.class);
    private static final AtomicBoolean isLibJitsiInitialized = new AtomicBoolean(false);

    protected final AppTaskOrchestrator taskOrchestrator;
    protected AudioSystem audioSystem;


    public BaseJmfDeviceProvider(AppTaskOrchestrator taskOrchestrator) {
        this.taskOrchestrator = taskOrchestrator;
        initializeLibJitsi();
        if (isLibJitsiInitialized.get()) {
            try {
                this.audioSystem = AudioSystem.getAudioSystem(getAudioSystemProtocol());
                if (this.audioSystem == null) {
                    logger.warn("Could not retrieve the {} AudioSystem.", getAudioSystemName());
                } else {
                    logger.info("Successfully retrieved the {} AudioSystem.", getAudioSystemName());
                }
            } catch (Exception e) {
                logger.warn("Error getting the {} AudioSystem", getAudioSystemName(), e);
            }
        }
    }

    private void initializeLibJitsi() {
        synchronized (isLibJitsiInitialized) {
            if (!isLibJitsiInitialized.get()) {
                logger.info("Performing first-time initialization of libjitsi...");
                SLF4JBridgeHandler.removeHandlersForRootLogger();
                SLF4JBridgeHandler.install();
                try {
                    System.setProperty("net.java.sip.communicator.service.media.DISABLE_VIDEO_SUPPORT", "true");
                    LibJitsi.start();
                    AudioSystem.initializeDeviceSystems(MediaType.AUDIO);
                    isLibJitsiInitialized.set(true);
                    logger.info("libjitsi initialized successfully.");
                } catch (Exception e) {
                    logger.error("Failed to initialize libjitsi", e);
                } catch (NoClassDefFoundError e) {
                    logger.warn("libjitsi not included in .jar, skipping initialization");
                }
            }
        }
    }

    protected abstract String getAudioSystemProtocol();
    protected abstract String getAudioSystemName();

    @Override
    public List<AudioDevice> getAudioDevices() {
        if (audioSystem == null) {
            return Collections.emptyList();
        }

        List<AudioDevice> allDevices = audioSystem.getDevices(AudioSystem.DataFlow.CAPTURE)
                .stream()
                .map(this::createAudioDevice)
                .collect(Collectors.toList());

        logger.info("Found {} {} capture devices.", allDevices.size(), getAudioSystemName());
        return allDevices;
    }

    /**
     * Creates a device-specific AudioDevice instance for the given device info.
     * Subclasses should override this to return their specific device implementation.
     *
     * @param deviceInfo the capture device info
     * @return an AudioDevice instance
     */
    protected abstract AudioDevice createAudioDevice(CaptureDeviceInfo2 deviceInfo);

    /**
     * An abstract base class for JMF-based audio devices. This class handles the common
     * device lifecycle (creation, connection, start, stop, disconnect) and leaves the
     * specific data acquisition strategy (push vs. pull) to subclasses.
     */
    protected abstract static class BaseJmfAudioDevice implements AudioDevice {
        protected final CaptureDeviceInfo2 deviceInfo;
        protected final String deviceName;
        protected AudioDataListener listener;
        protected LBAudioFormat format;
        protected DataSource dataSource;
        protected final Buffer jmfBuffer = new Buffer();

        public BaseJmfAudioDevice(CaptureDeviceInfo2 deviceInfo, String deviceName) {
            this.deviceInfo = deviceInfo;
            this.deviceName = deviceName;
        }

        @Override
        public String getName() {
            return this.deviceName;
        }

        @Override
        public LBAudioFormat getAudioFormat() {
            return format;
        }

        @Override
        public void setAudioListener(AudioDataListener listener) {
            this.listener = listener;
        }

        @Override
        public boolean isOpen() {
            return dataSource != null;
        }

        @Override
        public final boolean start() {
            if (isOpen()) {
                return false;
            }
            try {
                dataSource = Manager.createDataSource(deviceInfo.getLocator());
                if (dataSource instanceof PushBufferDataSource pushDataSource) {
                    PushBufferStream stream = pushDataSource.getStreams()[0];
                    this.format = new LBAudioFormat((AudioFormat) stream.getFormat());
                } else if (dataSource instanceof PullBufferDataSource pullDataSource) {
                    PullBufferStream stream = pullDataSource.getStreams()[0];
                    this.format = new LBAudioFormat((AudioFormat) stream.getFormat());
                } else {
                    throw new IllegalStateException("DataSource is neither Push nor Pull BufferDataSource: " + dataSource.getClass().getName());
                }

                dataSource.connect();
                dataSource.start();

                startReading();

                logger.info("Started JMF device: {}", getName());
                return true;
            } catch (Exception e) {
                logger.error("Failed to start JMF device: {}", getName(), e);
                if (dataSource != null) {
                    try { dataSource.stop(); } catch (Exception ignored) {}
                    try { dataSource.disconnect(); } catch (Exception ignored) {}
                }
                dataSource = null;
                return false;
            }
        }

        protected abstract void startReading();
        protected abstract void stopReading();

        protected void notifyListener() {
            if (listener != null) {
                byte[] data = (byte[]) jmfBuffer.getData();
                int offset = jmfBuffer.getOffset();
                int length = jmfBuffer.getLength();

                if (length > 0) {
                    byte[] dataCopy = new byte[length];
                    System.arraycopy(data, offset, dataCopy, 0, length);
                    listener.onDataAvailable(dataCopy, length);
                }
            }
        }

        @Override
        public final boolean stop() {
            if (!isOpen()) {
                return false;
            }
            try {
                stopReading();
                dataSource.stop();
                dataSource.disconnect();
                logger.info("Stopped JMF device: {}", getName());
            } catch (Exception e) {
                logger.warn("Error while stopping JMF device '{}'", getName(), e);
            } finally {
                dataSource = null;
            }
            return true;
        }
    }

    /**
     * An AudioDevice implementation that uses the JMF "push" model.
     * It receives data passively via a BufferTransferHandler callback.
     */
    protected class PushModelAudioDevice extends BaseJmfAudioDevice {
        private PushBufferStream stream;

        public PushModelAudioDevice(CaptureDeviceInfo2 deviceInfo, String deviceName) {
            super(deviceInfo, deviceName);
        }

        @Override
        protected void startReading() {
            if (!(dataSource instanceof PushBufferDataSource pushDataSource)) {
                throw new IllegalStateException("DataSource is not a PushBufferDataSource");
            }
            this.stream = pushDataSource.getStreams()[0];
            
            BufferTransferHandler transferHandler = (pushBufferStream) -> taskOrchestrator.dispatch(() -> {
                if (listener == null || !isOpen()) return;
                try {
                    pushBufferStream.read(jmfBuffer);
                    notifyListener();
                } catch (Exception e) {
                    logger.warn("Error reading from JMF capture stream for device '{}'", getName(), e);
                }
            });
            stream.setTransferHandler(transferHandler);
        }

        @Override
        protected void stopReading() {
            if (stream != null) {
                stream.setTransferHandler(null);
            }
        }

        @Override
        public boolean isOpen() {
            return super.isOpen() && stream != null;
        }
    }
}
