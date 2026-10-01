package com.thesift.entity.boss;

import com.thesift.registry.ModSounds;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Howler - the wind. A hunched pipe organ on four legs. It plants its feet and draws a long breath
 * through the pipes on its back (they glow), then blasts it out of its horn of a head: a cone of
 * sound that throws everything in front of it far back.
 */
public class Howler extends OrchestraMinion {
    private static final double BLAST_RANGE = 12.0;

    public Howler(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 28.0).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void addMovementGoals() {
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 0.9, true));
    }

    @Override
    protected int windupTicks() {
        return 30;
    }

    @Override
    protected int cooldownTicks() {
        return 90;
    }

    @Override
    protected double minRange() {
        return 2.0;
    }

    @Override
    protected double maxRange() {
        return 11.0;
    }

    @Override
    protected void beginWindup(ServerLevel level, LivingEntity target) {
        this.playSound(ModSounds.HOWLER_INHALE.get(), 1.5F, 1.0F);
    }

    @Override
    protected void strike(ServerLevel level, LivingEntity target) {
        this.playSound(ModSounds.HOWLER_BLAST.get(), 3.0F, 0.9F + this.random.nextFloat() * 0.2F);
        Vec3 from = this.position().add(0, this.getBbHeight() * 0.6, 0);
        Vec3 dir = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(from).normalize();
        for (int i = 1; i < 11; i++) {
            Vec3 p = from.add(dir.scale(i * 1.1));
            if (i % 3 == 0) {
                level.sendParticles(ParticleTypes.SONIC_BOOM, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
            level.sendParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 3, i * 0.06, i * 0.06, i * 0.06, 0.02);
        }
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(this.blockPosition()).inflate(BLAST_RANGE))) {
            if (e == this || isBandmate(e)) {
                continue;
            }
            Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(from);
            double dist = to.length();
            if (dist > BLAST_RANGE || to.normalize().dot(dir) < 0.8) {
                continue;
            }
            double force = 1.0 - dist / BLAST_RANGE * 0.5;
            e.hurtServer(level, this.damageSources().mobAttack(this), 4.0F);
            e.push(dir.x * 2.2 * force, 0.4 * force, dir.z * 2.2 * force);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.HOWLER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.HOWLER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.HOWLER_DEATH.get();
    }

    @Override
    protected int burstColor() {
        return 0x7FF7EE;
    }
}
