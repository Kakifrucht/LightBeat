package io.lightbeat.gui.frame;

import javax.swing.*;

/**
 * Sub frame that opens a color selection GUI. Stores data via {@link io.lightbeat.config.Config}.
 */
public class ColorSelectionFrame extends AbstractFrame {

    private JPanel mainPanel;
    private JButton saveButton;


    ColorSelectionFrame(int x, int y) {
        super("New Color Set", x, y);

        saveButton.addActionListener(e -> {
            //TODO store input in config
        });

        drawFrame(mainPanel, false);
    }

    @Override
    protected void onWindowClose() {}
}
