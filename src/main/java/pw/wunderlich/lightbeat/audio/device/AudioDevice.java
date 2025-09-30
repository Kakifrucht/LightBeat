package pw.wunderlich.lightbeat.audio.device;

/**
 * An audio device that pushes data to a registered listener.
 * Audio devices have a {@link #getName() name} and a fixed {@link #getAudioFormat() audio format}.
 * The user must register a listener via {@link #setAudioListener(AudioDataListener)} and then
 * call {@link #start()} to begin receiving data.
 */
public interface AudioDevice {

    /**
     * A listener that receives audio data from a device.
     */
    @FunctionalInterface
    interface AudioDataListener {
        /**
         * Called when new audio data is available from the device.
         *
         * @param data   A byte array containing the audio data. This buffer may be reused by the device,
         *               so a copy should be made if the data needs to be stored.
         * @param length The number of valid bytes in the data array.
         */
        void onDataAvailable(byte[] data, int length);
    }

    String getName();

    LBAudioFormat getAudioFormat();

    /**
     * Registers a listener to receive audio data.
     * Set to null to unregister.
     *
     * @param listener The listener to be called when audio data is available.
     */
    void setAudioListener(AudioDataListener listener);

    /**
     * Starts this audio device, which will begin capturing audio and invoking the registered listener.
     *
     * @return true if the audio device was started successfully.
     * @see #stop()
     */
    boolean start();

    /**
     * Indicates whether the audio device is currently started and capturing audio.
     *
     * @return true if the device is open and running.
     */
    boolean isOpen();

    /**
     * Stops this audio device, closing the stream and freeing its resources.
     * The registered listener will no longer be called.
     *
     * @return true if the device was successfully stopped.
     * @see #start()
     */
    boolean stop();
}
