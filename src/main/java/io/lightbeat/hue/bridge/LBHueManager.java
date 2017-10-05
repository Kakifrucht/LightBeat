package io.lightbeat.hue.bridge;

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
import io.lightbeat.hue.HueBeatObserver;
import io.lightbeat.hue.light.LBLight;
import io.lightbeat.hue.light.Light;
import io.lightbeat.hue.color.ColorSet;
import io.lightbeat.hue.color.CustomColorSet;
import io.lightbeat.hue.color.RandomColorSet;
import io.lightbeat.util.TimeThreshold;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Default {@link HueManager} implementation.
 */
public class LBHueManager implements HueManager, SDKCallbackReceiver {

    private final ComponentHolder componentHolder;
    private final PHHueSDK hueSDK;
    private final FrameManager frameManager;
    private final Config config;
    private final LightQueue lightQueue;

    private State currentState = State.NOT_CONNECTED;
    private HueStateObserver observerFrame;
    private PHBridge bridge;

    private HueBeatObserver beatObserver;
    private List<Light> lights;
    private Map<String, PHLightState> originalLightStates;


    public LBHueManager() {
        hueSDK = PHHueSDK.create();
        hueSDK.setAppName("LightBeat");
        hueSDK.setDeviceName(System.getProperty("os.name"));

        componentHolder = LightBeat.getComponentHolder();
        config = componentHolder.getConfig();
        this.frameManager = componentHolder.getFrameManager();
        observerFrame = frameManager.showConnectFrame();

        hueSDK.getNotificationManager().registerSDKListener(new HueSDKListener(this));

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

        this.lightQueue = new LightQueue(this);
    }

    @Override
    public PHBridge getBridge() {
        return bridge;
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

            if (originalLightStates != null) {
                try {
                    componentHolder.getExecutorService().awaitTermination(500, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ignored) {}
                recoverOriginalState();
            }

            lightQueue.markShutdown();
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
    public List<PHLight> getLights() {
        return bridge.getResourceCache().getAllLights();
    }

    @Override
    public List<Light> getSelectedLights() {

        List<Light> toReturn = new ArrayList<>(lights);
        if (toReturn.size() > 1) {
            Collections.shuffle(toReturn);
        }
        return toReturn;
    }

    @Override
    public ColorSet getColorSet() {
        String selectedColorSet = config.get(ConfigNode.COLOR_SET_SELECTED);
        if (selectedColorSet.equals("Random")) {
            return new RandomColorSet();
        } else {
            return new CustomColorSet(config, selectedColorSet);
        }
    }

    @Override
    public boolean initializeLights() {

        originalLightStates = new HashMap<>();
        lights = new ArrayList<>();

        int transitionTime = config.getInt(ConfigNode.LIGHTS_TRANSITION_TIME);
        List<String> disabledLights = componentHolder.getConfig().getStringList(ConfigNode.LIGHTS_DISABLED);
        for (PHLight phLight : getLights()) {

            if (disabledLights.contains(phLight.getUniqueId())) {
                continue;
            }

            PHLightState currentState = new PHLightState(phLight.getLastKnownLightState());
            currentState.setReachable(null);
            originalLightStates.put(phLight.getUniqueId(), currentState);

            lights.add(new LBLight(phLight, lightQueue, componentHolder.getExecutorService(), transitionTime));
        }

        if (!lights.isEmpty()) {

            for (Light light : lights) {
                if (light.isOff()) {
                    light.setOn(true);
                }
            }

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

        for (PHLight light : getLights()) {
            if (originalLightStates.containsKey(light.getUniqueId())) {
                lightQueue.addUpdate(light, originalLightStates.get(light.getUniqueId()));
            }
        }

        lights = null;
        originalLightStates = null;
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
