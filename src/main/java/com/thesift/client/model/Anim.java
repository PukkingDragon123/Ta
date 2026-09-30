package com.thesift.client.model;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.AnimationState;

/** Small easing helpers for procedural animation. */
public final class Anim {
    public static final float DEG = Mth.DEG_TO_RAD;

    private Anim() {
    }

    /** Seconds since the animation state was started, or -1 if not running. */
    public static float seconds(AnimationState state, float ageInTicks) {
        return state.isStarted() ? state.getTimeInMillis(ageInTicks) / 1000.0F : -1.0F;
    }

    public static float clamp01(float t) {
        return Mth.clamp(t, 0.0F, 1.0F);
    }

    public static float smooth(float t) {
        t = clamp01(t);
        return t * t * (3.0F - 2.0F * t);
    }

    /** Ease-out with a little overshoot, for snappy "pop" motions. */
    public static float backOut(float t) {
        t = clamp01(t);
        float c1 = 1.70158F;
        float c3 = c1 + 1.0F;
        return 1.0F + c3 * (float) Math.pow(t - 1.0F, 3) + c1 * (float) Math.pow(t - 1.0F, 2);
    }

    /** 0 -> 1 -> 0 bump over [start, end] with smooth edges; holds at 1 between the fades. */
    public static float envelope(float t, float start, float attack, float hold, float release) {
        if (t < start) {
            return 0.0F;
        }
        float x = t - start;
        if (x < attack) {
            return smooth(x / attack);
        }
        x -= attack;
        if (x < hold) {
            return 1.0F;
        }
        x -= hold;
        return x < release ? 1.0F - smooth(x / release) : 0.0F;
    }
}
