package pw.wunderlich.lightbeat.gui.frame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pw.wunderlich.lightbeat.config.Config;
import pw.wunderlich.lightbeat.hue.bridge.HueManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Abstract implementation of {@link HueFrame} that handles standard frame drawing.
 */
public abstract class AbstractFrame implements HueFrame {

    private static final Logger logger = LoggerFactory.getLogger(AbstractFrame.class);

    final ScheduledExecutorService executorService;
    final Config config;
    final HueManager hueManager;

    final JFrame frame = new JFrame();

    private final String frameTitle;
    int x;
    int y;


    AbstractFrame(Config config, ScheduledExecutorService executorService, HueManager hueManager, int x, int y) {
        this(config, executorService, hueManager, null, x, y);
    }

    AbstractFrame(Config config, String title, int x, int y) {
        this(config, null, null, title, x, y);
    }

    AbstractFrame(Config config, ScheduledExecutorService executorService, HueManager hueManager, String frameTitle, int x, int y) {
        this.frameTitle = "LightBeat" + (frameTitle != null ? (" - " + frameTitle) : "" );
        this.x = x;
        this.y = y;

        this.config = config;
        this.executorService = executorService;
        this.hueManager = hueManager;
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
            // ensure that frame is large enough
            frame.addComponentListener(new ComponentAdapter() {
                public void componentResized(ComponentEvent componentEvent) {
                    Component component = componentEvent.getComponent();
                    Dimension size = component.getSize();
                    Dimension minimumSize = component.getMinimumSize();
                    if (size.getWidth() < minimumSize.getWidth() || size.getHeight() < minimumSize.getHeight()) {
                        executorService.schedule(() -> runOnSwingThread(frame::pack), 500, TimeUnit.MILLISECONDS);
                    }
                }
            });

            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    dispose();
                    if (isPrimaryFrame) {
                        SwingWorker<Boolean, Void> shutdownWorker = new SwingWorker<>() {
                            @Override
                            protected Boolean doInBackground() {
                                logger.info("Attempting graceful shutdown of executor service...");
                                executorService.shutdown();
                                try {
                                    return executorService.awaitTermination(5, TimeUnit.SECONDS);
                                } catch (InterruptedException ex) {
                                    Thread.currentThread().interrupt();
                                    logger.warn("Shutdown wait was interrupted.");
                                    return false;
                                }
                            }

                            @Override
                            protected void done() {
                                try {
                                    boolean terminatedGracefully = get();
                                    if (terminatedGracefully) {
                                        logger.info("All tasks completed gracefully.");
                                    } else {
                                        logger.warn("Graceful shutdown timed out or was interrupted. Forcing shutdown...");
                                        executorService.shutdownNow();
                                    }
                                } catch (Exception ex) {
                                    logger.error("An error occurred during the shutdown process.", ex);
                                } finally {
                                    logger.info("Shutdown sequence complete. Terminating JVM.");
                                    Runtime.getRuntime().exit(0);
                                }
                            }
                        };

                        shutdownWorker.execute();
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
