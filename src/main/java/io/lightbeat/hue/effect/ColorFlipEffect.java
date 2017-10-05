package io.lightbeat.hue.effect;

import io.lightbeat.hue.color.Color;
import io.lightbeat.hue.color.ColorSet;
import io.lightbeat.hue.light.Light;

import java.util.HashMap;
import java.util.Map;

/**
 * Determines three colors with same distances in between and flips them between two colors at a time,
 * cycling through the colors in the process.
 */
public class ColorFlipEffect extends AbstractThresholdEffect {

    private Color[] colors;
    /**
     * String is light identifier, if boolean is true set to first hue of cycle, else second one
     */
    private Map<Light, Boolean> lightFlipDirection;

    private int colorIndex;
    private int nextColorsInBeats;

    private Color color1;
    private Color color2;


    public ColorFlipEffect(ColorSet colorSet, float brightnessThreshold, float activationProbability) {
        super(colorSet, brightnessThreshold, activationProbability);
    }

    @Override
    void initialize() {

        colors = new Color[3];
        this.colors[0] = colorSet.getNextColor();
        this.colors[1] = colorSet.getNextColor(colors[0]);
        this.colors[2] = colorSet.getNextColor(colors[1]);

        lightFlipDirection = new HashMap<>();
        colorIndex = 0;
        nextColorsInBeats = 0;
    }

    @Override
    public void execute() {

        if (lightFlipDirection.isEmpty()) {

            for (Light light : lightUpdate.getLights()) {
                if (light.getColorController().setControllingEffect(this)) {
                    lightFlipDirection.put(light, false);
                }
            }

            if (lightFlipDirection.isEmpty()) {
                return;
            }
        }

        if (--nextColorsInBeats <= 0) {

            nextColorsInBeats = 4 + rnd.nextInt(4);

            // init lights to next colors
            colorIndex = getNextIndex();
            color1 = colors[colorIndex];
            color2 = colors[getNextIndex()];

            for (Light light : lightFlipDirection.keySet()) {
                flipLightColor(light, rnd.nextBoolean());
            }

        } else {
            for (Light light : lightFlipDirection.keySet()) {
                flipLightColor(light, lightFlipDirection.get(light));
            }
        }
    }

    private void flipLightColor(Light light, boolean useColor1) {
        lightFlipDirection.put(light, !useColor1);
        light.getColorController().setColor(this, useColor1 ? color1 : color2);
        light.getColorController().setFadeColor(this, useColor1 ? color2 : color1);
    }

    @Override
    public void executionDone() {
        for (Light light : lightFlipDirection.keySet()) {
            light.getColorController().unsetControllingEffect(this);
        }
    }

    private int getNextIndex() {
        int next = colorIndex + 1;
        return next < colors.length ? next : 0;
    }
}
