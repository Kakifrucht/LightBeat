package pw.wunderlich.lightbeat.audio.device;

import org.jetbrains.annotations.NotNull;

import javax.media.format.AudioFormat;

/**
 * Wrapper around different audio format implementations.
 */
public record LBAudioFormat(double sampleRate, boolean littleEndian, int channels, int bytesPerSample) {

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

    @NotNull
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
