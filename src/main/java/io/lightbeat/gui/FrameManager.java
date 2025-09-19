package io.lightbeat.gui;

import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.theme.DarculaTheme;
import com.github.weisj.darklaf.theme.IntelliJTheme;
import io.lightbeat.ComponentHolder;
import io.lightbeat.config.ConfigNode;
import io.lightbeat.gui.frame.ConnectFrame;
import io.lightbeat.gui.frame.HueFrame;
import io.lightbeat.gui.frame.MainFrame;
import io.lightbeat.hue.bridge.AccessPoint;
import io.lightbeat.hue.bridge.BridgeConnection;
import io.lightbeat.hue.bridge.HueManager;
import io.lightbeat.hue.bridge.HueStateObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.List;

/**
 * Manages the applications main frame, only showing one main frame at a time, which are either
 * the connect-frame or the main application frame. Stores the current window position.
 * Implements {@link HueStateObserver} interface to receive state callbacks and update
 * the currently shown window accordingly.
 */
public class FrameManager implements HueStateObserver {

    private static final Logger logger = LoggerFactory.getLogger(FrameManager.class);

    private final ComponentHolder componentHolder;

    private HueFrame currentFrame;
    private int lastX = 100;
    private int lastY = 100;


    public FrameManager(ComponentHolder componentHolder) {

        this.componentHolder = componentHolder;
        HueManager hueManager = componentHolder.getHueManager();
        hueManager.setStateObserver(this);

        boolean lightTheme = componentHolder.getConfig().getBoolean(ConfigNode.WINDOW_LIGHT_THEME);
        LafManager.installTheme(lightTheme ? new IntelliJTheme() : new DarculaTheme());
    }

    public void shutdown() {
        if (currentFrame != null && currentFrame.getJFrame().isDisplayable()) {
            currentFrame.dispose();
            currentFrame = null;
        }
    }

    @Override
    public void isScanningForBridges() {
        showConnectFrame().isScanningForBridges();
    }

    @Override
    public void displayFoundBridges(List<AccessPoint> foundBridges) {
        showConnectFrame().displayFoundBridges(foundBridges);
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
    public void connectionWasLost(BridgeConnection.ConnectionListener.Error error) {
        showConnectFrame().connectionWasLost(error);
    }

    @Override
    public void disconnected() {
        showConnectFrame().disconnected();
    }

    private HueStateObserver showConnectFrame() {
        if (currentFrame instanceof ConnectFrame) {
            return (HueStateObserver) currentFrame;
        }

        disposeCurrentWindow();
        try {
            currentFrame = new ConnectFrame(componentHolder, lastX, lastY);
        } catch (Throwable t) {
            logger.error("Exception thrown during frame creation", t);
        }
        return (HueStateObserver) currentFrame;
    }

    private void showMainFrame() {
        if (currentFrame instanceof MainFrame) {
            return;
        }

        disposeCurrentWindow();
        try {
            currentFrame = new MainFrame(componentHolder, lastX, lastY);
        } catch (Throwable t) {
            logger.error("Exception thrown during frame creation", t);
        }
    }

    private void disposeCurrentWindow() {
        if (currentFrame != null) {
            lastX = currentFrame.getJFrame().getBounds().x;
            lastY = currentFrame.getJFrame().getBounds().y;
            SwingUtilities.invokeLater(currentFrame::dispose);
        }
    }
}
