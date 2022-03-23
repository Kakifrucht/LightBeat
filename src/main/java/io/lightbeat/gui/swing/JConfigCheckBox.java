package io.lightbeat.gui.swing;

import io.lightbeat.config.Config;
import io.lightbeat.config.ConfigNode;

import javax.swing.*;

/**
 * Checkbox that is dependent on a given {@link ConfigNode} boolean that may execute a runnable
 * on change with {@link #toRunOnChange}.
 */
public class JConfigCheckBox extends JCheckBox implements ConfigComponent {

    private Runnable toRunOnChange;

    private final boolean def;


    public JConfigCheckBox(Config config, ConfigNode configNode) {
        super();

        this.def = config.getDefaultBoolean(configNode);

        boolean configValue = config.getBoolean(configNode);
        setSelected(configValue);

        addItemListener(e -> {
            boolean isSelected = isSelected();
            if (isSelected != def) {
                config.putBoolean(configNode, isSelected);
            } else {
                config.remove(configNode);
            }

            if (toRunOnChange != null) {
                toRunOnChange.run();
            }
        });
    }

    public void setToRunOnChange(Runnable toRunOnChange) {
        this.toRunOnChange = toRunOnChange;
        SwingUtilities.invokeLater(toRunOnChange);
    }

    @Override
    public void restoreDefault() {
        setSelected(def);
    }
}
