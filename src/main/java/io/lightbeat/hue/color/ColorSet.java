package io.lightbeat.hue.color;

import java.util.List;

/**
 * Implementing class returns a {@link Color} upon querying {@link #getNextColor()}.
 */
public interface ColorSet {

    /**
     * Get the next color in this set.
     *
     * @return color
     */
    Color getNextColor();

    /**
     * Get the next color in this set which should be different from the given color.
     *
     *
     * @param differentFrom return value should be different from this parameter (no guarantee)
     * @return color
     */
    Color getNextColor(Color differentFrom);

    /**
     * Get all colors in this set. Returns null if colors are dynamically determined or random.
     *
     * @return list of colors or null
     */
    List<Color> getColors();
}
