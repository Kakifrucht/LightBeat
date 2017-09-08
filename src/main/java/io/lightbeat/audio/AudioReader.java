package io.lightbeat.audio;

import javax.sound.sampled.Mixer;
import java.util.List;

/**
 * Implementing class is able to return a list containing all readable audio mixers of the system,
 * and supports thread dispatchment to read from a given {@link Mixer}.
 */
public interface AudioReader {

    /**
     * Returns a list of the systems supported mixers for audio data retrieval.
     *
     * @return list
     */
    List<Mixer> getSupportedMixers();

    /**
     * Starts the audio read thread on the selected mixer.
     *
     * @param mixer to read audio data from
     */
    void start(Mixer mixer);

    /**
     * Stops the audio data read thread.
     */
    void stop();
}
