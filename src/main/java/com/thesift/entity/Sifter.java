package com.thesift.entity;

import com.thesift.music.MusicListener;
import com.thesift.music.band.BandPlayer;
import com.thesift.music.band.Bands;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModRings;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Sifter (CR1): a living bronze bell of the Rocky Dunes on four stubby teal legs, eyes on its
 * shoulder, its iron clapper hanging inside like a tongue. The clapper really swings (see
 * {@link BellClapper}): every waddle, stumble, fidget or blow knocks it against the lip, and each
 * strike tinks as loud as it was hit, lights the gold runes round its waist and sends a ring rippling
 * out over the sand. It plays a dune bell in a player's band and strikes on every note.
 *
 * <p>Neutral: it never starts a fight. Hit one and it clangs - its neighbours come to help - and it
 * bonks back until it calms down (or until music soothes it). On sand it naps half-buried with only
 * its shoulder and eyes showing; tread on one and it pops out ringing, startled but harmless.
 */
public class Sifter extends PathfinderMob implements MusicListener, BandPlayer {
    private static final EntityDataAccessor<Boolean> BURROWED = SynchedEntityData.defineId(Sifter.class, EntityDataSerializers.BOOLEAN);
    /** Ids 60-67 are vanilla's; 110-112 are the Sifter's own, -86..-90 the bell's (CR1). */
    private static final byte EVENT_BONK = 110;
    private static final byte EVENT_EMERGE = 111;
    private static final byte EVENT_BURROW = 112;
    /** It played a note in a band: the clapper swings hard into the lip (the band voice is the sound). */
    private static final byte EVENT_BAND_STRIKE = -86;
    /** A deliberate tink: it rings along with music, fidgets, or settles after a fright. */
    private static final byte EVENT_TINK = -87;
    /** Struck: the whole bell jolts and clangs. */
    private static final byte EVENT_CLANG = -88;
    /** How long it stays cross with whoever hit it before it calms down. */
    private static final int ANGER_TICKS = 300;
    /** Each Sifter's own note (semitones), so a dune of them tinks in a pentatonic chord. */
    private static final int[] VOICE = {0, 2, 4, 7, 9, 12};

    public final AnimationState bonkAnimation = new AnimationState();
    public final AnimationState emergeAnimation = new AnimationState();
    public final AnimationState burrowAnimation = new AnimationState();
    /** Client: the bell rocking on its lip and the clapper swinging inside. */
    public final BellClapper bell = new BellClapper();
    /** Client: squash and stretch of the whole bell on hops, landings and emerging. */
    public final Spring squash = new Spring(0.3F, 0.25F);
    private boolean wasOnGround = true;
    private double lastVX;
    private double lastVZ;
    private int lastStep;
    private int fidget = 60;
    /** Client: ticks in which strikes stay silent because a band note is already sounding for them. */
    private int silentStrikes;
    private int angerTicks;
    private int burrowCooldown = 100;
    /** S1 never freeze: how long it has napped in the sand, and how long it will before it digs out to look about. */
    private int burrowTicks;
    private int burrowPatience = 600;

    public Sifter(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.2)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    /**
     * S1 never freeze: vanilla stops a mob's random strolls once it has been 100 ticks out of
     * a player's 32-block reach, so Sift creatures seen across a valley stood frozen. The field
     * itself (which drives despawning) is left alone.
     */
    @Override
    public int getNoActionTime() {
        return 0;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new BurrowGoal());
        this.goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.4F));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        // neutral: it only ever turns on whoever hit it, and its clang calls its neighbours
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
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
        return isSand(this.level().getBlockState(this.blockPosition().below()));
    }

    /** Dreamsand or any sand: the dunes a Sifter lives in. */
    public static boolean isSand(BlockState state) {
        return state.is(ModBlocks.DREAMSAND.get()) || state.is(BlockTags.SAND);
    }

    /** It wanders back to the sand wherever it can. */
    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return isSand(level.getBlockState(pos.below())) ? 10.0F : super.getWalkTargetValue(pos, level);
    }

    /** A creature of the dunes, not a roaming monster: it stays where it lives. */
    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    // ------------------------------------------------------------------ temper

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (this.isBurrowed()) {
            this.emerge(level);
        }
        // a fresh blow renews its grudge
        this.angerTicks = 0;
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt) {
            level.broadcastEntityEvent(this, EVENT_CLANG);
        }
        return hurt;
    }

    /** Server: it stays cross for a while, then forgets (the target goal only ever sets a target when it is hit). */
    private void calmDown(ServerLevel level) {
        LivingEntity target = this.getTarget();
        if (target == null) {
            this.angerTicks = 0;
            return;
        }
        if (this.angerTicks <= 0) {
            this.angerTicks = ANGER_TICKS + this.random.nextInt(200);
        } else if (--this.angerTicks <= 0 || !target.isAlive()) {
            this.setTarget(null);
            this.angerTicks = 0;
            level.broadcastEntityEvent(this, EVENT_TINK);
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        level.broadcastEntityEvent(this, EVENT_BONK);
        this.playSound(ModSounds.SIFTER_CHOMP.get(), 1.0F, 0.95F + this.random.nextFloat() * 0.15F);
        boolean hit = super.doHurtTarget(level, target);
        if (hit) {
            level.sendParticles(ModRings.BELL_RING.get(), target.getX(), this.getY() + 0.3, target.getZ(), 0, 1.8, 1.0, 0.0, 1.0);
        }
        return hit;
    }

    /** Music soothes it: it forgets any grudge, and now and then rings along. */
    @Override
    public void hearMusic(BlockPos source, float strength) {
        if (this.level() instanceof ServerLevel server) {
            if (this.getTarget() != null) {
                this.setTarget(null);
                this.angerTicks = 0;
            }
            if (!this.isBurrowed() && Bands.leaderOf(this) == null && this.random.nextInt(3) == 0) {
                server.broadcastEntityEvent(this, EVENT_TINK);
            }
        }
    }

    /** In a band: every note it plays, its clapper strikes. */
    @Override
    public void playedBandNote(int pitch, float loudness) {
        this.level().broadcastEntityEvent(this, EVENT_BAND_STRIKE);
    }

    // ------------------------------------------------------------------ the sand

    private void emerge(ServerLevel level) {
        this.setBurrowed(false);
        this.burrowCooldown = 300;
        level.broadcastEntityEvent(this, EVENT_EMERGE);
        BlockState below = level.getBlockState(this.blockPosition().below());
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, below), this.getX(), this.getY() + 0.2, this.getZ(), 30, 0.4, 0.2, 0.4, 0.15);
        this.playSound(ModSounds.SIFTER_LEAP.get(), 1.0F, 0.9F + this.random.nextFloat() * 0.2F);
        this.playSound(ModSounds.SIFTER_RING.get(), 0.9F, 1.25F + this.random.nextFloat() * 0.2F);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level() instanceof ServerLevel server) {
            this.calmDown(server);
            if (this.burrowCooldown > 0) {
                this.burrowCooldown--;
            }
            if (this.isBurrowed()) {
                Player p = server.getNearestPlayer(this, 2.0);
                if (p != null && !p.isSpectator() && !p.isShiftKeyDown()) {
                    // trodden on: it pops out ringing - startled, not cross
                    this.emerge(server);
                    this.getLookControl().setLookAt(p);
                    this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.45, 0.0));
                } else if (!this.onSand()) {
                    this.setBurrowed(false);
                } else if (++this.burrowTicks > this.burrowPatience) {
                    // S1 never freeze: it has napped long enough and digs out to wander (and may dig in again later)
                    this.emerge(server);
                    this.burrowCooldown = 400 + this.random.nextInt(800);
                }
            } else {
                this.burrowTicks = 0;
            }
        }
    }

    // ------------------------------------------------------------------ the bell (client)

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.ringTick();
        }
    }

    private void ringTick() {
        boolean ground = this.onGround();
        if (ground && !this.wasOnGround) {
            // a landing: it squashes, and the bell jolts
            this.squash.kick(-0.35F);
            this.bell.nudge(0.07F, (this.random.nextFloat() - 0.5F) * 0.1F);
        } else if (!ground && this.wasOnGround) {
            this.squash.kick(0.3F);
        }
        this.wasOnGround = ground;
        this.squash.tick();
        // its own acceleration, along its facing and to its left (a stop throws the clapper forward)
        double vx = Mth.clamp(this.getX() - this.xo, -0.6, 0.6);
        double vz = Mth.clamp(this.getZ() - this.zo, -0.6, 0.6);
        double ax = vx - this.lastVX;
        double az = vz - this.lastVZ;
        this.lastVX = vx;
        this.lastVZ = vz;
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        float sin = Mth.sin(yaw);
        float cos = Mth.cos(yaw);
        float accFwd = (float) Mth.clamp(-ax * sin + az * cos, -0.15, 0.15);
        float accLeft = (float) Mth.clamp(ax * cos + az * sin, -0.15, 0.15);
        // the waddle: the bell rocks from foot to foot and leans into its stride (more so in a charge)
        float walk = this.isBurrowed() ? 0.0F : Math.min(1.0F, this.walkAnimation.speed() * 1.5F);
        float phase = this.walkAnimation.position() * 0.6662F;
        float leanX = (this.isAggressive() ? 0.2F : 0.1F) * walk;
        float leanZ = Mth.sin(phase) * 0.18F * walk;
        int step = Mth.floor(phase / Mth.PI);
        if (step != this.lastStep && walk > 0.15F) {
            // every footfall jolts it a little
            this.bell.nudge(0.0F, (Math.floorMod(step, 2) == 0 ? 0.04F : -0.04F) * walk);
        }
        this.lastStep = step;
        // idle, it shuffles and shrugs now and then - and tinks
        if (walk < 0.1F && !this.isBurrowed() && --this.fidget <= 0) {
            this.fidget = 80 + this.random.nextInt(160);
            this.bell.nudge((this.random.nextFloat() - 0.5F) * 0.11F, (this.random.nextBoolean() ? 0.11F : -0.11F));
        }
        float hit = this.bell.tick(leanX, leanZ, accFwd, accLeft);
        if (this.silentStrikes > 0) {
            this.silentStrikes--;
        }
        if (hit > 0.012F) {
            this.strike(hit);
        }
        if (this.isBurrowed() && this.random.nextInt(12) == 0) {
            this.level().addParticle(ModParticles.FOOTSTEP_PUFF.get(), this.getRandomX(0.5), this.getY() + 0.05, this.getRandomZ(0.5), 0, 0.02, 0);
        }
        // digging in: sand sprays out round the scrabbling feet for the first second
        if (this.isBurrowed() && this.burrowAnimation.isStarted() && this.burrowAnimation.getTimeInMillis(this.tickCount) < 1000) {
            this.digParticles(3, 0.12);
        }
        // charging, its feet kick up little sprays of sand
        if (ground && this.isAggressive() && this.getDeltaMovement().horizontalDistanceSqr() > 0.004 && this.random.nextInt(3) == 0) {
            this.digParticles(1, 0.05);
        }
    }

    /** Client: the clapper struck the lip this hard (radians per tick): a tink, the runes flare, a ring spreads. */
    private void strike(float strength) {
        this.bell.glow(strength);
        if (this.silentStrikes <= 0 && !this.isSilent()) {
            float volume = Mth.clamp(strength * 5.0F, 0.08F, 1.0F) * (this.isBurrowed() ? 0.4F : 1.0F);
            this.level().playLocalSound(this.getX(), this.getY() + 0.5, this.getZ(), ModSounds.SIFTER_TINK.get(), SoundSource.NEUTRAL, volume,
                    this.voicePitch(), false);
        }
        if (strength > 0.025F) {
            this.level().addParticle(ModRings.BELL_RING.get(), this.getX(), this.getY() + (this.isBurrowed() ? 0.1 : 0.3), this.getZ(),
                    0.5 + strength * 5.0, Math.min(1.0, strength * 6.0), 0.0);
        }
    }

    /** This Sifter's own note. */
    private float voicePitch() {
        int n = VOICE[Math.floorMod(this.getUUID().hashCode(), VOICE.length)];
        return (float) Math.pow(2.0, (n - 2) / 12.0);
    }

    /** A blow from a random side, this hard (radians per tick). */
    private void jolt(float strength) {
        float a = this.random.nextFloat() * Mth.TWO_PI;
        this.bell.nudge(Mth.cos(a) * strength, Mth.sin(a) * strength);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_BONK) {
            this.bonkAnimation.start(this.tickCount);
            // it throws its whole bell forward at you: the clapper slams the front of the lip
            this.bell.nudge(0.26F, 0.0F);
            this.squash.kick(0.25F);
        } else if (id == EVENT_EMERGE) {
            this.emergeAnimation.start(this.tickCount);
            this.burrowAnimation.stop();
            this.squash.kick(0.5F);
            this.jolt(0.2F);
            this.digParticles(30, 0.35);
        } else if (id == EVENT_BURROW) {
            this.burrowAnimation.start(this.tickCount);
            this.emergeAnimation.stop();
        } else if (id == EVENT_BAND_STRIKE) {
            // the band's bell note is the sound; the clapper still has to strike for it
            this.silentStrikes = 12;
            float side = this.random.nextBoolean() ? 1.0F : -1.0F;
            this.bell.fling(0.12F * side, 0.33F * side);
            this.bell.glow(0.2F);
        } else if (id == EVENT_TINK) {
            float a = this.random.nextFloat() * Mth.TWO_PI;
            this.bell.fling(Mth.cos(a) * 0.24F, Mth.sin(a) * 0.24F);
        } else if (id == EVENT_CLANG) {
            this.jolt(0.24F);
            this.squash.kick(-0.3F);
        } else {
            super.handleEntityEvent(id);
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

    // ------------------------------------------------------------------ sounds

    /** It does not hum on a timer: it fidgets, and its clapper does the tinking (see {@link #ringTick}). */
    @Override
    public void playAmbientSound() {
        if (!this.isBurrowed()) {
            this.level().broadcastEntityEvent(this, EVENT_TINK);
        }
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
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

    /** Settles into the sand for a nap when nothing is going on. */
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
                s.burrowTicks = 0;
                s.burrowPatience = 900 + s.random.nextInt(1500);
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

    /** A spray of sand and gold stars. */
    @Override
    public void makePoofParticles() {
        KillBurst.pop(this, 0xD9A65E, 0xFFD56C, KillBurst.STAR, ModParticles.FOOTSTEP_PUFF.get());
    }
}
