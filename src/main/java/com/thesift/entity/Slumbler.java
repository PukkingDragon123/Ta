package com.thesift.entity;

import com.thesift.music.MusicListener;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Slumbler: a huge, wide-mouthed Chrome salamander. Sleepy and peaceful unless you hit it - then it
 * lumbers after you with a crushing bite. It glides through Chrome, yawns enormous yawns and naps
 * on the lake shores. Drops Thick Hide and, rarely, a Chrome Pearl.
 */
public class Slumbler extends PathfinderMob implements MusicListener {
    private static final EntityDataAccessor<Boolean> SLEEPING = SynchedEntityData.defineId(Slumbler.class, EntityDataSerializers.BOOLEAN);
    private static final byte EVENT_YAWN = 60;
    private static final byte EVENT_BITE = 61;

    public final AnimationState yawnAnimation = new AnimationState();
    public final AnimationState biteAnimation = new AnimationState();
    public final AnimationState sleepAnimation = new AnimationState();
    private int yawnCooldown = 200;
    private int sleepTimer;
    private int wakeTimer = 400;

    public Slumbler(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.17)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SleepGoal());
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7, 0.002F));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SLEEPING, false);
    }

    public boolean isSlumbering() {
        return this.entityData.get(SLEEPING);
    }

    private void setSlumbering(boolean sleeping) {
        this.entityData.set(SLEEPING, sleeping);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (SLEEPING.equals(accessor)) {
            this.sleepAnimation.animateWhen(this.isSlumbering(), this.tickCount);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (this.isSlumbering()) {
            this.setSlumbering(false);
            this.wakeTimer = 600;
        }
        return super.hurtServer(level, source, damage);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        this.level().broadcastEntityEvent(this, EVENT_BITE);
        this.playSound(ModSounds.SLUMBLER_BITE.get(), 1.0F, 0.9F);
        return super.doHurtTarget(level, target);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_YAWN) {
            this.yawnAnimation.start(this.tickCount);
        } else if (id == EVENT_BITE) {
            this.biteAnimation.start(this.tickCount);
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level() instanceof ServerLevel server) {
            if (this.wakeTimer > 0) {
                this.wakeTimer--;
            }
            if (!this.isSlumbering() && this.getTarget() == null && --this.yawnCooldown <= 0) {
                this.yawnCooldown = 300 + this.random.nextInt(500);
                server.broadcastEntityEvent(this, EVENT_YAWN);
                this.playSound(ModSounds.SLUMBLER_YAWN.get(), 1.2F, 0.8F + this.random.nextFloat() * 0.2F);
            }
            if (this.isSlumbering() && this.tickCount % 40 == 0) {
                server.sendParticles(ModParticles.SIFT_MIST.get(), this.getX(), this.getY() + 0.8, this.getZ(), 1, 0.2, 0.1, 0.2, 0.0);
            }
        } else if (this.isInFluidType() && this.random.nextInt(8) == 0) {
            this.level().addParticle(ModParticles.CHROME_BUBBLE.get(), this.getRandomX(0.8), this.getY() + 0.5, this.getRandomZ(0.8), 0, 0.03, 0);
        }
    }

    @Override
    public void hearMusic(BlockPos source, float strength) {
        // Music is the only thing that reliably sends a Slumbler to sleep.
        if (!this.level().isClientSide() && this.getTarget() == null && !this.isSlumbering() && this.random.nextFloat() < 0.3F * strength) {
            this.setSlumbering(true);
            this.sleepTimer = 300 + this.random.nextInt(300);
        }
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.isSlumbering() ? null : ModSounds.SLUMBLER_AMBIENT.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.SLUMBLER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SLUMBLER_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(ModSounds.SLUMBLER_STEP.get(), 0.5F, 0.8F);
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Slumbering", this.isSlumbering());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setSlumbering(input.getBooleanOr("Slumbering", false));
    }

    /** Naps for a while when nothing is bothering it. */
    private class SleepGoal extends Goal {
        SleepGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            Slumbler s = Slumbler.this;
            if (s.isSlumbering()) {
                return true;
            }
            if (s.getTarget() != null || s.wakeTimer > 0 || !s.onGround() || s.random.nextInt(400) != 0) {
                return false;
            }
            s.setSlumbering(true);
            s.sleepTimer = 400 + s.random.nextInt(800);
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            Slumbler s = Slumbler.this;
            return s.isSlumbering() && s.getTarget() == null && s.sleepTimer > 0;
        }

        @Override
        public void start() {
            Slumbler.this.getNavigation().stop();
        }

        @Override
        public void tick() {
            Slumbler.this.sleepTimer--;
            Slumbler.this.getNavigation().stop();
        }

        @Override
        public void stop() {
            Slumbler.this.setSlumbering(false);
            Slumbler.this.wakeTimer = 600 + Slumbler.this.random.nextInt(600);
        }
    }

    /** Bubbles and droplets, like a popped soap bubble. */
    @Override
    public void makePoofParticles() {
        KillBurst.pop(this, 0x8FD0DC, 0xF59AD0, KillBurst.DROP, ModParticles.CHROME_BUBBLE.get());
    }
}
