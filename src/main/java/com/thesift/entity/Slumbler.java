package com.thesift.entity;

import com.thesift.music.MusicListener;
import com.thesift.registry.ModFluids;
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
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Slumbler: a huge, wide-mouthed Chrome salamander. Sleepy and peaceful unless you hit it - then it
 * lumbers after you with a crushing bite. It glides through Chrome, yawns enormous yawns, gulps
 * Chrome plankton from the shallows, nuzzles other Slumblers and hums along to music - and at night
 * it wades into shallow Chrome to nap half-submerged. Drops Thick Hide and, rarely, a Chrome Pearl.
 */
public class Slumbler extends PathfinderMob implements MusicListener {
    private static final EntityDataAccessor<Boolean> SLEEPING = SynchedEntityData.defineId(Slumbler.class, EntityDataSerializers.BOOLEAN);
    private static final byte EVENT_YAWN = 110;
    /** Ids 60-67 are vanilla's. */
    private static final byte EVENT_BITE = 111;
    private static final byte EVENT_GULP = 113;
    private static final byte EVENT_NUZZLE = 114;
    private static final byte EVENT_HUM = 115;

    public final AnimationState yawnAnimation = new AnimationState();
    public final AnimationState biteAnimation = new AnimationState();
    public final AnimationState sleepAnimation = new AnimationState();
    public final AnimationState gulpAnimation = new AnimationState();
    public final AnimationState nuzzleAnimation = new AnimationState();
    public final AnimationState humAnimation = new AnimationState();
    private int humCooldown;
    private int socialCooldown = 400;
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
        this.goalSelector.addGoal(4, new ForageGoal());
        this.goalSelector.addGoal(4, new NuzzleGoal());
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
        } else if (id == EVENT_GULP) {
            this.gulpAnimation.start(this.tickCount);
        } else if (id == EVENT_NUZZLE) {
            this.nuzzleAnimation.start(this.tickCount);
        } else if (id == EVENT_HUM) {
            this.humAnimation.start(this.tickCount);
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
            if (this.humCooldown > 0) {
                this.humCooldown--;
            }
            if (this.socialCooldown > 0) {
                this.socialCooldown--;
            }
            // napping in the Chrome, bubbles rise from its nostrils
            if (this.isSlumbering() && this.isInFluidType() && this.tickCount % 12 == 0) {
                Vec3 nose = this.position().add(Vec3.directionFromRotation(0.0F, this.yBodyRot).scale(1.4));
                server.sendParticles(ModParticles.CHROME_BUBBLE.get(), nose.x, this.getY() + 0.6, nose.z, 2, 0.1, 0.05, 0.1, 0.01);
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

    /**
     * A note was played nearby (see {@link CreatureLife}): awake, it hums along in its deep voice;
     * asleep, it only smiles and murmurs.
     */
    public void hearNote(ServerLevel level, int pitch) {
        if (this.humCooldown > 0 || this.getTarget() != null) {
            return;
        }
        this.humCooldown = 30 + this.random.nextInt(30);
        level.broadcastEntityEvent(this, EVENT_HUM);
        this.playSound(ModSounds.SLUMBLER_AMBIENT.get(), this.isSlumbering() ? 0.4F : 0.9F, 0.5F + com.thesift.music.Notes.soundPitch(pitch) * 0.35F);
        level.sendParticles(ModParticles.SIFT_NOTE.get(), this.getX(), this.getY() + this.getBbHeight() + 0.3, this.getZ(), 0, pitch / 24.0, 0.0, 0.0, 1.0);
    }

    /** Shallow Chrome to wade into for a nap: Chrome with ground just under it and air above. */
    private @Nullable BlockPos findShallowChrome() {
        BlockPos here = this.blockPosition();
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(here.offset(-10, -3, -10), here.offset(10, 2, 10))) {
            FluidState fs = this.level().getFluidState(p);
            if (fs.isEmpty() || !fs.getType().isSame(ModFluids.CHROME.get()) || !this.level().getBlockState(p.above()).isAir()
                    || !this.level().getFluidState(p.below()).isEmpty()) {
                continue;
            }
            double d = p.distSqr(here);
            if (d < bestD) {
                bestD = d;
                best = p.immutable();
            }
        }
        return best;
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
            if (s.getTarget() != null || s.wakeTimer > 0 || s.random.nextInt(s.level().isDarkOutside() ? 120 : 500) != 0) {
                return false;
            }
            this.spot = s.isInFluidType() ? null : s.findShallowChrome();
            this.walk = this.spot != null ? 200 : 0;
            if (this.spot == null && !s.onGround() && !s.isInFluidType()) {
                return false;
            }
            s.sleepTimer = 600 + s.random.nextInt(900);
            if (this.spot == null) {
                s.setSlumbering(true);
            }
            return true;
        }

        private @Nullable BlockPos spot;
        private int walk;

        @Override
        public boolean canContinueToUse() {
            Slumbler s = Slumbler.this;
            return (s.isSlumbering() || this.walk > 0) && s.getTarget() == null && s.sleepTimer > 0;
        }

        @Override
        public void start() {
            Slumbler s = Slumbler.this;
            if (this.spot != null) {
                s.getNavigation().moveTo(this.spot.getX() + 0.5, this.spot.getY(), this.spot.getZ() + 0.5, 0.8);
            } else {
                s.getNavigation().stop();
            }
        }

        @Override
        public void tick() {
            Slumbler s = Slumbler.this;
            if (!s.isSlumbering()) {
                // still wading out to its napping spot
                this.walk--;
                boolean there = this.spot == null || s.isInFluidType() || s.position().distanceToSqr(Vec3.atBottomCenterOf(this.spot)) < 2.5;
                if (there || this.walk <= 0 || s.getNavigation().isDone()) {
                    this.walk = 0;
                    s.setSlumbering(true);
                    s.getNavigation().stop();
                }
                return;
            }
            s.sleepTimer--;
            s.getNavigation().stop();
        }

        @Override
        public void stop() {
            Slumbler.this.setSlumbering(false);
            Slumbler.this.wakeTimer = 600 + Slumbler.this.random.nextInt(600);
        }
    }

    /** Awake in or beside the Chrome, it dips its huge mouth in and gulps a mouthful of plankton. */
    private final class ForageGoal extends Goal {
        private int ticks;

        ForageGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Slumbler s = Slumbler.this;
            return !s.isSlumbering() && s.getTarget() == null && s.random.nextInt(300) == 0
                    && (s.isInFluidType() || !s.level().getFluidState(s.blockPosition().relative(s.getDirection())).isEmpty());
        }

        @Override
        public boolean canContinueToUse() {
            return this.ticks > 0;
        }

        @Override
        public void start() {
            this.ticks = 45;
            Slumbler.this.getNavigation().stop();
            Slumbler.this.level().broadcastEntityEvent(Slumbler.this, EVENT_GULP);
        }

        @Override
        public void tick() {
            Slumbler s = Slumbler.this;
            this.ticks--;
            s.getNavigation().stop();
            if (this.ticks == 22 && s.level() instanceof ServerLevel server) {
                Vec3 mouth = s.position().add(Vec3.directionFromRotation(0.0F, s.yBodyRot).scale(1.5));
                server.sendParticles(ModParticles.CHROME_DROPLET.get(), mouth.x, s.getY() + 0.4, mouth.z, 8, 0.3, 0.1, 0.3, 0.08);
                s.playSound(ModSounds.SLUMBLER_BITE.get(), 0.5F, 1.3F);
                s.heal(1.0F);
            }
        }
    }

    /** Two Slumblers meeting rub their broad snouts together. */
    private final class NuzzleGoal extends Goal {
        private @Nullable Slumbler friend;
        private int ticks;

        NuzzleGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Slumbler s = Slumbler.this;
            if (s.isSlumbering() || s.getTarget() != null || s.socialCooldown > 0 || s.random.nextInt(100) != 0) {
                return false;
            }
            for (Slumbler other : s.level().getEntitiesOfClass(Slumbler.class, s.getBoundingBox().inflate(8.0),
                    o -> o != s && o.isAlive() && !o.isSlumbering() && o.getTarget() == null)) {
                this.friend = other;
                return true;
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return this.friend != null && this.friend.isAlive() && this.ticks > 0;
        }

        @Override
        public void start() {
            this.ticks = 120;
        }

        @Override
        public void tick() {
            Slumbler s = Slumbler.this;
            Slumbler f = this.friend;
            if (f == null) {
                return;
            }
            this.ticks--;
            s.getLookControl().setLookAt(f, 20.0F, 20.0F);
            if (s.distanceToSqr(f) > 9.0) {
                s.getNavigation().moveTo(f, 0.7);
            } else {
                s.getNavigation().stop();
                s.level().broadcastEntityEvent(s, EVENT_NUZZLE);
                s.playSound(ModSounds.SLUMBLER_AMBIENT.get(), 0.7F, 1.2F);
                if (s.level() instanceof ServerLevel server) {
                    server.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART, (s.getX() + f.getX()) / 2.0, s.getY() + 1.4,
                            (s.getZ() + f.getZ()) / 2.0, 1, 0.2, 0.1, 0.2, 0.0);
                }
                s.socialCooldown = 1200 + s.random.nextInt(1200);
                f.socialCooldown = s.socialCooldown;
                this.ticks = 0;
            }
        }
    }

    /** Bubbles and droplets, like a popped soap bubble. */
    @Override
    public void makePoofParticles() {
        KillBurst.pop(this, 0x8FD0DC, 0xF59AD0, KillBurst.DROP, ModParticles.CHROME_BUBBLE.get());
    }
}
