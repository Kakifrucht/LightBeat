package io.lightbeat.audio;

import io.lightbeat.audio.device.AudioDevice;

import java.util.List;

/**
 * Implementing class is able to return a list containing all readable audio mixers of the system,
 * and reads audio data from a selected {@link AudioDevice}.
 */
public interface AudioReader {

    /**
     * Returns a list of the systems supported devices for audio data read.
     *
     * @return list of supported audio devices
     */
    List<AudioDevice> getSupportedDevices();

    /**
     * Returns an {@link AudioDevice} matching the supplied name.
     *
     * @param name that device must match
     * @return audio device matching given name or null if no mixer does
     */
    AudioDevice getDeviceByName(String name);

    /**
     * Start reading and interpreting audio data on the selected audio device.
     *
     * @param audioDevice to read audio data from
     * @return whether the audio read thread was successfully started
     */
    boolean start(AudioDevice audioDevice);

    /**
     * @return true if the audio reader is currently opened and can be stopped via {@link #stop()}
     */
    boolean isOpen();

    /**
     * Stop reading and interpreting audio data.
     */
    void stop();
}
