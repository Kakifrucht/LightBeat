package io.lightbeat;

import io.lightbeat.audio.AudioReader;
import io.lightbeat.audio.BeatEventManager;
import io.lightbeat.config.Config;
import io.lightbeat.gui.FrameManager;
import io.lightbeat.hue.bridge.HueManager;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Implementing class instance should be easily referenceable, to allow retrieval
 * of project wide object instances without the need to pass them on manually.
 */
public interface ComponentHolder {

    ScheduledExecutorService getExecutorService();

    Config getConfig();

    AudioReader getAudioReader();

    BeatEventManager getAudioEventManager();

    FrameManager getFrameManager();

    HueManager getHueManager();
}
