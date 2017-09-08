package io.lightbeat.gui.frame;

import io.lightbeat.ComponentHolder;
import io.lightbeat.config.Config;
import io.lightbeat.LightBeat;
import io.lightbeat.hue.HueManager;

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

    JFrame frame;

    private final int x;
    private final int y;

    AbstractFrame(int x, int y) {
        this.x = x;
        this.y = y;
    }

    void drawFrame(Container toDraw, String frameTitle) {
        frame = new JFrame();
        SwingUtilities.invokeLater(() -> {
            frame.setTitle("LightBeat " + frameTitle);
            frame.setContentPane(toDraw);

            // load icons
            List<Image> icons = new ArrayList<>();
            for (int i = 16; i <= 64; i += 16) {
                icons.add(new ImageIcon(getClass().getResource("/icon_" + i + ".png")).getImage());
            }
            frame.setIconImages(icons);

            ToolTipManager.sharedInstance().setInitialDelay(150);
            ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);

            int widthWindow = toDraw.getPreferredSize().width;
            int heightWindow = toDraw.getPreferredSize().height;
            frame.setBounds(x, y, widthWindow, heightWindow);

            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    dispose();
                    componentHolder.getHueManager().shutdown();
                }
            });

            frame.setVisible(true);
            frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
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
