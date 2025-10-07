package pw.wunderlich.lightbeat.audio.device.provider;

import org.jitsi.impl.neomedia.device.AudioSystem;
import org.jitsi.impl.neomedia.device.CaptureDeviceInfo2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pw.wunderlich.lightbeat.AppTaskOrchestrator;
import pw.wunderlich.lightbeat.audio.device.AudioDevice;
import pw.wunderlich.lightbeat.audio.device.PullModelAudioDevice;

/**
 * Provides {@link AudioDevice}'s for PulseAudio devices on Linux.
 * This provider discovers both standard capture devices (microphones)
 * and loopback devices (monitors of output sinks).
 */
public class PulseAudioDeviceProvider extends LibJitsiDeviceProvider {

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
        return new PullModelAudioDevice(executor, deviceInfo.getLocator(), finalName);
    }
}
