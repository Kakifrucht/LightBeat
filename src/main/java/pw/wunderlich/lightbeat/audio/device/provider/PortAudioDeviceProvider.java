package pw.wunderlich.lightbeat.audio.device.provider;

import org.jitsi.impl.neomedia.device.AudioSystem;
import org.jitsi.impl.neomedia.device.CaptureDeviceInfo2;
import pw.wunderlich.lightbeat.AppTaskOrchestrator;
import pw.wunderlich.lightbeat.audio.device.AudioDevice;
import pw.wunderlich.lightbeat.audio.device.PullModelAudioDevice;

public class PortAudioDeviceProvider extends LibJitsiDeviceProvider {

    public PortAudioDeviceProvider(AppTaskOrchestrator taskOrchestrator) {
        super(taskOrchestrator);
    }

    @Override
    protected String getAudioSystemProtocol() {
        return AudioSystem.LOCATOR_PROTOCOL_PORTAUDIO;
    }

    @Override
    protected String getAudioSystemName() {
        return "PortAudio";
    }

    @Override
    protected AudioDevice createAudioDevice(CaptureDeviceInfo2 deviceInfo) {
        return new PullModelAudioDevice(executor, deviceInfo.getLocator(), getAudioSystemName() + ": " + deviceInfo.getName());
    }
}
