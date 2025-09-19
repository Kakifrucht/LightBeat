package io.lightbeat.audio.device;

/**
 * AudioDevice that can be read from via {@link #read(byte[], int)}.
 * Audio devices have a {@link #getName() name} and fixed {@link #getAudioFormat() audio format}.
 */
public interface AudioDevice {

    String getName();

    LBAudioFormat getAudioFormat();

    /**
     * Start this audio device to read data from it.
     * @return true if audio device was opened successfully
     * @see #stop()
     */
    boolean start();

    /**
     * Indicates whether the audio device can be read from via {@link #read(byte[], int)}.
     * @return true if device
     */
    boolean isOpen();

    /**
     * @return amount of bytes that can be read via {@link #read(byte[], int)}
     */
    int available();

    /**
     * Read data from device. Audio device must be started via {@link #start()} first.
     *
     * @param buffer byte array to put data in
     * @param toRead how much data should be read in bytes
     * @return how many bytes have been read
     */
    int read(byte[] buffer, int toRead);

    /**
     * Stop this audio device, closing the stream and freeing its resources.
     * @return true if device was successfully stopped
     * @see #start()
     */
    boolean stop();
}
