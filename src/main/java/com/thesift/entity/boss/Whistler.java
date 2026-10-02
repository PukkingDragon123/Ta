package com.thesift.entity.boss;

import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Whistler: a giant sculk crane with a flute for a beak. It never lands while it has
 * someone to fight; it wheels above them on its enormous wings, trailing souls.
 *
 * <ul>
 *   <li>Song Lock: it points its beak at you and plays. A thin line of light finds you first (the
 *   warning), then the song locks on - a beam like a Guardian's that, like the Warden's sonic
 *   boom, ignores armour - and it keeps on hurting you, slowly, for as long as the lock holds.
 *   Break it: get out of its sight behind something solid, get far away, or hit it hard enough
 *   and it falters out of the air.</li>
 *   <li>Dive: it climbs, folds its wings and drops on you beak first.</li>
 * </ul>
 */
public class Whistler extends MiniBoss {
    public static final int BEAM_CHARGE = 1;
    public static final int BEAM_LOCK = 2;
    public static final int DIVE_WINDUP = 3;
    public static final int DIVE = 4;
    public static final int CALL = 5;
    public static final int STUNNED = 6;
    public static final float SCALE = 1.5F;
    private static final EntityDataAccessor<Integer> BEAM_TARGET = SynchedEntityData.defineId(Whistler.class, EntityDataSerializers.INT);
    private static final int LOCK_TICKS = 140;
    private static final float LOCK_BREAK_DAMAGE = 14.0F;

    private @Nullable BlockPos home;
    private float orbit;
    private float lockDamage;
    private Vec3 diveAt = Vec3.ZERO;
    private final Set<Integer> diveHits = new HashSet<>();

    public Whistler(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.WHITE);
        this.setNoGravity(true);
        this.orbit = this.random.nextFloat() * Mth.TWO_PI;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 180.0).add(Attributes.ARMOR, 4.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED, 0.6).add(Attributes.ATTACK_DAMAGE, 8.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BEAM_TARGET, -1);
    }

    /** Who its song is locked on (or winding up for), client and server. */
    public @Nullable LivingEntity beamTarget() {
        int id = this.entityData.get(BEAM_TARGET);
        return id >= 0 && this.level().getEntity(id) instanceof LivingEntity le ? le : null;
    }

    private void setBeamTarget(@Nullable LivingEntity e) {
        this.entityData.set(BEAM_TARGET, e == null ? -1 : e.getId());
    }

    /** Where the song leaves its beak. */
    public Vec3 beakTip() {
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        return this.position().add(-Mth.sin(yaw) * 1.9, 2.25, Mth.cos(yaw) * 1.9);
    }

    @Override
    protected void addMovementGoals() {
        // it flies itself: see flyTowards
    }

    @Override
    protected boolean canChase(int state) {
        return false;
    }

    @Override
    protected double meleeReach() {
        return 0.8;
    }

    // ------------------------------------------------------------------ flight

    private void flyTowards(Vec3 goal, double accel, double max) {
        Vec3 d = goal.subtract(this.position());
        Vec3 v = this.getDeltaMovement().scale(0.92).add(d.normalize().scale(Math.min(1.0, d.length() * 0.25) * accel));
        if (v.length() > max) {
            v = v.normalize().scale(max);
        }
        this.setDeltaMovement(v);
    }

    private void face(Vec3 at) {
        double dx = at.x - this.getX();
        double dz = at.z - this.getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        this.setYRot(Mth.approachDegrees(this.getYRot(), yaw, 10.0F));
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.clientEffects();
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        if (this.home == null) {
            this.home = this.blockPosition();
        }
        super.customServerAiStep(level);
        if (this.getTarget() == null || !this.getTarget().isAlive()) {
            // no one to fight: drift in slow circles over home
            this.setBeamTarget(null);
            this.orbit += 0.01F;
            Vec3 h = Vec3.atCenterOf(this.home);
            Vec3 goal = h.add(Mth.cos(this.orbit) * 8.0, 6.0 + Mth.sin(this.tickCount * 0.02F) * 2.0, Mth.sin(this.orbit) * 8.0);
            this.flyTowards(goal, 0.03, 0.25);
            this.face(this.position().add(this.getDeltaMovement().scale(10.0)));
        }
    }

    // ------------------------------------------------------------------ attacks

    @Override
    protected void tickAttacks(ServerLevel level, LivingEntity target, int state) {
        int t = this.stateTicks;
        double dist = this.distanceTo(target);
        Vec3 tp = target.position();
        if (state != DIVE && state != STUNNED) {
            // wheel around the target, high up
            this.orbit += state == IDLE ? 0.025F : 0.008F;
            double r = state == IDLE ? 9.0 : 11.0;
            Vec3 goal = tp.add(Mth.cos(this.orbit) * r, 6.0 + Mth.sin(this.tickCount * 0.05F) * 1.2 + (state == DIVE_WINDUP ? t * 0.2 : 0.0),
                    Mth.sin(this.orbit) * r);
            this.flyTowards(goal, state == IDLE ? 0.06 : 0.03, state == IDLE ? 0.55 : 0.3);
            this.face(tp);
        }
        switch (state) {
            case IDLE -> {
                this.setBeamTarget(null);
                if (this.cooldown > 0) {
                    return;
                }
                if (dist < 28 && this.hasLineOfSight(target) && this.random.nextInt(5) < 3) {
                    this.setState(BEAM_CHARGE);
                    this.setBeamTarget(target);
                    this.playSound(ModSounds.WHISTLER_CHARGE.get(), 3.0F, 1.0F);
                } else {
                    this.setState(DIVE_WINDUP);
                    this.playSound(ModSounds.WHISTLER_SCREECH.get(), 3.0F, 1.1F);
                }
            }
            case BEAM_CHARGE -> {
                if (t % 5 == 0) {
                    this.playSound(SoundEvents.NOTE_BLOCK_FLUTE.value(), 2.0F, 0.5F + t * 0.03F);
                }
                if (!this.hasLineOfSight(target)) {
                    this.endAttack(30);
                } else if (t >= 32) {
                    this.lockDamage = 0.0F;
                    this.playSound(ModSounds.WHISTLER_LOCK.get(), 3.0F, 1.0F);
                    this.setState(BEAM_LOCK);
                }
            }
            case BEAM_LOCK -> {
                if (!this.hasLineOfSight(target) || dist > 32 || !target.isAlive()) {
                    // the lock is broken
                    this.playSound(ModSounds.WHISTLER_BREAK.get(), 2.0F, 1.2F);
                    this.endAttack(50);
                    return;
                }
                if (t % 10 == 0) {
                    // a slow, steady sonic toll that no armour stops
                    target.hurtServer(level, this.damageSources().sonicBoom(this), 2.0F + t / 50.0F);
                    target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 1), this);
                    this.playSound(SoundEvents.NOTE_BLOCK_FLUTE.value(), 2.5F, 0.7F + (t % 40) * 0.02F);
                    this.playSound(SoundEvents.NOTE_BLOCK_CHIME.value(), 1.5F, 1.2F);
                }
                if (t >= LOCK_TICKS) {
                    this.endAttack(60);
                }
            }
            case DIVE_WINDUP -> {
                if (t >= 22) {
                    this.diveAt = target.position().add(0, 0.6, 0);
                    this.diveHits.clear();
                    this.playSound(ModSounds.WHISTLER_DIVE.get(), 3.0F, 1.0F);
                    this.setState(DIVE);
                }
            }
            case DIVE -> {
                Vec3 d = this.diveAt.subtract(this.position());
                if (t < 30 && d.length() > 1.2) {
                    this.setDeltaMovement(d.normalize().scale(Math.min(1.25, 0.5 + t * 0.08)));
                    this.face(this.diveAt);
                } else {
                    this.setDeltaMovement(this.getDeltaMovement().scale(0.8).add(0, 0.12, 0));
                }
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.8))) {
                    if (e != this && !isBandmate(e) && this.diveHits.add(e.getId()) && e.hurtServer(level, this.damageSources().mobAttack(this), 11.0F)) {
                        Vec3 v = this.getDeltaMovement().multiply(1, 0, 1).normalize();
                        e.push(v.x * 1.2, 0.6, v.z * 1.2);
                        level.sendParticles(ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY() + 1.0, e.getZ(), 1, 0, 0, 0, 0);
                    }
                }
                if (t >= 46 || (this.horizontalCollision || this.verticalCollision) && t > 6) {
                    this.endAttack(40);
                }
            }
            case STUNNED -> {
                this.setBeamTarget(null);
                this.setDeltaMovement(this.getDeltaMovement().scale(0.9).add(0, this.onGround() ? 0.0 : -0.04, 0));
                if (t % 6 == 0) {
                    level.sendParticles(ModParticles.STAR_SPARKLE.get(), this.getX(), this.getY() + 2.6, this.getZ(), 3, 0.5, 0.2, 0.5, 0.0);
                }
                if (t >= 60) {
                    this.endAttack(20);
                }
            }
            default -> this.endAttack(20);
        }
    }

    private void endAttack(int cooldown) {
        this.setBeamTarget(null);
        this.setState(IDLE);
        this.cooldown = cooldown + this.random.nextInt(20);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, this.getState() == STUNNED ? amount * 1.5F : amount);
        if (hurt && this.getState() == BEAM_LOCK) {
            this.lockDamage += amount;
            if (this.lockDamage >= LOCK_BREAK_DAMAGE) {
                // hit hard enough mid-song, it falters and tumbles out of the air
                this.playSound(ModSounds.WHISTLER_BREAK.get(), 3.0F, 0.8F);
                level.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY() + 1.5, this.getZ(), 30, 0.8, 0.8, 0.8, 0.05);
                this.setBeamTarget(null);
                this.setState(STUNNED);
            }
        }
        return hurt;
    }

    // ------------------------------------------------------------------ client: soul trails and the beam

    private void clientEffects() {
        Level level = this.level();
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        double rx = Mth.cos(yaw);
        double rz = Mth.sin(yaw);
        int s = this.getState();
        if (this.deathTime == 0 && !(s == STUNNED && this.onGround())) {
            // the wingtips, flapping as the model flaps them
            float flap = WhistlerFlap.angle(this.tickCount, s);
            double span = 2.8;
            double up = Math.sin(flap) * span;
            double out = Math.cos(flap) * span;
            for (int side = -1; side <= 1; side += 2) {
                double x = this.getX() + rx * out * side;
                double z = this.getZ() + rz * out * side;
                double y = this.getY() + 1.95 + up;
                level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 0, 0.01, 0);
                if (this.random.nextInt(2) == 0) {
                    level.addParticle(ModParticles.PORTAL_SOUL.get(), x, y, z, 0, 0, 0);
                }
                if (this.random.nextInt(6) == 0) {
                    level.addParticle(ParticleTypes.SOUL, x, y, z, 0, -0.02, 0);
                }
            }
        }
        LivingEntity target = this.beamTarget();
        if (target != null && (s == BEAM_CHARGE || s == BEAM_LOCK)) {
            Vec3 from = this.beakTip();
            Vec3 to = target.position().add(0, target.getBbHeight() * 0.6, 0);
            Vec3 d = to.subtract(from);
            double len = d.length();
            boolean locked = s == BEAM_LOCK;
            int n = locked ? (int) (len * 3) : (int) (len * 0.8);
            DustParticleOptions dust = new DustParticleOptions(locked ? 0x4FF0FF : 0xBFFBFF, locked ? 1.3F : 0.6F);
            for (int i = 0; i < n; i++) {
                double k = this.random.nextDouble();
                Vec3 p = from.add(d.scale(k));
                level.addParticle(dust, p.x, p.y, p.z, 0, 0, 0);
            }
            if (locked) {
                double k = (this.tickCount % 12) / 12.0;
                Vec3 p = from.add(d.scale(k));
                if (this.tickCount % 3 == 0) {
                    level.addParticle(ParticleTypes.SONIC_BOOM, p.x, p.y, p.z, 0, 0, 0);
                }
                if (this.random.nextInt(2) == 0) {
                    Vec3 q = from.add(d.scale(this.random.nextDouble()));
                    level.addParticle(ModParticles.SIFT_NOTE.get(), q.x, q.y, q.z, this.random.nextDouble(), 0, 0);
                }
                level.addParticle(ParticleTypes.SCULK_SOUL, to.x + (this.random.nextDouble() - 0.5) * 0.6, to.y, to.z + (this.random.nextDouble() - 0.5) * 0.6,
                        0, 0.05, 0);
            }
        }
    }

    @Override
    public boolean isPushable() {
        return this.getState() != DIVE;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.WHISTLER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.WHISTLER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.WHISTLER_DEATH.get();
    }

    @Override
    protected int burstColorA() {
        return 0x4FF0FF;
    }

    @Override
    protected int burstColorB() {
        return 0xF2F1EA;
    }

    @Override
    public void makePoofParticles() {
        super.makePoofParticles();
        for (int i = 0; i < 30; i++) {
            this.level().addParticle(ParticleTypes.SOUL, this.getRandomX(1.5), this.getRandomY(), this.getRandomZ(1.5), 0, 0.08, 0);
        }
    }

    /** The wing beat, shared by the entity (wingtip particles) and its model. */
    public static final class WhistlerFlap {
        private WhistlerFlap() {
        }

        /** Wing angle above horizontal, radians, at the given age. */
        public static float angle(float age, int state) {
            return switch (state) {
                case DIVE -> -1.2F;
                case STUNNED -> -0.9F + Mth.sin(age * 0.6F) * 0.3F;
                case BEAM_LOCK -> 0.55F + Mth.sin(age * 0.12F) * 0.1F;
                case BEAM_CHARGE, CALL -> 0.7F + Mth.sin(age * 0.2F) * 0.2F;
                case DIVE_WINDUP -> Mth.sin(age * 0.45F) * 0.9F;
                default -> Mth.sin(age * 0.22F) * 0.75F;
            };
        }
    }
}
