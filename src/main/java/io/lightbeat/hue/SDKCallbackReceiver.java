package io.lightbeat.hue;

import com.philips.lighting.hue.sdk.PHAccessPoint;
import com.philips.lighting.model.PHBridge;

import java.util.List;

/**
 * Implementing class can receives callbacks from {@link com.philips.lighting.hue.sdk.PHSDKListener} instance.
 */
interface SDKCallbackReceiver {

    /**
     * Called by hue SDK listener when a connection to a bridge was established.
     *
     * @param bridge now connected bridge
     */
    void setConnected(PHBridge bridge);

    /**
     * Called by hue SDK listener when access points where found.
     *
     * @param accessPoints found access points
     */
    void setAccessPointsFound(List<PHAccessPoint> accessPoints);

    /**
     * Called by hue SDK listener when authentication/pushlinking is required.
     */
    void setAwaitingPushlink();

    /**
     * Called by hue SDK listener when connection was lost.
     */
    void connectionWasLost();
}
