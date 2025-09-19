package pw.wunderlich.lightbeat.audio.device;

import java.util.List;

/**
 * Implementing class provides implementations for {@link AudioDevice}'s.
 */
public interface DeviceProvider {

    /**
     * Get all audio devices managed by this device provider.
     * @return list containing all audio devices
     */
    List<AudioDevice> getAudioDevices();
}
