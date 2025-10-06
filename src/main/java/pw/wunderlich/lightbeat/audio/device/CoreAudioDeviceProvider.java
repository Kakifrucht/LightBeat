package pw.wunderlich.lightbeat.audio.device;

import org.jitsi.impl.neomedia.device.AudioSystem;
import org.jitsi.impl.neomedia.device.CaptureDeviceInfo2;
import pw.wunderlich.lightbeat.AppTaskOrchestrator;

/**
 * Provides {@link AudioDevice}'s for CoreAudio devices on macOS.
 * This provider discovers all available input devices, including virtual
 * ones like BlackHole which can be used for loopback.
 */
public class CoreAudioDeviceProvider extends BaseJmfDeviceProvider {

    public static boolean isMac() {
        return System.getProperty("os.name").toLowerCase().contains("mac");
    }


    public CoreAudioDeviceProvider(AppTaskOrchestrator taskOrchestrator) {
        super(taskOrchestrator);
        if (!isMac()) {
            throw new IllegalStateException("CoreAudio can only be used on macOS");
        }
    }

    @Override
    protected String getAudioSystemProtocol() {
        return AudioSystem.LOCATOR_PROTOCOL_MACCOREAUDIO;
    }

    @Override
    protected String getAudioSystemName() {
        return "CoreAudio";
    }

    @Override
    protected AudioDevice createAudioDevice(CaptureDeviceInfo2 deviceInfo) {
        return new PushModelAudioDevice(deviceInfo, deviceInfo.getName());
    }
}
