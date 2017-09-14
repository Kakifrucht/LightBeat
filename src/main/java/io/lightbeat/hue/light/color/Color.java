package io.lightbeat.hue.light.color;

/**
 * Color represented in hue/saturation format.
 */
public class Color {

    private final int hue;
    private final int saturation;


    public Color(int hue, int saturation) {
        this.hue = hue;
        this.saturation = saturation;
    }

    public int getHue() {
        return hue;
    }

    public int getSaturation() {
        return saturation;
    }
}
