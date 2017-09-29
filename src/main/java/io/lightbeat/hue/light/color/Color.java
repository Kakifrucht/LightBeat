package io.lightbeat.hue.light.color;

/**
 * Class represents a color.
 */
public interface Color {

    int getRGB();

    float getHue();

    float getSaturation();

    /**
     * @param derivationRange range for new hue and saturation values, can be both added and substracted
     * @return new color that is similiar from this one
     */
    Color getDerivedColor(double derivationRange);

    /**
     * @param color color to compare to
     * @param colorRandomizationRange range that was used for a potential {@link #getDerivedColor(double)} call
     * @return true if supplied argument could be a result of calling {@link #getDerivedColor(double)}
     *         or if {@link #equals(Object)} is true
     */
    boolean isSimilar(Color color, double colorRandomizationRange);
}
