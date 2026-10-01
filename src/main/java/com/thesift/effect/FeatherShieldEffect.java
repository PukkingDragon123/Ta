package com.thesift.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Feather Shield (from the Crane Flute): a whirl of feathers around you. Arrows and other
 * projectiles are turned aside entirely and every other blow is softened to a quarter - but the
 * feathers fill your view (see the client overlay).
 */
public class FeatherShieldEffect extends MobEffect {
    public FeatherShieldEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return tickCount % 2 == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplifier) {
        double a = mob.tickCount * 0.5;
        for (int i = 0; i < 3; i++) {
            double t = a + i * Math.PI * 2 / 3;
            double y = mob.getY() + 0.3 + ((mob.tickCount * 0.07 + i * 0.33) % 1.0) * mob.getBbHeight();
            level.sendParticles(ParticleTypes.WHITE_ASH, mob.getX() + Math.cos(t) * 0.9, y, mob.getZ() + Math.sin(t) * 0.9, 2, 0.05, 0.05, 0.05, 0.0);
            level.sendParticles(ParticleTypes.CLOUD, mob.getX() + Math.cos(t) * 0.9, y, mob.getZ() + Math.sin(t) * 0.9, 1, 0.0, 0.0, 0.0, 0.0);
        }
        return true;
    }

    /** Game bus: the shield's protection. */
    public static void onIncomingDamage(LivingIncomingDamageEvent event, net.minecraft.core.Holder<MobEffect> shield) {
        LivingEntity e = event.getEntity();
        if (!e.hasEffect(shield)) {
            return;
        }
        if (event.getSource().getDirectEntity() instanceof Projectile) {
            event.setCanceled(true);
        } else if (!event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setAmount(event.getAmount() * 0.25F);
        }
        if (e.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CLOUD, e.getX(), e.getY() + 1.0, e.getZ(), 8, 0.5, 0.5, 0.5, 0.05);
        }
    }
}
