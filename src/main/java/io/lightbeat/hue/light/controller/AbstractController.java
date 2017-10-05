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

    final Light lightToControl;
    private volatile LightEffect controllingEffect;


    AbstractController(Light lightToControl) {
        this.lightToControl = lightToControl;
    }

    public boolean canControl(LightEffect effect) {
        return controllingEffect == null || controllingEffect.equals(effect);
    }

    /**
     * Manually reserve the ability to take edit this controllers stored values.
     * Usually control must be manually given back by calling {@link #unsetControllingEffect(LightEffect)}.
     *
     * @param effect effect that reserves the right to change this controllers values
     * @return true if control was given to the specified effect
     */
    public boolean setControllingEffect(LightEffect effect) {
        if (canControl(effect)) {
            this.controllingEffect = effect;
            return true;
        }

        return false;
    }

    public void unsetControllingEffect(LightEffect effect) {
        if (controllingEffect.equals(effect)) {
            controllingEffect = null;
        }
    }
}
