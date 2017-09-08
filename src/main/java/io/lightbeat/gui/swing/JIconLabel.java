package io.lightbeat.gui.swing;

import com.sun.istack.internal.Nullable;

import javax.swing.*;

/**
 * Label that displays holds multiple cached {@link Icon}'s that can be swapped with {@link #flipIcon()}.
 */
public class JIconLabel extends JLabel {

    private final Icon icon;
    private final Icon secondIcon;

    private boolean firstIconSelected = true;


    public JIconLabel(String resourceName, @Nullable String secondResourceName) {
        this.icon = new ImageIcon(getClass().getResource("/" + resourceName));
        setIcon(icon);

        if (secondResourceName != null) {
            this.secondIcon = new ImageIcon(getClass().getResource("/" + secondResourceName));
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
}
