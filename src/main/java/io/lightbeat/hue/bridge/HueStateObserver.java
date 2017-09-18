package io.lightbeat.hue.bridge;

import com.philips.lighting.hue.sdk.PHAccessPoint;

import java.util.List;

/**
 * Implementing class will interpret communication received by the hue bridge
 * to render the information to the end user and allowing interaction.
 */
public interface HueStateObserver {

    void isScanningForBridges(boolean connectFailed);

    void displayFoundBridges(List<PHAccessPoint> list);

    void requestPushlink();

    void isAttemptingConnection();

    void pushlinkHasFailed();
}
