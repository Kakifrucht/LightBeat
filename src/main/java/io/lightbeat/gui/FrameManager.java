package io.lightbeat.gui;

import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.theme.DarculaTheme;
import com.github.weisj.darklaf.theme.IntelliJTheme;
import com.philips.lighting.hue.sdk.PHAccessPoint;
import io.lightbeat.ComponentHolder;
import io.lightbeat.config.ConfigNode;
import io.lightbeat.gui.frame.ConnectFrame;
import io.lightbeat.gui.frame.HueFrame;
import io.lightbeat.gui.frame.MainFrame;
import io.lightbeat.hue.bridge.HueManager;
import io.lightbeat.hue.bridge.HueStateObserver;

import javax.swing.*;
import java.util.List;

/**
 * Manages the applications main frame, only showing one main frame at a time, which are either
 * the connect frame or the main application frame. Stores the current window position.
 * Implements {@link HueStateObserver} interface to receive state callbacks and update
 * the currently shown window accordingly.
 */
public class FrameManager implements HueStateObserver {

    private final ComponentHolder componentHolder;

    private HueFrame currentFrame;
    private int lastX = 100;
    private int lastY = 100;


    public FrameManager(ComponentHolder componentHolder) {

        this.componentHolder = componentHolder;
        HueManager hueManager = componentHolder.getHueManager();
        hueManager.setStateObserver(this);

        boolean darkTheme = componentHolder.getConfig().getBoolean(ConfigNode.WINDOW_DARK_THEME);
        LafManager.install(darkTheme ? new DarculaTheme() : new IntelliJTheme());

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
        if (currentFrame != null) {
            showConnectFrame().isAttemptingConnection();
        }
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

    @Override
    public void connectionWasLost() {
        showConnectFrame().connectionWasLost();
    }

    private HueStateObserver showConnectFrame() {
        if (currentFrame instanceof ConnectFrame) {
            return (HueStateObserver) currentFrame;
        }

        disposeCurrentWindow();
        currentFrame = new ConnectFrame(componentHolder, lastX, lastY);
        return (HueStateObserver) currentFrame;
    }

    private void showMainFrame() {
        if (currentFrame instanceof MainFrame) {
            return;
        }

        disposeCurrentWindow();
        currentFrame = new MainFrame(componentHolder, lastX, lastY);
    }

    private void disposeCurrentWindow() {
        if (currentFrame != null) {
            lastX = currentFrame.getJFrame().getBounds().x;
            lastY = currentFrame.getJFrame().getBounds().y;
            SwingUtilities.invokeLater(currentFrame::dispose);
        }
    }
}
