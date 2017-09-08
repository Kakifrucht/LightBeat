package io.lightbeat.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Configuration handler for application. Access data via various get methods.
 */
public class LBConfig implements Config {

    private static final Logger logger = LoggerFactory.getLogger(LBConfig.class);

    private final Preferences preferences;


    public LBConfig() {
        preferences = Preferences.userNodeForPackage(getClass());
        preferences.addPreferenceChangeListener(evt -> logger.info("Set {} to value {}", evt.getKey(), evt.getNewValue()));
    }

    @Override
    public String get(ConfigNode node, String def) {
        return preferences.get(node.getKey(), def);
    }

    @Override
    public void put(ConfigNode node, String value) {
        preferences.put(node.getKey(), value);
    }

    @Override
    public int getInt(ConfigNode node, int def) {
        return preferences.getInt(node.getKey(), def);
    }

    @Override
    public void putInt(ConfigNode node, int value) {
        preferences.putInt(node.getKey(), value);
    }

    @Override
    public long getLong(ConfigNode node, long def) {
        return preferences.getLong(node.getKey(), def);
    }

    @Override
    public void putLong(ConfigNode node, long value) {
        preferences.putLong(node.getKey(), value);
    }

    @Override
    public boolean getBoolean(ConfigNode node, boolean def) {
        return preferences.getBoolean(node.getKey(), def);
    }

    @Override
    public void putBoolean(ConfigNode node, boolean value) {
        preferences.putBoolean(node.getKey(), value);
    }

    @Override
    public List<String> getStringList(ConfigNode node) {

        String value = preferences.get(node.getKey(), null);
        if (value == null || value.length() == 0) {
            return new ArrayList<>();
        }

        return new ArrayList<>(Arrays.asList(value.split("■")));
    }

    @Override
    public void putStringList(ConfigNode node, List<String> list) {

        if (list.isEmpty()) {
            preferences.remove(node.getKey());
            return;
        }

        StringBuilder listToString = new StringBuilder();
        for (String lightString : list) {
            listToString.append(lightString).append("■");
        }
        if (listToString.length() > 0) {
            listToString.setLength(listToString.length() - 1);
        }

        preferences.put(node.getKey(), listToString.toString());
    }
}
