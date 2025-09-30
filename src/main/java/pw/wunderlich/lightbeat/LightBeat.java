package pw.wunderlich.lightbeat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pw.wunderlich.lightbeat.audio.LBAudioReader;
import pw.wunderlich.lightbeat.config.Config;
import pw.wunderlich.lightbeat.config.ConfigNode;
import pw.wunderlich.lightbeat.config.LBConfig;
import pw.wunderlich.lightbeat.gui.FrameManager;
import pw.wunderlich.lightbeat.hue.bridge.AccessPoint;
import pw.wunderlich.lightbeat.hue.bridge.LBHueManager;

import java.util.Objects;

/**
 * Entry point for application. Starts modules to bootstrap the application.
 *
 * @author Fabian Prieto Wunderlich
 */
public class LightBeat {

    private static final Logger logger = LoggerFactory.getLogger(LightBeat.class);

    public static void main(String[] args) {
        new LightBeat();
    }

    public static String getVersion() {
        String version = LightBeat.class.getPackage().getImplementationVersion();
        return Objects.requireNonNullElse(version, "");
    }


    private LightBeat() {

        logger.info("LightBeat v{} starting", getVersion());

        final var taskOrchestrator = new AppTaskOrchestrator();
        final var config = new LBConfig();

        final var audioReader = new LBAudioReader(config, taskOrchestrator);
        final var hueManager = new LBHueManager(config, taskOrchestrator);

        // enter swing UI
        new FrameManager(config, taskOrchestrator, audioReader, audioReader, hueManager);

        final var accessPoints = hueManager.getPreviousBridges();
        if (accessPoints.isEmpty()) {
            var accessPoint = getLastConnectedLegacy(config);
            if (accessPoint != null) {
                hueManager.setAttemptConnection(accessPoint);
            } else {
                hueManager.doBridgesScan();
            }
        } else {
            hueManager.setAttemptConnection(accessPoints.getFirst());
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
