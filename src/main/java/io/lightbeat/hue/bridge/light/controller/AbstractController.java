package io.lightbeat.hue.bridge.light.controller;

import io.lightbeat.hue.visualizer.effect.LightEffect;
import io.lightbeat.hue.bridge.light.Light;

/**
 * Extending classes control certain aspects (like color control) of the light.
 * Controllers offer the ability for {@link LightEffect}'s to reserve the control over that aspect via
 * {@link #setControllingEffect(LightEffect)}. Manual reservations are however not necessary and depending on
 * the implementation implied.
 */
public abstract class AbstractController {

    final Light controlledLight;
    private volatile LightEffect controllingEffect;


    AbstractController(Light controlledLight) {
        this.controlledLight = controlledLight;
    }

    public boolean canControl(LightEffect effect) {
        return controllingEffect == null || controllingEffect.equals(effect);
    }

    /**
     * Manually reserve the ability to edit this controller's stored values.
     * Usually control must be manually given back by calling {@link #unsetControllingEffect(LightEffect)}.
     *
     * @param effect effect that reserves the right to change this controller's values
     * @return true if control was given to or {@link #canControl(LightEffect)} returns true
     */
    public boolean setControllingEffect(LightEffect effect) {

        if (effect.equals(controllingEffect)) {
            return true;
        }

        if (canControl(effect)) {
            this.controllingEffect = effect;
            return true;
        }

        return false;
    }

    /**
     * Unreserve this controller.
     *
     * @param effect effect to deregister
     * @return true if effect was deregistered
     */
    public boolean unsetControllingEffect(LightEffect effect) {
        if (effect.equals(controllingEffect)) {
            controllingEffect = null;
            return true;
        }

        return false;
    }
}
