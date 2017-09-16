package io.lightbeat.hue.light.color;

import java.awt.*;

/**
 * Implementing class returns a {@link Color} upon querying {@link #getNextColor()}.
 */
public interface ColorSet {

    /**
     * Get the next color in this set.
     *
     * @return hue of color
     */
    Color getNextColor();
}
