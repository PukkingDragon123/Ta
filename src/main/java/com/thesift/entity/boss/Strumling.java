package com.thesift.entity.boss;

import com.thesift.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A Sculk Spider (entity id {@code strumling}): the Weaver's brood, grown. Long jointed legs of bone,
 * an abdomen armoured in bone plates with glowing sacs between them, a cluster of eyes and a pair
 * of hooked fangs. It skitters in on a fast gait, runs up walls, crouches, and pounces.
 */
public class Strumling extends OrchestraMinion {
    private static final EntityDataAccessor<Boolean> CLIMBING = SynchedEntityData.defineId(Strumling.class, EntityDataSerializers.BOOLEAN);

    public Strumling(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 6;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 16.0).add(Attributes.MOVEMENT_SPEED, 0.32).add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 24.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CLIMBING, false);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WallClimberNavigation(this, level);
    }

    public boolean isClimbing() {
        return this.entityData.get(CLIMBING);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            this.entityData.set(CLIMBING, this.horizontalCollision);
        } else if (this.random.nextInt(10) == 0) {
            // the sacs on its back glow and weep sculk
            this.level().addParticle(ParticleTypes.SCULK_CHARGE_POP, this.getRandomX(0.4), this.getY() + 0.6, this.getRandomZ(0.4), 0, 0.01, 0);
        }
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.SPIDER_STEP, 0.2F, 0.7F);
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
        this.playSound(ModSounds.STRUMMER_HISS.get(), 0.9F, 1.4F);
    }

    @Override
    protected void strike(ServerLevel level, LivingEntity target) {
        Vec3 d = target.position().subtract(this.position());
        Vec3 h = d.multiply(1, 0, 1);
        this.setDeltaMovement(h.normalize().scale(Math.min(1.1, h.length() * 0.2)).add(0, 0.45 + Math.max(0.0, d.y) * 0.08, 0));
    }

    @Override
    public boolean onClimbable() {
        return this.isClimbing();
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
        return 0x29DFEB;
    }

    /** Sculk crawlers are at home on sculk and in the Sculk Swamp's mud whatever the light (the Sift is never dark). */
    @Override
    public float getWalkTargetValue(BlockPos pos, net.minecraft.world.level.LevelReader level) {
        BlockState below = level.getBlockState(pos.below());
        if (below.is(com.thesift.registry.ModBlocks.SCULK_MUD.get()) || below.is(Blocks.SCULK)) {
            return 0.5F;
        }
        return super.getWalkTargetValue(pos, level);
    }
}
