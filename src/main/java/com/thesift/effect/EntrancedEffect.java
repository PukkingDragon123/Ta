package com.thesift.effect;

import com.thesift.registry.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.warden.Warden;

/**
 * Entranced by the Conductor's Staff: the creature forgets what it was doing, stands still and
 * sings - notes rise from its head and it hums along - until the spell wears off.
 */
public class EntrancedEffect extends MobEffect {
    public EntrancedEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (entity instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.setTarget(null);
            mob.setAggressive(false);
            mob.setDeltaMovement(0.0, Math.min(0.0, mob.getDeltaMovement().y), 0.0);
            if (mob instanceof Warden warden) {
                warden.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
                warden.getBrain().eraseMemory(MemoryModuleType.ROAR_TARGET);
                warden.getBrain().setMemoryWithExpiry(MemoryModuleType.VIBRATION_COOLDOWN, Unit.INSTANCE, 20L);
            }
        }
        if (entity.tickCount % 6 == 0) {
            level.sendParticles(ModParticles.SIFT_NOTE.get(), entity.getX() + (entity.getRandom().nextDouble() - 0.5) * 0.6,
                    entity.getY() + entity.getBbHeight() + 0.25, entity.getZ() + (entity.getRandom().nextDouble() - 0.5) * 0.6, 0,
                    entity.getRandom().nextDouble(), 0.0, 0.0, 1.0);
        }
        if (entity.tickCount % 12 == 0) {
            float[] scale = {0.75F, 0.84F, 1.0F, 1.12F, 1.26F, 1.5F};
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.NEUTRAL, 0.7F,
                    scale[entity.getRandom().nextInt(scale.length)]);
        }
        return true;
    }
}
