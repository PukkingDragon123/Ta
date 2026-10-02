package com.thesift.entity.boss;

import com.thesift.entity.KillBurst;
import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.SpawnUtil;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * A mini-boss of the Sift: one of the three great players the Conductor keeps - the Thumper, the
 * Whistler and the Strummer. Each runs a little state machine of telegraphed attacks (the state is
 * synced, so the client can animate every wind-up and every blow from the moment it changed),
 * shows a boss bar, and calls up its young.
 */
public abstract class MiniBoss extends Monster {
    protected static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(MiniBoss.class, EntityDataSerializers.INT);
    public static final int IDLE = 0;

    private final ServerBossEvent bossEvent;
    /** Ticks in the current state (both sides; reset whenever the state changes). */
    protected int stateTicks;
    /** tickCount when the state last changed, for the client's animations. */
    public int stateStart;
    protected int cooldown = 40;
    protected int summonCooldown = 160;
    protected int meleeCooldown;

    protected MiniBoss(EntityType<? extends Monster> type, Level level, BossEvent.BossBarColor color) {
        super(type, level);
        this.xpReward = 120;
        this.bossEvent = new ServerBossEvent(UUID.randomUUID(), Component.translatable(type.getDescriptionId()), color, BossEvent.BossBarOverlay.NOTCHED_6);
        this.setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, IDLE);
    }

    public int getState() {
        return this.entityData.get(STATE);
    }

    protected void setState(int state) {
        this.entityData.set(STATE, state);
        this.stateTicks = 0;
        this.stateStart = this.tickCount;
    }

    /** For the Codex's page: shows an attack without any of its effects. */
    public void codexPose(int state) {
        this.entityData.set(STATE, state);
        this.stateTicks = 0;
        this.stateStart = this.tickCount;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (STATE.equals(accessor)) {
            this.stateTicks = 0;
            this.stateStart = this.tickCount;
        }
    }

    /** Ticks since the state changed, with the partial tick, for animation. */
    public float stateTime(float partialTicks) {
        return this.tickCount - this.stateStart + partialTicks;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.addMovementGoals();
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, MiniBoss.class, OrchestraMinion.class, Dictator.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    /** Walking at its target between attacks, and wandering when it has none. */
    protected void addMovementGoals() {
        this.goalSelector.addGoal(2, new ChaseGoal());
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
    }

    /** Run every server tick, target or not, before the attack state machine. */
    protected void tickAlways(ServerLevel level, int state) {
    }

    /** States that play out to the end even when it loses its target. */
    protected boolean holdsState(int state) {
        return false;
    }

    /** Renames its boss bar (the bar's look follows the name's translation key). */
    protected void setBossBarName(Component name) {
        this.bossEvent.setName(name);
    }

    /** The attack state machine, run every server tick while it has a living target. */
    protected abstract void tickAttacks(ServerLevel level, LivingEntity target, int state);

    /** Whether the chase goal may walk it towards the target in this state. */
    protected boolean canChase(int state) {
        return state == IDLE;
    }

    protected double chaseSpeed() {
        return 1.0;
    }

    /** Reach of its plain melee bite (blocks from its edge). */
    protected double meleeReach() {
        return 1.6;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        this.stateTicks++;
        this.tickAlways(level, this.getState());
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            if (this.getState() != IDLE && this.stateTicks > 20 && !this.holdsState(this.getState())) {
                this.setState(IDLE);
            }
            return;
        }
        int state = this.getState();
        if (state == IDLE) {
            if (this.cooldown > 0) {
                this.cooldown--;
            }
            if (this.summonCooldown > 0) {
                this.summonCooldown--;
            }
            if (this.meleeCooldown > 0) {
                this.meleeCooldown--;
            } else if (this.distanceTo(target) < this.getBbWidth() * 0.5 + this.meleeReach() + target.getBbWidth() * 0.5 && this.hasLineOfSight(target)) {
                this.doHurtTarget(level, target);
                this.meleeCooldown = 24;
            }
        }
        this.tickAttacks(level, target, state);
    }

    /** True for anything on the Conductor's side, which its attacks never hurt. */
    public static boolean isBandmate(LivingEntity e) {
        return e instanceof MiniBoss || e instanceof OrchestraMinion || e instanceof Dictator;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof LivingEntity le && isBandmate(le)) {
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    /** Calls up to `count` of its young around it, keeping no more than `max` alive nearby. */
    protected <T extends Mob> int summon(ServerLevel level, EntityType<T> type, int count, int max, double radius) {
        int alive = level.getEntities(type, new AABB(this.blockPosition()).inflate(32.0), Entity::isAlive).size();
        int n = Math.min(count, max - alive);
        int made = 0;
        for (int i = 0; i < n; i++) {
            double a = this.random.nextDouble() * Math.PI * 2;
            BlockPos at = BlockPos.containing(this.getX() + Math.cos(a) * radius, this.getY() + 1, this.getZ() + Math.sin(a) * radius);
            var spawned = SpawnUtil.trySpawnMob(type, EntitySpawnReason.MOB_SUMMONED, level, at, 10, 3, 3, SpawnUtil.Strategy.ON_TOP_OF_COLLIDER, false);
            if (spawned.isPresent()) {
                T m = spawned.get();
                m.setTarget(this.getTarget());
                level.sendParticles(ParticleTypes.POOF, m.getX(), m.getY() + 0.3, m.getZ(), 10, 0.3, 0.3, 0.3, 0.02);
                made++;
            }
        }
        return made;
    }

    /** Hurts everything not in the band within `radius`; returns how many it hit. */
    protected int hitAround(ServerLevel level, double radius, float damage, double push, double lift, boolean groundOnly) {
        int hits = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius, 2.0, radius))) {
            if (e == this || isBandmate(e) || (groundOnly && !e.onGround()) || e.distanceTo(this) > radius + this.getBbWidth() * 0.5) {
                continue;
            }
            if (e.hurtServer(level, this.damageSources().mobAttack(this), damage)) {
                double dx = e.getX() - this.getX();
                double dz = e.getZ() - this.getZ();
                double d = Math.max(0.01, Math.sqrt(dx * dx + dz * dz));
                e.push(dx / d * push, lift, dz / d * push);
                hits++;
            }
        }
        return hits;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(name == null ? Component.translatable(this.getType().getDescriptionId()) : this.getDisplayName());
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    /** Its colours for the final burst. */
    protected abstract int burstColorA();

    protected abstract int burstColorB();

    @Override
    public void makePoofParticles() {
        KillBurst.pop(this, this.burstColorA(), this.burstColorB(), KillBurst.NOTE, ParticleTypes.SCULK_SOUL);
        KillBurst.pop(this, this.burstColorB(), 0xFFFFFF, KillBurst.STAR, null);
    }

    /** Walks it at the target while it is between attacks. */
    private final class ChaseGoal extends Goal {
        private int repath;

        ChaseGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = MiniBoss.this.getTarget();
            return t != null && t.isAlive() && MiniBoss.this.canChase(MiniBoss.this.getState());
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity t = MiniBoss.this.getTarget();
            if (t == null) {
                return;
            }
            MiniBoss.this.getLookControl().setLookAt(t, 30.0F, 30.0F);
            if (--this.repath <= 0) {
                this.repath = 8 + MiniBoss.this.random.nextInt(6);
                MiniBoss.this.getNavigation().moveTo(t, MiniBoss.this.chaseSpeed());
            }
        }

        @Override
        public void stop() {
            MiniBoss.this.getNavigation().stop();
        }
    }
}
