package com.thesift.entity.boss;

import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.world.phys.Vec3;

/**
 * A Whistling: a crane chick, all fluff and attitude. It flutters about, pecks, and from a few
 * blocks away it pipes a shrill little note at you through its flute beak.
 */
public class Whistling extends OrchestraMinion {
    public Whistling(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 12.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void addMovementGoals() {
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.1, true));
    }

    @Override
    protected int windupTicks() {
        return 12;
    }

    @Override
    protected int cooldownTicks() {
        return 60;
    }

    @Override
    protected double minRange() {
        return 2.5;
    }

    @Override
    protected double maxRange() {
        return 9.0;
    }

    @Override
    protected void beginWindup(ServerLevel level, LivingEntity target) {
        this.playSound(SoundEvents.NOTE_BLOCK_FLUTE.value(), 1.0F, 1.8F);
    }

    @Override
    protected void strike(ServerLevel level, LivingEntity target) {
        this.playSound(SoundEvents.NOTE_BLOCK_FLUTE.value(), 1.5F, 2.0F);
        Vec3 from = this.getEyePosition();
        Vec3 to = target.position().add(0, target.getBbHeight() * 0.6, 0);
        Vec3 d = to.subtract(from);
        DustParticleOptions dust = new DustParticleOptions(0x4FF0FF, 0.8F);
        for (int i = 0; i < d.length() * 3; i++) {
            Vec3 p = from.add(d.scale(i / (d.length() * 3)));
            level.sendParticles(dust, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        level.sendParticles(ModParticles.SIFT_NOTE.get(), to.x, to.y + 0.4, to.z, 2, 0.2, 0.2, 0.2, 1.0);
        if (this.hasLineOfSight(target)) {
            target.hurtServer(level, this.damageSources().sonicBoom(this), 2.0F);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // flutter: falls slowly and hops with a flap
        Vec3 v = this.getDeltaMovement();
        if (!this.onGround() && v.y < 0.0) {
            this.setDeltaMovement(v.multiply(1.0, 0.6, 1.0));
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.WHISTLING_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.WHISTLING_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.WHISTLING_DEATH.get();
    }

    @Override
    protected int burstColor() {
        return 0xF4F2EA;
    }
}
