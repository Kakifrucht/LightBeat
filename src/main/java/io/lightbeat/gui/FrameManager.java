package io.lightbeat.gui;

import com.bulenkov.darcula.DarculaLaf;
import com.philips.lighting.hue.sdk.PHAccessPoint;
import io.lightbeat.LightBeat;
import io.lightbeat.gui.frame.ConnectFrame;
import io.lightbeat.gui.frame.HueFrame;
import io.lightbeat.gui.frame.MainFrame;
import io.lightbeat.hue.bridge.HueManager;
import io.lightbeat.hue.bridge.HueStateObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.List;

/**
 * Manages the applications main frame, only showing one main frame at a time, which are either
 * the connect frame or the main application frame. Stores the current window position.
 * Implements {@link HueStateObserver} interface to receive state callbacks and update
 * the currently shown window accordingly.
 */
public class FrameManager implements HueStateObserver {

    private static final Logger logger = LoggerFactory.getLogger(FrameManager.class);

    private HueFrame currentFrame;
    private int lastX = 100;
    private int lastY = 100;


    public FrameManager() {

        HueManager hueManager = LightBeat.getComponentHolder().getHueManager();
        hueManager.setStateObserver(this);

        try { // set darcula theme by default
            UIManager.getFont("Label.font"); // workaround due to bug in initializer (bulenkov/Darcula issue #29)
            UIManager.setLookAndFeel(new DarculaLaf());
        } catch (Exception e) {
            logger.warn("Exception caught during initialization of Darcula look and feel", e);
        }

        boolean doesAttemptConnection = hueManager.attemptStoredConnection();
        if (!doesAttemptConnection) {
            hueManager.doBridgesScan();
        }
    }

    public void shutdown() {
        if (currentFrame != null && currentFrame.getJFrame().isDisplayable()) {
            currentFrame.dispose();
            currentFrame = null;
        }
    }

    @Override
    public void isScanningForBridges(boolean connectFailed) {
        showConnectFrame().isScanningForBridges(connectFailed);
    }

    @Override
    public void displayFoundBridges(List<PHAccessPoint> list) {
        showConnectFrame().displayFoundBridges(list);
    }

    @Override
    public void isAttemptingConnection() {
        showConnectFrame().isAttemptingConnection();
    }

    @Override
    public void requestPushlink() {
        showConnectFrame().requestPushlink();
    }

    @Override
    public void pushlinkHasFailed() {
        showConnectFrame().pushlinkHasFailed();
    }

    @Override
    public void hasConnected() {
        showMainFrame();
    }

    private HueStateObserver showConnectFrame() {
        if (isConnectFrame()) {
            return (HueStateObserver) currentFrame;
        }

        disposeCurrentWindow();
        currentFrame = new ConnectFrame(lastX, lastY);
        return (HueStateObserver) currentFrame;
    }

    private void showMainFrame() {
        if (!isConnectFrame()) {
            return;
        }

        disposeCurrentWindow();
        currentFrame = new MainFrame(lastX, lastY);
    }

    private boolean isConnectFrame() {
        return currentFrame instanceof ConnectFrame;
    }

    private void disposeCurrentWindow() {
        if (currentFrame != null) {
            lastX = currentFrame.getJFrame().getBounds().x;
            lastY = currentFrame.getJFrame().getBounds().y;
            SwingUtilities.invokeLater(currentFrame::dispose);
        }
    }
}
