package com.thesift.entity.boss;

import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * A Thumpling: one of the Thumper's hatchlings. It waddles after you and nips, and every so often
 * it rears back and beats the toy drum on its back - a little shockwave that bowls you over.
 */
public class Thumpling extends OrchestraMinion {
    public Thumpling(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 6;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 16.0).add(Attributes.MOVEMENT_SPEED, 0.24).add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.ARMOR, 8.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.4).add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void addMovementGoals() {
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.1, true));
    }

    @Override
    protected int windupTicks() {
        return 14;
    }

    @Override
    protected int cooldownTicks() {
        return 90;
    }

    @Override
    protected double minRange() {
        return 0.0;
    }

    @Override
    protected double maxRange() {
        return 3.0;
    }

    @Override
    protected void beginWindup(ServerLevel level, LivingEntity target) {
        this.playSound(SoundEvents.NOTE_BLOCK_SNARE.value(), 1.0F, 1.6F);
    }

    @Override
    protected void strike(ServerLevel level, LivingEntity target) {
        this.playSound(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 1.5F, 1.4F);
        level.sendParticles(ModParticles.RESONANCE_RING.get(), this.getX(), this.getY() + 0.1, this.getZ(), 0, 2.5, 0.0, 0.0, 1.0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(2.8, 1.0, 2.8))) {
            if (e != this && !isBandmate(e) && e.onGround() && e.hurtServer(level, this.damageSources().mobAttack(this), 3.0F)) {
                double dx = e.getX() - this.getX();
                double dz = e.getZ() - this.getZ();
                double d = Math.max(0.1, Math.sqrt(dx * dx + dz * dz));
                e.push(dx / d * 0.6, 0.35, dz / d * 0.6);
                e.hurtMarked = true;
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.THUMPLING_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.THUMPLING_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.THUMPLING_DEATH.get();
    }

    @Override
    protected int burstColor() {
        return 0x83B45C;
    }
}
