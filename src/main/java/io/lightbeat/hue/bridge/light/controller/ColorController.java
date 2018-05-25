package io.lightbeat.hue.bridge.light.controller;

import io.lightbeat.hue.bridge.color.Color;
import io.lightbeat.hue.visualizer.effect.LightEffect;
import io.lightbeat.hue.bridge.light.Light;

/**
 * Controls the lights color and fade color.
 */
public class ColorController extends AbstractController {

    private volatile Color color;
    private volatile Color fadeColor;

    private volatile Color lastSetColor;
    private volatile boolean colorWasUpdated;
    private volatile boolean fadeColorWasUpdated;


    public ColorController(Light controlledLight) {
        super(controlledLight);
    }

    public void applyUpdates() {
        if (colorWasUpdated) {
            updateColor(color);
            if (colorWasUpdated) {
                colorWasUpdated = false;
            }
        }
    }

    @Override
    protected void applyFadeUpdatesExecute() {
        updateColor(fadeColor);
        if (fadeColorWasUpdated) {
            fadeColorWasUpdated = false;
        }
    }

    public void setColor(LightEffect effect, Color color) {
        if (canControl(effect)) {
            this.colorWasUpdated = true;
            this.color = color;
        }
    }

    /**
     * Sets the color that will be used for the {@link Light}'s fade effect.
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

    public void undoColorChange(LightEffect effect) {
        if (canControl(effect)) {
            if (colorWasUpdated) {
                setColor(effect, null);
            }

            if (fadeColorWasUpdated) {
                setFadeColor(effect, null);
            }
        }
    }

    private void updateColor(Color color) {
        if (color != null && !color.equals(lastSetColor)) {
            controlledLight.getStateBuilder().setColor(color);
            lastSetColor = color;
        }
    }
}
