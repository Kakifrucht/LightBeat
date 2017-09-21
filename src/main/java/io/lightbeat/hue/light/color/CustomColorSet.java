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

    private Queue<Color> colorQueue;
    private Set<Color> colorsAtQueueEnd;

    public CustomColorSet(Config config, String setName) {
        for (String colorString : config.getStringList(ConfigNode.getCustomNode("color.sets." + setName))) {
            int color = Integer.parseInt(colorString);
            colors.add(new LBColor(color));
        }
    }

    @Override
    public Color getNextColor() {

        if (colorQueue == null || colorQueue.isEmpty()) {

            Collections.shuffle(colors);

            // add color at the end if it was at the end of previous queue (no color duplication)
            if (colorsAtQueueEnd != null) {
                for (int i = 0; i < 3; i++) {
                    Color colorAt = colors.get(i);
                    if (colorsAtQueueEnd.contains(colorAt)) {
                        colors.remove(i);
                        colors.add(colorAt);
                    }
                }
            }

            colorsAtQueueEnd = new HashSet<>();
            for (int i = colors.size() - 1; i >= colors.size() - 3; i--) {
                colorsAtQueueEnd.add(colors.get(i));
            }

            colorQueue = new LinkedList<>(colors);
        }

        return colorQueue.poll();
    }

    @Override
    public List<Color> getColors() {
        return colors;
    }
}
