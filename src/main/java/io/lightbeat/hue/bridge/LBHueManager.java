package io.lightbeat.hue.bridge;

import com.philips.lighting.hue.sdk.*;
import com.philips.lighting.hue.sdk.exception.PHHeartbeatException;
import com.philips.lighting.model.*;
import io.lightbeat.ComponentHolder;
import io.lightbeat.config.Config;
import io.lightbeat.config.ConfigNode;
import io.lightbeat.hue.visualizer.HueBeatObserver;
import io.lightbeat.hue.bridge.color.ColorSet;
import io.lightbeat.hue.bridge.color.CustomColorSet;
import io.lightbeat.hue.bridge.color.RandomColorSet;
import io.lightbeat.hue.bridge.light.LBLight;
import io.lightbeat.hue.bridge.light.Light;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Default {@link HueManager} implementation.
 */
public class LBHueManager implements HueManager {

    private static final Logger logger = LoggerFactory.getLogger(LBHueManager.class);

    private final ComponentHolder componentHolder;
    private final Config config;
    private final PHHueSDK hueSDK;

    private final LightQueue lightQueue;

    private State currentState = State.NOT_CONNECTED;
    private PHBridge bridge;

    private HueStateObserver stateObserver;
    private Map<Light, PHLightState> originalLightStates;


    public LBHueManager(ComponentHolder componentHolder) {

        this.componentHolder = componentHolder;
        this.config = componentHolder.getConfig();

        hueSDK = PHHueSDK.create();
        hueSDK.setAppName("LightBeat");
        hueSDK.setDeviceName(System.getProperty("os.name"));

        PHSDKListener listener = new PHSDKListener() {
            @Override
            public void onBridgeConnected(PHBridge phBridge, String username) {

                PHBridgeConfiguration bridgeConfiguration = phBridge.getResourceCache().getBridgeConfiguration();
                logger.info("Connected to bridge at {} with username {}", bridgeConfiguration.getIpAddress(), username);

                config.put(ConfigNode.BRIDGE_USERNAME, username);
                config.put(ConfigNode.BRIDGE_IPADDRESS, bridgeConfiguration.getIpAddress());

                hueSDK.setSelectedBridge(phBridge);
                try {
                    hueSDK.getHeartbeatManager().enableLightsHeartbeat(phBridge, PHHueSDK.HB_INTERVAL);
                } catch (PHHeartbeatException e) {
                    logger.warn("Exception while enabling lights heartbeat", e);
                }

                bridge = phBridge;
                currentState = State.CONNECTED;
                stateObserver.hasConnected();
            }

            @Override
            public void onAuthenticationRequired(PHAccessPoint phAccessPoint) {
                logger.info("Authentication to connect to bridge at {} is required", phAccessPoint.getIpAddress());
                hueSDK.startPushlinkAuthentication(phAccessPoint);

                currentState = State.AWAITING_PUSHLINK;
                stateObserver.requestPushlink();
            }

            @Override
            public void onAccessPointsFound(List<PHAccessPoint> list) {
                logger.info("Access point scan finished, found {} bridges", list.size());
                currentState = State.NOT_CONNECTED;
                stateObserver.displayFoundBridges(list);
            }

            @Override
            public void onError(int errorCode, String message) {

                if (errorCode == PHMessageType.PUSHLINK_BUTTON_NOT_PRESSED || errorCode == 42) {
                    return;
                }

                logger.error("Error ocurred, code {} - {}", errorCode, message);
                if (errorCode == PHMessageType.BRIDGE_NOT_FOUND) {
                    onAccessPointsFound(Collections.emptyList());
                } else if (errorCode == PHHueError.BRIDGE_NOT_RESPONDING || errorCode == PHMessageType.PUSHLINK_AUTHENTICATION_FAILED) {

                    if (currentState.equals(State.AWAITING_PUSHLINK)) {
                        stateObserver.pushlinkHasFailed();
                    } else {
                        currentState = State.CONNECTION_LOST;
                        stateObserver.connectionWasLost();
                    }
                }
            }

            @Override
            public void onConnectionLost(PHAccessPoint phAccessPoint) {
                logger.warn("Connection to bridge at {} was lost", phAccessPoint.getIpAddress());
                PHBridge selectedBridge = hueSDK.getSelectedBridge();
                hueSDK.getHeartbeatManager().disableAllHeartbeats(selectedBridge);
                hueSDK.disconnect(selectedBridge);
                hueSDK.setSelectedBridge(null);
                stateObserver.connectionWasLost();
                currentState = State.CONNECTION_LOST;
            }

            @Override
            public void onParsingErrors(List<PHHueParsingError> list) {
                logger.warn("{} parsing error(s) has/have occurred:", list.size());
                for (int i = 0; i < list.size(); i++) {
                    PHHueParsingError error = list.get(i);
                    logger.warn(" Error #{}: {}", i + 1, error.getMessage());
                }
            }

            @Override
            public void onCacheUpdated(List<Integer> list, PHBridge phBridge) {}

            @Override
            public void onConnectionResumed(PHBridge phBridge) {}
        };

        hueSDK.getNotificationManager().registerSDKListener(listener);
        this.lightQueue = new LightQueue(this);
    }

    @Override
    public PHBridge getBridge() {
        return bridge;
    }

    @Override
    public void setStateObserver(HueStateObserver observer) {
        this.stateObserver = observer;
    }

    @Override
    public boolean attemptStoredConnection() {

        String bridgeUsername = config.get(ConfigNode.BRIDGE_USERNAME);
        String bridgeIP = config.get(ConfigNode.BRIDGE_IPADDRESS);

        if (bridgeUsername != null && bridgeIP != null) {
            PHAccessPoint accessPoint = new PHAccessPoint();
            accessPoint.setUsername(bridgeUsername);
            accessPoint.setIpAddress(bridgeIP);
            setAttemptConnection(accessPoint);
            return true;
        }

        return false;
    }

    @Override
    public void doBridgesScan() {
        if (!currentState.equals(State.SCANNING_FOR_BRIDGES)) {
            PHBridgeSearchManager searchManager = (PHBridgeSearchManager) hueSDK.getSDKService(PHHueSDK.SEARCH_BRIDGE);
            searchManager.search(true, true);
            stateObserver.isScanningForBridges(currentState.equals(State.CONNECTION_LOST));
            currentState = State.SCANNING_FOR_BRIDGES;
        }
    }

    @Override
    public void setAttemptConnection(PHAccessPoint accessPoint) {
        currentState = State.ATTEMPTING_CONNECTION;
        stateObserver.isAttemptingConnection();
        hueSDK.connect(accessPoint);
    }

    @Override
    public boolean isConnected() {
        return currentState.equals(State.CONNECTED);
    }

    @Override
    public void shutdown() {

        if (isConnected()) {

            hueSDK.getHeartbeatManager().disableAllHeartbeats(bridge);
            recoverOriginalState();
            lightQueue.markShutdown();

            return;
        } else if (currentState.equals(State.AWAITING_PUSHLINK)) {
            hueSDK.stopPushlinkAuthentication();
        }

        hueSDK.destroySDK();
    }

    @Override
    public List<PHLight> getLights() {

        List<PHLight> lights = bridge.getResourceCache().getAllLights();
        lights.removeIf(light -> !light.supportsBrightness());

        return lights;
    }

    @Override
    public ColorSet getColorSet() {
        String selectedColorSet = config.get(ConfigNode.COLOR_SET_SELECTED);
        if (selectedColorSet == null || selectedColorSet.equals("Random")) {
            return new RandomColorSet();
        } else {
            return new CustomColorSet(config, selectedColorSet);
        }
    }

    @Override
    public boolean initializeLights() {

        List<Light> lights = new ArrayList<>();
        originalLightStates = new HashMap<>();

        int transitionTime = config.getInt(ConfigNode.BRIGHTNESS_FADE_TIME);
        List<String> disabledLights = componentHolder.getConfig().getStringList(ConfigNode.LIGHTS_DISABLED);
        for (PHLight phLight : getLights()) {

            if (disabledLights.contains(phLight.getUniqueId())) {
                continue;
            }

            PHLightState currentState = new PHLightState(phLight.getLastKnownLightState());
            currentState.setReachable(null);

            Light light = new LBLight(phLight, lightQueue, componentHolder.getExecutorService(), transitionTime);
            originalLightStates.put(light, currentState);
            lights.add(light);
        }

        if (!lights.isEmpty()) {

            for (Light light : lights) {
                if (!light.isOn()) {
                    light.setOn(true);
                }
            }

            HueBeatObserver beatObserver = new HueBeatObserver(componentHolder, new ArrayList<>(lights));
            componentHolder.getAudioEventManager().registerBeatObserver(beatObserver);
            return true;
        }

        return false;
    }

    @Override
    public void recoverOriginalState() {

        if (originalLightStates != null) {

            for (Light light : originalLightStates.keySet()) {
                lightQueue.addUpdate(light, originalLightStates.get(light));
            }

            originalLightStates = null;
        }
    }

    enum State {
        NOT_CONNECTED,
        SCANNING_FOR_BRIDGES,
        ATTEMPTING_CONNECTION,
        AWAITING_PUSHLINK,
        CONNECTED,
        CONNECTION_LOST
    }
}
