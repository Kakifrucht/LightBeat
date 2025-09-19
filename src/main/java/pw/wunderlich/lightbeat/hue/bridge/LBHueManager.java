package pw.wunderlich.lightbeat.hue.bridge;

import io.github.zeroone3010.yahueapi.HueBridge;
import io.github.zeroone3010.yahueapi.State;
import io.github.zeroone3010.yahueapi.discovery.HueBridgeDiscoveryService;
import pw.wunderlich.lightbeat.ComponentHolder;
import pw.wunderlich.lightbeat.config.Config;
import pw.wunderlich.lightbeat.config.ConfigNode;
import pw.wunderlich.lightbeat.hue.bridge.color.ColorSet;
import pw.wunderlich.lightbeat.hue.bridge.color.CustomColorSet;
import pw.wunderlich.lightbeat.hue.bridge.color.RandomColorSet;
import pw.wunderlich.lightbeat.hue.bridge.light.LBLight;
import pw.wunderlich.lightbeat.hue.bridge.light.Light;
import pw.wunderlich.lightbeat.hue.visualizer.HueBeatObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Default {@link HueManager} implementation.
 */
public class LBHueManager implements HueManager {

    private static final Logger logger = LoggerFactory.getLogger(LBHueManager.class);
    private static final String CONFIG_BRIDGE_PREFIX = "bridge.entry.";

    private final ComponentHolder componentHolder;
    private final Config config;

    private final LightQueue lightQueue;

    private BridgeConnection bridgeConnection;
    private ManagerState currentState = ManagerState.NOT_CONNECTED;
    private ColorSet colorSet;

    private HueStateObserver stateObserver;

    private Future<?> bridgeScanTask;
    private Map<Light, State> originalLightStates;


    public LBHueManager(ComponentHolder componentHolder) {
        this.componentHolder = componentHolder;
        this.config = componentHolder.getConfig();

        this.lightQueue = new LightQueue(this, componentHolder.getExecutorService());
    }

    @Override
    public BridgeConnection getBridge() {
        return bridgeConnection;
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
                    String key = config.get(ConfigNode.getCustomNode(CONFIG_BRIDGE_PREFIX + bridgeIp));
                    if (key == null) {
                        return null;
                    }
                    return new AccessPoint(bridgeIp, key);
                }).filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void doBridgesScan() {
        if (!currentState.equals(ManagerState.SCANNING_FOR_BRIDGES)) {
            Future<List<HueBridge>> bridgesFuture = new HueBridgeDiscoveryService()
                    .discoverBridges(bridge -> logger.info("Discovered bridge at {}", bridge.getIp()));

            bridgeScanTask = componentHolder.getExecutorService().schedule(() -> {
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
                        .map(bridge -> new AccessPoint(bridge.getIp())).collect(Collectors.toList());
                stateObserver.displayFoundBridges(accessPoints);
            }, 0, TimeUnit.SECONDS);

            stateObserver.isScanningForBridges();
            currentState = ManagerState.SCANNING_FOR_BRIDGES;
        }
    }

    @Override
    public void setAttemptConnection(AccessPoint accessPoint) {
        String bridgeIp = accessPoint.ip();
        currentState = ManagerState.ATTEMPTING_CONNECTION;
        stateObserver.isAttemptingConnection();
        BridgeConnection.ConnectionListener listener = new BridgeConnection.ConnectionListener() {
            @Override
            public void connectionSuccess(String key) {
                logger.info("Connected to bridge at {} with key {}", bridgeIp, key);

                List<String> bridgeList = new ArrayList<>(config.getStringList(ConfigNode.BRIDGE_LIST));
                bridgeList.remove(bridgeIp);
                bridgeList.add(0, bridgeIp);
                config.putList(ConfigNode.BRIDGE_LIST, bridgeList);
                config.put(ConfigNode.getCustomNode(CONFIG_BRIDGE_PREFIX + bridgeIp), key);

                currentState = ManagerState.CONNECTED;
                stateObserver.hasConnected();
            }

            @Override
            public void connectionError(Error error) {
                if (error.equals(Error.CONNECTION_LOST)) {
                    logger.info("Connection to bridge at {} was lost", bridgeIp);
                } else {
                    logger.info("Connection to bridge at {} could not be established (Error {})", bridgeIp, error);
                }

                currentState = ManagerState.CONNECTION_LOST;
                stateObserver.connectionWasLost(error);
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
        bridgeConnection = new BridgeConnection(accessPoint, componentHolder.getExecutorService(), listener);
    }

    @Override
    public boolean isConnected() {
        return currentState.equals(ManagerState.CONNECTED);
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

    @Override
    public void shutdown() {
        if (isConnected()) {
            recoverOriginalState();
            lightQueue.markShutdown();
        } else if (bridgeConnection != null) {
            bridgeConnection.disconnect();
        }

        if (bridgeScanTask != null && !bridgeScanTask.isDone()) {
            bridgeScanTask.cancel(true);
        }
    }

    @Override
    public ColorSet getColorSet() {
        String selectedColorSet = config.get(ConfigNode.COLOR_SET_SELECTED);
        ColorSet colorSet;
        if (selectedColorSet == null || selectedColorSet.equals("Random")) {
            colorSet = new RandomColorSet();
        } else {
            colorSet = new CustomColorSet(config, selectedColorSet);
        }

        if (!colorSet.equals(this.colorSet)) {
            this.colorSet = colorSet;
        }

        return this.colorSet;
    }

    @Override
    public boolean initializeLights() {

        bridgeConnection.refresh();

        List<Light> lights = new ArrayList<>();
        originalLightStates = new HashMap<>();

        List<String> disabledLights = componentHolder.getConfig().getStringList(ConfigNode.LIGHTS_DISABLED);
        for (io.github.zeroone3010.yahueapi.Light apiLight : bridgeConnection.getLights()) {

            if (disabledLights.contains(apiLight.getId())) {
                continue;
            }

            Light light = new LBLight(apiLight, lightQueue, componentHolder.getExecutorService());
            originalLightStates.put(light, apiLight.getState());
            lights.add(light);
        }

        if (!lights.isEmpty()) {
            lights.stream().filter(l -> !l.isOn()).forEach(light -> light.setOn(true));
            HueBeatObserver beatObserver = new HueBeatObserver(componentHolder, new ArrayList<>(lights));
            componentHolder.getAudioEventManager().registerBeatObserver(beatObserver);
            return true;
        }

        return false;
    }

    @Override
    public void recoverOriginalState() {
        if (originalLightStates != null) {
            originalLightStates.forEach(lightQueue::addUpdate);
            originalLightStates = null;
        }
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
