package com.thesift.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Sculk Corruption, the Conductor's curse: like Wither, but slower to hurt - one point every
 * three seconds (faster at higher levels) - and the longer it lasts the darker your sight grows,
 * until the world is a pinhole in the black (the darkening is drawn by the client overlay).
 */
public class SculkCorruptionEffect extends MobEffect {
    public SculkCorruptionEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        int every = Math.max(10, 60 >> amplifier);
        return tickCount % every == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplifier) {
        mob.hurtServer(level, mob.damageSources().wither(), 1.0F);
        level.sendParticles(ParticleTypes.SCULK_SOUL, mob.getX(), mob.getY() + mob.getBbHeight() * 0.6, mob.getZ(), 2, 0.3, 0.3, 0.3, 0.02);
        level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, mob.getX(), mob.getY() + mob.getBbHeight() * 0.4, mob.getZ(), 4, 0.3, 0.4, 0.3, 0.02);
        return true;
    }
}
