package io.lightbeat.config;

import java.util.List;

/**
 * Implementing class allows access to the applications configuration.
 */
public interface Config {

    String get(ConfigNode node);

    void put(ConfigNode node, String value);

    int getInt(ConfigNode node);

    int getDefaultInt(ConfigNode node);

    void putInt(ConfigNode node, int value);

    long getLong(ConfigNode node);

    void putLong(ConfigNode node, long value);

    boolean getBoolean(ConfigNode node);

    void putBoolean(ConfigNode node, boolean value);

    List<String> getStringList(ConfigNode node);

    void putList(ConfigNode node, List<?> list);

    void remove(ConfigNode node);
}
