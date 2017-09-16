package io.lightbeat.gui.frame;

import io.lightbeat.config.ConfigNode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Sub frame that opens a color selection GUI. Stores data via {@link io.lightbeat.config.Config}.
 */
public class ColorSelectionFrame extends AbstractFrame {

    private JPanel mainPanel;

    private JSlider colorSlider;
    private JPanel currentColorPanel;
    private JButton addColorButton;

    private JPanel selectedColorsPanel;

    private JFormattedTextField colorSetNameField;
    private JButton saveButton;


    ColorSelectionFrame(MainFrame mainFrame, int x, int y) {
        super("New Color Set", x, y);

        colorSlider.addChangeListener(e -> {
            int color = Color.HSBtoRGB((float) colorSlider.getValue() / 1000f, 1.0f, 1.0f);
            currentColorPanel.setBackground(new Color(color));
        });

        addColorButton.addActionListener(e -> {
            JPanel coloredPanel = new JPanel();
            coloredPanel.setVisible(true);
            coloredPanel.setBackground(currentColorPanel.getBackground());
            coloredPanel.setToolTipText("Click to remove");

            coloredPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    selectedColorsPanel.remove(coloredPanel);
                    selectedColorsPanel.updateUI();

                    updateSaveButton();
                }
            });
            selectedColorsPanel.add(coloredPanel);
            selectedColorsPanel.updateUI();

            updateSaveButton();
        });

        colorSetNameField.addKeyListener(new KeyAdapter() {

            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == ' ') {
                    e.consume();
                    return;
                }
                updateSaveButton();
            }
        });

        saveButton.addActionListener(e -> {

            String setName = colorSetNameField.getText().replaceAll(" ", "");

            String random = "Random";
            List<String> storedPresets = config.getStringList(ConfigNode.COLOR_SET_LIST);
            storedPresets.add(random);
            for (String storedPreset : storedPresets) {
                if (storedPreset.equalsIgnoreCase(setName)) {
                    JOptionPane.showMessageDialog(frame,
                            "A color set with given name already exists.",
                            "Name Already Taken",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            storedPresets.remove(random);

            storedPresets.add(setName);
            config.putList(ConfigNode.COLOR_SET_LIST, storedPresets);

            List<Integer> colorList = new ArrayList<>();
            for (Component component : selectedColorsPanel.getComponents()) {
                if (component instanceof JPanel) {
                    JPanel panel = (JPanel) component;
                    colorList.add(panel.getBackground().getRGB());
                }
            }

            config.putList(ConfigNode.getCustomNode("color.sets." + setName), colorList);

            mainFrame.refreshColorSets();
            dispose();
        });

        frame.getRootPane().setDefaultButton(saveButton);
        drawFrame(mainPanel, false);
    }

    @Override
    protected void onWindowClose() {}

    private void updateSaveButton() {
        saveButton.setEnabled(selectedColorsPanel.getComponentCount() >= 8 && colorSetNameField.getText().length() > 0);
    }
}
