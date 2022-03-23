package io.lightbeat.gui.frame;

import io.lightbeat.ComponentHolder;
import io.lightbeat.config.Config;
import io.lightbeat.hue.bridge.HueManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Abstract implementation of {@link HueFrame} that handles standard frame drawing.
 */
public abstract class AbstractFrame implements HueFrame {

    final ComponentHolder componentHolder;
    final ScheduledExecutorService executorService;
    final Config config;
    final HueManager hueManager;

    final JFrame frame = new JFrame();

    private final String frameTitle;
    private final int x;
    private final int y;


    AbstractFrame(ComponentHolder componentHolder, int x, int y) {
        this(componentHolder, null, x, y);
    }

    AbstractFrame(ComponentHolder componentHolder, String frameTitle, int x, int y) {
        this.frameTitle = "LightBeat" + (frameTitle != null ? (" - " + frameTitle) : "" );
        this.x = x;
        this.y = y;

        this.componentHolder = componentHolder;
        this.config = componentHolder.getConfig();
        this.executorService = componentHolder.getExecutorService();
        this.hueManager = componentHolder.getHueManager();
    }

    void drawFrame(Container mainContainer, boolean isPrimaryFrame) {
        runOnSwingThread(() -> {

            frame.setTitle(frameTitle);
            frame.setContentPane(mainContainer);

            // load icons
            List<Image> icons = new ArrayList<>();
            for (int i = 16; i <= 64; i += 16) {
                icons.add(new ImageIcon(Objects.requireNonNull(getClass().getResource("/png/icon_" + i + ".png"))).getImage());
            }
            frame.setIconImages(icons);

            ToolTipManager.sharedInstance().setInitialDelay(150);
            ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);

            frame.setBounds(x, y, 10, 10);

            if (!isPrimaryFrame) {
                frame.setType(Window.Type.UTILITY);
            }

            frame.pack();

            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    if (isPrimaryFrame) {
                        componentHolder.shutdownAll();
                    } else {
                        dispose();
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
