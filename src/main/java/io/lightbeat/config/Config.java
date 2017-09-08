package io.lightbeat.config;

import java.util.List;

/**
 * Implementing class allows access to the applications configuration.
 */
public interface Config {

    String get(ConfigNode node, String def);

    void put(ConfigNode node, String value);

    int getInt(ConfigNode node, int def);

    void putInt(ConfigNode node, int value);

    long getLong(ConfigNode node, long def);

    void putLong(ConfigNode node, long value);

    boolean getBoolean(ConfigNode node, boolean def);

    void putBoolean(ConfigNode node, boolean value);

    List<String> getStringList(ConfigNode node);

    void putStringList(ConfigNode node, List<String> list);
}
