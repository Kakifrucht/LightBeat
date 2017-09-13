package io.lightbeat.hue.light.effect;

import com.philips.lighting.model.PHLight;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Determines three colors with same distances in between and flips them between two colors at a time,
 * cycling through the colors in the process.
 */
public class ColorFlipEffect extends AbstractThresholdEffect {

    private int[] hues = new int[3];
    /**
     * String is light identifier, if boolean is true set to first hue of cycle, else second one
     */
    private Map<String, Boolean> lightFlipDirection;

    private int hueIndex;
    private int nextHuesInBeats;

    private int hue1;
    private int hue2;


    public ColorFlipEffect() {
        super(0.6f, 0.3f);
    }

    @Override
    public void executeEffect() {

        List<PHLight> lights = lightUpdate.getLights();
        if (--nextHuesInBeats <= 0) {

            nextHuesInBeats = 2 + rnd.nextInt(4);
            lightFlipDirection.clear();

            // init lights to next colors
            hueIndex = getNextIndex();
            hue1 = hues[hueIndex];
            hue2 = hues[getNextIndex()];

            for (PHLight light : lights) {
                boolean initAsHue1 = rnd.nextBoolean();
                lightUpdate.getBuilder(light).setHue(initAsHue1 ? hue1 : hue2);
                lightFlipDirection.put(light.getUniqueId(), !initAsHue1);
            }
        } else {
            // flip lights
            for (PHLight light : lights) {
                boolean useHue1 = lightFlipDirection.get(light.getUniqueId());
                lightFlipDirection.put(light.getUniqueId(), !useHue1);
                lightUpdate.getBuilder(light).setHue(useHue1 ? hue1 : hue2);
            }
        }
    }

    private int getNextIndex() {
        int next = hueIndex + 1;
        return next < hues.length ? next : 0;
    }

    @Override
    void initializeEffect() {
        int baseHue = rnd.nextInt(65535);
        int differenceToNextHue = rnd.nextInt(14345) + 7500;

        hues = new int[3];
        this.hues[0] = baseHue;
        this.hues[1] = (baseHue + differenceToNextHue) % 65535;
        this.hues[2] = (baseHue + (differenceToNextHue * 2)) % 65535;

        lightFlipDirection = new HashMap<>();
        hueIndex = 0;
        nextHuesInBeats = 0;
    }
}
