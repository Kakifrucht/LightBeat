package pw.wunderlich.lightbeat.hue.bridge.light;

import pw.wunderlich.lightbeat.hue.bridge.light.controller.BrightnessController;
import pw.wunderlich.lightbeat.hue.bridge.light.controller.ColorController;
import pw.wunderlich.lightbeat.hue.bridge.light.controller.StrobeController;

/**
 * Implementing class represents a controllable light. Update its state via exposed controllers
 * or by getting the current builder with {@link #getStateBuilder()} and send it via {@link #doLightUpdate(int)}.
 */
public interface Light {

    io.github.zeroone3010.yahueapi.Light getBase();

    ColorController getColorController();

    BrightnessController getBrightnessController();

    StrobeController getStrobeController();

    /**
     * Get the lights state builder. If light is currently turned off this method returns a builder that will
     * be copied once the light was turned on again via {@link #setOn(boolean)}.
     *
     * @return builder
     */
    LightStateBuilder getStateBuilder();

    boolean isOn();

    /**
     * Turn this light on or off while preparing a temporary builder to be returned via {@link #getStateBuilder()}.
     * Does nothing if the light is already in it's desired state.
     * If a strobe is currently active it will cancel it.
     *
     * @param on whether to turn the light on or off
     */
    void setOn(boolean on);

    /**
     * Adds controller update information to this light's builder (retrieved with {@link #getStateBuilder()})
     * and updates the light accordingly. Will only send the update if resulting light state would cause an update.
     * Resets the builder for the next call of this method.
     *
     * @param transitionTime if transitionTime > 0, will cause a light fade as well in 100 ms steps
     *                       (fadeTime of 2 would be 200ms fade)
     */
    void doLightUpdate(int transitionTime);

    /**
     * Store the current physical light state for later restoration. Removes notification effects from the state.
     * State gets overridden if a state was stored before restoring it via {@link #restoreState()}.
     */
    void storeState();

    /**
     * Restore the previously stored light state.
     * If a state was previously saved via {@link #storeState()}, this method should schedule or send
     * an update that returns the light to that stored state and then discard the stored snapshot.
     * Does nothing if no state was stored.
     */
    void restoreState();
}
