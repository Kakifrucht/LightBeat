package io.lightbeat.hue.light;

import io.lightbeat.hue.light.controller.BrightnessController;
import io.lightbeat.hue.light.controller.ColorController;
import io.lightbeat.hue.light.controller.StrobeController;

/**
 * Implementing class represents a controllable light. Update it's state via it's controllers
 * or by getting the current builder with {@link #getStateBuilder()} and send it via {@link #doLightUpdate()}.
 */
public interface Light {

    ColorController getColorController();

    BrightnessController getBrightnessController();

    StrobeController getStrobeController();

    /**
     * Get the lights state builder. If light is currently turned off this method returns a builder that will
     * be applied once the light was turned on again via {@link #setOn(boolean)}.
     *
     * @return builder
     */
    LightStateBuilder getStateBuilder();

    /**
     * Turn this light on or off while preparing a temporary builder to be returned via {@link #getStateBuilder()}.
     * Does nothing if the light is already in it's desired state.
     * If a strobe is currently active it will cancel it.
     *
     * @param on whether to turn the light on or off
     */
    void setOn(boolean on);

    boolean isOff();

    /**
     * Send this lights state with the information set in it's builder, retrieved with {@link #getStateBuilder()}.
     * Resets the builder for the next call of this method.
     */
    void doLightUpdate();

    void doLightUpdateFade();
}
