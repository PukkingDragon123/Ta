package com.thesift.entity;

import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Gobbler: a huge-mouthed, blind deep-sea catfish of the Deep Dark Ocean, kin to the Warden.
 * It cannot see. It hunts by feel - fast swimming, thrashing and music all carry through the water
 * to its sensory pits, while anything that keeps still or sneaks goes unnoticed. When it has
 * found you it creeps closer, opens wide and lunges; caught, you are gulped down for a moment
 * (sneak to wriggle free) and spat back out. The Lullaby lulls it: a calm Gobbler drifts
 * peacefully and ignores even the loudest swimmer.
 */
public class Gobbler extends SiftFish implements Enemy {
    private static final byte EVENT_LUNGE = 110;
    private static final byte EVENT_GULP = 111;
    private static final byte EVENT_SPIT = 112;
    private static final byte EVENT_CALM = 113;
    private static final EntityDataAccessor<Boolean> CALM = SynchedEntityData.defineId(Gobbler.class, EntityDataSerializers.BOOLEAN);
    private static final double HEAR_RANGE = 24.0;
    private static final int WINDUP = 12;
    private static final int LUNGE = 10;
    private static final int GULP_TICKS = 50;

    public final AnimationState lungeAnimation = new AnimationState();
    public final AnimationState gulpAnimation = new AnimationState();
    public final AnimationState spitAnimation = new AnimationState();
    public final AnimationState calmAnimation = new AnimationState();
    private @Nullable LivingEntity prey;
    private @Nullable Vec3 heardAt;
    private int heardTicks;
    private int senseTimer;
    private int lungeTicks;
    private int gulpTicks;
    private int cooldown;
    private int calmTicks;

    public Gobbler(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 44.0).add(Attributes.MOVEMENT_SPEED, 0.6).add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6).add(Attributes.FOLLOW_RANGE, HEAR_RANGE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CALM, false);
    }

    public boolean isCalm() {
        return this.entityData.get(CALM);
    }

    /** The Lullaby: the Gobbler forgets its hunt, spits out whatever it holds and drifts, lulled. */
    public void calm(int ticks) {
        if (!(this.level() instanceof ServerLevel server)) {
            return;
        }
        this.calmTicks = Math.max(this.calmTicks, ticks);
        this.entityData.set(CALM, true);
        this.prey = null;
        this.heardAt = null;
        this.lungeTicks = 0;
        this.setAggressive(false);
        this.setTarget(null);
        this.spit(server);
        server.broadcastEntityEvent(this, EVENT_CALM);
        this.playSound(ModSounds.GOBBLER_CALM.get(), 1.2F, 1.0F);
        server.sendParticles(ModParticles.SIFT_NOTE.get(), this.getX(), this.getY() + 1.2, this.getZ(), 6, 0.8, 0.4, 0.8, 1.0);
    }

    /** Something the Gobbler can feel through the water (a vibration at {@code at}). */
    public void sense(Vec3 at, @Nullable LivingEntity source) {
        if (this.isCalm() || this.position().distanceToSqr(at) > HEAR_RANGE * HEAR_RANGE) {
            return;
        }
        if (this.heardAt == null && this.random.nextInt(2) == 0) {
            this.playSound(ModSounds.GOBBLER_SNIFF.get(), 1.0F, 0.8F + this.random.nextFloat() * 0.3F);
        }
        this.heardAt = at;
        this.heardTicks = 80;
        if (source != null) {
            this.prey = source;
        }
    }

    /** How loud a creature is to the blind Gobbler: sneaking and keeping still make no sound at all. */
    private static double loudness(LivingEntity e) {
        if (e.isSteppingCarefully() || e.isPassenger()) {
            return 0.0;
        }
        double v = e.getKnownMovement().length();
        if (e.isSprinting() || e.isSwimming()) {
            v *= 1.6;
        }
        return v;
    }

    private boolean isPrey(LivingEntity e) {
        if (!e.isAlive() || e == this || e instanceof Gobbler) {
            return false;
        }
        if (e instanceof Player p) {
            return !p.isCreative() && !p.isSpectator();
        }
        return e instanceof SiftFish;
    }

    @Override
    protected double cruiseSpeed() {
        return 0.05;
    }

    @Override
    protected double speedFactor() {
        if (this.isCalm()) {
            return 0.6;
        }
        if (this.lungeTicks > LUNGE) {
            return 0.25; // the wind-up: hanging back, mouth gaping
        }
        if (this.lungeTicks > 0) {
            return 7.0;
        }
        if (this.cooldown > 0) {
            return 1.0;
        }
        return this.heardAt != null ? 2.4 : 1.0;
    }

    @Override
    protected @Nullable Vec3 wantedPosition() {
        if (this.isCalm() || this.gulpTicks > 0) {
            return null;
        }
        if (--this.senseTimer <= 0) {
            this.senseTimer = 5;
            for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(HEAR_RANGE), this::isPrey)) {
                double loud = loudness(e);
                double d = Math.sqrt(this.distanceToSqr(e));
                // fast movement carries further; anything thrashing in the water close by is always felt
                if (loud > 0.09 && (e.isInWater() || d < 6.0) && d < HEAR_RANGE * Math.min(1.0, loud * 4.0)) {
                    this.sense(e.getBoundingBox().getCenter(), e);
                }
            }
        }
        if (this.heardAt == null) {
            return null;
        }
        if (this.cooldown > 0) {
            // circle off while it digests
            double a = this.tickCount * 0.08 + this.getId();
            return this.heardAt.add(Math.cos(a) * 7.0, 1.0, Math.sin(a) * 7.0);
        }
        return this.heardAt;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(this.level() instanceof ServerLevel server)) {
            return;
        }
        if (this.calmTicks > 0 && --this.calmTicks == 0) {
            this.entityData.set(CALM, false);
        }
        if (this.cooldown > 0) {
            this.cooldown--;
        }
        if (this.heardTicks > 0 && --this.heardTicks == 0) {
            this.heardAt = null;
            this.prey = null;
        }
        this.setAggressive(this.heardAt != null && !this.isCalm());
        if (this.gulpTicks > 0) {
            this.holdGulped(server);
            return;
        }
        if (this.prey != null && (!this.prey.isAlive() || this.distanceToSqr(this.prey) > HEAR_RANGE * HEAR_RANGE)) {
            this.prey = null;
        }
        if (this.lungeTicks > 0) {
            this.lungeTicks--;
            if (this.lungeTicks <= LUNGE && this.prey != null && this.inLiquid()
                    && this.getBoundingBox().inflate(0.6).intersects(this.prey.getBoundingBox())) {
                this.gulp(server, this.prey);
            } else if (this.lungeTicks == 0) {
                this.cooldown = 40;
            }
        } else if (this.prey != null && this.heardAt != null && this.cooldown <= 0 && this.inLiquid() && !this.isCalm()
                && this.distanceToSqr(this.prey) < 6.0 * 6.0) {
            // open wide... and strike
            this.lungeTicks = WINDUP + LUNGE;
            this.heardAt = this.prey.getBoundingBox().getCenter();
            server.broadcastEntityEvent(this, EVENT_LUNGE);
            this.playSound(ModSounds.GOBBLER_LUNGE.get(), 1.6F, 0.9F + this.random.nextFloat() * 0.2F);
        }
    }

    private void gulp(ServerLevel level, LivingEntity victim) {
        this.lungeTicks = 0;
        level.broadcastEntityEvent(this, EVENT_GULP);
        this.playSound(ModSounds.GOBBLER_GULP.get(), 1.5F, 0.9F);
        Vec3 mouth = this.mouthPos();
        level.sendParticles(ParticleTypes.BUBBLE, mouth.x, mouth.y, mouth.z, 20, 0.5, 0.5, 0.5, 0.1);
        if (victim instanceof Player && this.getPassengers().isEmpty() && victim.startRiding(this, true, true)) {
            this.gulpTicks = GULP_TICKS;
            victim.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
            victim.hurtServer(level, this.damageSources().mobAttack(this), 3.0F);
        } else {
            // a little fish is simply swallowed
            victim.hurtServer(level, this.damageSources().mobAttack(this), 12.0F);
            this.cooldown = 60;
        }
    }

    private void holdGulped(ServerLevel level) {
        Entity held = this.getFirstPassenger();
        if (!(held instanceof LivingEntity victim) || !victim.isAlive()) {
            this.gulpTicks = 0;
            this.cooldown = 80;
            return;
        }
        if (--this.gulpTicks % 16 == 0 && this.gulpTicks > 0) {
            victim.hurtServer(level, this.damageSources().mobAttack(this), 2.0F);
            this.playSound(ModSounds.GOBBLER_GULP.get(), 0.8F, 0.6F);
        }
        if (this.gulpTicks <= 0) {
            this.spit(level);
        }
    }

    /** Out you come: spat forward in a gush of bubbles. */
    private void spit(ServerLevel level) {
        Entity held = this.getFirstPassenger();
        this.gulpTicks = 0;
        if (held == null) {
            return;
        }
        held.stopRiding();
        Vec3 dir = this.getLookAngle();
        Vec3 mouth = this.mouthPos();
        held.teleportTo(mouth.x + dir.x * 0.8, mouth.y, mouth.z + dir.z * 0.8);
        held.push(dir.x * 1.6, 0.4, dir.z * 1.6);
        level.broadcastEntityEvent(this, EVENT_SPIT);
        this.playSound(ModSounds.GOBBLER_SPIT.get(), 1.4F, 1.0F);
        level.sendParticles(ParticleTypes.BUBBLE, mouth.x, mouth.y, mouth.z, 30, 0.4, 0.4, 0.4, 0.3);
        level.sendParticles(ParticleTypes.SCULK_SOUL, mouth.x, mouth.y, mouth.z, 4, 0.3, 0.3, 0.3, 0.02);
        this.cooldown = 120;
        this.heardAt = null;
    }

    private Vec3 mouthPos() {
        Vec3 dir = Vec3.directionFromRotation(0.0F, this.getYRot());
        return this.position().add(dir.scale(this.getBbWidth() * 0.55)).add(0.0, this.getBbHeight() * 0.45, 0.0);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().isEmpty() && passenger instanceof LivingEntity;
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        Vec3 m = this.mouthPos();
        moveFunction.accept(passenger, m.x, m.y - passenger.getBbHeight() * 0.5, m.z);
    }

    @Override
    public boolean dismountsUnderwater() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && this.isAlive()) {
            if (this.isCalm() && source.getEntity() != null) {
                this.calmTicks = 1; // pain wakes it
            }
            if (this.gulpTicks > 0 && damage >= 5.0F) {
                this.spit(level); // a hard hit makes it cough you up
            }
            if (source.getEntity() instanceof LivingEntity attacker && !(attacker instanceof Gobbler)) {
                this.sense(attacker.getBoundingBox().getCenter(), attacker);
            }
        }
        return hurt;
    }

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EVENT_LUNGE -> this.lungeAnimation.start(this.tickCount);
            case EVENT_GULP -> this.gulpAnimation.start(this.tickCount);
            case EVENT_SPIT -> this.spitAnimation.start(this.tickCount);
            case EVENT_CALM -> this.calmAnimation.start(this.tickCount);
            default -> super.handleEntityEvent(id);
        }
    }

    /** Sculk souls drift up from the glowing chest; a lulled Gobbler hums little notes. */
    @Override
    protected void clientEffects() {
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(yaw);
        double fz = Mth.cos(yaw);
        if (this.random.nextInt(this.isAggressive() ? 6 : 16) == 0) {
            this.level().addParticle(ParticleTypes.SCULK_SOUL, this.getX() + fx * 0.2, this.getY() + 0.3, this.getZ() + fz * 0.2, 0.0, 0.03, 0.0);
        }
        if (this.inLiquid() && this.random.nextInt(10) == 0) {
            this.level().addParticle(ModParticles.CHROME_BUBBLE.get(), this.getX() + fx * 1.2, this.getY() + 0.6, this.getZ() + fz * 1.2, 0.0, 0.02, 0.0);
        }
        if (this.isCalm() && this.random.nextInt(30) == 0) {
            this.level().addParticle(ModParticles.SIFT_NOTE.get(), this.getX(), this.getY() + 1.4, this.getZ(), this.random.nextDouble(), 0.0, 0.0);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("CalmTicks", this.calmTicks);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.calmTicks = input.getIntOr("CalmTicks", 0);
        this.entityData.set(CALM, this.calmTicks > 0);
    }

    @Override
    protected SoundEvent getFlopSound() {
        return ModSounds.GOBBLER_FLOP.get();
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ModSounds.GOBBLER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.GOBBLER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.GOBBLER_DEATH.get();
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 1;
    }

    @Override
    public void makePoofParticles() {
        KillBurst.pop(this, 0x0F8C99, 0x3FF5E6, KillBurst.DROP, ParticleTypes.SCULK_SOUL);
    }
}
