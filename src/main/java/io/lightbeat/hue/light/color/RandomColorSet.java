package io.lightbeat.hue.light.color;

import java.awt.Color;
import java.util.*;

/**
 * Color set returning random colors with maximum saturation.
 * Color spectrum will be accessed evenly.
 */
public class RandomColorSet implements ColorSet {

    private Queue<Color> randomColors;

    private float currentColor = 0f;


    @Override
    public Color getNextColor() {

        if (randomColors == null || randomColors.isEmpty()) {

            List<Color> randomColors = new ArrayList<>();
            Random rnd = new Random();

            for (int i = 0; i < 16; i++) {
                currentColor += rnd.nextFloat() / 4f;
                currentColor %= 1f;
                int rgb = Color.HSBtoRGB(currentColor, 1f, 1f);
                randomColors.add(new Color(rgb));
            }

            Collections.shuffle(randomColors);
            this.randomColors = new LinkedList<>(randomColors);
        }

        return randomColors.poll();
    }
}