package pw.wunderlich.lightbeat.hue.visualizer.effect;

import pw.wunderlich.lightbeat.AppTaskOrchestrator;
import pw.wunderlich.lightbeat.hue.bridge.color.Color;
import pw.wunderlich.lightbeat.hue.bridge.color.ColorSet;
import pw.wunderlich.lightbeat.hue.bridge.light.Light;
import pw.wunderlich.lightbeat.hue.visualizer.LightUpdate;
import pw.wunderlich.lightbeat.util.TimeThreshold;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Rapidly loops through three selected colors, which update every {@link #COLOR_CHANGE_IN_MILLIS} milliseconds,
 * for one selected and controllable light until the next beat is received.
 */
public class ColorStrobeEffect extends AbstractThresholdEffect {

    private static final long COLOR_CHANGE_IN_MILLIS = 5000L;
    private static final long MAXIMUM_STROBE_DELAY_MILLIS = 1000L;

    private final AppTaskOrchestrator taskOrchestrator;
    private final TimeThreshold newColorThreshold = new TimeThreshold();
    private Color[] colors;

    private Future<?> currentFuture;
    private Light currentLight;


    public ColorStrobeEffect(AppTaskOrchestrator taskOrchestrator, double brightnessThreshold, double activationProbability) {
        super(brightnessThreshold, activationProbability);
        this.taskOrchestrator = taskOrchestrator;
    }

    @Override
    void initialize(LightUpdate lightUpdate) {
        newColorThreshold.setCurrentThreshold(0);
    }

    @Override
    void execute(LightUpdate lightUpdate) {

        if (currentFuture != null) {
            currentFuture.cancel(true);
            resetFadeColor(currentLight);
        }

        if (newColorThreshold.isMet()) {
            setNewColors(lightUpdate);
        }

        currentLight = lightUpdate.getMainLights().stream()
                .filter(light -> light.getColorController().canControl(this))
                .findFirst()
                .orElse(null);

        if (currentLight == null) {
            return;
        }

        long delay = lightUpdate.getTimeSinceLastBeat();
        while (delay > MAXIMUM_STROBE_DELAY_MILLIS) {
            delay /= 2;
        }

        currentFuture = taskOrchestrator.schedulePeriodicTask(new Runnable() {

            int currentColor = 0;

            @Override
            public void run() {

                if (currentLight.getStrobeController().isStrobing()) {
                    return;
                }

                if (++currentColor > 2) {
                    currentColor = 0;
                }

                currentLight.getColorController().setColor(ColorStrobeEffect.this, colors[currentColor]);
                currentLight.doLightUpdate(0);
            }
        }, 0L, delay, TimeUnit.MILLISECONDS);
    }

    @Override
    void executionDone(LightUpdate lightUpdate) {

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

    private void setNewColors(LightUpdate lightUpdate) {

        newColorThreshold.setCurrentThreshold(COLOR_CHANGE_IN_MILLIS);

        ColorSet colorSet = lightUpdate.getColorSet();
        colors = new Color[3];
        colors[0] = colorSet.getNextColor(colors[2]);
        colors[1] = colorSet.getNextColor(colors[0]);
        colors[2] = colorSet.getNextColor(colors[1]);

        lightUpdate.getLights().stream()
                .filter(light -> light.getColorController().setControllingEffect(this))
                .forEach(this::resetFadeColor);
    }

    private void resetFadeColor(Light light) {
        light.getColorController().setFadeColor(this, colors[0]);
    }
}
