package io.lightbeat.gui.frame;

import com.github.weisj.darklaf.components.color.PopupColorChooser;
import io.lightbeat.config.ConfigNode;
import io.lightbeat.gui.swing.JColorPanel;
import io.lightbeat.gui.swing.JColorTile;
import io.lightbeat.gui.swing.WrapLayout;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
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
    private JButton colorRandomizerButton;

    private JPanel selectedColorsPanel;

    private JTextField colorSetNameField;
    private JButton saveButton;

    private boolean isEditing;
    private String originalName;
    private float selectedSaturation = 1f;


    /**
     * Constructor to create a new set.
     *
     * @param mainFrame will receive a callback upon saving to refresh its contents
     */
    ColorSelectionFrame(MainFrame mainFrame) {
        this("New Color Set", mainFrame);
    }

    /**
     * Constructor to edit a set. Loads color data and makes name unchangeable.
     *
     * @param mainFrame will receive a callback upon saving to refresh its contents
     * @param setNameToEdit name of set to edit
     */
    ColorSelectionFrame(MainFrame mainFrame, String setNameToEdit) {
        this("Edit " + setNameToEdit, mainFrame);

        this.isEditing = true;
        this.originalName = setNameToEdit;

        colorSetNameField.setText(setNameToEdit);
        saveButton.setText("Edit Color Set");

        List<String> colorSetString = config.getStringList(ConfigNode.getCustomNode("color.sets." + setNameToEdit));
        for (String rgbString : colorSetString) {
            Color storedColor = new Color(Integer.parseInt(rgbString));
            addColorTile(storedColor);
        }
    }

    private ColorSelectionFrame(String title, MainFrame mainFrame) {
        super(mainFrame.componentHolder, title, mainFrame.getJFrame().getX() + 10, mainFrame.getJFrame().getY() + 10);

        MouseAdapter colorSelectEventAdapter = new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    PopupColorChooser.showColorChooser(colorSelectorPanel, currentColorPanel.getBackground(), color -> {

                        float[] hsb = new float[3];
                        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsb);

                        selectedSaturation = hsb[1];
                        currentColorPanel.setBackground(Color.getHSBColor(hsb[0], hsb[1], 1f));
                    }, () -> {});
                } else {
                    updateCurrentColorPanel(e.getX(), e.getY());
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                updateCurrentColorPanel(e.getX(), e.getY());
            }

            private void updateCurrentColorPanel(int x, int y) {

                int width = colorSelectorPanel.getWidth();
                int height = colorSelectorPanel.getHeight();

                int boundedX = Math.min(width, Math.max(x, 0));
                int boundedY = Math.min(height, Math.max(y, 0));

                float hue = (float) boundedX / width;
                selectedSaturation = (float) (height - boundedY) / height;
                int color = Color.HSBtoRGB(hue, selectedSaturation, 1.0f);

                currentColorPanel.setBackground(new Color(color));
            }
        };

        colorSelectorPanel.addMouseListener(colorSelectEventAdapter);
        colorSelectorPanel.addMouseMotionListener(colorSelectEventAdapter);

        currentColorPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                addColorTile(currentColorPanel.getBackground());
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                currentColorPanel.setBorder(new LineBorder(Color.LIGHT_GRAY));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                currentColorPanel.setBorder(new LineBorder(Color.BLACK));
            }
        });

        colorRandomizerButton.addActionListener(e -> {
            int rgb = Color.HSBtoRGB((float) Math.random(), selectedSaturation, 1.0f);
            currentColorPanel.setBackground(new Color(rgb));
        });

        selectedColorsPanel.setLayout(new WrapLayout(0));

        // restrict max input length
        colorSetNameField.setDocument(new PlainDocument() {
            @Override
            public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
                int newLength = getLength() + str.length();
                if (newLength <= 20) {
                    updateSaveButton(newLength);
                    super.insertString(offs, str, a);
                }
            }
        });

        colorSetNameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                updateSaveButton(-1);
            }
        });

        saveButton.addActionListener(e -> {

            String setName = colorSetNameField.getText();
            if (!setName.equalsIgnoreCase(originalName)) {

                if (setName.equalsIgnoreCase("random")) {
                    showNameTakenDialog();
                    return;
                }

                for (String setNameInList : config.getStringList(ConfigNode.COLOR_SET_LIST)) {
                    if (setNameInList.equalsIgnoreCase(setName)) {
                        showNameTakenDialog();
                        return;
                    }
                }
            }

            List<String> storedPresets = config.getStringList(ConfigNode.COLOR_SET_LIST);
            if (isEditing && !setName.equals(originalName)) {

                // replace in list, keep selected
                for (int i = 0; i < storedPresets.size(); i++) {
                    if (storedPresets.get(i).equals(originalName)) {
                        storedPresets.set(i, setName);
                        break;
                    }
                }

                config.remove(ConfigNode.getCustomNode("color.sets." + originalName));
                config.put(ConfigNode.COLOR_SET_SELECTED, setName);

            } else if (!isEditing) {
                storedPresets.add(setName);
            }

            List<Integer> colorList = new ArrayList<>();
            for (Component component : selectedColorsPanel.getComponents()) {
                if (component instanceof JPanel) {
                    JPanel panel = (JPanel) component;
                    colorList.add(panel.getBackground().getRGB());
                }
            }

            config.putList(ConfigNode.COLOR_SET_LIST, storedPresets);
            config.putList(ConfigNode.getCustomNode("color.sets." + setName), colorList);

            mainFrame.refreshColorSets();
            dispose();
        });

        frame.getRootPane().setDefaultButton(saveButton);
        drawFrame(mainPanel, false);
        runOnSwingThread(saveButton::requestFocus);
    }

    private void addColorTile(Color color) {

        JColorTile tile = new JColorTile(color);
        tile.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                selectedColorsPanel.remove(tile);
                revalidateSelectedColorsPanel();
            }
        });

        selectedColorsPanel.add(tile);
        revalidateSelectedColorsPanel();
    }

    private void updateSaveButton(int textFieldLength) {
        int setNameLength = colorSetNameField.getText().length();
        if (textFieldLength >= 0) {
            setNameLength = textFieldLength;
        }
        saveButton.setEnabled(selectedColorsPanel.getComponentCount() >= 3 && setNameLength > 0);
    }

    private void showNameTakenDialog() {
        JOptionPane.showMessageDialog(frame,
                "A color set with given name already exists.",
                "Name Already Taken",
                JOptionPane.ERROR_MESSAGE);
    }

    private void revalidateSelectedColorsPanel() {
        selectedColorsPanel.revalidate();
        selectedColorsPanel.repaint();
        updateSaveButton(-1);
    }

    @Override
    protected void onWindowClose() {}
}
