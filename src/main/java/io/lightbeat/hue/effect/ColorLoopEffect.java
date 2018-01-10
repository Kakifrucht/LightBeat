package io.lightbeat.hue.effect;

import io.lightbeat.hue.color.Color;
import io.lightbeat.hue.color.ColorSet;
import io.lightbeat.hue.light.Light;

import java.util.HashMap;
import java.util.Map;

/**
 * Selects three colors and loops through them for all lights that received an update from another effect.
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
                if (light.getColorController().canControl(this)) {
                    lightCurrentColorIndex.put(light, 0);
                    light.getColorController().setFadeColor(this, colors[0]);
                }
            }

            if (lightCurrentColorIndex.isEmpty()) {
                return;
            }
        }

        for (Light light : lightUpdate.getLights()) {
            if (lightCurrentColorIndex.containsKey(light) && light.getColorController().wasUpdated()) {
                light.getColorController().setFadeColor(this, getCurrentColor(light));
                light.getColorController().setColor(this, getNextColor(light));
            }
        }
    }

    private Color getCurrentColor(Light light) {
        return colors[lightCurrentColorIndex.get(light)];
    }

    private Color getNextColor(Light light) {

        int currentIndex = lightCurrentColorIndex.get(light) + 1;
        if (currentIndex > 2) {
            currentIndex = 0;
        }

        lightCurrentColorIndex.put(light, currentIndex);
        return colors[currentIndex];
    }
}
