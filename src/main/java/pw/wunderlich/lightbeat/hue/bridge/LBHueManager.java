package pw.wunderlich.lightbeat.hue.bridge;

import io.github.zeroone3010.yahueapi.HueBridge;
import io.github.zeroone3010.yahueapi.discovery.HueBridgeDiscoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pw.wunderlich.lightbeat.AppTaskOrchestrator;
import pw.wunderlich.lightbeat.config.Config;
import pw.wunderlich.lightbeat.config.ConfigNode;
import pw.wunderlich.lightbeat.hue.bridge.light.LBLight;
import pw.wunderlich.lightbeat.hue.bridge.light.Light;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Default {@link HueManager} implementation.
 */
public class LBHueManager implements HueManager {

    private static final Logger logger = LoggerFactory.getLogger(LBHueManager.class);
    private static final String CONFIG_BRIDGE_PREFIX = "bridge.entry.";

    private final Config config;
    private final AppTaskOrchestrator taskOrchestrator;

    private BridgeConnection bridgeConnection;
    private ManagerState currentState = ManagerState.NOT_CONNECTED;

    private HueStateObserver stateObserver;


    public LBHueManager(Config config, AppTaskOrchestrator taskOrchestrator) {
        this.config = config;
        this.taskOrchestrator = taskOrchestrator;
    }

    @Override
    public BridgeConnection getBridge() {
        return bridgeConnection;
    }

    @Override
    public List<Light> getLights(boolean disabledLights) {
        bridgeConnection.refresh();
        var disabledLightsList = config.getStringList(ConfigNode.LIGHTS_DISABLED);

        return bridgeConnection.getLights()
                .stream()
                .filter(light -> !disabledLights || !disabledLightsList.contains(light.getId()))
                .map((light -> new LBLight(light, taskOrchestrator)))
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public void setStateObserver(HueStateObserver observer) {
        this.stateObserver = observer;
    }

    @Override
    public List<AccessPoint> getPreviousBridges() {
        return config.getStringList(ConfigNode.BRIDGE_LIST)
                .stream()
                .map(bridgeIp -> {
                    List<String> bridgeData = config.getStringList(ConfigNode.getCustomNode(CONFIG_BRIDGE_PREFIX + bridgeIp));
                    if (bridgeData.isEmpty()) {
                        return null;
                    } else if (bridgeData.size() == 1) { // only ip
                        return new AccessPoint(bridgeIp, bridgeData.getFirst());
                    } else {
                        return new AccessPoint(bridgeIp, bridgeData.getFirst(), bridgeData.get(1), bridgeData.get(2));
                    }
                }).filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void doBridgesScan() {
        if (currentState.equals(ManagerState.SCANNING_FOR_BRIDGES)) {
            return;
        }

        var bridgesFuture = new HueBridgeDiscoveryService()
                .discoverBridges(bridge -> logger.info("Discovered bridge at {}", bridge.getIp()));

        taskOrchestrator.dispatch(() -> {
            List<HueBridge> bridgesFound;
            try {
                bridgesFound = bridgesFuture.get();
            } catch (InterruptedException e) {
                return;
            } catch (Exception e) {
                logger.warn("Exception during scan for bridges", e);
                bridgesFound = Collections.emptyList();
            }

            logger.info("Access point scan finished, found {} bridges", bridgesFound.size());
            currentState = ManagerState.NOT_CONNECTED;

            List<AccessPoint> accessPoints = bridgesFound.stream()
                    .map(bridge -> new AccessPoint(bridge.getIp(), null, bridge.getName()))
                    .collect(Collectors.toList());
            stateObserver.displayFoundBridges(accessPoints);
        });

        stateObserver.isScanningForBridges();
        currentState = ManagerState.SCANNING_FOR_BRIDGES;
    }

    @Override
    public void setAttemptConnection(AccessPoint accessPoint) {
        String bridgeIp = accessPoint.ip();
        currentState = ManagerState.ATTEMPTING_CONNECTION;
        stateObserver.isAttemptingConnection();
        BridgeConnection.ConnectionListener listener = new BridgeConnection.ConnectionListener() {
            @Override
            public void connectionSuccess(String key, String name, String certificateHash) {
                logger.info("Connected to bridge {} at {} with key {}", name, bridgeIp, key);

                var bridgeList = new ArrayList<>(config.getStringList(ConfigNode.BRIDGE_LIST));
                bridgeList.remove(bridgeIp);
                bridgeList.addFirst(bridgeIp);
                config.putList(ConfigNode.BRIDGE_LIST, bridgeList);

                var bridgeData = new ArrayList<>();
                bridgeData.add(key);
                bridgeData.add(name);
                bridgeData.add(certificateHash);
                config.putList(ConfigNode.getCustomNode(CONFIG_BRIDGE_PREFIX + bridgeIp), bridgeData);

                currentState = ManagerState.CONNECTED;
                stateObserver.hasConnected();
            }

            @Override
            public void connectionError(AccessPoint ap, Error error) {
                if (error.equals(Error.CONNECTION_LOST)) {
                    logger.info("Connection to bridge at {} was lost", bridgeIp);
                } else {
                    logger.info("Connection to bridge at {} could not be established (Error {})", bridgeIp, error);
                }

                currentState = ManagerState.CONNECTION_LOST;
                stateObserver.connectionWasLost(ap, error);
            }

            @Override
            public void pushlinkRequired() {
                logger.info("Authentication to connect to bridge at {} is required", bridgeIp);

                currentState = ManagerState.AWAITING_PUSHLINK;
                stateObserver.requestPushlink();
            }

            @Override
            public void pushlinkFailed() {
                currentState = ManagerState.NOT_CONNECTED;
                stateObserver.pushlinkHasFailed();
            }
        };
        bridgeConnection = new BridgeConnection(accessPoint, taskOrchestrator, listener);
    }

    @Override
    public void disconnect() {
        if (currentState.equals(ManagerState.CONNECTED)) {
            bridgeConnection.disconnect();
            logger.info("Disconnected from bridge");
            bridgeConnection = null;
        }

        currentState = ManagerState.NOT_CONNECTED;
        stateObserver.disconnected();
    }

    private enum ManagerState {
        NOT_CONNECTED,
        SCANNING_FOR_BRIDGES,
        ATTEMPTING_CONNECTION,
        AWAITING_PUSHLINK,
        CONNECTED,
        CONNECTION_LOST
    }
}
