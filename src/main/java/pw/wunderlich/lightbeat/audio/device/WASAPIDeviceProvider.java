package pw.wunderlich.lightbeat.audio.device;

import org.jitsi.impl.neomedia.device.AudioSystem;
import org.jitsi.impl.neomedia.device.CaptureDeviceInfo2;
import org.jitsi.impl.neomedia.device.WASAPISystem;
import org.jitsi.impl.neomedia.jmfext.media.protocol.wasapi.AudioCaptureClient;
import org.jitsi.impl.neomedia.jmfext.media.protocol.wasapi.WASAPI;
import org.jitsi.service.libjitsi.LibJitsi;
import org.jitsi.utils.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.media.format.AudioFormat;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Handler;
import java.util.stream.Collectors;

/**
 * Provides {@link AudioDevice}'s for WASAPI loopback devices.
 * Only supported on Windows.
 */
public class WASAPIDeviceProvider implements DeviceProvider {

    private static final Logger logger = LoggerFactory.getLogger(WASAPIDeviceProvider.class);

    public static boolean isWindows() {
        return System.getProperty("os.name").startsWith("Windows");
    }


    private WASAPISystem wasapiSystem;

    public WASAPIDeviceProvider() {
        if (!isWindows()) {
            throw new IllegalStateException("WASAPI can only be used on Windows");
        }

        logger.info("Initializing WASAPI devices...");

        // jitsi uses JUL for logging, bridge it to slf4j
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        try {
            LibJitsi.start();
            WASAPISystem.initializeDeviceSystems(MediaType.AUDIO);
            wasapiSystem = (WASAPISystem) AudioSystem.getAudioSystem(AudioSystem.LOCATOR_PROTOCOL_WASAPI);
            if (wasapiSystem == null) {
                logger.warn("Couldn't initialize libjitsi, WASAPI loopback won't be supported");
            }
        } catch (Exception e) {
            logger.warn("Couldn't initialize WASAPI devices", e);
        } catch (NoClassDefFoundError e) {
            logger.warn("libjitsi not included in .jar, skipping WASAPI init");
        }
    }

    public List<AudioDevice> getAudioDevices() {
        if (wasapiSystem == null) {
            return Collections.emptyList();
        }

        return wasapiSystem.getDevices(AudioSystem.DataFlow.PLAYBACK)
                .stream()
                .map(WASAPIAudioDevice::new)
                .collect(Collectors.toList());
    }

    class WASAPIAudioDevice implements AudioDevice {

        private final CaptureDeviceInfo2 device;

        private AudioCaptureClient client;
        private LBAudioFormat format;

        private Field availableField;
        private Field startedField;
        private Field eventHandleCmdField;
        private java.util.logging.Logger julLogger;
        private Handler deviceInvalidatedHandler;

        private boolean exceptionWasThrown = false;


        private WASAPIAudioDevice(CaptureDeviceInfo2 device) {
            this.device = device;
        }

        @Override
        public String getName() {
            return "Loopback: " + device.getName();
        }

        @Override
        public LBAudioFormat getAudioFormat() {
            return format;
        }

        @Override
        public boolean start() {

            if (client != null) {
                return false;
            }

            Arrays.stream(device.getFormats())
                    .filter(AudioFormat.class::isInstance)
                    .map(AudioFormat.class::cast)
                    .filter(f ->
                            f.getEncoding().equals(AudioFormat.LINEAR)
                                    && f.getSigned() == AudioFormat.SIGNED
                                    && f.getChannels() == 1
                                    && f.getSampleSizeInBits() / 8 <= 2
                    )
                    .forEach(audioFormat -> {
                        if (client != null) {
                            return;
                        }
                        try {
                            client = new AudioCaptureClient(
                                    wasapiSystem,
                                    device.getLocator(),
                                    AudioSystem.DataFlow.PLAYBACK,
                                    WASAPI.AUDCLNT_SHAREMODE_SHARED | WASAPI.AUDCLNT_STREAMFLAGS_LOOPBACK,
                                    1,
                                    audioFormat,
                                    stream -> {}
                            );
                            this.format = new LBAudioFormat(audioFormat);
                            availableField = client.getClass().getDeclaredField("availableLength");
                            availableField.setAccessible(true);
                            // reflect liveness-related fields
                            startedField = client.getClass().getDeclaredField("started");
                            startedField.setAccessible(true);
                            eventHandleCmdField = client.getClass().getDeclaredField("eventHandleCmd");
                            eventHandleCmdField.setAccessible(true);

                            exceptionWasThrown = false;
                            client.start();

                            installDeviceInvalidationWatcher();
                        } catch (Exception e) {
                            logger.debug("Couldn't initialize with {}", audioFormat);
                        }
                    });

            return client != null;
        }

        @Override
        public boolean isOpen() {
            return client != null
                    && !exceptionWasThrown
                    && getLastActivityTimestampMillis() > System.currentTimeMillis() - 2000L;
        }

        @Override
        public int available() {
            if (client == null) {
                return 0;
            }

            try {
                return (int) availableField.get(client);
            } catch (Exception e) {
                return 0;
            }
        }

        private long getLastActivityTimestampMillis() {
            if (client == null) return 0L;
            try {
                boolean started = (boolean) startedField.get(client);
                Object eventHandleCmd = eventHandleCmdField.get(client);
                // Consider the stream "alive" while started and the event thread is present
                return (started && eventHandleCmd != null) ? System.currentTimeMillis() : 0L;
            } catch (Exception e) {
                return 0L;
            }
        }

        private void installDeviceInvalidationWatcher() {
            try {
                uninstallDeviceInvalidationWatcher();

                julLogger = java.util.logging.Logger.getLogger(
                        "org.jitsi.impl.neomedia.jmfext.media.protocol.wasapi.AudioCaptureClient");

                deviceInvalidatedHandler = new Handler() {
                    @Override
                    public void publish(java.util.logging.LogRecord record) {
                        if (record == null) return;
                        final String msg = String.valueOf(record.getMessage());
                        final Throwable t = record.getThrown();
                        final String throwableStr = t != null ? String.valueOf(t) : "";

                        // AUDCLNT_E_DEVICE_INVALIDATED = 0x88890004
                        // Catch both GetNextPacketSize and Read error logs.
                        boolean isWasapiErrorContext =
                                msg.contains("IAudioCaptureClient_GetNextPacketSize")
                                        || msg.contains("IAudioCaptureClient_Read");
                        if (isWasapiErrorContext
                                && (msg.contains("0x88890004") || throwableStr.contains("0x88890004"))) {
                            exceptionWasThrown = true;
                        }
                    }
                    @Override public void flush() {}
                    @Override public void close() throws SecurityException {}
                };

                julLogger.addHandler(deviceInvalidatedHandler);
            } catch (Throwable ignored) {}
        }

        private void uninstallDeviceInvalidationWatcher() {
            try {
                if (julLogger != null && deviceInvalidatedHandler != null) {
                    julLogger.removeHandler(deviceInvalidatedHandler);
                }
            } catch (Throwable ignored) {
            } finally {
                deviceInvalidatedHandler = null;
                julLogger = null;
            }
        }

        @Override
        public int read(byte[] buffer, int toRead) {
            if (!isOpen()) {
                return 0;
            }

            try {
                return client.read(buffer, 0, toRead);
            } catch (IOException e) {
                logger.warn("Couldn't read from audio device", e);
                exceptionWasThrown = true;
                return 0;
            }
        }

        @Override
        public boolean stop() {
            if (client == null) {
                return false;
            }

            try {
                client.stop();
            } catch (IOException e) {
                logger.warn("Couldn't stop audio device, force closing", e);
            }

            uninstallDeviceInvalidationWatcher();

            client.close();
            client = null;
            return true;
        }
    }
}
