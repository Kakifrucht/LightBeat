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
    BRIGHTNESS_FADE_DIFFERENCE("brightness.fade.difference"),
    BRIGHTNESS_FADE_TIME("brightness.fade.time"),
    BRIGHTNESS_MIN("brightness.min"),
    BRIGHTNESS_MAX("brightness.max"),
    CUSTOM(null),
    COLOR_RANDOMIZATION_RANGE("color.randomization"),
    COLOR_SET_LIST("color.set.list"),
    COLOR_SET_PRESET_LIST("color.set.preset.list"),
    COLOR_SET_SELECTED("color.set.selected"),
    EFFECT_ALERT("effect.alert"),
    EFFECT_COLOR_STROBE("effect.colorstrobe"),
    EFFECT_STROBE("effect.strobe"),
    LAST_AUDIO_SOURCE("frame.lastaudiosource"),
    LIGHT_AMOUNT_PROBABILITY("lights.amountprobability"),
    LIGHTS_DISABLED("lights.disabled"),
    SHOW_ADVANCED_SETTINGS("frame.showadvanced"),
    UPDATE_DISABLE_NOTIFICATION("frame.updatedisablenotification"),
    WINDOW_LOCATION("window.location"),
    WINDOW_LOOK_AND_FEEL("window.lookandfeel");


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
