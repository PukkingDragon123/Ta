package com.thesift.effect;

import com.thesift.registry.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.warden.Warden;

/**
 * Deafened: the target cannot hear. For Wardens this keeps their vibration listener on cooldown,
 * stops them sniffing, and makes them forget whoever they were chasing when the effect lands.
 */
public class DeafenedEffect extends MobEffect {
    public DeafenedEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplifier) {
        if (mob instanceof Warden warden) {
            warden.getBrain().setMemoryWithExpiry(MemoryModuleType.VIBRATION_COOLDOWN, Unit.INSTANCE, 20L);
            warden.getBrain().setMemoryWithExpiry(MemoryModuleType.SNIFF_COOLDOWN, Unit.INSTANCE, 20L);
            warden.getBrain().eraseMemory(MemoryModuleType.DISTURBANCE_LOCATION);
            if (warden.tickCount % 10 == 0) {
                level.sendParticles(ModParticles.SIFT_NOTE.get(), warden.getX(), warden.getY() + warden.getBbHeight() + 0.3, warden.getZ(),
                        0, 0.55, 0.0, 0.0, 1.0);
            }
        }
        return true;
    }

    @Override
    public void onEffectStarted(LivingEntity mob, int amplifier) {
        if (mob instanceof Warden warden) {
            warden.getEntityAngryAt().ifPresent(warden::clearAnger);
            warden.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            warden.getBrain().eraseMemory(MemoryModuleType.ROAR_TARGET);
        }
    }
}
