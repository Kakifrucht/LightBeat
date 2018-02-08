package io.lightbeat.hue.bridge;

import com.philips.lighting.hue.sdk.PHAccessPoint;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHLight;
import io.lightbeat.hue.color.ColorSet;

import java.util.List;

/**
 * Implementing class manages the current connection state to the hue bridge
 * and passes it's information to be rendered/interfaced through a {@link HueStateObserver}.
 */
public interface HueManager {

    /**
     * @return the currently connected bridge
     */
    PHBridge getBridge();

    /**
     * Sets the given parameter as the state observer to receive api callbacks.
     *
     * @param observer to receive callbacks from state changes
     */
    void setStateObserver(HueStateObserver observer);

    /**
     * Attempts to connect to the last connected bridge.
     *
     * @return true if there is a previous bridge stored
     */
    boolean attemptStoredConnection();

    /**
     * Causes a scan for bridges in the connected network.
     */
    void doBridgesScan();

    /**
     * Tries to establish a connection with the provided access point.
     *
     * @param accessPoint to establish a connection with
     */
    void setAttemptConnection(PHAccessPoint accessPoint);

    /**
     * @return true if a bridge is connected
     */
    boolean isConnected();

    /**
     * Shutdown all hue SDK related processes.
     */
    void shutdown();

    /**
     * Get all connected lights.
     *
     * @return list of lights
     */
    List<PHLight> getLights();

    /**
     * @return the currently selected color set
     */
    ColorSet getColorSet();

    /**
     * Initializes the lights and store their state to be recovered by {@link #recoverOriginalState()}.
     *
     * @return false if no lights are selected/available
     */
    boolean initializeLights();

    /**
     * Recovers light states that were stored by {@link #initializeLights()}.
     */
    void recoverOriginalState();
}
