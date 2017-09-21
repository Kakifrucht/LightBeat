package io.lightbeat;

import io.lightbeat.audio.AudioReader;
import io.lightbeat.audio.BeatEventManager;
import io.lightbeat.audio.LBAudioReader;
import io.lightbeat.config.Config;
import io.lightbeat.config.LBConfig;
import io.lightbeat.gui.FrameManager;
import io.lightbeat.hue.bridge.HueManager;
import io.lightbeat.hue.bridge.LBHueManager;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Entry point for application. Starts modules to bootstrap the application.
 * Offers access to {@link ComponentHolder} to get modules via static {@link #getComponentHolder()} method.
 *
 * @author Fabian Prieto Wunderlich
 */
public class LightBeat implements ComponentHolder {

    private static ComponentHolder instance;

    public static void main(String[] args) {
        new LightBeat();
    }

    public static ComponentHolder getComponentHolder() {
        return instance;
    }

    public static String getVersion() {
        Properties properties = new Properties();
        try {
            properties.load(LightBeat.class.getClassLoader().getResourceAsStream("metadata.properties"));
        } catch (IOException e) {
            return null;
        }
        return properties.getProperty("version");
    }

    //- static end -//

    private final ScheduledExecutorService executorService;
    private final Config config = new LBConfig();
    private final LBAudioReader audioReader;
    private final FrameManager frameManager;
    private final HueManager hueManager;


    private LightBeat() {
        instance = this;

        executorService = Executors.newScheduledThreadPool(4);

        audioReader = new LBAudioReader(config, executorService);
        frameManager = new FrameManager();
        hueManager = new LBHueManager();
    }

    @Override
    public ScheduledExecutorService getExecutorService() {
        return executorService;
    }

    @Override
    public Config getConfig() {
        return config;
    }

    @Override
    public AudioReader getAudioReader() {
        return audioReader;
    }

    @Override
    public BeatEventManager getAudioEventManager() {
        return audioReader;
    }

    @Override
    public FrameManager getFrameManager() {
        return frameManager;
    }

    @Override
    public HueManager getHueManager() {
        return hueManager;
    }
}
