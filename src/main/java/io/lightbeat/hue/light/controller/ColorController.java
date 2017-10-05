package io.lightbeat.hue.light.controller;

import io.lightbeat.hue.color.Color;
import io.lightbeat.hue.effect.LightEffect;
import io.lightbeat.hue.light.Light;

/**
 * Controls the lights color and fade color.
 */
public class ColorController extends AbstractController {

    private volatile Color color;
    private volatile Color fadeColor;

    private volatile boolean colorWasUpdated;
    private volatile boolean fadeColorWasUpdated;


    public ColorController(Light lightToControl) {
        super(lightToControl);
    }

    public void applyUpdates() {
        if (colorWasUpdated) {
            lightToControl.getStateBuilder().setColor(color);
            colorWasUpdated = false;
        }
    }

    public void setColor(LightEffect effect, Color color) {
        if (canControl(effect)) {
            this.colorWasUpdated = color != null && !color.equals(this.color);
            this.color = color;
        }
    }

    /**
     * Sets the color that will be used for the {@link Light}'s {@link Light#doLightUpdateFade()}.
     *
     * @param effect effect that wants to set the color
     * @param fadeColor color to be used for the fading effect
     */
    public void setFadeColor(LightEffect effect, Color fadeColor) {
        if (canControl(effect)) {
            fadeColorWasUpdated = fadeColor != null && !fadeColor.equals(this.fadeColor);
            this.fadeColor = fadeColor;
        }
    }

    public Color getFadeColor() {
        return fadeColor;
    }

    public boolean isColorWasUpdated() {
        return colorWasUpdated;
    }

    public boolean isFadeColorWasUpdated() {
        boolean wasUpdated = fadeColorWasUpdated;
        fadeColorWasUpdated = false;
        return wasUpdated;
    }
}
