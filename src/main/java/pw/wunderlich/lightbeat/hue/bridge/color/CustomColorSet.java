package pw.wunderlich.lightbeat.hue.bridge.color;

import pw.wunderlich.lightbeat.config.Config;
import pw.wunderlich.lightbeat.config.ConfigNode;

import java.util.*;

/**
 * Set consisting of the colors stored in {@link Config}, accessed via set name.
 * List will pe parsed and return colors randomly upon calling {@link #getNextColor()}.
 */
public class CustomColorSet implements ColorSet {

    private final Config config;
    private final String name;
    private final List<Color> colors = new ArrayList<>();

    private final Queue<Color> colorQueue = new LinkedList<>();


    public CustomColorSet(Config config, String name) {
        this.config = config;
        this.name = name;

        for (String colorString : config.getStringList(ConfigNode.getCustomNode("color.sets." + name))) {
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

        if (colorQueue.isEmpty()) {

            Collections.shuffle(colors);

            for (Color color : colors) {
                colorQueue.add(color.getDerivedColor(getColorRandomizationRange()));
            }
        }

        return colorQueue.poll();
    }

    @Override
    public Color getNextColor(Color differentFrom) {

        Color nextColor = getNextColor();
        if (differentFrom == null) {
            return nextColor;
        }

        double colorRandomizationRange = getColorRandomizationRange();
        int maxIterations = 5;
        while (nextColor.isSimilar(differentFrom, colorRandomizationRange) && maxIterations-- > 0) {
            nextColor = getNextColor();
        }
        return nextColor;
    }

    @Override
    public List<Color> getColors() {
        return colors;
    }

    private double getColorRandomizationRange() {
        return (double) config.getInt(ConfigNode.COLOR_RANDOMIZATION_RANGE) / 100d;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CustomColorSet that = (CustomColorSet) o;
        if (!name.equals(that.name)) {
            return false;
        }

        // compare colors
        List<Color> thatColors = that.colors;
        if (thatColors.size() != colors.size()) {
            return false;
        }

        for (int i = 0; i < colors.size(); i++) {
            if (!colors.get(i).equals(thatColors.get(i))) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, colors);
    }
}
