package pw.wunderlich.lightbeat.hue.bridge.color;

import org.junit.jupiter.api.Test;

import static java.awt.Color.HSBtoRGB;
import static org.junit.jupiter.api.Assertions.*;

class LBColorTest {

    @Test
    void constructorFromRGB_setsFieldsCorrectly() {
        int rgb = HSBtoRGB(0.42f, 0.77f, 1f);
        LBColor color = new LBColor(rgb);

        assertEquals(rgb, color.getRGB(), "RGB should match constructor argument");
        // Basic sanity: hue/sat in [0,1]
        assertTrue(color.getHue() >= 0f && color.getHue() < 1f, "Hue should be in [0,1)");
        assertTrue(color.getSaturation() >= 0f && color.getSaturation() <= 1f, "Saturation should be in [0,1]");
    }

    @Test
    void constructorFromHSB_setsFieldsAndRgbCorrectly() {
        float h = 0.25f;
        float s = 0.6f;

        LBColor color = new LBColor(h, s);
        assertEquals(h, color.getHue(), 0f, "Hue should match constructor argument");
        assertEquals(s, color.getSaturation(), 0f, "Saturation should match constructor argument");
        assertEquals(HSBtoRGB(h, s, 1f), color.getRGB(), "RGB should be calculated from HSB with brightness=1");
    }

    @Test
    void getDerivedColor_withZeroBound_returnsSameColor() {
        LBColor base = new LBColor(0.33f, 0.5f);
        Color derived = base.getDerivedColor(0d);

        assertEquals(base, derived, "Derived color with bound=0 should be exactly the same color (rgb-equality)");
    }

    @Test
    void getDerivedColor_respectsBound_andRanges() {
        LBColor base = new LBColor(0.75f, 0.40f);
        double bound = 0.05;

        for (int i = 0; i < 100; i++) {
            Color d = base.getDerivedColor(bound);

            // Hue cyclical minimal distance
            double hueDiff = Math.abs(d.getHue() - base.getHue());
            hueDiff = Math.min(hueDiff, 1.0 - hueDiff);
            assertTrue(hueDiff <= bound + 1e-9, "Hue difference should be <= bound (cyclical)");

            // Saturation linear, clamped to [0,1]
            double satDiff = Math.abs(d.getSaturation() - base.getSaturation());
            assertTrue(satDiff <= bound + 1e-9, "Saturation difference should be <= bound (non-cyclical)");
            assertTrue(d.getSaturation() >= 0f && d.getSaturation() <= 1f, "Derived saturation should be within [0,1]");
        }
    }

    @Test
    void isSimilar_returnsTrueForEquality() {
        LBColor a = new LBColor(0.12f, 0.34f);
        LBColor b = new LBColor(a.getRGB());

        assertTrue(a.isSimilar(b, 0d), "Equal colors should be similar regardless of bound");
    }

    @Test
    void isSimilar_hueWrapAround_consideredSimilar() {
        LBColor a = new LBColor(0.99f, 1.0f);
        LBColor b = new LBColor(0.01f, 1.0f);

        assertTrue(a.isSimilar(b, 0.03), "Hues near 0/1 boundary should be similar within cyclical bound");
        assertFalse(a.isSimilar(b, 0.005), "Small bound should not consider them similar");
    }

    @Test
    void isSimilar_saturationIsNonCyclical_andRespectsBound() {
        LBColor lowSat = new LBColor(0.5f, 0.10f);
        LBColor highSat = new LBColor(0.5f, 0.90f);

        assertFalse(lowSat.isSimilar(highSat, 0.2), "Large saturation difference should not be similar");

        LBColor nearSat = new LBColor(0.5f, 0.28f);
        assertTrue(lowSat.isSimilar(nearSat, 0.2), "Saturation within bound should be similar");
        assertFalse(lowSat.isSimilar(nearSat, 0.15), "Saturation beyond bound should not be similar");
    }

    @Test
    void equalsAndHashCode_byRgb() {
        LBColor a = new LBColor(0.2f, 0.8f);
        LBColor b = new LBColor(a.getRGB());
        LBColor c = new LBColor(0.21f, 0.8f); // likely different rgb

        assertEquals(a, b, "Colors with same RGB should be equal");
        assertEquals(a.hashCode(), b.hashCode(), "Equal colors should have same hash code");

        assertNotEquals(a, c, "Different RGB should not be equal");
    }
}
