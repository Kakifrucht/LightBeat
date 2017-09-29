package io.lightbeat.hue.light.color;

import io.lightbeat.config.Config;
import io.lightbeat.config.ConfigNode;

import java.util.*;

/**
 * Set consisting of the colors stored in {@link Config}, accessed via set name.
 * List will pe parsed and return colors randomly upon calling {@link #getNextColor()}.
 */
public class CustomColorSet implements ColorSet {

    private final List<Color> colors = new ArrayList<>();

    private Queue<Color> colorQueue = new LinkedList<>();


    public CustomColorSet(Config config, String setName) {
        for (String colorString : config.getStringList(ConfigNode.getCustomNode("color.sets." + setName))) {
            int color = Integer.parseInt(colorString);
            colors.add(new LBColor(color));
        }

        List<Color> colorsCopy = new ArrayList<>(colors);
        while (colors.size() < 12) {
            colors.addAll(colorsCopy);
        }
    }

    @Override
    public synchronized Color getNextColor() {

        if (colorQueue == null || colorQueue.isEmpty()) {

            Collections.shuffle(colors);

            colorQueue.clear();
            for (Color color : colors) {
                colorQueue.add(color.getDerivedColor());
            }
        }

        return colorQueue.poll();
    }

    @Override
    public Color getNextColor(Color differentFrom) {
        Color nextColor = getNextColor();

        int maxIterations = 5;
        while (nextColor.isSimilar(differentFrom) && maxIterations-- > 0) {
            nextColor = getNextColor();
        }
        return nextColor;
    }

    @Override
    public List<Color> getColors() {
        return colors;
    }
}
