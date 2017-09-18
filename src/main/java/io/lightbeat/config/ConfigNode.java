package io.lightbeat.config;

/**
 * Contains list of all config nodes used.
 */
public enum ConfigNode {

    AUTOSTART("autostart"),
    BEAT_MIN_TIME_BETWEEN("beat.mintimebetween"),
    BEAT_SENSITIVITY("beat.sensitivity"),
    BRIDGE_USERNAME("bridge.username"),
    BRIDGE_IPADDRESS("bridge.ipaddress"),
    BRIGHTNESS_MIN("brightness.min"),
    BRIGHTNESS_MAX("brightness.max"),
    BRIGHTNESS_SENSITIVITY("brightness.sensitivity"),
    CUSTOM(null),
    COLOR_SET_LIST("color.set.list"),
    COLOR_SET_PRESET_LIST("color.set.preset.list"),
    COLOR_SET_SELECTED("color.set.selected"),
    LAST_AUDIO_SOURCE("frame.lastaudiosource"),
    UPDATE_DISABLE_NOTIFICATION("frame.updatedisablenotification"),
    LIGHTS_DISABLED("lights.disabled"),
    LIGHTS_TRANSITION_TIME("lights.transitiontime"),
    SHOW_ADVANCED_SETTINGS("frame.showadvanced"),
    WINDOW_LOCATION("window.location");


    private String key;

    ConfigNode(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    private void setKey(String key) {
        if (CUSTOM.equals(this)) {
            this.key = key;
        }
    }

    public static ConfigNode getCustomNode(String key) {
        ConfigNode node = CUSTOM;
        node.setKey(key.replace(" ", "_"));
        return node;
    }
}
