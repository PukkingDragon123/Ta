package com.thesift.entity.jungle;

import com.thesift.entity.KillBurst;
import com.thesift.registry.ModCaveJungle;
import com.thesift.registry.ModItems;
import com.thesift.registry.ModParticles;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * P4 Cave Jungle: the Colossus Ponder - a frog the size of a house, wearing the jungle floor: Lumen Moss, ferns,
 * glowcaps and trailing vines grow on its back. It ponders. It ignores you, mostly - but it does not look where it
 * puts its feet, and its heavy steps hurt whatever small thing stands too close. Its tadpoles live in the logs
 * nearby; hurt one and every Ponder around goes on a rampage: it roars, bounds after you and lands like a falling
 * house. Now and then it lays a giant egg - bake it for a meal that fills you for a day.
 */
public class ColossusPonder extends PathfinderMob {
    private static final EntityDataAccessor<Boolean> RAMPAGE = SynchedEntityData.defineId(ColossusPonder.class, EntityDataSerializers.BOOLEAN);
    private static final byte EVENT_CROAK = 100;
    private static final byte EVENT_STOMP = 101;
    private static final byte EVENT_LAY = 102;
    private static final byte EVENT_ROAR = 103;
    private static final byte EVENT_SLAM = 104;

    public final AnimationState croakAnimation = new AnimationState();
    public final AnimationState stompAnimation = new AnimationState();
    public final AnimationState layAnimation = new AnimationState();
    public final AnimationState roarAnimation = new AnimationState();
    public final AnimationState slamAnimation = new AnimationState();

    private int rampageTicks;
    private int eggTime;
    private int stepTimer;
    private boolean leaping;
    private int leapTicks;

    public ColossusPonder(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 20;
        this.eggTime = 6000 + this.random.nextInt(6000);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 150.0).add(Attributes.ARMOR, 8.0).add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ATTACK_DAMAGE, 12.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(RAMPAGE, false);
    }

    @Override
    public int getNoActionTime() {
        return 0;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RampageGoal());
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7, 0.002F));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F, 0.02F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    public boolean isRampaging() {
        return this.entityData.get(RAMPAGE);
    }

    /** Something hurt one of its tadpoles (or it): every Ponder within 32 blocks rampages after {@code culprit}. */
    public static void alarm(ServerLevel level, Vec3 at, LivingEntity culprit) {
        if (culprit instanceof ColossusPonder || culprit instanceof PonderTadpole || culprit instanceof Player p && (p.isCreative() || p.isSpectator())) {
            return;
        }
        for (ColossusPonder c : level.getEntitiesOfClass(ColossusPonder.class, new AABB(at, at).inflate(32.0), ColossusPonder::isAlive)) {
            c.rampage(level, culprit);
        }
    }

    private void rampage(ServerLevel level, LivingEntity culprit) {
        this.setTarget(culprit);
        this.rampageTicks = 600;
        if (!this.isRampaging()) {
            this.entityData.set(RAMPAGE, true);
            level.broadcastEntityEvent(this, EVENT_ROAR);
            this.playSound(ModCaveJungle.PONDER_ROAR.get(), 3.0F, 0.9F);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && this.isAlive() && source.getEntity() instanceof LivingEntity attacker) {
            this.rampage(level, attacker);
        }
        return hurt;
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return 0.0F;
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    /** A Ponder arrives with its brood: two to four tadpoles. */
    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
            @Nullable SpawnGroupData data) {
        if (reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION || reason == EntitySpawnReason.SPAWN_ITEM_USE) {
            int n = 2 + this.random.nextInt(3);
            for (int i = 0; i < n; i++) {
                PonderTadpole t = ModCaveJungle.PONDER_TADPOLE.get().create(level.getLevel(), EntitySpawnReason.NATURAL);
                if (t != null) {
                    t.snapTo(this.getX() + (this.random.nextDouble() - 0.5) * 3.0, this.getY(), this.getZ() + (this.random.nextDouble() - 0.5) * 3.0,
                            this.random.nextFloat() * 360.0F, 0.0F);
                    level.addFreshEntity(t);
                }
            }
        }
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    // ------------------------------------------------------------------ heavy feet

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.rampageTicks > 0) {
            LivingEntity t = this.getTarget();
            if (--this.rampageTicks <= 0 || t == null || !t.isAlive() || this.distanceToSqr(t) > 48.0 * 48.0) {
                this.rampageTicks = 0;
                this.entityData.set(RAMPAGE, false);
                this.setTarget(null);
            }
        }
        // every heavy step on the move: it does not look where it treads
        boolean moving = this.getDeltaMovement().horizontalDistanceSqr() > 2.5E-4 && this.onGround();
        if (moving && ++this.stepTimer >= (this.isRampaging() ? 9 : 16)) {
            this.stepTimer = 0;
            this.stomp(level, this.isRampaging() ? 2.4 : 1.6, this.isRampaging() ? 5.0F : 3.0F, false);
        }
        // the leap of a rampage lands like a falling house
        if (this.leaping) {
            this.leapTicks++;
            if (this.leapTicks > 5 && this.onGround()) {
                this.leaping = false;
                level.broadcastEntityEvent(this, EVENT_SLAM);
                this.stomp(level, 4.0, 12.0F, true);
            }
        }
        if (!this.isRampaging() && --this.eggTime <= 0) {
            this.eggTime = 9000 + this.random.nextInt(9000);
            this.spawnAtLocation(level, new ItemStack(ModItems.PONDER_EGG.get()));
            level.broadcastEntityEvent(this, EVENT_LAY);
            this.playSound(ModCaveJungle.PONDER_LAY.get(), 1.5F, 1.0F);
        }
    }

    /** A footfall: anything small under or beside its feet is hurt and shoved (a slam hits everything in reach). */
    private void stomp(ServerLevel level, double reach, float damage, boolean slam) {
        level.broadcastEntityEvent(this, EVENT_STOMP);
        this.playSound(ModCaveJungle.PONDER_STOMP.get(), slam ? 3.0F : 1.4F, slam ? 0.7F : 0.85F + this.random.nextFloat() * 0.2F);
        BlockState ground = level.getBlockState(this.blockPosition().below());
        if (!ground.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), this.getX(), this.getY() + 0.1, this.getZ(), slam ? 60 : 16,
                    reach * 0.5, 0.05, reach * 0.5, 0.15);
        }
        if (slam) {
            level.sendParticles(ModParticles.RESONANCE_RING.get(), this.getX(), this.getY() + 0.1, this.getZ(), 0, reach + 1.0, 0.0, 0.0, 1.0);
            level.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 0.2, this.getZ(), 30, reach * 0.5, 0.1, reach * 0.5, 0.08);
        }
        AABB box = this.getBoundingBox().inflate(reach - this.getBbWidth() * 0.5 + 0.5, 0.0, reach - this.getBbWidth() * 0.5 + 0.5)
                .setMaxY(this.getY() + 1.2);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, v -> this.squashable(v, slam))) {
            Vec3 d = e.position().subtract(this.position());
            double len = Math.max(0.3, d.horizontalDistance());
            e.hurtServer(level, this.damageSources().mobAttack(this), damage);
            e.push(d.x / len * (slam ? 1.2 : 0.5), slam ? 0.5 : 0.25, d.z / len * (slam ? 1.2 : 0.5));
        }
    }

    private boolean squashable(LivingEntity e, boolean slam) {
        if (e == this || !e.isAlive() || e instanceof ColossusPonder || e instanceof PonderTadpole || e.getVehicle() == this) {
            return false;
        }
        if (e instanceof Player p && (p.isCreative() || p.isSpectator())) {
            return false;
        }
        // only things that fit under a foot, unless it means it
        return slam || e.getBbHeight() < 2.2F && e.onGround();
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
        return false;
    }

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EVENT_CROAK -> this.croakAnimation.start(this.tickCount);
            case EVENT_STOMP -> this.stompAnimation.start(this.tickCount);
            case EVENT_LAY -> this.layAnimation.start(this.tickCount);
            case EVENT_ROAR -> {
                this.slamAnimation.stop();
                this.roarAnimation.start(this.tickCount);
            }
            case EVENT_SLAM -> {
                this.roarAnimation.stop();
                this.slamAnimation.start(this.tickCount);
                com.thesift.world.Rumble.at(this.position(), 1.6F, 24.0F, 14);
            }
            default -> super.handleEntityEvent(id);
        }
    }

    @Override
    public void playAmbientSound() {
        super.playAmbientSound();
        if (this.level() instanceof ServerLevel server) {
            server.broadcastEntityEvent(this, EVENT_CROAK);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() && this.random.nextInt(8) == 0) {
            // spores and glow drifting off the garden on its back
            this.level().addParticle(ModParticles.GLOW_DUST.get(), this.getRandomX(1.0), this.getY() + 2.2 + this.random.nextDouble() * 0.5,
                    this.getRandomZ(1.0), 0.0, 0.01, 0.0);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("EggTime", this.eggTime);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.eggTime = input.getIntOr("EggTime", 9000);
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ModCaveJungle.PONDER_CROAK.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModCaveJungle.PONDER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModCaveJungle.PONDER_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 2.0F;
    }

    @Override
    public void makePoofParticles() {
        KillBurst.pop(this, 0x316333, 0x45B4B8, KillBurst.DROP, ParticleTypes.SPORE_BLOSSOM_AIR);
    }

    /** Rampaging: bound after the culprit, and leap on it from a distance. */
    private class RampageGoal extends Goal {
        private int leapCooldown;
        private int attackCooldown;

        RampageGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = ColossusPonder.this.getTarget();
            return t != null && t.isAlive();
        }

        @Override
        public void tick() {
            ColossusPonder c = ColossusPonder.this;
            LivingEntity t = c.getTarget();
            if (t == null) {
                return;
            }
            c.getLookControl().setLookAt(t, 20.0F, 20.0F);
            double d = c.distanceToSqr(t);
            if (this.leapCooldown > 0) {
                this.leapCooldown--;
            }
            if (this.attackCooldown > 0) {
                this.attackCooldown--;
            }
            if (d > 25.0 && this.leapCooldown <= 0 && c.onGround() && !c.leaping) {
                // the leap: a huge bound towards the culprit
                Vec3 to = t.position().subtract(c.position());
                double flat = Math.max(0.1, to.horizontalDistance());
                double speed = Mth.clamp(flat * 0.08, 0.6, 1.4);
                c.setDeltaMovement(to.x / flat * speed, 0.85, to.z / flat * speed);
                c.leaping = true;
                c.leapTicks = 0;
                this.leapCooldown = 70;
                c.playSound(ModCaveJungle.PONDER_ROAR.get(), 2.0F, 1.2F);
                return;
            }
            if (c.getNavigation().isDone() || c.tickCount % 10 == 0) {
                c.getNavigation().moveTo(t, 1.5);
            }
            if (d < 12.0 && this.attackCooldown <= 0 && c.level() instanceof ServerLevel server) {
                this.attackCooldown = 30;
                c.doHurtTarget(server, t);
                server.broadcastEntityEvent(c, EVENT_SLAM);
            }
        }

        @Override
        public void stop() {
            ColossusPonder.this.getNavigation().stop();
        }
    }
}
