package io.lightbeat.hue.bridge;

import io.lightbeat.hue.bridge.color.ColorSet;

import java.util.List;

/**
 * Implementing class manages the current connection state to the hue bridge
 * and passes its information to be rendered/interfaced through a {@link HueStateObserver}.
 */
public interface HueManager {

    /**
     * @return connection to current bridge
     */
    BridgeConnection getBridge();

    /**
     * Sets the given parameter as the state observer to receive api callbacks.
     *
     * @param observer to receive callbacks from state changes
     */
    void setStateObserver(HueStateObserver observer);

    /**
     * @return list containing previously connected bridges
     */
    List<AccessPoint> getPreviousBridges();

    /**
     * Causes a scan for bridges in the connected network.
     */
    void doBridgesScan();

    /**
     * Tries to establish a connection with the provided access point.
     *
     * @param accessPoint wrapper containing ip of bridge and key
     */
    void setAttemptConnection(AccessPoint accessPoint);

    /**
     * @return true if a bridge is connected
     */
    boolean isConnected();

    /**
     * Disconnect from currently connected bridge.
     */
    void disconnect();

    /**
     * Shutdown all hue SDK related processes.
     */
    void shutdown();

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
