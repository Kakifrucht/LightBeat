package io.lightbeat.gui.swing;


/**
 * Implementing class handles configuration related components
 * that can be reset to a default via {@link #restoreDefault()}.
 */
public interface ConfigComponent {

    /**
     * Restore this components setting to the default value.
     */
    void restoreDefault();
}
