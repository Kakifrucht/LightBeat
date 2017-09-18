package io.lightbeat.gui.swing;

import io.lightbeat.config.Config;
import io.lightbeat.config.ConfigNode;

import javax.swing.*;

/**
 * Checkbox that is dependant on a given {@link ConfigNode} boolean that may execute a runnable
 * on change with {@link #toRunOnChange}.
 */
public class JConfigCheckBox extends JCheckBox {

    private Runnable toRunOnChange;


    public JConfigCheckBox(Config config, ConfigNode configNode) {

        boolean configValue = config.getBoolean(configNode);
        setSelected(configValue);

        addItemListener(e -> {
            config.putBoolean(configNode, isSelected());
            if (toRunOnChange != null) {
                toRunOnChange.run();
            }
        });
    }

    public void setToRunOnChange(Runnable toRunOnChange) {
        this.toRunOnChange = toRunOnChange;
        SwingUtilities.invokeLater(toRunOnChange);
    }
}
