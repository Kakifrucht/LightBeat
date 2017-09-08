package io.lightbeat.hue.light.effect;

import com.philips.lighting.model.PHLight;
import io.lightbeat.hue.light.LightStateBuilder;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Effect to add a strobing, while turning all but one lights off and strobing the other ones (amount
 * dependant on light configuration). The strobe will be synchronized to the beat. May also randomly
 * turn one light off and on.
 */
public class StrobeEffect extends AbstractRandomEffect {

    private volatile PHLight light;
    private final Set<String> lightsStrobing = Collections.synchronizedSet(new HashSet<>());

    private int nextLightInBeats;
    private long lastBeatTimeStamp;


    public StrobeEffect() {
        super(0.8f, 1.0f, 0.05f);
    }

    @Override
    public void executeEffect() {

        List<PHLight> lights = new ArrayList<>(lightUpdate.getLights());
        if (lights.size() <= 1) {
            return;
        }

        int brightness = lightUpdate.getBrightness();
        if (nextLightInBeats-- <= 0) {

            Set<PHLight> lightsToUpdate = new HashSet<>();
            if (light != null) {
                // turn current light off
                lightUpdate.getBuilder(light).setOn(false);
                lightsToUpdate.add(light);
            } else {
                // turn all lights off at the beginning
                lightUpdate.copyBuilderToAll(LightStateBuilder.create().setOn(false));
                lightsToUpdate.addAll(lights);
            }

            for (PHLight phLight : lights) {
                if (light == null || !phLight.getUniqueId().equals(light.getUniqueId())) {
                    light = phLight;
                    lightUpdate.getBuilder(phLight).setOn(true);
                    if (light.getLastKnownLightState().getBrightness() != brightness) {
                        // due to turning all lights off at the beginning brightness must eventually be set again
                        lightUpdate.getBuilder(light).setBrightness(brightness);
                    }
                    lightsToUpdate.add(phLight);
                    break;
                }
            }

            // remove lights from update that don't need to be updated
            for (PHLight phLight : lights) {
                if (!lightsToUpdate.contains(phLight)) {
                    lightUpdate.removeLightFromUpdate(phLight);
                }
            }

            nextLightInBeats = 5 + rnd.nextInt(6);
        } else {

            // add alert to main light
            lightUpdate.getBuilder(light).setAlertMode(PHLight.PHLightAlertMode.ALERT_SELECT);

            // strobe random lights, depending on how many lights are in the configuration (minimum 1)
            List<PHLight> lightsToStrobe = new ArrayList<>();
            int amountToStrobe = Math.max((lights.size() - 1) / 2, 1);
            for (PHLight phLight : lights) {

                if (!phLight.getUniqueId().equals(light.getUniqueId()) && !lightsStrobing.contains(phLight.getUniqueId())) {

                    lightsToStrobe.add(phLight);
                    LightStateBuilder builder = lightUpdate.getBuilder(phLight);
                    builder.setOn(true);

                    if (phLight.getLastKnownLightState().getBrightness() != brightness) {
                        builder.setBrightness(brightness);
                    }

                    if (--amountToStrobe == 0) {
                        break;
                    }
                }
            }

            // remove disabled/non active lights from light update
            for (PHLight phLight : lights) {
                if (!phLight.getUniqueId().equals(light.getUniqueId()) && !lightsToStrobe.contains(phLight)) {
                    lightUpdate.removeLightFromUpdate(phLight);
                }
            }

            if (!lightsToStrobe.isEmpty()) {
                // strobe on beat, at least for 250 ms and at max for 500 ms
                long strobeDelay = System.currentTimeMillis() - lastBeatTimeStamp;
                while (strobeDelay > 500) {
                    strobeDelay /= 2;
                }
                strobeDelay = Math.max(strobeDelay, 250L);
                runStrobeThread(lightsToStrobe, strobeDelay, -1);
            }
        }

        lastBeatTimeStamp = System.currentTimeMillis();
    }

    @Override
    void initializeEffect() {
        light = null;
        nextLightInBeats = 0;
        lastBeatTimeStamp = Long.MAX_VALUE;
    }

    @Override
    public void executionDone() {
        lightUpdate.copyBuilderToAll(LightStateBuilder.create().setOn(true));
    }

    @Override
    void executeEffectOnceRandomly() {

        // strobe all lights but one
        List<PHLight> lights = lightUpdate.getLights();
        List<PHLight> lightsToStrobe = new ArrayList<>();
        for (int i = 1; i < lights.size(); i++) {
            PHLight light = lights.get(i);
            if (!lightsStrobing.contains(light.getUniqueId())) {
                lightsToStrobe.add(light);
                lightUpdate.getBuilder(light).setOn(false);
            }
        }

        runStrobeThread(lightsToStrobe, 350L, lightUpdate.getBrightness());
    }

    private void runStrobeThread(List<PHLight> lightsToStrobe, long strobeDelay, int brightness) {

        synchronized (lightsStrobing) {
            lightsToStrobe.stream()
                    .map(PHLight::getUniqueId)
                    .forEach(lightsStrobing::add);
        }

        executorService.schedule(() -> {
            boolean setOn = brightness >= 0;
            if (isActive || setOn) {

                LightStateBuilder strobeBuilder = LightStateBuilder.create().setOn(setOn);

                if (setOn) {
                    strobeBuilder.setBrightness(brightness);
                }

                for (PHLight lightToStrobe : lightsToStrobe) {
                    // only update if we turn it on or it is not the main light (fixes race condition)
                    if (setOn || !lightToStrobe.getUniqueId().equals(light.getUniqueId())) {
                        strobeBuilder.updateState(lightToStrobe);
                    }
                }
            }

            lightsToStrobe.forEach(light -> lightsStrobing.remove(light.getUniqueId()));
        }, strobeDelay, TimeUnit.MILLISECONDS);
    }
}
