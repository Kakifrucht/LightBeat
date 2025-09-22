package pw.wunderlich.lightbeat.hue.bridge;

import java.util.List;

/**
 * Implementing class will interpret communication received by the hue bridge
 * to render the information to the end user and allowing interaction.
 */
public interface HueStateObserver {

    void isScanningForBridges();

    void displayFoundBridges(List<AccessPoint> foundBridges);

    void isAttemptingConnection();

    void requestPushlink();

    void pushlinkHasFailed();

    void hasConnected();

    void connectionWasLost(AccessPoint accessPoint, BridgeConnection.ConnectionListener.Error error);

    void disconnected();
}
