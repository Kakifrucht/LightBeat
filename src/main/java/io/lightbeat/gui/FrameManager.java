package io.lightbeat.gui;

import com.bulenkov.darcula.DarculaLaf;
import io.lightbeat.gui.frame.ConnectFrame;
import io.lightbeat.gui.frame.HueFrame;
import io.lightbeat.gui.frame.MainFrame;
import io.lightbeat.hue.bridge.HueStateObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

/**
 * Manages the applications main frame, only showing one main frame at a time, which are either
 * the connect frame or the main application frame. Stores the current window position.
 */
public class FrameManager {

    private static final Logger logger = LoggerFactory.getLogger(FrameManager.class);

    private HueFrame currentFrame;
    private int lastX = 100;
    private int lastY = 100;


    public FrameManager() {
        try {
            // darcula theme by default
            UIManager.getFont("Label.font"); // workaround due to bug in initializer (bulenkov/Darcula issue #29)
            UIManager.setLookAndFeel(new DarculaLaf());
        } catch (Exception e) {
            logger.warn("Exception caught during initialization of Darcula look and feel", e);
        }
    }

    public HueStateObserver showConnectFrame() {
        if (currentFrame instanceof ConnectFrame) {
            return (HueStateObserver) currentFrame;
        }

        disposeCurrentWindow();
        currentFrame = new ConnectFrame(lastX, lastY);
        return (HueStateObserver) currentFrame;
    }

    public void showMainFrame() {
        if (currentFrame instanceof MainFrame) {
            return;
        }

        disposeCurrentWindow();
        currentFrame = new MainFrame(lastX, lastY);
    }

    private void disposeCurrentWindow() {
        if (currentFrame != null) {
            lastX = currentFrame.getJFrame().getBounds().x;
            lastY = currentFrame.getJFrame().getBounds().y;
            SwingUtilities.invokeLater(currentFrame::dispose);
        }
    }
}
