package pw.wunderlich.lightbeat;

import pw.wunderlich.lightbeat.audio.AudioReader;
import pw.wunderlich.lightbeat.audio.BeatEventManager;
import pw.wunderlich.lightbeat.config.Config;
import pw.wunderlich.lightbeat.hue.bridge.HueManager;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Implementing class offers references to all components of LightBeat.
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
