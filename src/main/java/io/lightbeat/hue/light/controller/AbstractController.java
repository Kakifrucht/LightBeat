package io.lightbeat.hue.light.controller;

import io.lightbeat.hue.effect.LightEffect;
import io.lightbeat.hue.light.Light;

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
     * Manually reserve the ability to take edit this controllers stored values.
     * Usually control must be manually given back by calling {@link #unsetControllingEffect(LightEffect)}.
     *
     * @param effect effect that reserves the right to change this controllers values
     * @return true if control was given to or {@link #canControl(LightEffect)} returns true
     */
    public boolean setControllingEffect(LightEffect effect) {
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

    public void applyFadeUpdates() {
        if (controlledLight.isOn() && !controlledLight.getStrobeController().isStrobing()) {
            applyFadeUpdatesExecute();
        }
    }

    protected abstract void applyFadeUpdatesExecute();
}
