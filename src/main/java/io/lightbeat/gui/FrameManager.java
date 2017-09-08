package io.lightbeat.gui;

import io.lightbeat.gui.frame.MainFrame;
import io.lightbeat.gui.frame.ConnectFrame;
import io.lightbeat.gui.frame.HueFrame;
import io.lightbeat.hue.HueStateObserver;

import javax.swing.*;

/**
 * Manages the applications main frame, only showing one main frame at a time, which are either
 * the connect frame or the main application frame. Stores the current window position.
 */
public class FrameManager {

    private HueFrame currentFrame;
    private int lastX = 100;
    private int lastY = 100;


    public FrameManager() {
        try {
            // set system look and feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
    }

    public void showMainFrame() {
        if (currentFrame instanceof MainFrame) {
            return;
        }

        disposeCurrentWindow();
        currentFrame = new MainFrame(lastX, lastY);
    }

    public HueStateObserver showConnectFrame() {
        if (currentFrame instanceof ConnectFrame) {
            return (HueStateObserver) currentFrame;
        }

        disposeCurrentWindow();
        currentFrame = new ConnectFrame(lastX, lastY);
        return (HueStateObserver) currentFrame;
    }

    private void disposeCurrentWindow() {
        if (currentFrame != null) {
            HueFrame currentFrameLocal = currentFrame;
            lastX = currentFrameLocal.getJFrame().getBounds().x;
            lastY = currentFrameLocal.getJFrame().getBounds().y;
            SwingUtilities.invokeLater(currentFrameLocal::dispose);
        }
    }
}
