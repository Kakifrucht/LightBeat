package io.lightbeat.hue.light.controller;

import com.philips.lighting.model.PHLightState;
import io.lightbeat.hue.color.Color;
import io.lightbeat.hue.effect.LightEffect;
import io.lightbeat.hue.light.Light;
import io.lightbeat.hue.light.LightStateBuilder;

/**
 * Controls the lights color and fade color.
 */
public class ColorController extends AbstractController {

    private volatile Color color;
    private volatile Color fadeColor;

    private volatile boolean colorWasUpdated;
    private volatile boolean fadeColorWasUpdated;


    public ColorController(Light controlledLight) {
        super(controlledLight);
    }

    public void applyUpdates() {
        if (colorWasUpdated) {
            controlledLight.getStateBuilder().setColor(color);
            colorWasUpdated = false;
        }
    }

    @Override
    public void applyFadeUpdatesExecute(LightStateBuilder stateBuilder, PHLightState lastUpdate) {
        if (fadeColorWasUpdated || (lastUpdate != null && lastUpdate.getHue() != null)) {
            stateBuilder.setColor(fadeColor);

            if (fadeColorWasUpdated) {
                fadeColorWasUpdated = false;
            }
        }
    }

    public void setColor(LightEffect effect, Color color) {
        if (canControl(effect)) {
            this.colorWasUpdated = color != null && !color.equals(this.color);
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
}
