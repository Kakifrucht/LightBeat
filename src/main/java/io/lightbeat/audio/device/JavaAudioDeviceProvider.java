package io.lightbeat.audio.device;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides {@link AudioDevice}'s via Java audio API.
 */
public class JavaAudioDeviceProvider implements DeviceProvider {

    private static final Logger logger = LoggerFactory.getLogger(JavaAudioDeviceProvider.class);

    private final AudioFormat format = new AudioFormat(44100f, 16, 1, true, false);
    private final Line.Info lineInfo = new Line.Info(TargetDataLine.class);


    @Override
    public List<AudioDevice> getAudioDevices() {
        return Arrays.stream(AudioSystem.getMixerInfo())
                .map(AudioSystem::getMixer)
                .filter(mixer -> mixer.isLineSupported(lineInfo))
                .map(mixer -> new AudioDevice() {

                    TargetDataLine dataLine;
                    LBAudioFormat audioFormat;

                    @Override
                    public String getName() {
                        return mixer.getMixerInfo().getName();
                    }

                    @Override
                    public LBAudioFormat getAudioFormat() {
                        return audioFormat;
                    }

                    @Override
                    public boolean start() {
                        if (isOpen()) {
                            return false;
                        }

                        try {
                            dataLine = (TargetDataLine) mixer.getLine(lineInfo);
                            dataLine.open(format);
                            audioFormat = new LBAudioFormat(format);
                            dataLine.start();
                        } catch (LineUnavailableException e) {
                            dataLine = null;
                            logger.warn("Could not start audio capture thread, as selected audio mixer is not supported", e);
                            return false;
                        }

                        return true;
                    }

                    @Override
                    public boolean isOpen() {
                        return dataLine != null && dataLine.isOpen();
                    }

                    @Override
                    public int available() {
                        return dataLine != null ? dataLine.available() : 0;
                    }

                    @Override
                    public int read(byte[] buffer, int toRead) {
                        if (!isOpen() || dataLine.available() < toRead) {
                            return 0;
                        }
                        return dataLine.read(buffer, 0, toRead);
                    }

                    @Override
                    public boolean stop() {
                        if (!isOpen()) {
                            return false;
                        }
                        dataLine.stop();
                        dataLine.close();
                        return true;
                    }
                }).collect(Collectors.toList());
    }
}
