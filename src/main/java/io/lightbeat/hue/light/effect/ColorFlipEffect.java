package io.lightbeat.hue.light.effect;

import io.lightbeat.hue.light.Light;
import io.lightbeat.hue.light.color.Color;

import java.util.HashMap;
import java.util.List;
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


    public ColorFlipEffect(float brightnessThreshold, float activationProbability) {
        super(brightnessThreshold, activationProbability);
    }

    @Override
    void initializeEffect() {

        colors = new Color[3];
        this.colors[0] = lightUpdate.getColorSet().getNextColor();
        this.colors[1] = lightUpdate.getColorSet().getNextColor(colors[0]);
        this.colors[2] = lightUpdate.getColorSet().getNextColor(colors[1]);

        lightFlipDirection = new HashMap<>();
        colorIndex = 0;
        nextColorsInBeats = 0;
    }

    @Override
    public void executeEffect() {

        List<Light> lights = lightUpdate.getLights();
        if (--nextColorsInBeats <= 0) {

            nextColorsInBeats = 2 + rnd.nextInt(4);
            lightFlipDirection.clear();

            // init lights to next colors
            colorIndex = getNextIndex();
            color1 = colors[colorIndex];
            color2 = colors[getNextIndex()];

            for (Light light : lights) {
                boolean initAsHue1 = rnd.nextBoolean();
                light.getStateBuilder().setColor(initAsHue1 ? color1 : color2);
                lightFlipDirection.put(light, !initAsHue1);
            }
        } else {
            // flip lights
            for (Light light : lights) {
                boolean useHue1 = lightFlipDirection.get(light);
                lightFlipDirection.put(light, !useHue1);
                light.getStateBuilder().setColor(useHue1 ? color1 : color2);
            }
        }
    }

    private int getNextIndex() {
        int next = colorIndex + 1;
        return next < colors.length ? next : 0;
    }
}
