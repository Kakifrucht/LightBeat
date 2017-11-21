package io.lightbeat.gui.swing;

import javax.swing.*;
import java.awt.*;

/**
 * Label that holds multiple cached {@link Icon}'s that can be swapped with {@link #flipIcon()}.
 */
public class JIconLabel extends JLabel {

    private final int width;
    private final int height;

    private ImageIcon icon;
    private ImageIcon secondIcon;

    private boolean firstIconSelected = true;


    public JIconLabel(String resourceName, String secondResourceName, int width, int height) {
        super();

        this.width = width;
        this.height = height;

        this.icon = getImageIcon(resourceName);
        setIcon(icon);

        if (secondResourceName != null) {
            this.secondIcon = getImageIcon(secondResourceName);
        } else {
            secondIcon = null;
        }
    }

    public void flipIcon() {
        if (secondIcon != null) {
            setIcon(firstIconSelected ? secondIcon : icon);
            firstIconSelected = !firstIconSelected;
        }
    }

    private ImageIcon getImageIcon(String resourceName) {
        ImageIcon icon = new ImageIcon(getClass().getResource(resourceName));
        Image image = icon.getImage();
        Image scaledImage = image.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImage);
    }
}
