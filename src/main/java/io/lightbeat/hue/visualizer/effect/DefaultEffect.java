package io.lightbeat.hue.visualizer.effect;

import io.lightbeat.ComponentHolder;
import io.lightbeat.hue.bridge.color.Color;
import io.lightbeat.hue.bridge.light.Light;
import io.lightbeat.hue.visualizer.LightUpdate;

/**
 * Default effect that updates the color of all main lights for the current light update.
 * It changes the color and fade color only when {@link LightUpdate#isBrightnessChange()} is true.
 */
public class DefaultEffect extends AbstractEffect {

    private Color color;
    private Color fadeColor;


    public DefaultEffect(ComponentHolder componentHolder) {
        super(componentHolder);
    }

    @Override
    void execute(LightUpdate lightUpdate) {

        if (color == null) {
            updateColor(lightUpdate);
        }

        if (lightUpdate.isBrightnessChange()) {
            updateBrightness(lightUpdate);
        }

        for (Light light : lightUpdate.getMainLights()) {
            light.getColorController().setColor(this, color);
        }
    }

    @Override
    public void noBeatReceived(LightUpdate lightUpdate) {
        updateBrightness(lightUpdate);
    }

    private void updateColor(LightUpdate lightUpdate) {
        fadeColor = colorSet.getNextColor(fadeColor);
        color = colorSet.getNextColor(fadeColor);

        for (Light light : lightUpdate.getLights()) {
            light.getColorController().setFadeColor(this, fadeColor);
        }
    }

    private void updateBrightness(LightUpdate lightUpdate) {

        boolean brightnessWasIncreased = false;
        for (Light light : lightUpdate.getLights()) {
            light.getBrightnessController().setBrightness(lightUpdate.getBrightness(), lightUpdate.getBrightnessFade());
            brightnessWasIncreased = light.getBrightnessController().isBrightnessWasIncreased();
        }

        // update colors if brightness was increased
        if (brightnessWasIncreased) {
            updateColor(lightUpdate);
        }
    }
}
