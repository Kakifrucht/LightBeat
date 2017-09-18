package io.lightbeat.hue.bridge;

import com.philips.lighting.hue.sdk.PHAccessPoint;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHLight;
import io.lightbeat.hue.light.LightQueue;
import io.lightbeat.hue.light.color.ColorSet;

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
     * @return the queue to send light updates to
     */
    LightQueue getQueue();

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
    List<PHLight> getAllLights();

    /**
     * Get connected and selected lights.
     *
     * @param randomized if true will return in randomized order
     * @return list of selected (not disabled) lights
     */
    List<PHLight> getLights(boolean randomized);

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
