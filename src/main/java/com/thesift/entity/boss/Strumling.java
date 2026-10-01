package com.thesift.entity.boss;

import com.thesift.registry.ModSounds;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** A Strumling: a quick, twitchy spiderling of the Strummer's brood. It skitters in and pounces. */
public class Strumling extends OrchestraMinion {
    public Strumling(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 4;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.34).add(Attributes.ATTACK_DAMAGE, 2.5)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void addMovementGoals() {
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.2, true));
    }

    @Override
    protected int windupTicks() {
        return 8;
    }

    @Override
    protected int cooldownTicks() {
        return 50;
    }

    @Override
    protected double minRange() {
        return 2.5;
    }

    @Override
    protected double maxRange() {
        return 7.0;
    }

    @Override
    protected void beginWindup(ServerLevel level, LivingEntity target) {
        this.playSound(ModSounds.STRUMMER_HISS.get(), 0.8F, 1.8F);
    }

    @Override
    protected void strike(ServerLevel level, LivingEntity target) {
        Vec3 d = target.position().subtract(this.position());
        Vec3 h = d.multiply(1, 0, 1);
        this.setDeltaMovement(h.normalize().scale(Math.min(1.0, h.length() * 0.2)).add(0, 0.42, 0));
        this.hasImpulse = true;
    }

    @Override
    public boolean onClimbable() {
        return this.horizontalCollision;
    }

    @Override
    public void makeStuckInBlock(BlockState state, Vec3 speedMultiplier) {
        if (!state.is(Blocks.COBWEB)) {
            super.makeStuckInBlock(state, speedMultiplier);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.STRUMLING_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.STRUMLING_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.STRUMLING_DEATH.get();
    }

    @Override
    protected int burstColor() {
        return 0xC46CFF;
    }
}
