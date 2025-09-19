package pw.wunderlich.lightbeat.hue.bridge.color;

/**
 * Class represents a color.
 */
public interface Color {

    int getRGB();

    float getHue();

    float getSaturation();

    /**
     * @param derivationRange range for new hue and saturation values, can be both added and subtracted
     * @return new color that is similar from this one
     */
    Color getDerivedColor(double derivationRange);

    /**
     * @param color color to compare to
     * @param colorRandomizationRange range that was used for a potential {@link #getDerivedColor(double)} call
     * @return true if supplied argument could be a result of calling {@link #getDerivedColor(double)}
     *         or if it is the same color
     */
    boolean isSimilar(Color color, double colorRandomizationRange);
}
