package pw.wunderlich.lightbeat.audio.device;

import org.jitsi.impl.neomedia.device.AudioSystem;
import org.jitsi.impl.neomedia.device.CaptureDeviceInfo2;
import org.jitsi.impl.neomedia.device.WASAPISystem;
import org.jitsi.impl.neomedia.jmfext.media.protocol.wasapi.AudioCaptureClient;
import org.jitsi.impl.neomedia.jmfext.media.protocol.wasapi.WASAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pw.wunderlich.lightbeat.AppTaskOrchestrator;

import javax.media.format.AudioFormat;
import javax.media.protocol.BufferTransferHandler;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Handler;
import java.util.stream.Collectors;

/**
 * Provides {@link AudioDevice}'s for WASAPI devices on Windows.
 * This provider discovers both standard capture devices (microphones)
 * and offers a special loopback capture for each playback device.
 */
public class WASAPIDeviceProvider extends BaseJmfDeviceProvider {

    private static final Logger logger = LoggerFactory.getLogger(WASAPIDeviceProvider.class);

    public static boolean isWindows() {
        return System.getProperty("os.name").startsWith("Windows");
    }


    public WASAPIDeviceProvider(AppTaskOrchestrator taskOrchestrator) {
        super(taskOrchestrator);
        if (!isWindows()) {
            throw new IllegalStateException("WASAPI can only be used on Windows");
        }
    }

    @Override
    protected String getAudioSystemProtocol() {
        return AudioSystem.LOCATOR_PROTOCOL_WASAPI;
    }

    @Override
    protected String getAudioSystemName() {
        return "WASAPI";
    }

    @Override
    public List<AudioDevice> getAudioDevices() {
        if (audioSystem == null) {
            return Collections.emptyList();
        }

        var captureDevices = getDevices(AudioSystem.DataFlow.CAPTURE);
        var loopbackDevices = getDevices(AudioSystem.DataFlow.PLAYBACK);
        logger.info("Found {} WASAPI capture devices, created {} WASAPI loopback devices.", captureDevices.size(), loopbackDevices.size());

        var allDevices = new ArrayList<>(captureDevices);
        allDevices.addAll(loopbackDevices);
        return allDevices;
    }

    @Override
    protected AudioDevice createAudioDevice(CaptureDeviceInfo2 deviceInfo) {
        return new PushModelAudioDevice(deviceInfo, deviceInfo.getName());
    }

    private List<AudioDevice> getDevices(AudioSystem.DataFlow dataFlow) {
        return audioSystem.getDevices(dataFlow)
                .stream()
                .map(deviceInfo -> {
                    if (dataFlow.equals(AudioSystem.DataFlow.CAPTURE)) {
                        return createAudioDevice(deviceInfo);
                    } else {
                        return new WASAPILoopbackAudioDevice(deviceInfo);
                    }
                })
                .collect(Collectors.toList());
    }

    class WASAPILoopbackAudioDevice implements AudioDevice {

        private final CaptureDeviceInfo2 device;
        private AudioCaptureClient client;
        private LBAudioFormat format;
        private AudioDataListener listener;

        private Field availableField;
        private java.util.logging.Logger julLogger;
        private Handler deviceInvalidatedHandler;

        private boolean exceptionWasThrown = false;
        private byte[] readBuffer = new byte[4096];

        private WASAPILoopbackAudioDevice(CaptureDeviceInfo2 device) {
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
        public void setAudioListener(AudioDataListener listener) {
            this.listener = listener;
        }

        @Override
        public boolean start() {
            if (client != null) {
                return false;
            }

            BufferTransferHandler transferHandler = stream -> taskOrchestrator.dispatch(() -> {
                if (listener == null || client == null || !isOpen()) {
                    return;
                }

                try {
                    int availableBytes = (int) availableField.get(client);
                    if (availableBytes > 0) {
                        if (availableBytes > readBuffer.length) {
                            readBuffer = new byte[availableBytes];
                        }
                        int bytesRead = client.read(readBuffer, 0, availableBytes);
                        if (bytesRead > 0) {
                            listener.onDataAvailable(readBuffer, bytesRead);
                        }
                    }
                } catch (Exception e) {
                    if (isOpen()) {
                        logger.warn("Couldn't read from WASAPI audio device", e);
                        exceptionWasThrown = true;
                    }
                }
            });

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
                                    (WASAPISystem) audioSystem,
                                    device.getLocator(),
                                    AudioSystem.DataFlow.PLAYBACK,
                                    WASAPI.AUDCLNT_SHAREMODE_SHARED | WASAPI.AUDCLNT_STREAMFLAGS_LOOPBACK,
                                    1,
                                    audioFormat,
                                    transferHandler
                            );
                            this.format = new LBAudioFormat(audioFormat);
                            availableField = client.getClass().getDeclaredField("availableLength");
                            availableField.setAccessible(true);

                            exceptionWasThrown = false;
                            client.start();

                            installDeviceInvalidationWatcher();
                            logger.info("Started WASAPI loopback device: {}", getName());
                        } catch (Exception e) {
                            logger.debug("Couldn't initialize loopback with {}", audioFormat, e);
                        }
                    });

            return client != null;
        }

        @Override
        public boolean isOpen() {
            return client != null && !exceptionWasThrown;
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
            logger.info("Stopped WASAPI loopback device: {}", getName());
            return true;
        }
    }
}
