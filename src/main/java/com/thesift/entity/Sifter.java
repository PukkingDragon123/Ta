package com.thesift.entity;

import com.thesift.music.MusicListener;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Sifter: a skittering crab-legged trap under a sandstone-and-bone carapace. In the Rocky Dunes it
 * digs itself into the Dreamsand until only its lid and glowing lure show, then bursts out and snaps
 * its lid shut on whatever came close. Music lulls it: a Sifter that hears a drum forgets what it was
 * chasing for a while.
 */
public class Sifter extends Monster implements MusicListener {
    private static final EntityDataAccessor<Boolean> BURROWED = SynchedEntityData.defineId(Sifter.class, EntityDataSerializers.BOOLEAN);
    private static final byte EVENT_CHOMP = 110;
    /** Ids 60-67 are vanilla's; ours start at 110. */
    private static final byte EVENT_EMERGE = 111;
    private static final byte EVENT_BURROW = 112;

    public final AnimationState chompAnimation = new AnimationState();
    public final AnimationState emergeAnimation = new AnimationState();
    public final AnimationState burrowAnimation = new AnimationState();
    public final Spring squash = new Spring(0.3F, 0.25F);
    public final Spring antennaLeft = new Spring(0.2F, 0.12F);
    public final Spring antennaRight = new Spring(0.22F, 0.12F);
    private boolean wasOnGround = true;
    private int calmTicks;
    private int burrowCooldown = 100;

    public Sifter(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 14.0)
                .add(Attributes.MOVEMENT_SPEED, 0.31)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new BurrowGoal());
        this.goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.45F));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.15, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, (target, level) -> this.calmTicks <= 0));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BURROWED, false);
    }

    public boolean isBurrowed() {
        return this.entityData.get(BURROWED);
    }

    public void setBurrowed(boolean b) {
        this.entityData.set(BURROWED, b);
    }

    private boolean onSand() {
        BlockState below = this.level().getBlockState(this.blockPosition().below());
        return below.is(ModBlocks.DREAMSAND.get()) || below.is(BlockTags.SAND);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (this.isBurrowed()) {
            this.emerge(level);
        }
        this.calmTicks = 0;
        return super.hurtServer(level, source, damage);
    }

    private void emerge(ServerLevel level) {
        this.setBurrowed(false);
        this.burrowCooldown = 300;
        level.broadcastEntityEvent(this, EVENT_EMERGE);
        BlockState below = level.getBlockState(this.blockPosition().below());
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, below), this.getX(), this.getY() + 0.2, this.getZ(), 30, 0.4, 0.2, 0.4, 0.15);
        this.playSound(ModSounds.SIFTER_LEAP.get(), 1.0F, 0.8F);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        this.level().broadcastEntityEvent(this, EVENT_CHOMP);
        this.playSound(ModSounds.SIFTER_CHOMP.get(), 1.0F, 1.0F + this.random.nextFloat() * 0.2F);
        return super.doHurtTarget(level, target);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_CHOMP) {
            this.chompAnimation.start(this.tickCount);
        } else if (id == EVENT_EMERGE) {
            this.emergeAnimation.start(this.tickCount);
            this.burrowAnimation.stop();
            this.squash.kick(0.5F);
            this.antennaLeft.kick(-0.8F);
            this.antennaRight.kick(-0.7F);
            this.digParticles(30, 0.35);
        } else if (id == EVENT_BURROW) {
            this.burrowAnimation.start(this.tickCount);
            this.emergeAnimation.stop();
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public void hearMusic(BlockPos source, float strength) {
        if (!this.level().isClientSide()) {
            this.calmTicks = (int) (140 * strength) + 40;
            this.setTarget(null);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level() instanceof ServerLevel server) {
            if (this.calmTicks > 0) {
                this.calmTicks--;
                if (this.tickCount % 15 == 0) {
                    server.sendParticles(ModParticles.SIFT_NOTE.get(), this.getX(), this.getY() + 1.0, this.getZ(), 0, 0.8, 0, 0, 1);
                }
            }
            if (this.burrowCooldown > 0) {
                this.burrowCooldown--;
            }
            if (this.isBurrowed()) {
                Player p = server.getNearestPlayer(this, 5.0);
                if (p != null && !p.isCreative() && !p.isSpectator() && (!p.isShiftKeyDown() || this.distanceTo(p) < 2.5F)) {
                    this.emerge(server);
                    this.setTarget(p);
                    this.setDeltaMovement(this.getDeltaMovement().add(0, 0.55, 0));
                } else if (!this.onSand()) {
                    this.setBurrowed(false);
                }
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            boolean ground = this.onGround();
            if (ground && !this.wasOnGround) {
                this.squash.kick(-0.35F);
                this.antennaLeft.kick(0.5F);
                this.antennaRight.kick(0.45F);
            } else if (!ground && this.wasOnGround) {
                this.squash.kick(0.3F);
                this.antennaLeft.kick(-0.4F);
                this.antennaRight.kick(-0.45F);
            }
            this.wasOnGround = ground;
            float speed = (float) this.getDeltaMovement().horizontalDistance();
            this.antennaLeft.setTarget(speed * -2.5F);
            this.antennaRight.setTarget(speed * -2.5F);
            this.squash.tick();
            this.antennaLeft.tick();
            this.antennaRight.tick();
            if (this.isBurrowed() && this.random.nextInt(12) == 0) {
                this.level().addParticle(ModParticles.FOOTSTEP_PUFF.get(), this.getRandomX(0.5), this.getY() + 0.05, this.getRandomZ(0.5), 0, 0.02, 0);
            }
            // digging in: sand sprays out behind the scrabbling legs for the first second
            if (this.isBurrowed() && this.burrowAnimation.isStarted() && this.burrowAnimation.getTimeInMillis(this.tickCount) < 1000) {
                this.digParticles(3, 0.12);
            }
            // running, its legs kick up little sprays of sand
            if (ground && this.isAggressive() && this.getDeltaMovement().horizontalDistanceSqr() > 0.004 && this.random.nextInt(3) == 0) {
                this.digParticles(1, 0.05);
            }
        }
    }

    /** Client: grains of whatever it stands on, sprayed up and out. */
    private void digParticles(int count, double speed) {
        BlockState below = this.level().getBlockState(this.blockPosition().below());
        if (below.isAir()) {
            return;
        }
        BlockParticleOption grains = new BlockParticleOption(ParticleTypes.BLOCK, below);
        for (int i = 0; i < count; i++) {
            double a = this.random.nextDouble() * Math.PI * 2.0;
            this.level().addParticle(grains, this.getX() + Math.cos(a) * 0.5, this.getY() + 0.1, this.getZ() + Math.sin(a) * 0.5,
                    Math.cos(a) * speed, 0.15 + this.random.nextDouble() * speed * 2.0, Math.sin(a) * speed);
        }
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.isBurrowed() ? null : ModSounds.SIFTER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.SIFTER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SIFTER_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(ModSounds.SIFTER_STEP.get(), 0.3F, 1.3F);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Burrowed", this.isBurrowed());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setBurrowed(input.getBooleanOr("Burrowed", false));
    }

    /** Digs into sand to lie in ambush when nothing is going on. */
    private class BurrowGoal extends Goal {
        BurrowGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            Sifter s = Sifter.this;
            return s.isBurrowed() || s.getTarget() == null && s.burrowCooldown <= 0 && s.onGround() && s.onSand() && s.random.nextInt(60) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return Sifter.this.isBurrowed();
        }

        @Override
        public void start() {
            Sifter s = Sifter.this;
            s.getNavigation().stop();
            if (!s.isBurrowed() && s.level() instanceof ServerLevel server) {
                s.setBurrowed(true);
                server.broadcastEntityEvent(s, EVENT_BURROW);
                s.playSound(ModSounds.SIFTER_STEP.get(), 1.0F, 0.6F);
                BlockState below = server.getBlockState(s.blockPosition().below());
                server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, below), s.getX(), s.getY() + 0.1, s.getZ(), 20, 0.3, 0.1, 0.3, 0.05);
            }
        }

        @Override
        public void tick() {
            Sifter.this.getNavigation().stop();
            Sifter.this.setDeltaMovement(Sifter.this.getDeltaMovement().multiply(0.0, 1.0, 0.0));
        }
    }

    /** A spray of pink sand and stars. */
    @Override
    public void makePoofParticles() {
        KillBurst.pop(this, 0xF1BDD4, 0xFFD98A, KillBurst.STAR, ModParticles.FOOTSTEP_PUFF.get());
    }
}
