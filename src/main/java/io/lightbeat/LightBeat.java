package io.lightbeat;

import io.lightbeat.audio.AudioReader;
import io.lightbeat.audio.BeatEventManager;
import io.lightbeat.audio.LBAudioReader;
import io.lightbeat.config.Config;
import io.lightbeat.config.LBConfig;
import io.lightbeat.gui.FrameManager;
import io.lightbeat.hue.bridge.HueManager;
import io.lightbeat.hue.bridge.LBHueManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Entry point for application. Starts modules to bootstrap the application.
 * Implements {@link ComponentHolder} interface for accessing seperate modules.
 *
 * @author Fabian Prieto Wunderlich
 */
public class LightBeat implements ComponentHolder {

    private static final Logger logger = LoggerFactory.getLogger(LightBeat.class);

    public static void main(String[] args) {
        new LightBeat();
    }


    private final ScheduledExecutorService executorService;
    private final Config config;

    private final LBAudioReader audioReader;
    private final HueManager hueManager;
    private final FrameManager frameManager;


    private LightBeat() {

        logger.info("LightBeat v" + getVersion() + " starting");

        executorService = Executors.newScheduledThreadPool(2);
        config = new LBConfig();

        audioReader = new LBAudioReader(config, executorService);
        hueManager = new LBHueManager(this);
        frameManager = new FrameManager(this);
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
    public HueManager getHueManager() {
        return hueManager;
    }

    @Override
    public void shutdownAll() {

        logger.info("Shutting down LightBeat");

        audioReader.stop();
        frameManager.shutdown();
        hueManager.shutdown();
        try {
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.warn("Executor service forcefully shut down, some tasks have not completed");
        }

        // ensure that we exit
        Runtime.getRuntime().exit(0);
    }

    @Override
    public String getVersion() {

        final String ERROR_STRING = "Error";

        Properties properties = new Properties();
        try {
            properties.load(LightBeat.class.getClassLoader().getResourceAsStream("metadata.properties"));
        } catch (IOException e) {
            return ERROR_STRING;
        }

        String version = properties.getProperty("version");
        if (version == null) {
            version = ERROR_STRING;
        }

        return version;
    }
}
