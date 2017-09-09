package io.lightbeat.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.prefs.Preferences;

/**
 * Configuration handler for application. Access data via various get methods.
 * Also contains static default values if no other value is stored.
 */
public class LBConfig implements Config {

    private static final Logger logger = LoggerFactory.getLogger(LBConfig.class);

    private final Preferences preferences;
    private final Map<ConfigNode, Integer> defaultInts = new HashMap<>();


    public LBConfig() {
        preferences = Preferences.userNodeForPackage(getClass());
        preferences.addPreferenceChangeListener(evt -> logger.info("Set {} to value {}", evt.getKey(), evt.getNewValue()));

        defaultInts.put(ConfigNode.BEAT_SENSITIVITY, 5);
        defaultInts.put(ConfigNode.BEAT_MIN_TIME_BETWEEN, 200);
        defaultInts.put(ConfigNode.BRIGHTNESS_MAX, 254);
        defaultInts.put(ConfigNode.BRIGHTNESS_SENSITIVITY, 20);
    }

    @Override
    public String get(ConfigNode node) {
        return preferences.get(node.getKey(), null);
    }

    @Override
    public void put(ConfigNode node, String value) {
        preferences.put(node.getKey(), value);
    }

    @Override
    public int getInt(ConfigNode node) {
        return preferences.getInt(node.getKey(), getDefaultInt(node));
    }

    @Override
    public int getDefaultInt(ConfigNode node) {
        return defaultInts.getOrDefault(node, 0);
    }

    @Override
    public void putInt(ConfigNode node, int value) {
        preferences.putInt(node.getKey(), value);
    }

    @Override
    public long getLong(ConfigNode node) {
        return preferences.getLong(node.getKey(), 0);
    }

    @Override
    public void putLong(ConfigNode node, long value) {
        preferences.putLong(node.getKey(), value);
    }

    @Override
    public boolean getBoolean(ConfigNode node) {
        return preferences.getBoolean(node.getKey(), false);
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
