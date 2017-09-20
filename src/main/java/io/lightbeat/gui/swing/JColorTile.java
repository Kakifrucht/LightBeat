package io.lightbeat.gui.swing;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Colored panel that changes it's borders color upon hovering.
 */
public class JColorTile extends JPanel {


    public JColorTile(Color color) {
        super();

        setBackground(color);
        setPreferredSize(new Dimension(20, 20));
        setToolTipText("Click to remove");

        Border border = new LineBorder(Color.BLACK);
        Border hoverBorder = new LineBorder(Color.LIGHT_GRAY);

        setBorder(border);

        addMouseListener(new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent e) {
                setBorder(hoverBorder);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setBorder(border);
            }
        });
    }
}
