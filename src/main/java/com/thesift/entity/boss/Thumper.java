package com.thesift.entity.boss;

import com.thesift.registry.ModEntities;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Thumper: a giant snapping turtle with a war drum strapped to its shell. Nothing gets
 * through that shell - every blow anywhere else clanks off - but the drum on top is its weak
 * point. Its attacks:
 *
 * <ul>
 *   <li>Slam: rears up on its hind legs and crashes down; shockwave rings roll out across the
 *   ground (jump them).</li>
 *   <li>Charge: drums itself into a fury, lowers its head and thunders forward in a straight
 *   line. If it runs into a wall it is dazed: its drum hangs low and every hit on it counts
 *   double.</li>
 *   <li>Spin: tucks into its shell and spins like a top after you, throwing everything away.</li>
 *   <li>Drum roll: calls up its hatchlings, the Thumplings.</li>
 * </ul>
 */
public class Thumper extends MiniBoss {
    public static final int SLAM = 1;
    public static final int CHARGE_WINDUP = 2;
    public static final int CHARGE = 3;
    public static final int SPIN = 4;
    public static final int DAZED = 5;
    public static final int DRUMROLL = 6;
    /** Rendered at twice its model size. */
    public static final float SCALE = 2.0F;

    private Vec3 chargeDir = Vec3.ZERO;
    private final Set<Integer> chargeHits = new HashSet<>();

    public Thumper(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.GREEN);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 240.0).add(Attributes.ARMOR, 8.0).add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ATTACK_DAMAGE, 10.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.STEP_HEIGHT, 1.5).add(Attributes.ATTACK_KNOCKBACK, 1.5);
    }

    @Override
    protected double meleeReach() {
        return 1.4;
    }

    // ------------------------------------------------------------------ the drum, its weak point

    /** The drum on its back, in world space. */
    public AABB drumBox() {
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        double back = 0.125;
        double cx = this.getX() + Mth.sin(yaw) * back;
        double cz = this.getZ() - Mth.cos(yaw) * back;
        double low = this.getState() == DAZED ? 2.0 : 2.55;
        return new AABB(cx - 0.85, this.getY() + low, cz - 0.85, cx + 0.85, this.getY() + 3.95, cz + 0.85);
    }

    /** Did this blow land on the drum? Projectiles must touch it; melee must be aimed at it. */
    public boolean hitsDrum(DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return true;
        }
        AABB drum = this.drumBox();
        Entity direct = source.getDirectEntity();
        if (direct instanceof Projectile p) {
            return drum.inflate(0.6).intersects(p.getBoundingBox()) || drum.inflate(0.6).contains(p.position());
        }
        if (direct instanceof LivingEntity le) {
            Vec3 eye = le.getEyePosition();
            Vec3 end = eye.add(le.getLookAngle().scale(8.0));
            return drum.inflate(0.3).contains(eye) || drum.inflate(0.3).clip(eye, end).isPresent();
        }
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.getState() == SPIN && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            this.clank(level, source);
            return false;
        }
        if (!this.hitsDrum(source)) {
            this.clank(level, source);
            return false;
        }
        boolean dazed = this.getState() == DAZED;
        boolean hurt = super.hurtServer(level, source, dazed ? amount * 2.0F : amount);
        if (hurt) {
            AABB d = this.drumBox();
            this.playSound(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 3.0F, 0.6F + this.random.nextFloat() * 0.2F);
            this.playSound(ModSounds.THUMPER_DRUM_HIT.get(), 2.0F, dazed ? 0.8F : 1.0F);
            level.sendParticles(ModParticles.RESONANCE_RING.get(), d.getCenter().x, d.maxY - 0.05, d.getCenter().z, 0, 1.4, 0.0, 0.0, 1.0);
            level.sendParticles(ModParticles.SIFT_NOTE.get(), d.getCenter().x, d.maxY + 0.3, d.getCenter().z, 6, 0.5, 0.3, 0.5, 1.0);
            level.sendParticles(ParticleTypes.SCULK_SOUL, d.getCenter().x, d.maxY, d.getCenter().z, 4, 0.4, 0.1, 0.4, 0.04);
        }
        return hurt;
    }

    /** A blow on the shell: a clank and a shower of sparks, and nothing else. */
    private void clank(ServerLevel level, DamageSource source) {
        if (this.tickCount % 2 == 0 || source.getDirectEntity() instanceof Projectile) {
            Entity e = source.getDirectEntity();
            Vec3 at = e != null ? e.position().add(0, e.getBbHeight() * 0.7, 0).lerp(this.position().add(0, 1.4, 0), 0.6) : this.position().add(0, 1.4, 0);
            level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 8, 0.2, 0.2, 0.2, 0.3);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 4, 0.2, 0.2, 0.2, 0.2);
            this.playSound(ModSounds.THUMPER_CLANK.get(), 1.5F, 0.9F + this.random.nextFloat() * 0.3F);
        }
    }

    // ------------------------------------------------------------------ attacks

    @Override
    protected boolean canChase(int state) {
        return state == IDLE;
    }

    @Override
    protected void tickAttacks(ServerLevel level, LivingEntity target, int state) {
        int t = this.stateTicks;
        double dist = this.distanceTo(target);
        switch (state) {
            case IDLE -> {
                if (this.cooldown > 0) {
                    return;
                }
                if (this.summonCooldown <= 0 && this.random.nextInt(3) == 0) {
                    this.setState(DRUMROLL);
                    this.summonCooldown = 320;
                } else if (dist < 6.5) {
                    this.setState(this.random.nextBoolean() ? SLAM : SPIN);
                } else if (dist < 22 && this.hasLineOfSight(target)) {
                    this.setState(this.random.nextInt(4) == 0 ? SPIN : CHARGE_WINDUP);
                }
                if (this.getState() != IDLE) {
                    this.getNavigation().stop();
                    this.announce(level);
                }
            }
            case SLAM -> {
                this.getNavigation().stop();
                this.getLookControl().setLookAt(target, 10.0F, 10.0F);
                if (t == 22) {
                    this.slamImpact(level);
                }
                if (t >= 42) {
                    this.endAttack(30);
                }
            }
            case CHARGE_WINDUP -> {
                this.getNavigation().stop();
                this.getLookControl().setLookAt(target, 30.0F, 30.0F);
                this.faceTowards(target);
                if (t % 4 == 0) {
                    this.playSound(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 2.0F, 0.7F + t * 0.02F);
                    level.sendParticles(ModParticles.SIFT_NOTE.get(), this.getX(), this.getY() + 4.0, this.getZ(), 1, 0.4, 0.2, 0.4, 1.0);
                }
                if (t >= 26) {
                    Vec3 d = target.position().subtract(this.position()).multiply(1, 0, 1);
                    this.chargeDir = d.lengthSqr() < 1.0E-4 ? this.getLookAngle().multiply(1, 0, 1).normalize() : d.normalize();
                    this.chargeHits.clear();
                    this.playSound(ModSounds.THUMPER_ROAR.get(), 3.0F, 1.0F);
                    this.setState(CHARGE);
                }
            }
            case CHARGE -> this.tickCharge(level, t);
            case SPIN -> this.tickSpin(level, target, t);
            case DAZED -> {
                this.getNavigation().stop();
                if (t % 10 == 0) {
                    AABB d = this.drumBox();
                    level.sendParticles(ModParticles.STAR_SPARKLE.get(), d.getCenter().x, d.maxY + 0.6, d.getCenter().z, 4, 0.6, 0.2, 0.6, 0.0);
                }
                if (t >= 80) {
                    this.endAttack(20);
                }
            }
            case DRUMROLL -> {
                this.getNavigation().stop();
                if (t % 3 == 0) {
                    this.playSound(SoundEvents.NOTE_BLOCK_SNARE.value(), 1.5F, 0.8F + this.random.nextFloat() * 0.4F);
                }
                if (t == 24) {
                    this.summon(level, ModEntities.THUMPLING.get(), 3, 6, 3.5);
                    this.playSound(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 3.0F, 0.5F);
                }
                if (t >= 36) {
                    this.endAttack(30);
                }
            }
            default -> this.endAttack(20);
        }
    }

    private void announce(ServerLevel level) {
        this.level().broadcastEntityEvent(this, (byte) 60);
        this.playSound(ModSounds.THUMPER_WINDUP.get(), 2.0F, 0.9F + this.random.nextFloat() * 0.2F);
    }

    private void endAttack(int cooldown) {
        this.setState(IDLE);
        this.cooldown = cooldown + this.random.nextInt(20);
    }

    private void faceTowards(Entity e) {
        double dx = e.getX() - this.getX();
        double dz = e.getZ() - this.getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        this.setYRot(Mth.approachDegrees(this.getYRot(), yaw, 8.0F));
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();
    }

    private void slamImpact(ServerLevel level) {
        this.playSound(ModSounds.THUMPER_SLAM.get(), 4.0F, 0.7F);
        this.playSound(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 4.0F, 0.5F);
        Vec3 c = this.position();
        for (int i = 0; i < 4; i++) {
            level.sendParticles(ModParticles.RESONANCE_RING.get(), c.x, c.y + 0.1, c.z, 0, 2.5 + i * 2.0, 0.0, 0.0, 1.0);
        }
        BlockState ground = level.getBlockState(this.blockPosition().below());
        if (!ground.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), c.x, c.y + 0.1, c.z, 80, 3.0, 0.1, 3.0, 0.3);
        }
        level.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.5, c.z, 3, 1.5, 0.2, 1.5, 0.0);
        this.hitAround(level, 7.5, 9.0F, 1.1, 0.75, true);
    }

    private void tickCharge(ServerLevel level, int t) {
        double speed = Math.min(0.85, 0.3 + t * 0.06);
        this.setDeltaMovement(this.chargeDir.x * speed, this.getDeltaMovement().y, this.chargeDir.z * speed);
        float yaw = (float) (Mth.atan2(this.chargeDir.z, this.chargeDir.x) * Mth.RAD_TO_DEG) - 90.0F;
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
        if (t % 2 == 0) {
            BlockState ground = level.getBlockState(this.blockPosition().below());
            if (!ground.isAir()) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), this.getX(), this.getY() + 0.1, this.getZ(), 12, 1.2, 0.1, 1.2, 0.15);
            }
            level.sendParticles(ParticleTypes.CLOUD, this.getX() - this.chargeDir.x * 1.5, this.getY() + 0.3, this.getZ() - this.chargeDir.z * 1.5, 3, 0.5, 0.2, 0.5, 0.02);
        }
        if (t % 6 == 0) {
            this.playSound(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 2.0F, 0.6F);
        }
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.6))) {
            if (e == this || isBandmate(e) || !this.chargeHits.add(e.getId())) {
                continue;
            }
            if (e.hurtServer(level, this.damageSources().mobAttack(this), 13.0F)) {
                e.push(this.chargeDir.x * 2.2, 0.7, this.chargeDir.z * 2.2);
                e.hurtMarked = true;
                this.playSound(ModSounds.THUMPER_SLAM.get(), 2.0F, 1.3F);
            }
        }
        if (this.horizontalCollision && t > 4) {
            // smack into a wall: dazed, its drum hanging low
            this.setDeltaMovement(Vec3.ZERO);
            this.playSound(ModSounds.THUMPER_SLAM.get(), 4.0F, 0.6F);
            this.playSound(ModSounds.THUMPER_DAZED.get(), 2.0F, 1.0F);
            level.sendParticles(ParticleTypes.EXPLOSION, this.getX() + this.chargeDir.x * 1.6, this.getY() + 1.2, this.getZ() + this.chargeDir.z * 1.6,
                    2, 0.4, 0.4, 0.4, 0.0);
            this.setState(DAZED);
            return;
        }
        if (t >= 48) {
            this.endAttack(30);
        }
    }

    private void tickSpin(ServerLevel level, LivingEntity target, int t) {
        if (t < 10 || t > 74) {
            this.getNavigation().stop();
        } else {
            Vec3 d = target.position().subtract(this.position()).multiply(1, 0, 1);
            if (d.lengthSqr() > 1.0E-4) {
                d = d.normalize();
                Vec3 v = this.getDeltaMovement();
                this.setDeltaMovement(Mth.lerp(0.15, v.x, d.x * 0.55), v.y, Mth.lerp(0.15, v.z, d.z * 0.55));
            }
            if (t % 5 == 0) {
                this.playSound(ModSounds.THUMPER_SPIN.get(), 1.6F, 0.8F + (t % 20) * 0.02F);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, this.getX(), this.getY() + 1.0, this.getZ(), 3, 1.4, 0.3, 1.4, 0.0);
            }
            if (t % 8 == 0) {
                this.hitAround(level, 1.6, 6.0F, 1.6, 0.45, false);
            }
        }
        if (t >= 86) {
            this.endAttack(30);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 60) {
            // the client hears the wind-up cue: a puff of dust
            for (int i = 0; i < 8; i++) {
                this.level().addParticle(ParticleTypes.CLOUD, this.getRandomX(1.0), this.getY() + 0.2, this.getRandomZ(1.0), 0, 0.02, 0);
            }
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            int s = this.getState();
            if (s == SPIN && this.random.nextInt(2) == 0) {
                double a = this.random.nextDouble() * Math.PI * 2;
                this.level().addParticle(ParticleTypes.CRIT, this.getX() + Math.cos(a) * 1.6, this.getY() + 0.8, this.getZ() + Math.sin(a) * 1.6,
                        -Math.sin(a) * 0.4, 0.05, Math.cos(a) * 0.4);
            }
            if ((s == IDLE || s == CHARGE_WINDUP) && this.random.nextInt(s == IDLE ? 20 : 3) == 0) {
                AABB d = this.drumBox();
                this.level().addParticle(ModParticles.GLOW_DUST.get(), d.getCenter().x + (this.random.nextDouble() - 0.5), d.maxY + 0.1,
                        d.getCenter().z + (this.random.nextDouble() - 0.5), 0, 0.02, 0);
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.THUMPER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.THUMPER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.THUMPER_DEATH.get();
    }

    @Override
    protected int burstColorA() {
        return 0x73A050;
    }

    @Override
    protected int burstColorB() {
        return 0xCD5F42;
    }
}
