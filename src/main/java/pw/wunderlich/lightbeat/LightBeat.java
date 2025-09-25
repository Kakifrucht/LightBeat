package pw.wunderlich.lightbeat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pw.wunderlich.lightbeat.audio.LBAudioReader;
import pw.wunderlich.lightbeat.config.Config;
import pw.wunderlich.lightbeat.config.ConfigNode;
import pw.wunderlich.lightbeat.config.LBConfig;
import pw.wunderlich.lightbeat.gui.FrameManager;
import pw.wunderlich.lightbeat.hue.bridge.AccessPoint;
import pw.wunderlich.lightbeat.hue.bridge.HueManager;
import pw.wunderlich.lightbeat.hue.bridge.LBHueManager;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Entry point for application. Starts modules to bootstrap the application.
 *
 * @author Fabian Prieto Wunderlich
 */
public class LightBeat {

    private static final Logger logger = LoggerFactory.getLogger(LightBeat.class);

    /**
     * Number of threads for the ScheduledExecutorService, used throughout the application.
     * Count was chosen as a trade-off to be able to handle high light counts to not block
     * during bridge communication i/o and to not overwhelm the bridge with too many commands.
     */
    private static final int THREAD_COUNT = 8;

    public static void main(String[] args) {
        new LightBeat();
    }

    public static String getVersion() {
        String version = LightBeat.class.getPackage().getImplementationVersion();
        return Objects.requireNonNullElse(version, "");
    }


    private LightBeat() {

        logger.info("LightBeat v{} starting", getVersion());

        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(THREAD_COUNT);
        Config config = new LBConfig();

        LBAudioReader audioReader = new LBAudioReader(config, executorService);
        HueManager hueManager = new LBHueManager(config, executorService);

        // enter swing UI
        new FrameManager(config, executorService, audioReader, audioReader, hueManager);

        List<AccessPoint> accessPoints = hueManager.getPreviousBridges();
        if (accessPoints.isEmpty()) {
            AccessPoint accessPoint = getLastConnectedLegacy(config);
            if (accessPoint != null) {
                hueManager.setAttemptConnection(accessPoint);
            } else {
                hueManager.doBridgesScan();
            }
        } else {
            hueManager.setAttemptConnection(accessPoints.get(0));
        }
    }

    private AccessPoint getLastConnectedLegacy(Config config) {
        // will be removed sooner or later, alongside their config nodes
        String oldIp = config.get(ConfigNode.BRIDGE_IPADDRESS_LEGACY);
        if (oldIp != null) {
            String oldUsername = config.get(ConfigNode.BRIDGE_USERNAME_LEGACY);
            return new AccessPoint(oldIp, oldUsername);
        }
        return null;
    }


}
