package io.lightbeat.hue.bridge.color;

import static java.awt.Color.HSBtoRGB;
import static java.awt.Color.RGBtoHSB;

/**
 * Default {@link Color} implementation.
 * Contains constructors to be initialized via rgb or hue/saturation values.
 * Handles the conversions.
 */
public class LBColor implements Color {

    private final int rgb;
    private final float hue;
    private final float saturation;


    LBColor(int rgb) {

        this.rgb = rgb;

        // convert to hsb
        java.awt.Color color = new java.awt.Color(rgb);
        float[] hsb = RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);

        this.hue = hsb[0];
        this.saturation = hsb[1];
    }

    LBColor(float hue, float saturation) {
        this.rgb = HSBtoRGB(hue, saturation, 1f);
        this.hue = hue;
        this.saturation = saturation;
    }

    @Override
    public int getRGB() {
        return rgb;
    }

    @Override
    public float getHue() {
        return hue;
    }

    @Override
    public float getSaturation() {
        return saturation;
    }

    @Override
    public Color getDerivedColor(double derivationBound) {
        float newHue = getRandomizedFloat(this.hue, derivationBound, true);
        float newSaturation = getRandomizedFloat(this.saturation, derivationBound, false);
        return new LBColor(newHue, newSaturation);
    }

    private float getRandomizedFloat(float value, double bound, boolean isCyclical) {
        if (bound == 0d) {
            return value;
        }

        double randomOffset = (Math.random() * 2 * bound) - bound;
        double randomized = value + randomOffset;

        if (randomized < 0d) {
            return isCyclical ? (float) (randomized + 1d) : 0f;
        }
        if (randomized > 1d) {
            return isCyclical ? (float) (randomized - 1d) : 1f;
        }
        return (float) randomized;
    }

    @Override
    public boolean isSimilar(Color color, double derivationBound) {

        if (color == null) {
            return false;
        }

        if (this.equals(color)) {
            return true;
        }

        float otherHue = color.getHue();
        float otherSaturation = color.getSaturation();

        return otherHue <= this.hue + derivationBound
                && otherHue >= this.hue - derivationBound
                && otherSaturation <= this.saturation + derivationBound
                && otherSaturation >= this.saturation - derivationBound;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Color && rgb == ((Color) o).getRGB();
    }

    @Override
    public int hashCode() {
        return rgb;
    }
}
