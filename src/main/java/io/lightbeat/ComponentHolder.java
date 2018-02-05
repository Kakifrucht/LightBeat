package io.lightbeat;

import io.lightbeat.audio.AudioReader;
import io.lightbeat.audio.BeatEventManager;
import io.lightbeat.config.Config;
import io.lightbeat.hue.bridge.HueManager;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Implementing class instance should be easily referenceable, to allow retrieval
 * of project wide object instances without the need to pass them on manually.
 * Also allows to shut all of them down at once via {@link #shutdownAll()} and
 * getting the version number via {@link #getVersion()}.
 */
public interface ComponentHolder {

    ScheduledExecutorService getExecutorService();

    Config getConfig();

    AudioReader getAudioReader();

    BeatEventManager getAudioEventManager();

    HueManager getHueManager();

    void shutdownAll();

    String getVersion();
}
