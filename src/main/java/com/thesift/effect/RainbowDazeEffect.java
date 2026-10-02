package com.thesift.effect;

import com.thesift.registry.ModChrome;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * Rainbow Daze: what a soak in Chrome leaves you with. Nothing is hurt or healed; the world just
 * swims, sways and slowly turns through the colours of the rainbow (all drawn by
 * client/ChromeClient), and rainbow motes drift round anyone who has it.
 */
public class RainbowDazeEffect extends MobEffect {
    /** How long the daze lingers after you climb out of Chrome. */
    public static final int LINGER = 160;

    public RainbowDazeEffect(MobEffectCategory category, int color) {
        // looked up when the particles are made: particle types register after mob effects
        super(category, color, instance -> ModChrome.RAINBOW_MOTE.get());
    }

    /** Called every tick something soaks in Chrome: tops the daze up without resending it each tick. */
    public static void soak(LivingEntity living) {
        MobEffectInstance current = living.getEffect(ModChrome.RAINBOW_DAZE);
        if (current == null || current.getDuration() < LINGER - 40) {
            living.addEffect(new MobEffectInstance(ModChrome.RAINBOW_DAZE, LINGER, 0, false, true, true));
        }
    }
}
