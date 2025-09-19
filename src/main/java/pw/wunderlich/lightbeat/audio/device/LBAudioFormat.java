package pw.wunderlich.lightbeat.audio.device;

import javax.media.format.AudioFormat;

/**
 * Wrapper around different audio format implementations.
 */
public class LBAudioFormat {

    private final double sampleRate;
    private final boolean littleEndian;
    private final int channels;
    private final int bytesPerSample;


    LBAudioFormat(double sampleRate, boolean littleEndian, int channels, int bytesPerSample) {
        this.sampleRate = sampleRate;
        this.littleEndian = littleEndian;
        this.channels = channels;
        this.bytesPerSample = bytesPerSample;
    }

    public LBAudioFormat(AudioFormat format) {
        this(
                format.getSampleRate(),
                format.getEndian() == AudioFormat.LITTLE_ENDIAN,
                format.getChannels(),
                format.getSampleSizeInBits() / 8
        );
    }

    public LBAudioFormat(javax.sound.sampled.AudioFormat format) {
        this(
                format.getSampleRate(),
                !format.isBigEndian(),
                format.getChannels(),
                format.getSampleSizeInBits() / 8
        );
    }

    /**
     * @return amount of bytes one frame consist (audio sample across channels)
     */
    public int getBytesPerFrame() {
        return channels * bytesPerSample;
    }

    public double getSampleRate() {
        return sampleRate;
    }

    public boolean isLittleEndian() {
        return littleEndian;
    }

    public int getChannels() {
        return channels;
    }

    public int getBytesPerSample() {
        return bytesPerSample;
    }

    @Override
    public String toString() {
        return "LBAudioFormat{" +
                "sampleRate=" + sampleRate +
                ", littleEndian=" + littleEndian +
                ", channels=" + channels +
                ", bytesPerSample=" + bytesPerSample +
                '}';
    }
}
