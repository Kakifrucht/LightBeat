package io.lightbeat.hue.effect;

import io.lightbeat.hue.color.Color;
import io.lightbeat.hue.color.ColorSet;
import io.lightbeat.hue.light.Light;

import java.util.HashMap;
import java.util.Map;

/**
 * Selects three colors and loops through them for one random light at a time.
 */
public class ColorLoopEffect extends AbstractThresholdEffect {

    private Map<Light, Integer> lightCurrentColorIndex;
    private Color[] colors;


    public ColorLoopEffect(ColorSet colorSet, double brightnessThreshold, double activationProbability) {
        super(colorSet, brightnessThreshold, activationProbability);
    }

    @Override
    void initialize() {
        colors = new Color[3];
        colors[0] = colorSet.getNextColor();
        colors[1] = colorSet.getNextColor(colors[0]);
        colors[2] = colorSet.getNextColor(colors[1]);

        lightCurrentColorIndex = new HashMap<>();
    }

    @Override
    void execute() {

        if (lightCurrentColorIndex.isEmpty()) {

            for (Light light : lightUpdate.getLights()) {
                if (light.getColorController().setControllingEffect(this)) {
                    lightCurrentColorIndex.put(light, 0);
                    light.getColorController().setFadeColor(this, colors[0]);
                }
            }

            if (lightCurrentColorIndex.isEmpty()) {
                return;
            }
        }

        Light lightToUpdate = lightUpdate.getLights().get(0);
        int lightColorIndex = lightCurrentColorIndex.get(lightToUpdate);

        Color fadeColor = colors[lightColorIndex];
        Color nextColor;

        lightColorIndex += 1;
        if (lightColorIndex > 2) {
            lightColorIndex = 0;
        }

        lightCurrentColorIndex.put(lightToUpdate, lightColorIndex);
        nextColor = colors[lightColorIndex];

        lightToUpdate.getColorController().setFadeColor(this, fadeColor);
        lightToUpdate.getColorController().setColor(this, nextColor);
    }

    @Override
    void executionDone() {
        for (Light light : lightCurrentColorIndex.keySet()) {
            light.getColorController().setFadeColor(this, colors[0]);
            light.getColorController().unsetControllingEffect(this);
        }
    }
}
