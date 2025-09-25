package pw.wunderlich.lightbeat.gui;

import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.theme.IntelliJTheme;
import com.github.weisj.darklaf.theme.OneDarkTheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pw.wunderlich.lightbeat.audio.AudioReader;
import pw.wunderlich.lightbeat.audio.BeatEventManager;
import pw.wunderlich.lightbeat.config.Config;
import pw.wunderlich.lightbeat.config.ConfigNode;
import pw.wunderlich.lightbeat.gui.frame.ConnectFrame;
import pw.wunderlich.lightbeat.gui.frame.HueFrame;
import pw.wunderlich.lightbeat.gui.frame.MainFrame;
import pw.wunderlich.lightbeat.hue.bridge.AccessPoint;
import pw.wunderlich.lightbeat.hue.bridge.BridgeConnection;
import pw.wunderlich.lightbeat.hue.bridge.HueManager;
import pw.wunderlich.lightbeat.hue.bridge.HueStateObserver;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Manages the applications main frame, only showing one main frame at a time, which are either
 * the connect-frame or the main application frame. Stores the current window position.
 * Implements {@link HueStateObserver} interface to receive state callbacks and update
 * the currently shown window accordingly.
 */
public class FrameManager implements HueStateObserver {

    private static final Logger logger = LoggerFactory.getLogger(FrameManager.class);

    private final Config config;
    private final ScheduledExecutorService executorService;
    private final AudioReader audioReader;
    private final BeatEventManager beatEventManager;
    private final HueManager hueManager;

    private HueFrame currentFrame;
    private int lastX = 100;
    private int lastY = 100;


    public FrameManager(Config config, ScheduledExecutorService executorService,
                        AudioReader audioReader, BeatEventManager beatEventManager,
                        HueManager hueManager) {
        this.config = config;
        this.executorService = executorService;
        this.audioReader = audioReader;
        this.beatEventManager = beatEventManager;
        this.hueManager = hueManager;

        this.hueManager.setStateObserver(this);

        boolean lightTheme = this.config.getBoolean(ConfigNode.WINDOW_LIGHT_THEME);
        LafManager.installTheme(lightTheme ? new IntelliJTheme() : new OneDarkTheme());
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
    public void connectionWasLost(AccessPoint accessPoint, BridgeConnection.ConnectionListener.Error error) {
        showConnectFrame().connectionWasLost(accessPoint, error);
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
            currentFrame = new ConnectFrame(executorService, hueManager, lastX, lastY);
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
            currentFrame = new MainFrame(config, executorService, audioReader, beatEventManager, hueManager, lastX, lastY);
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
