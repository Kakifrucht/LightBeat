package io.lightbeat.gui.frame;

import io.lightbeat.ComponentHolder;
import io.lightbeat.LightBeat;
import io.lightbeat.config.Config;
import io.lightbeat.hue.bridge.HueManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Abstract implementation of {@link HueFrame} that handles standard frame drawing.
 */
public abstract class AbstractFrame implements HueFrame {

    final ComponentHolder componentHolder = LightBeat.getComponentHolder();
    final Config config = componentHolder.getConfig();
    final ScheduledExecutorService executorService = componentHolder.getExecutorService();

    final JFrame frame = new JFrame();
    Dimension minimumSize;
    Dimension preferredSize;

    private final String frameTitle;
    private final int x;
    private final int y;


    AbstractFrame(int x, int y) {
        this(null, x, y);
    }

    AbstractFrame(String frameTitle, int x, int y) {
        this.frameTitle = "LightBeat" + (frameTitle != null ? (" - " + frameTitle) : "" );
        this.x = x;
        this.y = y;
    }

    void drawFrame(Container mainContainer, boolean isMainFrame) {
        runOnSwingThread(() -> {

            frame.setTitle(frameTitle);
            frame.setContentPane(mainContainer);

            // load icons
            List<Image> icons = new ArrayList<>();
            for (int i = 16; i <= 64; i += 16) {
                icons.add(new ImageIcon(getClass().getResource("/png/icon_" + i + ".png")).getImage());
            }
            frame.setIconImages(icons);

            ToolTipManager.sharedInstance().setInitialDelay(150);
            ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);

            // store, because frame.pack() overwrites these
            minimumSize = frame.getMinimumSize();
            preferredSize = frame.getPreferredSize();
            frame.setBounds(x, y, minimumSize.width, minimumSize.height);

            if (!isMainFrame) {
                frame.setType(Window.Type.UTILITY);
            }

            frame.pack();

            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    dispose();
                    if (isMainFrame) {
                        componentHolder.getHueManager().shutdown();
                    }
                }
            });

            frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            frame.setVisible(true);
        });
    }

    void runOnSwingThread(Runnable toRun) {
        if (SwingUtilities.isEventDispatchThread()) {
            toRun.run();
        } else {
            SwingUtilities.invokeLater(toRun);
        }
    }

    HueManager getHueManager() {
        return componentHolder.getHueManager();
    }

    @Override
    public JFrame getJFrame() {
        return frame;
    }

    @Override
    public void dispose() {
        frame.dispose();
        onWindowClose();
    }

    protected abstract void onWindowClose();
}
