package io.lightbeat.hue.effect;

import io.lightbeat.LightBeat;
import io.lightbeat.hue.color.Color;
import io.lightbeat.hue.color.ColorSet;
import io.lightbeat.hue.light.Light;
import io.lightbeat.util.TimeThreshold;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Rapidly loops through three selected colors, which change every {@link #COLOR_CHANGE_IN_MILLIS} milliseconds,
 * for one selected and controllable light until the next beat is received.
 */
public class ColorStrobeEffect extends AbstractThresholdEffect {

    private static final long COLOR_CHANGE_IN_MILLIS = 3000L;
    private static final long MAXIMUM_DELAY_MILLIS = 400L;

    private Color[] colors;
    private TimeThreshold newColorThreshold = new TimeThreshold();

    private Future currentFuture;
    private Light currentLight;


    public ColorStrobeEffect(ColorSet colorSet, double brightnessThreshold, double activationProbability) {
        super(colorSet, brightnessThreshold, activationProbability);
    }

    @Override
    void initialize() {
        newColorThreshold.setCurrentThreshold(0);
    }

    @Override
    void execute() {

        if (currentFuture != null) {
            currentFuture.cancel(false);
            currentLight.getColorController().setFadeColor(this, colors[0]);
        }

        if (newColorThreshold.isMet()) {
            setNewColors();
        }

        for (Light light : lightUpdate.getLights()) {
            if (light.getColorController().canControl(this)) {
                currentLight = light;
                break;
            }
        }

        if (currentLight == null) {
            return;
        }

        long delay = lightUpdate.getTimeSinceLastBeat();
        while (delay > MAXIMUM_DELAY_MILLIS) {
            delay /= 2;
        }

        currentFuture = LightBeat.getComponentHolder().getExecutorService().scheduleAtFixedRate(new Runnable() {

            int currentColor = 0;

            @Override
            public void run() {

                if (++currentColor > 2) {
                    currentColor = 0;
                }

                currentLight.getColorController().setColor(ColorStrobeEffect.this, colors[currentColor]);
                currentLight.doLightUpdate(false);
            }
        }, 0L, delay, TimeUnit.MILLISECONDS);
    }

    @Override
    void executionDone() {

        if (currentFuture != null && !currentFuture.isDone()) {
            currentFuture.cancel(true);
        }

        currentFuture = null;
        currentLight = null;

        for (Light light : lightUpdate.getLights()) {
            if (light.getColorController().unsetControllingEffect(this)) {
                light.getColorController().setFadeColor(this, colors[0]);
            }
        }
    }

    private void setNewColors() {

        newColorThreshold.setCurrentThreshold(COLOR_CHANGE_IN_MILLIS);

        colors = new Color[3];
        colors[0] = colorSet.getNextColor(colors[2]);
        colors[1] = colorSet.getNextColor(colors[0]);
        colors[2] = colorSet.getNextColor(colors[1]);

        for (Light light : lightUpdate.getLights()) {
            if (light.getColorController().setControllingEffect(this)) {
                light.getColorController().setFadeColor(this, colors[0]);
            }
        }
    }
}
