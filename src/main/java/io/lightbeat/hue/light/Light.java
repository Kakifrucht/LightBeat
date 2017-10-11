package io.lightbeat.hue.light;

import com.philips.lighting.model.PHLightState;
import io.lightbeat.hue.light.controller.BrightnessController;
import io.lightbeat.hue.light.controller.ColorController;
import io.lightbeat.hue.light.controller.StrobeController;

/**
 * Implementing class represents a controllable light. Update it's state via it's controllers
 * or by getting the current builder with {@link #getStateBuilder()} and send it via {@link #doLightUpdate()}.
 */
public interface Light {

    PHLightState getLastKnownLightState();

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
     * Adds controller update information to this lights builder (retrieved with {@link #getStateBuilder()})
     * and updates the light accordingly. Will only send the update if resulting light state would do no changes.
     * Resets the builder for the next call of this method.
     */
    void doLightUpdate();

    void doLightUpdateFade();
}
