package io.lightbeat.hue.bridge;

import com.philips.lighting.hue.sdk.PHAccessPoint;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.hue.sdk.PHMessageType;
import com.philips.lighting.hue.sdk.PHSDKListener;
import com.philips.lighting.hue.sdk.exception.PHHeartbeatException;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHBridgeConfiguration;
import com.philips.lighting.model.PHHueError;
import com.philips.lighting.model.PHHueParsingError;
import io.lightbeat.config.Config;
import io.lightbeat.config.ConfigNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Implementation of {@link PHSDKListener}, to handle all lamp related events received by the SDK.
 * Sets SDK related variables and passes it's data to {@link HueManager}.
 */
class HueSDKListener implements PHSDKListener {

    private static final Logger logger = LoggerFactory.getLogger(HueSDKListener.class);

    private final Config config;
    private final SDKCallbackReceiver callbackReceiver;
    private final PHHueSDK hueSDK = PHHueSDK.getStoredSDKObject();


    HueSDKListener(Config config, SDKCallbackReceiver callbackReceiver) {
        this.config = config;
        this.callbackReceiver = callbackReceiver;
    }

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

        callbackReceiver.setConnected(phBridge);
    }

    @Override
    public void onAuthenticationRequired(PHAccessPoint phAccessPoint) {
        logger.info("Authentication to connect to bridge at {} is required", phAccessPoint.getIpAddress());
        hueSDK.startPushlinkAuthentication(phAccessPoint);
        callbackReceiver.setAwaitingPushlink();
    }

    @Override
    public void onAccessPointsFound(List<PHAccessPoint> list) {
        logger.info("Access point scan finished, found {} bridges", list.size());
        callbackReceiver.setAccessPointsFound(list);
    }

    @Override
    public void onError(int errorCode, String message) {

        if (errorCode == PHMessageType.PUSHLINK_BUTTON_NOT_PRESSED || errorCode == 42) {
            return;
        }

        logger.error("Error ocurred, code {} - {}", errorCode, message);
        if (errorCode == PHMessageType.BRIDGE_NOT_FOUND) {
            callbackReceiver.setAccessPointsFound(null);
        } else if (errorCode == PHHueError.BRIDGE_NOT_RESPONDING || errorCode == PHMessageType.PUSHLINK_AUTHENTICATION_FAILED) {
            callbackReceiver.connectionWasLost();
        }
    }

    @Override
    public void onConnectionLost(PHAccessPoint phAccessPoint) {
        logger.warn("Connection to bridge at {} was lost", phAccessPoint.getIpAddress());
        PHBridge selectedBridge = hueSDK.getSelectedBridge();
        hueSDK.getHeartbeatManager().disableAllHeartbeats(selectedBridge);
        hueSDK.disconnect(selectedBridge);
        hueSDK.setSelectedBridge(null);
        callbackReceiver.connectionWasLost();
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
}
