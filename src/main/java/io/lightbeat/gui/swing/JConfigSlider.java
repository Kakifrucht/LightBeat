package io.lightbeat.gui.swing;

import io.lightbeat.config.Config;
import io.lightbeat.config.ConfigNode;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Function;

/**
 * Slider that sets a given {@link ConfigNode} to its value whenever it changes.
 * Allows restoring default value via {@link #restoreDefault()}.
 */
public class JConfigSlider extends JSlider implements ConfigComponent {

    private final int def;
    private final Function<Integer, String> valueFormatter;
    private final String defaultFormatted;

    private int lastValue;
    private String toolTipText;

    private JSlider boundedSlider;
    private boolean boundedMustBeHigher;
    private int minDifference;
    private boolean toolTipIsSet = false;


    /**
     * @param config LightBeat config object
     * @param nodeToChange node that this slider will read and write
     * @param valueFormatter optional function that takes an integer and returns a string that will be displayed
     *                       on the slider's tooltip on hover
     */
    public JConfigSlider(Config config, ConfigNode nodeToChange, Function<Integer, String> valueFormatter) {
        super();
        this.def = config.getDefaultInt(nodeToChange);
        this.valueFormatter = valueFormatter;
        this.defaultFormatted = valueFormatter != null ? valueFormatter.apply(def) : String.valueOf(def);

        // temporary until UI designer overwrites value
        setMaximum(Integer.MAX_VALUE);

        lastValue = config.getInt(nodeToChange);
        setValue(lastValue);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!toolTipIsSet) {
                    updateTooltip();
                }
            }
        });

        addChangeListener(e -> {
            if (!getValueIsAdjusting()) {
                int value = getValue();
                int actualValue = value;

                if (boundedSlider != null) {
                    if (boundedMustBeHigher) {
                        actualValue = Math.min(boundedSlider.getValue() - minDifference, value);
                    } else {
                        actualValue = Math.max(boundedSlider.getValue() + minDifference, value);
                    }
                }

                if (actualValue != value) {
                    setValue(actualValue);
                    return;
                }

                if (lastValue != value) {
                    lastValue = value;
                    updateTooltip();
                    if (lastValue != def) {
                        config.putInt(nodeToChange, lastValue);
                    } else {
                        config.remove(nodeToChange);
                    }
                }
            }
        });
    }

    public void setBoundedSlider(JSlider boundedSlider, boolean boundedMustBeHigher, int minDifference) {
        this.boundedSlider = boundedSlider;
        this.boundedMustBeHigher = boundedMustBeHigher;
        this.minDifference = minDifference;
    }

    @Override
    public void restoreDefault() {
        setValue(def);
    }

    private void updateTooltip() {

        if (toolTipText == null) {
            toolTipText = "<html>" + getToolTipText();
        }

        String valueFormatted = String.valueOf(getValue());
        if (valueFormatter != null) {
            valueFormatted = valueFormatter.apply(getValue());
        }

        setToolTipText(String.format("%s<br><br>Current Value: %s | Default: %s</html>", toolTipText, valueFormatted, defaultFormatted));
        if (!toolTipIsSet) {
            toolTipIsSet = true;
        }
    }
}
