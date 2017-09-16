package io.lightbeat.hue.light.color;

import io.lightbeat.config.Config;
import io.lightbeat.config.ConfigNode;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Set consisting of the colors stored in {@link Config}, accessed via set name.
 * List will pe parsed and return colors randomly upon calling {@link #getNextColor()}.
 */
public class CustomColorSet implements ColorSet {

    private final List<Color> colors = new ArrayList<>();
    private Queue<Color> colorQueue;


    public CustomColorSet(Config config, String setName) {
        for (String colorString : config.getStringList(ConfigNode.getCustomNode("color.sets." + setName))) {
            int color = Integer.parseInt(colorString);
            colors.add(new Color(color));
        }
    }

    @Override
    public Color getNextColor() {

        if (colorQueue == null || colorQueue.isEmpty()) {
            Collections.shuffle(colors);
            colorQueue = new LinkedList<>(colors);
        }

        return colorQueue.poll();
    }
}
