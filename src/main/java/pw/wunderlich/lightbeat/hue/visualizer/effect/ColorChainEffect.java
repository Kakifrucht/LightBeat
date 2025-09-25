package pw.wunderlich.lightbeat.hue.visualizer.effect;

import pw.wunderlich.lightbeat.hue.bridge.color.Color;
import pw.wunderlich.lightbeat.hue.bridge.color.ColorSet;
import pw.wunderlich.lightbeat.hue.bridge.light.Light;
import pw.wunderlich.lightbeat.hue.visualizer.LightUpdate;

import java.util.ArrayList;
import java.util.List;

/**
 * Passes one color to all lights one by one and selects a new one after all have the new color.
 * Keeps the order of the lights and may strobe multiple lights at once, if {@link LightUpdate#getMainLights()}
 * contains more than one light.
 */
public class ColorChainEffect extends AbstractThresholdEffect {

    private List<Light> lightsInOrder;

    private Color currentColor;
    private Color currentFadeColor;
    private int currentIndex;


    public ColorChainEffect(double brightnessThreshold, double activationProbability) {
        super(brightnessThreshold, activationProbability);
    }

    @Override
    void initialize(LightUpdate lightUpdate) {
        lightsInOrder = new ArrayList<>();
        currentIndex = Integer.MAX_VALUE;
        currentColor = null;
    }

    @Override
    void execute(LightUpdate lightUpdate) {

        if (lightsInOrder.isEmpty()) {

            for (Light light : lightUpdate.getLights()) {
                if (light.getColorController().setControllingEffect(this)) {
                    light.getColorController().undoColorChange(this);
                    lightsInOrder.add(light);
                }
            }

            if (lightsInOrder.isEmpty()) {
                return;
            }
        }

        for (int i = 0; i < lightUpdate.getMainLights().size(); i++) {

            if (currentIndex++ >= lightsInOrder.size() - 1) {
                ColorSet colorSet = lightUpdate.getColorSet();
                currentColor = currentColor != null ? currentFadeColor : colorSet.getNextColor();
                currentFadeColor = colorSet.getNextColor(currentColor);
                currentIndex = 0;
            }

            Light nextLight = lightsInOrder.get(currentIndex);
            nextLight.getColorController().setColor(this, currentColor);
            nextLight.getColorController().setFadeColor(this, currentFadeColor);
            nextLight.getBrightnessController().forceBrightnessUpdate();
        }
    }

    @Override
    public void executionDone(LightUpdate lightUpdate) {
        for (Light light : lightsInOrder) {
            light.getColorController().setFadeColor(this, currentFadeColor);
            light.getColorController().unsetControllingEffect(this);
        }
    }
}
