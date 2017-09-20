package io.lightbeat.hue.light.color;

import static java.awt.Color.*;

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
    public int getHue() {
        return (int) (hue * 65535);
    }

    @Override
    public int getSaturation() {
        return (int) (saturation * 254);
    }
}
