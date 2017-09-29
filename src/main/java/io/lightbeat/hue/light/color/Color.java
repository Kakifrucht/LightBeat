package io.lightbeat.hue.light.color;

/**
 * Class represents a color.
 */
public interface Color {

    int getRGB();

    float getHue();

    float getSaturation();

    /**
     * @return new color that is similiar from this one
     */
    Color getDerivedColor();

    /**
     * @param color color to compare to
     * @return true if supplied argument could be a result of calling {@link #getDerivedColor()}
     *         or if {@link #equals(Object)} is true
     */
    boolean isSimilar(Color color);
}
