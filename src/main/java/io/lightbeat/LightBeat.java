package io.lightbeat;

import io.lightbeat.audio.AudioReader;
import io.lightbeat.audio.BeatEventManager;
import io.lightbeat.audio.LBAudioReader;
import io.lightbeat.config.Config;
import io.lightbeat.config.ConfigNode;
import io.lightbeat.config.LBConfig;
import io.lightbeat.gui.FrameManager;
import io.lightbeat.hue.bridge.AccessPoint;
import io.lightbeat.hue.bridge.HueManager;
import io.lightbeat.hue.bridge.LBHueManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
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

        List<AccessPoint> accessPoints = hueManager.getPreviousBridges();
        if (accessPoints.isEmpty()) {
            AccessPoint accessPoint = getLastConnectedLegacy();
            if (accessPoint != null) {
                hueManager.setAttemptConnection(accessPoint);
            } else {
                hueManager.doBridgesScan();
            }

        } else {
            hueManager.setAttemptConnection(accessPoints.get(0));
        }
    }

    private AccessPoint getLastConnectedLegacy() {
        // will be removed sooner or later, alongside their confignodes
        String oldIp = config.get(ConfigNode.BRIDGE_IPADDRESS_LEGACY);
        if (oldIp != null) {
            String oldUsername = config.get(ConfigNode.BRIDGE_USERNAME_LEGACY);
            return new AccessPoint(oldIp, oldUsername);
        }
        return null;
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
