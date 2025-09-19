package pw.wunderlich.lightbeat.gui.frame;

import javax.swing.*;

/**
 * Implementing class represents a window frame that is disposable.
 */
public interface HueFrame {

    /**
     * @return the associated JFrame
     */
    JFrame getJFrame();

    /**
     * Closes this frame.
     */
    void dispose();
}
