package com.thesift.world;

import net.minecraft.world.phys.Vec3;

/**
 * Ground-shaking moments - a titan's footfall, its roar - that the client turns into camera shake.
 * The server never reads this: entities call {@link #at} from their client-side entity events, and
 * the client's camera fades the shake with distance from the source and with time.
 */
public final class Rumble {
    /** Where the strongest live shake comes from, how strong it is, and how far it carries. */
    public static Vec3 source = Vec3.ZERO;
    public static float strength;
    public static float radius = 1.0F;
    public static int ticks;
    public static int duration = 1;

    private Rumble() {
    }

    /** A shake of `strength` (roughly degrees of wobble) felt up to `radius` blocks away, for `ticks` ticks. */
    public static void at(Vec3 from, float strength, float radius, int ticks) {
        float now = ticks > 0 && Rumble.ticks > 0 ? Rumble.strength * Rumble.ticks / (float) Rumble.duration : 0.0F;
        if (strength < now) {
            return;
        }
        Rumble.source = from;
        Rumble.strength = strength;
        Rumble.radius = Math.max(1.0F, radius);
        Rumble.ticks = ticks;
        Rumble.duration = Math.max(1, ticks);
    }

    /** How hard it shakes at `pos` right now (0 when nothing is shaking). */
    public static float felt(Vec3 pos) {
        if (ticks <= 0 || strength <= 0.0F) {
            return 0.0F;
        }
        double d = pos.distanceTo(source);
        float falloff = (float) Math.max(0.0, 1.0 - d / radius);
        return strength * falloff * falloff * ticks / (float) duration;
    }

    public static void tick() {
        if (ticks > 0) {
            ticks--;
        }
    }
}
