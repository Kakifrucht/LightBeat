package io.lightbeat.hue.light.color;

import static java.awt.Color.HSBtoRGB;
import static java.awt.Color.RGBtoHSB;

/**
 * Default {@link Color} implementation.
 * Contains constructors to be initialized via rgb or hue/saturation values.
 * Handles the conversions.
 */
public class LBColor implements Color {

    private static final double DERIVATION_BOUND = 0.02d;

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
    public Color getDerivedColor() {
        return new LBColor(getRandomizedFloat(hue), getRandomizedFloat(saturation));
    }

    private float getRandomizedFloat(float toRandomize) {
        // add random value between -DERIVATION_BOUND and +DERIVATION_BOUND
        double normalizationVal = 0.5d / DERIVATION_BOUND;
        double randomness = toRandomize + ((Math.random() / normalizationVal) - DERIVATION_BOUND);
        return (float) Math.min(Math.max(randomness, 0d), 1d);
    }

    @Override
    public boolean isSimilar(Color color) {

        if (color == null) {
            return false;
        }

        if (this.equals(color)) {
            return true;
        }

        float otherHue = color.getHue();
        float otherSaturation = color.getSaturation();

        return otherHue <= this.hue + DERIVATION_BOUND
                && otherHue >= this.hue - DERIVATION_BOUND
                && otherSaturation <= this.saturation + DERIVATION_BOUND
                && otherSaturation >= this.saturation - DERIVATION_BOUND;
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
