package com.thesift.client;

import java.util.Locale;
import net.minecraft.world.entity.LivingEntity;

/**
 * Facial expressions. Each mob paints a texture per expression it can make (see tools/mobs.py
 * and the 'expr' face maps); its renderer picks one every frame from what the mob is doing.
 */
public enum Expression {
    NEUTRAL,
    BLINK,
    HAPPY,
    ANGRY,
    HURT,
    SLEEP,
    DEAD;

    /** Texture file suffix: bulb_sky.png, bulb_sky_blink.png, ... */
    public String suffix() {
        return this == NEUTRAL ? "" : "_" + this.name().toLowerCase(Locale.ROOT);
    }

    /** What to show when a mob has no texture for this expression. */
    public Expression fallback() {
        return switch (this) {
            case DEAD -> HURT;
            case SLEEP -> BLINK;
            default -> NEUTRAL;
        };
    }

    /**
     * Blinks every few seconds, for three ticks, each mob on its own rhythm; now and then a quick
     * double blink.
     */
    public static boolean blinking(LivingEntity e, int period) {
        int t = Math.floorMod(e.tickCount + e.getId() * 37, period);
        return t < 3 || (Math.floorMod(e.getId(), 3) == 0 && t >= 6 && t < 8);
    }

    /** The usual priority: dying, hurting, asleep, happy, angry, blinking. */
    public static Expression pick(LivingEntity e, boolean angry, boolean happy, boolean asleep) {
        if (e.deathTime > 0 || e.isDeadOrDying()) {
            return DEAD;
        }
        if (e.hurtTime > 0) {
            return HURT;
        }
        if (asleep) {
            return SLEEP;
        }
        if (happy) {
            return HAPPY;
        }
        if (angry) {
            return blinking(e, 140) ? BLINK : ANGRY;
        }
        return blinking(e, 90) ? BLINK : NEUTRAL;
    }
}
