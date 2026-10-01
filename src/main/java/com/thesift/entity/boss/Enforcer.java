package com.thesift.entity.boss;

import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Enforcer - the percussion. A squat living drum that lumbers after you and, when you get close,
 * raises both bone mallets and beats its own drum head: a shockwave rolls out along the ground,
 * hurting and throwing everything standing on it. Jump to let it pass under you.
 */
public class Enforcer extends OrchestraMinion {
    private static final double SHOCK_RADIUS = 5.5;

    public Enforcer(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 34.0).add(Attributes.MOVEMENT_SPEED, 0.22).add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7).add(Attributes.ARMOR, 6.0).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void addMovementGoals() {
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0, true));
    }

    @Override
    protected int windupTicks() {
        return 18;
    }

    @Override
    protected int cooldownTicks() {
        return 70;
    }

    @Override
    protected double minRange() {
        return 0.0;
    }

    @Override
    protected double maxRange() {
        return 5.0;
    }

    @Override
    protected void beginWindup(ServerLevel level, LivingEntity target) {
        this.playSound(ModSounds.ENFORCER_WINDUP.get(), 1.2F, 0.8F);
    }

    @Override
    protected void strike(ServerLevel level, LivingEntity target) {
        this.playSound(ModSounds.ENFORCER_SLAM.get(), 2.5F, 0.7F + this.random.nextFloat() * 0.1F);
        Vec3 c = this.position();
        // three rings rolling outward from the drum
        for (int i = 0; i < 3; i++) {
            level.sendParticles(ModParticles.RESONANCE_RING.get(), c.x, c.y + 0.1, c.z, 0, 2.0 + i * 1.8, 0.0, 0.0, 1.0);
        }
        BlockState ground = level.getBlockState(this.blockPosition().below());
        if (!ground.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), c.x, c.y + 0.1, c.z, 40, 2.5, 0.1, 2.5, 0.2);
        }
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(this.blockPosition()).inflate(SHOCK_RADIUS, 2.0, SHOCK_RADIUS))) {
            if (e == this || isBandmate(e) || !e.onGround() || e.distanceTo(this) > SHOCK_RADIUS) {
                continue;
            }
            Vec3 away = e.position().subtract(c).multiply(1, 0, 1).normalize();
            e.hurtServer(level, this.damageSources().mobAttack(this), 7.0F);
            e.push(away.x * 0.9, 0.55, away.z * 0.9);
            e.hurtMarked = true;
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ENFORCER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENFORCER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENFORCER_DEATH.get();
    }
}
