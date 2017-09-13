package io.lightbeat.gui.swing;

import io.lightbeat.config.Config;
import io.lightbeat.config.ConfigNode;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Slider that sets a given {@link ConfigNode} to it's value whenever it changes.
 * Allows to restore default value via {@link #restoreDefault()}.
 */
public class JConfigSlider extends JSlider {

    private final int def;

    private int lastValue;
    private String toolTipText;

    private JSlider boundedSlider;
    private boolean boundedMustBeHigher;
    private int minDifference;
    private boolean toolTipIsSet = false;


    public JConfigSlider(Config config, ConfigNode nodeToChange) {
        this.def = config.getDefaultInt(nodeToChange);

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
                    config.putInt(nodeToChange, lastValue);
                }
            }
        });
    }

    public void setBoundedSlider(JSlider boundedSlider, boolean boundedMustBeHigher, int minDifference) {
        this.boundedSlider = boundedSlider;
        this.boundedMustBeHigher = boundedMustBeHigher;
        this.minDifference = minDifference;
    }

    public void restoreDefault() {
        setValue(def);
    }

    private void updateTooltip() {

        if (toolTipText == null) {
            toolTipText = "<html>" + getToolTipText();
        }

        String customText = toolTipText + "<br><br>Current Value: " + getValue() + " | Default: " + def + "</html>";
        setToolTipText(customText);
        if (!toolTipIsSet) {
            toolTipIsSet = true;
        }
    }
}
