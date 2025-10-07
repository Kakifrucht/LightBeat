package pw.wunderlich.lightbeat.audio.device.provider;

import org.jitsi.impl.neomedia.device.AudioSystem;
import org.jitsi.impl.neomedia.device.CaptureDeviceInfo2;
import org.jitsi.service.libjitsi.LibJitsi;
import org.jitsi.utils.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import pw.wunderlich.lightbeat.audio.device.AudioDevice;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * An abstract base class for DeviceProviders that use libjitsi.
 * This class handles the common initialization logic and provides a reusable
 * JMF-based AudioDevice implementation.
 */
public abstract class LibJitsiDeviceProvider implements DeviceProvider {

    private static final Logger logger = LoggerFactory.getLogger(LibJitsiDeviceProvider.class);
    private static final AtomicBoolean isLibJitsiInitialized = new AtomicBoolean(false);

    protected final Executor executor;
    protected AudioSystem audioSystem;


    public LibJitsiDeviceProvider(Executor executor) {
        this.executor = executor;
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
}
