package io.lightbeat.hue.light.color;

/**
 *
 */
public interface ColorSet {

    /**
     * Get the next color in this set.
     *
     * @return hue of color
     */
    Color getNextColor();
}
