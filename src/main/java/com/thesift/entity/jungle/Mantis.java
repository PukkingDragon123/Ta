package com.thesift.entity.jungle;

import com.thesift.block.PointedCrystalBlock;
import com.thesift.entity.KillBurst;
import com.thesift.entity.caravan.Caravan;
import com.thesift.registry.ModCaveJungle;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * P4 Cave Jungle: the Mantis - the Cave Jungle's great predator. A towering praying mantis of jungle-green chitin
 * with azure crystals growing out of its back, neck and arms (it eats crystal, and it shows). It stalks Caravans -
 * their gem-crusted shells are its favourite meal - and anything else that walks. From afar it opens its wings and
 * dives; close in it strikes with its scythes, and now and then it grabs its prey and slices at it, held fast
 * between the blades. When it is hurt and nothing is near, it cracks open a crystal cluster and eats it to heal.
 * Its scythe arm is a prized drop (the Reaper Scythe is made from it).
 */
public class Mantis extends Monster {
    private static final EntityDataAccessor<Boolean> DIVING = SynchedEntityData.defineId(Mantis.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> GRABBING = SynchedEntityData.defineId(Mantis.class, EntityDataSerializers.BOOLEAN);
    private static final byte EVENT_STRIKE = 100;
    private static final byte EVENT_EAT = 101;
    private static final byte EVENT_SLICE = 102;
    private static final int GRAB_TICKS = 60;

    public final AnimationState strikeAnimation = new AnimationState();
    public final AnimationState eatAnimation = new AnimationState();
    public final AnimationState sliceAnimation = new AnimationState();

    private int grabTicks;
    private int grabCooldown = 60;
    private int diveCooldown = 80;

    public Mantis(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 60.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 9.0).add(Attributes.ARMOR, 6.0).add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DIVING, false);
        builder.define(GRABBING, false);
    }

    @Override
    public int getNoActionTime() {
        return 0;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new DiveGoal());
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.25, false) {
            @Override
            public boolean canUse() {
                return !Mantis.this.isGrabbing() && !Mantis.this.isDiving() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !Mantis.this.isGrabbing() && !Mantis.this.isDiving() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(4, new EatCrystalGoal());
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Caravan.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Crocotodo.class, true, (target, level) -> this.random.nextInt(4) == 0));
    }

    public boolean isDiving() {
        return this.entityData.get(DIVING);
    }

    public boolean isGrabbing() {
        return this.entityData.get(GRABBING);
    }

    /** It hunts by sight in the dark as well as in the moss-light. */
    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return 0.0F;
    }

    // ------------------------------------------------------------------ strike, grab and slice

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        level.broadcastEntityEvent(this, EVENT_STRIKE);
        this.playSound(ModCaveJungle.MANTIS_STRIKE.get(), 1.0F, 0.9F + this.random.nextFloat() * 0.2F);
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity victim && victim.isAlive() && this.grabCooldown <= 0 && this.random.nextFloat() < 0.35F) {
            this.grab(level, victim);
        }
        return hit;
    }

    /** Clamps its prey between the scythes (anything up to a player's size). */
    private void grab(ServerLevel level, LivingEntity victim) {
        if (!this.getPassengers().isEmpty() || victim.isPassenger() || victim.getBbWidth() > 1.4F || victim.getBbHeight() > 2.2F) {
            return;
        }
        if (victim.startRiding(this, true, true)) {
            this.grabTicks = GRAB_TICKS;
            this.entityData.set(GRABBING, true);
            this.getNavigation().stop();
            this.playSound(ModCaveJungle.MANTIS_SLICE.get(), 1.0F, 0.7F);
        }
    }

    private void holdGrabbed(ServerLevel level) {
        Entity held = this.getFirstPassenger();
        if (!(held instanceof LivingEntity victim) || !victim.isAlive()) {
            this.release(level);
            return;
        }
        this.getNavigation().stop();
        if (--this.grabTicks % 14 == 0 && this.grabTicks > 0) {
            victim.hurtServer(level, this.damageSources().mobAttack(this), 4.0F);
            level.broadcastEntityEvent(this, EVENT_SLICE);
            this.playSound(ModCaveJungle.MANTIS_SLICE.get(), 1.0F, 1.0F + this.random.nextFloat() * 0.2F);
            Vec3 at = this.holdPos();
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, at.x, at.y + 0.6, at.z, 1, 0.0, 0.0, 0.0, 0.0);
            level.sendParticles(ParticleTypes.CRIT, at.x, at.y + 0.6, at.z, 6, 0.3, 0.3, 0.3, 0.2);
        }
        if (this.grabTicks <= 0) {
            this.release(level);
        }
    }

    /** Lets go, flinging what is left of its prey aside. */
    private void release(ServerLevel level) {
        this.grabTicks = 0;
        this.grabCooldown = 140;
        this.entityData.set(GRABBING, false);
        Entity held = this.getFirstPassenger();
        if (held == null) {
            return;
        }
        held.stopRiding();
        Vec3 dir = Vec3.directionFromRotation(0.0F, this.getYRot());
        Vec3 at = this.holdPos();
        held.teleportTo(at.x + dir.x * 0.6, this.getY() + 0.2, at.z + dir.z * 0.6);
        held.push(dir.x * 1.1, 0.35, dir.z * 1.1);
    }

    /** Where the prey is held: between the scythes, in front of its chest. */
    private Vec3 holdPos() {
        Vec3 dir = Vec3.directionFromRotation(0.0F, this.yBodyRot);
        return this.position().add(dir.scale(1.15)).add(0.0, 0.9, 0.0);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().isEmpty() && passenger instanceof LivingEntity;
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        Vec3 h = this.holdPos();
        moveFunction.accept(passenger, h.x, h.y - passenger.getBbHeight() * 0.35, h.z);
    }

    /** Its prey never steers it. */
    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return null;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.grabCooldown > 0) {
            this.grabCooldown--;
        }
        if (this.diveCooldown > 0) {
            this.diveCooldown--;
        }
        if (this.isGrabbing() || this.grabTicks > 0) {
            this.holdGrabbed(level);
        }
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
        return false;
    }

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EVENT_STRIKE -> this.strikeAnimation.start(this.tickCount);
            case EVENT_EAT -> this.eatAnimation.start(this.tickCount);
            case EVENT_SLICE -> this.sliceAnimation.start(this.tickCount);
            default -> super.handleEntityEvent(id);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() && this.random.nextInt(12) == 0) {
            // a crystal on its back glints
            this.level().addParticle(ParticleTypes.GLOW, this.getRandomX(0.6), this.getY() + 1.2 + this.random.nextDouble() * 1.2,
                    this.getRandomZ(0.6), 0.0, 0.01, 0.0);
        }
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ModCaveJungle.MANTIS_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModCaveJungle.MANTIS_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModCaveJungle.MANTIS_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(ModCaveJungle.MANTIS_STEP.get(), 0.4F, 1.0F);
    }

    @Override
    public void makePoofParticles() {
        KillBurst.pop(this, 0x316B2E, 0x52AED8, KillBurst.STAR, ParticleTypes.GLOW);
    }

    private static boolean crystal(BlockState st) {
        return st.getBlock() instanceof AmethystClusterBlock || st.getBlock() instanceof PointedCrystalBlock;
    }

    // ------------------------------------------------------------------ goals

    /** From five to fourteen blocks away it opens its wings and dives at its prey, scythes first. */
    private class DiveGoal extends Goal {
        private int time;

        DiveGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            Mantis m = Mantis.this;
            LivingEntity t = m.getTarget();
            if (t == null || !t.isAlive() || m.diveCooldown > 0 || m.isGrabbing() || !m.onGround()) {
                return false;
            }
            double d = m.distanceToSqr(t);
            return d > 25.0 && d < 196.0 && m.hasLineOfSight(t);
        }

        @Override
        public boolean canContinueToUse() {
            return this.time < 40 && Mantis.this.isDiving();
        }

        @Override
        public void start() {
            Mantis m = Mantis.this;
            LivingEntity t = m.getTarget();
            if (t == null) {
                return;
            }
            this.time = 0;
            Vec3 to = t.position().subtract(m.position());
            double flat = Math.sqrt(to.x * to.x + to.z * to.z);
            Vec3 dir = flat < 1.0E-3 ? Vec3.ZERO : new Vec3(to.x / flat, 0.0, to.z / flat);
            double speed = Mth.clamp(flat * 0.11, 0.6, 1.5);
            m.setDeltaMovement(dir.x * speed, 0.65 + flat * 0.015, dir.z * speed);
            m.setYRot((float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90.0F);
            m.yBodyRot = m.getYRot();
            m.entityData.set(DIVING, true);
            m.diveCooldown = 120;
            m.playSound(ModCaveJungle.MANTIS_DIVE.get(), 1.2F, 1.0F);
        }

        @Override
        public void tick() {
            Mantis m = Mantis.this;
            this.time++;
            LivingEntity t = m.getTarget();
            if (t != null) {
                m.getLookControl().setLookAt(t, 30.0F, 30.0F);
            }
            if (this.time > 4 && m.onGround()) {
                m.entityData.set(DIVING, false);
                if (t != null && m.distanceToSqr(t) < 9.0 && m.level() instanceof ServerLevel server) {
                    m.grabCooldown = 0;
                    m.doHurtTarget(server, t);
                }
            }
        }

        @Override
        public void stop() {
            Mantis.this.entityData.set(DIVING, false);
        }
    }

    /** Hurt and alone, it cracks a crystal cluster off the rock and eats it. */
    private class EatCrystalGoal extends Goal {
        private @Nullable BlockPos food;
        private int time;
        private int cooldown;

        EatCrystalGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Mantis m = Mantis.this;
            if (m.getTarget() != null || --this.cooldown > 0) {
                return false;
            }
            this.cooldown = 100;
            if (m.getHealth() >= m.getMaxHealth() && m.random.nextInt(4) != 0) {
                return false;
            }
            BlockPos o = m.blockPosition();
            for (int i = 0; i < 80; i++) {
                BlockPos p = o.offset(m.random.nextInt(21) - 10, m.random.nextInt(9) - 4, m.random.nextInt(21) - 10);
                if (crystal(m.level().getBlockState(p))) {
                    this.food = p;
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return this.food != null && this.time < 200 && Mantis.this.getTarget() == null && crystal(Mantis.this.level().getBlockState(this.food));
        }

        @Override
        public void start() {
            this.time = 0;
        }

        @Override
        public void stop() {
            this.food = null;
        }

        @Override
        public void tick() {
            Mantis m = Mantis.this;
            BlockPos p = this.food;
            if (p == null) {
                return;
            }
            this.time++;
            m.getLookControl().setLookAt(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5);
            if (m.distanceToSqr(Vec3.atCenterOf(p)) > 6.0) {
                if (m.getNavigation().isDone() || this.time % 20 == 0) {
                    m.getNavigation().moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0.9);
                }
                return;
            }
            m.getNavigation().stop();
            if (this.time % 20 != 0 || !(m.level() instanceof ServerLevel server)) {
                return;
            }
            BlockState st = server.getBlockState(p);
            server.broadcastEntityEvent(m, EVENT_EAT);
            m.playSound(ModCaveJungle.MANTIS_CRUNCH.get(), 1.0F, 0.9F + m.random.nextFloat() * 0.2F);
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, st), p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 14, 0.3, 0.3, 0.3, 0.1);
            m.heal(5.0F);
            if (m.random.nextInt(3) == 0) {
                if (Boolean.TRUE.equals(server.getGameRules().get(GameRules.MOB_GRIEFING))) {
                    server.destroyBlock(p, false);
                }
                this.food = null;
            }
        }
    }
}
