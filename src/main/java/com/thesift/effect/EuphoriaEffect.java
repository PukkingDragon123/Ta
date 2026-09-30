package com.thesift.effect;

import com.thesift.TheSift;
import com.thesift.registry.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Gentle regeneration, a skip in your step and little music notes following you around. */
public class EuphoriaEffect extends MobEffect {
    public EuphoriaEffect(MobEffectCategory category, int color) {
        super(category, color);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, TheSift.id("euphoria_speed"), 0.12F, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        this.addAttributeModifier(Attributes.JUMP_STRENGTH, TheSift.id("euphoria_jump"), 0.15F, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return tickCount % 10 == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplifier) {
        if (mob.tickCount % 50 == 0 && mob.getHealth() < mob.getMaxHealth()) {
            mob.heal(1.0F + amplifier);
        }
        double color = level.getRandom().nextFloat();
        level.sendParticles(ModParticles.SIFT_NOTE.get(), mob.getRandomX(0.8), mob.getY() + mob.getBbHeight() * 0.8, mob.getRandomZ(0.8),
                0, color, 0.0, 0.0, 1.0);
        return true;
    }
}
