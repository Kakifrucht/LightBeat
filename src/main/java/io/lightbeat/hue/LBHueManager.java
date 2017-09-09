package io.lightbeat.hue;

import com.philips.lighting.hue.sdk.PHAccessPoint;
import com.philips.lighting.hue.sdk.PHBridgeSearchManager;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;
import io.lightbeat.ComponentHolder;
import io.lightbeat.LightBeat;
import io.lightbeat.config.Config;
import io.lightbeat.config.ConfigNode;
import io.lightbeat.gui.FrameManager;
import io.lightbeat.hue.light.HueBeatObserver;
import io.lightbeat.hue.light.LightQueue;
import io.lightbeat.util.TimeThreshold;

import java.util.*;

/**
 * Default {@link HueManager} implementation.
 */
public class LBHueManager implements HueManager, SDKCallbackReceiver {

    private final ComponentHolder componentHolder;
    private final PHHueSDK hueSDK;
    private final FrameManager frameManager;
    private final LightQueue queue;

    private State currentState = State.NOT_CONNECTED;
    private HueStateObserver observerFrame;
    private PHBridge bridge;

    private HueBeatObserver beatObserver;
    private List<PHLight> lights;
    private Map<String, PHLightState> originalState;


    public LBHueManager() {
        hueSDK = PHHueSDK.create();
        hueSDK.setAppName("LightBeat");
        hueSDK.setDeviceName(System.getProperty("os.name"));

        componentHolder = LightBeat.getComponentHolder();
        this.frameManager = componentHolder.getFrameManager();
        observerFrame = frameManager.showConnectFrame();

        hueSDK.getNotificationManager().registerSDKListener(new HueSDKListener(this));

        Config config = componentHolder.getConfig();
        String bridgeUsername = config.get(ConfigNode.BRIDGE_USERNAME);
        String bridgeIP = config.get(ConfigNode.BRIDGE_IPADDRESS);
        if (bridgeUsername != null && bridgeIP != null) {
            PHAccessPoint accessPoint = new PHAccessPoint();
            accessPoint.setUsername(bridgeUsername);
            accessPoint.setIpAddress(bridgeIP);
            setAttemptConnection(accessPoint);
        } else {
            doBridgesScan();
        }

        this.queue = new LightQueue(this);
    }

    @Override
    public PHBridge getBridge() {
        return bridge;
    }

    @Override
    public LightQueue getQueue() {
        return queue;
    }

    @Override
    public void doBridgesScan() {
        if (!currentState.equals(State.SCANNING_FOR_BRIDGES)) {
            PHBridgeSearchManager searchManager = (PHBridgeSearchManager) hueSDK.getSDKService(PHHueSDK.SEARCH_BRIDGE);
            searchManager.search(true, true);
            observerFrame.isScanningForBridges(currentState.equals(State.ATTEMPTING_CONNECTION));
            currentState = State.SCANNING_FOR_BRIDGES;
        }
    }

    @Override
    public void setAttemptConnection(PHAccessPoint accessPoint) {
        currentState = State.ATTEMPTING_CONNECTION;
        observerFrame.isAttemptingConnection();
        hueSDK.connect(accessPoint);
    }

    @Override
    public boolean isConnected() {
        return currentState.equals(State.CONNECTED);
    }

    @Override
    public void shutdown() {

        componentHolder.getExecutorService().shutdown();

        if (isConnected()) {
            hueSDK.getHeartbeatManager().disableAllHeartbeats(bridge);
            queue.markShutdown();
            if (originalState != null) {
                recoverOriginalState();
            }
            return;
        } else if (currentState.equals(State.AWAITING_PUSHLINK)) {
            hueSDK.stopPushlinkAuthentication();
        }

        hueSDK.destroySDK();

        // dispatch thread that force exits if still running after 10 seconds
        // fixes bug with hueSDK, for example after failed pushlink shutdown doesn't seem to work properly
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

    @Override
    public List<PHLight> getAllLights() {
        return bridge.getResourceCache().getAllLights();
    }

    @Override
    public List<PHLight> getLights(boolean randomized) {

        List<PHLight> toReturn = new ArrayList<>(lights);
        if (randomized && toReturn.size() > 1) {
            Collections.shuffle(toReturn);
        }
        return toReturn;
    }

    @Override
    public boolean initializeLights() {

        originalState = new HashMap<>();
        lights = new ArrayList<>();
        List<PHLight> allLights = getAllLights();
        List<String> disabledLights = componentHolder.getConfig().getStringList(ConfigNode.LIGHTS_DISABLED);
        for (PHLight light : allLights) {

            if (disabledLights.contains(light.getUniqueId())) {
                continue;
            }

            PHLightState currentState = new PHLightState(light.getLastKnownLightState());
            currentState.setReachable(null);
            originalState.put(light.getUniqueId(), currentState);

            lights.add(light);
        }

        if (!lights.isEmpty()) {
            beatObserver = new HueBeatObserver(this, componentHolder.getConfig());
            componentHolder.getAudioEventManager().registerBeatObserver(beatObserver);
            return true;
        }

        return false;
    }

    @Override
    public void recoverOriginalState() {

        if (lights == null) {
            return;
        }

        componentHolder.getAudioEventManager().unregisterBeatObserver(beatObserver);

        for (PHLight light : lights) {
            if (originalState.containsKey(light.getUniqueId())) {
                queue.addUpdate(light, originalState.get(light.getUniqueId()));
            }
        }

        lights = null;
        originalState = null;
    }

    @Override
    public void setConnected(PHBridge bridge) {
        currentState = State.CONNECTED;
        this.bridge = bridge;
        frameManager.showMainFrame();
    }

    @Override
    public void setAccessPointsFound(List<PHAccessPoint> accessPoints) {
        currentState = State.NOT_CONNECTED;
        observerFrame.displayFoundBridges(accessPoints);
    }

    @Override
    public void setAwaitingPushlink() {
        currentState = State.AWAITING_PUSHLINK;
        observerFrame.requestPushlink();
    }

    @Override
    public void connectionWasLost() {

        if (currentState.equals(State.AWAITING_PUSHLINK)) {
            currentState = State.NOT_CONNECTED;
            observerFrame.pushlinkHasFailed();
        } else {
            observerFrame = frameManager.showConnectFrame();
            doBridgesScan();
        }
    }

    enum State {
        NOT_CONNECTED,
        SCANNING_FOR_BRIDGES,
        ATTEMPTING_CONNECTION,
        AWAITING_PUSHLINK,
        CONNECTED
    }
}
