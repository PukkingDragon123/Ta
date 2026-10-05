package com.thesift.entity;

import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSculkSea;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * CR3 Fish &amp; Coral Organs - the Sculk Coral Organ: a living reef of sculk coral grown into organ pipes,
 * rooted on the Sculk Ocean floor (worldgen places it, see CoralOrganFeature). Every so often it plays an
 * eerie chord - its pipes swelling and glowing, notes drifting up - loudest when someone is near. Swimmers in
 * its reach, and anyone on or at the water's surface above it, it harpoons: its bone horn turns, charges with
 * a rising shriek and fires a hooked line (a {@link CoralHook}) that drags its catch down to the pipes and
 * holds it there to drown, clamping it every few seconds. To escape, break the line (hit it - the hooked one
 * strikes the line whatever they aim at, friends can cut it too) or destroy the organ. It never moves, but
 * slowly turns to face what it hunts; when nothing else is about it hooks the odd fish for a snack.
 * Destroyed, it leaves echo shards, sculk coral and whatever it dragged down before you.
 */
public class CoralOrgan extends Mob implements Enemy {
    private static final byte EVENT_CHORD = -97;
    private static final byte EVENT_CHARGE = -98;
    private static final byte EVENT_FIRE = -99;
    private static final byte EVENT_CLAMP = -100;
    private static final EntityDataAccessor<Integer> AIM = SynchedEntityData.defineId(CoralOrgan.class, EntityDataSerializers.INT);
    /** How far the horn reaches (the hook flies a little further). */
    public static final double RANGE = 26.0;
    private static final int CHARGE_TICKS = 26;
    private static final double HOOK_SPEED = 1.15;
    /** Eerie chord shapes, in semitones above the root: diminished, half-diminished, minor 7th, a cluster, augmented, tritone. */
    private static final int[][] CHORDS = {{0, 3, 6}, {0, 3, 6, 10}, {0, 3, 7, 10}, {0, 1, 7}, {0, 4, 8}, {0, 6, 11}, {0, 3, 6, 9}};

    public final AnimationState chordAnimation = new AnimationState();
    public final AnimationState chargeAnimation = new AnimationState();
    public final AnimationState fireAnimation = new AnimationState();
    public final AnimationState clampAnimation = new AnimationState();
    private @Nullable CoralHook hook;
    private @Nullable LivingEntity aim;
    private int targetCheck;
    private int charge = -1;
    private int cooldown = 80;
    private int chordTimer = 60;
    private int[] chord = new int[0];
    private int chordRoot;
    private int chordStep;
    private int chordDelay;

    public CoralOrgan(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        this.xpReward = 15;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 50.0).add(Attributes.ARMOR, 4.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0).add(Attributes.FOLLOW_RANGE, RANGE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(AIM, -1);
    }

    /** Client and server: what the horn is aimed at (its hook's catch while reeling), or null. */
    public @Nullable Entity aimTarget() {
        int id = this.entityData.get(AIM);
        return id < 0 ? null : this.level().getEntity(id);
    }

    /** Where the hook leaves the horn: in front of the organ, at the height of its mouth. */
    public Vec3 mouth() {
        Vec3 f = Vec3.directionFromRotation(0.0F, this.yBodyRot);
        return this.position().add(f.x * 0.85, 0.62, f.z * 0.85);
    }

    // ------------------------------------------------------------------ rooted

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public void travel(Vec3 input) {
        // rooted: it only ever settles straight down onto the sea floor
        if (this.onGround()) {
            this.setDeltaMovement(Vec3.ZERO);
        } else {
            this.setDeltaMovement(0.0, Math.max(-0.25, this.getDeltaMovement().y - 0.03), 0.0);
            this.move(MoverType.SELF, this.getDeltaMovement());
        }
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    public int getNoActionTime() {
        return 0;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 220;
    }

    // ------------------------------------------------------------------ hunting

    /** Swimmers, and anyone on or at the water's surface (boats included). */
    private boolean wet(Entity e) {
        if (e.isInWater()) {
            return true;
        }
        Level level = this.level();
        if (level.getFluidState(BlockPos.containing(e.getX(), e.getY() - 0.4, e.getZ())).is(FluidTags.WATER)) {
            return true;
        }
        Entity v = e.getVehicle();
        return v != null && level.getFluidState(BlockPos.containing(v.getX(), v.getY() - 0.4, v.getZ())).is(FluidTags.WATER);
    }

    private boolean canHook(LivingEntity e) {
        if (!e.isAlive() || e == this || e instanceof CoralOrgan || e instanceof SculkFish || CoralHook.isHooked(e)) {
            return false;
        }
        if (e instanceof Player p && (p.isCreative() || p.isSpectator())) {
            return false;
        }
        return this.wet(e) && this.distanceToSqr(e) < RANGE * RANGE && this.clearShot(e);
    }

    private boolean clearShot(Entity e) {
        Vec3 from = this.mouth();
        Vec3 to = e.getBoundingBox().getCenter();
        HitResult hit = this.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hit.getType() == HitResult.Type.MISS;
    }

    private @Nullable LivingEntity findTarget() {
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (Player p : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(RANGE), this::canHook)) {
            double d = this.distanceToSqr(p);
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        if (best == null && this.random.nextInt(30) == 0) {
            // nobody about: a fish will do
            for (LivingEntity f : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(12.0),
                    e -> (e instanceof KazooFish || e instanceof net.minecraft.world.entity.animal.fish.AbstractFish) && this.canHook(e))) {
                best = f;
                break;
            }
        }
        return best;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();
        if (!(this.level() instanceof ServerLevel server)) {
            this.clientEffects();
            return;
        }
        this.playChordNotes(server);
        if (--this.chordTimer <= 0) {
            boolean near = server.getNearestPlayer(this, 32.0) != null;
            this.chordTimer = near ? 90 + this.random.nextInt(60) : 160 + this.random.nextInt(120);
            this.startChord(server, false);
        }
        if (this.hook != null && this.hook.isRemoved()) {
            this.hook = null;
            this.cooldown = 70 + this.random.nextInt(50);
        }
        if (this.hook != null) {
            Entity caught = this.hook.hooked();
            this.setAim(caught != null ? caught : this.aim);
            if (caught != null && this.tickCount % 24 == 0) {
                // reeling in: a descending run of notes, like something being dragged down
                this.startChord(server, true);
            }
        } else if (this.charge >= 0) {
            this.charging(server);
        } else {
            if (this.cooldown > 0) {
                this.cooldown--;
            }
            if (--this.targetCheck <= 0) {
                this.targetCheck = 10;
                this.aim = server.getDifficulty() == Difficulty.PEACEFUL ? null : this.findTarget();
                this.setAim(this.aim);
                if (this.aim != null && this.cooldown <= 0) {
                    this.charge = 0;
                    server.broadcastEntityEvent(this, EVENT_CHARGE);
                    this.playSound(ModSculkSea.ORGAN_CHARGE.get(), 2.0F, 0.85F + this.random.nextFloat() * 0.2F);
                }
            }
        }
        this.turnTowards(this.aimTarget());
    }

    private void setAim(@Nullable Entity e) {
        this.entityData.set(AIM, e == null ? -1 : e.getId());
    }

    /** The whole reef turns, slowly, to face its prey. */
    private void turnTowards(@Nullable Entity e) {
        if (e == null) {
            return;
        }
        double dx = e.getX() - this.getX();
        double dz = e.getZ() - this.getZ();
        float want = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        float yaw = this.getYRot() + Mth.clamp(Mth.wrapDegrees(want - this.getYRot()), -3.0F, 3.0F);
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }

    private void charging(ServerLevel level) {
        if (this.aim == null || !this.aim.isAlive() || !this.canHook(this.aim)) {
            this.charge = -1;
            this.cooldown = 30;
            this.aim = null;
            this.setAim(null);
            return;
        }
        this.charge++;
        Vec3 m = this.mouth();
        if (this.charge % 3 == 0) {
            level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, m.x, m.y, m.z, 2, 0.15, 0.15, 0.15, 0.01);
        }
        if (this.charge >= CHARGE_TICKS) {
            this.charge = -1;
            this.fire(level, this.aim);
        }
    }

    private void fire(ServerLevel level, LivingEntity target) {
        Vec3 from = this.mouth();
        Vec3 at = target.getBoundingBox().getCenter();
        double t = from.distanceTo(at) / HOOK_SPEED;
        // aim where it will be, not where it is
        Vec3 lead = at.add(target.getDeltaMovement().scale(Math.min(t, 20.0) * 0.6));
        Vec3 v = lead.subtract(from).normalize().scale(HOOK_SPEED);
        CoralHook h = ModSculkSea.CORAL_HOOK.get().create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        if (h == null) {
            return;
        }
        h.launch(this, from, v);
        if (level.addFreshEntity(h)) {
            this.hook = h;
        }
        level.broadcastEntityEvent(this, EVENT_FIRE);
        this.playSound(ModSculkSea.ORGAN_FIRE.get(), 2.0F, 0.9F + this.random.nextFloat() * 0.2F);
        level.sendParticles(ParticleTypes.BUBBLE, from.x, from.y, from.z, 14, 0.2, 0.2, 0.2, 0.15);
        level.sendParticles(ModParticles.RESONANCE_RING.get(), from.x, from.y, from.z, 0, 1.0, 0.0, 0.0, 1.0);
    }

    /** The hook has dragged its catch to the pipes: they clamp shut on it (called by the hook every second or so). */
    public void clamp(ServerLevel level, LivingEntity caught) {
        level.broadcastEntityEvent(this, EVENT_CLAMP);
        this.playSound(ModSculkSea.ORGAN_CLAMP.get(), 1.6F, 0.8F + this.random.nextFloat() * 0.2F);
        if (caught instanceof Player) {
            caught.hurtServer(level, this.damageSources().mobAttack(this), 2.0F);
        } else {
            // a fish: swallowed whole, and the organ plays a happy little chord about it
            level.sendParticles(ParticleTypes.SCULK_SOUL, caught.getX(), caught.getY() + 0.2, caught.getZ(), 5, 0.2, 0.2, 0.2, 0.02);
            caught.discard();
            this.startChord(level, false);
        }
    }

    // ------------------------------------------------------------------ the music

    private void startChord(ServerLevel level, boolean falling) {
        int[] shape = CHORDS[this.random.nextInt(CHORDS.length)];
        this.chordRoot = 3 + this.random.nextInt(8);
        if (falling) {
            // a descending run instead of a rolled chord
            int[] run = new int[shape.length];
            for (int i = 0; i < shape.length; i++) {
                run[i] = shape[shape.length - 1 - i] + 2;
            }
            shape = run;
        }
        this.chord = shape;
        this.chordStep = 0;
        this.chordDelay = 0;
        level.broadcastEntityEvent(this, EVENT_CHORD);
    }

    /** Rolls the current chord out, a note every couple of ticks, from the pipes. */
    private void playChordNotes(ServerLevel level) {
        if (this.chordStep >= this.chord.length || --this.chordDelay > 0) {
            return;
        }
        this.chordDelay = 3;
        int note = Mth.clamp(this.chordRoot + this.chord[this.chordStep], 0, 24);
        float pitch = (float) Math.pow(2.0, (note - 12) / 12.0);
        float detune = 1.0F + (this.random.nextFloat() - 0.5F) * 0.02F;
        level.playSound(null, this.getX(), this.getY() + 1.5, this.getZ(), ModSculkSea.ORGAN_PIPE.get(), SoundSource.HOSTILE, 2.6F, pitch * detune);
        level.playSound(null, this.getX(), this.getY() + 1.0, this.getZ(), ModSculkSea.ORGAN_REED.get(), SoundSource.HOSTILE, 1.4F, pitch);
        // a note rises from the pipe that sounded it
        int pipe = (this.chordStep * 2 + this.chordRoot) % 5;
        double side = (pipe - 2) * 0.3;
        double up = 1.2 + (2 - Math.abs(pipe - 2)) * 0.37;
        Vec3 f = Vec3.directionFromRotation(0.0F, this.yBodyRot);
        double px = this.getX() - f.z * side;
        double pz = this.getZ() + f.x * side;
        level.sendParticles(ModParticles.SIFT_NOTE.get(), px, this.getY() + up, pz, 0, note / 24.0, 0.0, 0.0, 1.0);
        level.sendParticles(ParticleTypes.BUBBLE, px, this.getY() + up, pz, 3, 0.05, 0.05, 0.05, 0.05);
        this.chordStep++;
    }

    private void clientEffects() {
        if (this.random.nextInt(6) == 0) {
            this.level().addParticle(ModParticles.GLOW_DUST.get(), this.getRandomX(0.9), this.getY() + this.random.nextDouble() * 1.9,
                    this.getRandomZ(0.9), 0.0, 0.01, 0.0);
        }
        if (this.random.nextInt(25) == 0) {
            this.level().addParticle(ParticleTypes.BUBBLE, this.getX(), this.getY() + 1.9, this.getZ(), 0.0, 0.05, 0.0);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EVENT_CHORD -> this.chordAnimation.start(this.tickCount);
            case EVENT_CHARGE -> this.chargeAnimation.start(this.tickCount);
            case EVENT_FIRE -> {
                this.chargeAnimation.stop();
                this.fireAnimation.start(this.tickCount);
            }
            case EVENT_CLAMP -> this.clampAnimation.start(this.tickCount);
            default -> super.handleEntityEvent(id);
        }
    }

    // ------------------------------------------------------------------ hurt, death, sounds

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && this.isAlive()) {
            level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, this.getX(), this.getY() + 1.0, this.getZ(), 6, 0.5, 0.6, 0.5, 0.02);
            // a dissonant shriek of a chord when struck
            if (this.chordStep >= this.chord.length) {
                this.startChord(level, false);
            }
        }
        return hurt;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ModSculkSea.ORGAN_DRONE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSculkSea.ORGAN_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSculkSea.ORGAN_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 1.8F;
    }

    /** It collapses in a burst of glowing coral, notes and souls. */
    @Override
    public void makePoofParticles() {
        KillBurst.pop(this, 0x2FB8B0, 0x54ECDE, KillBurst.NOTE, ParticleTypes.SCULK_SOUL);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Cooldown", this.cooldown);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.cooldown = input.getIntOr("Cooldown", 80);
    }
}
