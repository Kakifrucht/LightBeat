package io.lightbeat.hue.light.color;

import java.awt.*;
import java.util.List;

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

    /**
     * Get all colors in this set. Returns null if colors are dynamically determined or random.
     *
     * @return list of colors or null
     */
    List<Color> getColors();
}
