package io.lightbeat;

import io.lightbeat.audio.AudioReader;
import io.lightbeat.audio.BeatEventManager;
import io.lightbeat.audio.LBAudioReader;
import io.lightbeat.config.Config;
import io.lightbeat.config.LBConfig;
import io.lightbeat.gui.FrameManager;
import io.lightbeat.hue.bridge.HueManager;
import io.lightbeat.hue.bridge.LBHueManager;
import io.lightbeat.util.TimeThreshold;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(LightBeat.class);
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

    public static void shutdown() {
        logger.info("Shutting down LightBeat");

        instance.getAudioReader().stop();
        instance.getFrameManager().shutdown();
        instance.getHueManager().shutdown();
        instance.getExecutorService().shutdown();

        // dispatch thread that force exits if still running after 10 seconds
        // fixes bug with hueSDK, for example after failed pushlink the huesdk shutdown call doesn't seem to work properly
        TimeThreshold forceShutdownThreshold = new TimeThreshold(10000L);
        new Thread(() -> {
            // under normal circumstances the thread count won't fall below 4
            while (Thread.activeCount() > 4) {
                if (forceShutdownThreshold.isMet()) {
                    Runtime.getRuntime().exit(0);
                }

                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException ignored) {}
            }
        }).start();
    }

    //- static end -//

    private final ScheduledExecutorService executorService;
    private final Config config;

    private final LBAudioReader audioReader;
    private final HueManager hueManager;
    private final FrameManager frameManager;


    private LightBeat() {
        logger.info("LightBeat v" + getVersion() + " starting");
        instance = this;

        executorService = Executors.newScheduledThreadPool(2);
        config = new LBConfig();

        audioReader = new LBAudioReader(config, executorService);
        hueManager = new LBHueManager(this);
        frameManager = new FrameManager();
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
