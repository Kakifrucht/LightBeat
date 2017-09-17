package io.lightbeat.gui.frame;

import io.lightbeat.config.ConfigNode;
import io.lightbeat.gui.swing.JColorPanel;

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

    private JColorPanel colorSelectorPanel;
    private JPanel currentColorPanel;
    private JButton addColorButton;

    private JPanel selectedColorsPanel;

    private JFormattedTextField colorSetNameField;
    private JButton saveButton;

    private boolean isEditing;


    ColorSelectionFrame(MainFrame mainFrame) {
        this("New Color Set", mainFrame);
    }

    ColorSelectionFrame(MainFrame mainFrame, String setNameToEdit) {
        this("Edit Color Set", mainFrame);

        this.isEditing = true;

        colorSetNameField.setText(setNameToEdit);
        colorSetNameField.setEnabled(false);

        saveButton.setText("Edit Color Set");

        List<String> colorSetString = config.getStringList(ConfigNode.getCustomNode("color.sets." + setNameToEdit));
        for (String rgbString : colorSetString) {
            Color storedColor = new Color(Integer.parseInt(rgbString));
            addColoredPanel(storedColor);
            selectedColorsPanel.updateUI();
        }

        updateSaveButton();
    }

    private ColorSelectionFrame(String title, MainFrame mainFrame) {
        super(title, mainFrame.getJFrame().getX() + 10, mainFrame.getJFrame().getY() + 10);

        MouseAdapter selectorEvent = new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                updateCurrentColorPanel(e.getX(), e.getY());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                updateCurrentColorPanel(e.getX(), e.getY());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                updateCurrentColorPanel(e.getX(), e.getY());
            }

            private void updateCurrentColorPanel(int x, int y) {
                int width = colorSelectorPanel.getWidth();
                int height = colorSelectorPanel.getHeight();
                if (x > width || x < 0 || y > height || y < 0) {
                    return;
                }

                float hueValue = (float) x / width;
                int color = Color.HSBtoRGB(hueValue, 1.0f, 1.0f);
                currentColorPanel.setBackground(new Color(color));
            }
        };

        colorSelectorPanel.addMouseListener(selectorEvent);
        colorSelectorPanel.addMouseMotionListener(selectorEvent);

        addColorButton.addActionListener(e -> {
            addColoredPanel(currentColorPanel.getBackground());
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

            // if not editing, check for duplicate name and add to color list
            if (!isEditing) {
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
            }

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

    private void addColoredPanel(Color color) {
        JPanel coloredPanel = new JPanel();
        coloredPanel.setVisible(true);
        coloredPanel.setBackground(color);
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
    }

    private void updateSaveButton() {
        saveButton.setEnabled(selectedColorsPanel.getComponentCount() >= 8 && colorSetNameField.getText().length() > 0);
    }

    @Override
    protected void onWindowClose() {}
}
