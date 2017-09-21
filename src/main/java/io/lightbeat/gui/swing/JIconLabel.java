package io.lightbeat.gui.swing;

import javax.swing.*;

/**
 * Label that displays holds multiple cached {@link Icon}'s that can be swapped with {@link #flipIcon()}.
 */
public class JIconLabel extends JLabel {

    private final Icon icon;
    private final Icon secondIcon;

    private boolean firstIconSelected = true;


    public JIconLabel(String resourceName, String secondResourceName) {
        super();
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
        return new ImageIcon(getClass().getResource(resourceName));
    }
}
