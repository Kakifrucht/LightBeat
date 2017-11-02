package io.lightbeat.audio;

import javax.sound.sampled.Mixer;
import java.util.List;

/**
 * Implementing class is able to return a list containing all readable audio mixers of the system,
 * and reads audio data from a selected {@link Mixer}.
 */
public interface AudioReader {

    /**
     * Returns a list of the systems supported mixers for audio data read.
     *
     * @return list of supported mixer
     */
    List<Mixer> getSupportedMixers();

    /**
     * Start reading and interpreting audio data on the selected mixer.
     *
     * @param mixer to read audio data from
     * @return whether the audio read thread was successfully started
     */
    boolean start(Mixer mixer);

    /**
     * @return true if the audio reader is currently running
     */
    boolean isRunning();

    /**
     * Stop reading and interpreting audio data.
     */
    void stop();
}
