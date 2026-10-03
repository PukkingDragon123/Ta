package com.thesift.entity;

import com.thesift.registry.ModItems;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSwifter;
import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Swifter: a fierce, fluffy three-tailed cloud fox of the White Forest, faster than anything
 * else in the Sift. It runs in long bounds and, when it means it, dashes like a jet - a crouch, a
 * wiggle, then a straight streak with three vapour trails behind it.
 *
 * <p>It hunts Bulbs: it stalks one, pounces, snatches it in its jaws, rockets about ten blocks
 * straight up, flips over at the top and slams back down, the Bulb first, in a ring of wind and
 * dust. It leaves players alone unless one hurts it (then the whole family joins in).
 *
 * <p>Swifters raise their cubs around dens of fluff and twigs ({@link #getDen()}). When a player
 * breaks a den the cubs nearby sit down and cry and the den's grown Swifters attack that player
 * with dash after dash until every crying cub has been calmed with a Glowing Slime Ball.
 */
public class Swifter extends Animal {
    // what the body is doing (synced: the model poses it, the carried Bulb hangs accordingly)
    public static final byte IDLE = 0;
    public static final byte CROUCH = 1;
    public static final byte DASH = 2;
    public static final byte CLIMB = 3;
    public static final byte FLIP = 4;
    public static final byte DIVE = 5;
    public static final byte SLEEP = 6;
    public static final byte CRY = 7;

    private static final byte EVENT_DASH = 83;
    private static final byte EVENT_GRAB = 84;
    private static final byte EVENT_SLAM = 85;
    private static final byte EVENT_CALM = 86;
    private static final byte EVENT_CRY = 87;
    private static final byte EVENT_SNARL = 88;
    private static final EntityDataAccessor<Byte> MODE = SynchedEntityData.defineId(Swifter.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> ANGRY = SynchedEntityData.defineId(Swifter.class, EntityDataSerializers.BOOLEAN);

    /** Blocks per tick in a jet dash (an arrow flies about 3). */
    private static final double DASH_SPEED = 1.15;
    /** How far a den's family is from its den. */
    private static final double FAMILY_RANGE = 48.0;
    private static final int RAGE_TICKS = 12000;
    private static final int CRY_TICKS = 12000;

    public final AnimationState grabAnimation = new AnimationState();
    public final AnimationState slamAnimation = new AnimationState();
    public final AnimationState calmAnimation = new AnimationState();
    public final AnimationState snarlAnimation = new AnimationState();
    // client-side pose blends, eased every tick so that no pose ever snaps (see SwifterModel)
    public float jetO;
    public float jet;
    public float sleepO;
    public float sleep;
    public float crouchO;
    public float crouch;
    public float cryO;
    public float cry;
    public float pitchO;
    public float pitch;

    private @Nullable BlockPos den;
    private @Nullable UUID angerTarget;
    private int angerTicks;
    private int huntCooldown = 400;
    private int napCooldown = 600;
    private int cryTicks;
    /** S1 never freeze: how long the Bulb in its jaws has been carried (a hunt's flight lasts under three seconds). */
    private int carryTicks;

    public Swifter(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.36)
                .add(Attributes.ATTACK_DAMAGE, 4.0).add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MODE, IDLE);
        builder.define(ANGRY, false);
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
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new CryGoal());
        this.goalSelector.addGoal(2, new PanicGoal(this, 1.7) {
            @Override
            public boolean canUse() {
                return Swifter.this.isBaby() && !Swifter.this.isCrying() && super.canUse();
            }
        });
        this.goalSelector.addGoal(3, new DashAttackGoal());
        this.goalSelector.addGoal(4, new HuntGoal());
        this.goalSelector.addGoal(5, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(6, new TemptGoal(this, 1.1, stack -> stack.is(ModItems.GLOWING_SLIME_BALL.get()), false));
        this.goalSelector.addGoal(7, new FollowParentGoal(this, 1.15));
        this.goalSelector.addGoal(8, new NapGoal());
        this.goalSelector.addGoal(9, new HomeGoal());
        this.goalSelector.addGoal(10, new WaterAvoidingRandomStrollGoal(this, 0.9));
        this.goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(12, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
    }

    // ------------------------------------------------------------------ state

    public byte getMode() {
        return this.entityData.get(MODE);
    }

    private void setMode(byte mode) {
        if (this.getMode() != mode) {
            this.entityData.set(MODE, mode);
        }
    }

    public boolean isCrying() {
        return this.getMode() == CRY;
    }

    public boolean isNapping() {
        return this.getMode() == SLEEP;
    }

    public boolean isAngry() {
        return this.entityData.get(ANGRY);
    }

    public boolean isCarrying() {
        return this.getFirstPassenger() instanceof Bulb;
    }

    /** The den this Swifter belongs to (it also marks the family: everyone with the same den), or null. */
    public @Nullable BlockPos getDen() {
        return this.den;
    }

    public void setDen(@Nullable BlockPos den) {
        this.den = den;
    }

    private boolean sameDen(Swifter other) {
        return this.den != null && this.den.equals(other.den);
    }

    private void wakeUp() {
        if (this.isNapping()) {
            this.setMode(IDLE);
        }
    }

    // ------------------------------------------------------------------ the den, the cubs, the anger

    /** Called when a player breaks {@code den}: its cubs cry, its grown Swifters go for the breaker. */
    public static void denBroken(ServerLevel level, BlockPos den, Player breaker) {
        for (Swifter swifter : level.getEntitiesOfClass(Swifter.class, new AABB(den).inflate(FAMILY_RANGE), e -> e.isAlive() && den.equals(e.den))) {
            if (swifter.isBaby()) {
                swifter.startCrying(level);
            } else {
                swifter.enrage(level, breaker);
            }
        }
    }

    private void startCrying(ServerLevel level) {
        this.cryTicks = CRY_TICKS;
        this.setMode(CRY);
        this.getNavigation().stop();
        level.broadcastEntityEvent(this, EVENT_CRY);
        this.playSound(ModSwifter.CRY.get(), 1.2F, this.getVoicePitch());
    }

    private void enrage(ServerLevel level, Player breaker) {
        this.wakeUp();
        this.angerTarget = breaker.getUUID();
        this.angerTicks = RAGE_TICKS;
        this.entityData.set(ANGRY, true);
        this.setTarget(breaker);
        level.broadcastEntityEvent(this, EVENT_SNARL);
        this.playSound(ModSwifter.SNARL.get(), 1.5F, 0.9F);
    }

    /** A crying cub fed a Glowing Slime Ball: hearts, a purr - and its parents may forgive. */
    private void calmDown(ServerLevel level) {
        this.cryTicks = 0;
        this.setMode(IDLE);
        level.broadcastEntityEvent(this, EVENT_CALM);
        this.playSound(ModSwifter.CALM.get(), 1.0F, this.getVoicePitch());
        level.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + this.getBbHeight() + 0.3, this.getZ(), 4, 0.3, 0.2, 0.3, 0.0);
        this.parentsReconsider(level);
    }

    private void parentsReconsider(ServerLevel level) {
        if (this.den == null) {
            return;
        }
        for (Swifter parent : level.getEntitiesOfClass(Swifter.class, this.getBoundingBox().inflate(FAMILY_RANGE),
                e -> e.isAlive() && !e.isBaby() && this.sameDen(e))) {
            parent.checkStandDown(level);
        }
    }

    /** The parents stand down once none of their cubs is crying any more. */
    private void checkStandDown(ServerLevel level) {
        if (this.angerTarget == null) {
            return;
        }
        boolean stillCrying = !level.getEntitiesOfClass(Swifter.class, this.getBoundingBox().inflate(FAMILY_RANGE),
                e -> e.isAlive() && e.isCrying() && this.sameDen(e)).isEmpty();
        if (!stillCrying) {
            this.standDown(level);
        }
    }

    private void standDown(ServerLevel level) {
        LivingEntity current = this.getTarget();
        if (current != null && current.getUUID().equals(this.angerTarget)) {
            this.setTarget(null);
        }
        this.angerTarget = null;
        this.angerTicks = 0;
        this.entityData.set(ANGRY, false);
        this.setAggressive(false);
        level.broadcastEntityEvent(this, EVENT_CALM);
        this.playSound(ModSwifter.CALM.get(), 0.8F, 0.85F);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (this.isCrying() && stack.is(ModItems.GLOWING_SLIME_BALL.get())) {
            if (this.level() instanceof ServerLevel server) {
                stack.consume(1, player);
                this.calmDown(server);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ModItems.GLOWING_SLIME_BALL.get());
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        Swifter cub = ModSwifter.SWIFTER.get().create(level, EntitySpawnReason.BREEDING);
        if (cub != null) {
            cub.setDen(this.den);
        }
        return cub;
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.easePoses();
            this.clientEffects();
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.resetFallDistance(); // a cloud fox always lands softly, even from the top of a slam
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (this.huntCooldown > 0) {
            this.huntCooldown--;
        }
        if (this.napCooldown > 0) {
            this.napCooldown--;
        }
        byte mode = this.getMode();
        if (this.isNoGravity() && mode != DASH && mode != CLIMB && mode != FLIP && mode != DIVE) {
            this.setNoGravity(false); // never left hanging in the air
        }
        if (mode == SLEEP && this.hurtTime > 0) {
            this.setMode(IDLE);
        }
        // S1 never freeze: a Bulb still held long after the hunt (one cut short by a reload) is let go, not carried forever
        if (this.isCarrying() && ++this.carryTicks > 120) {
            Entity held = this.getFirstPassenger();
            if (held != null) {
                held.stopRiding();
            }
            this.carryTicks = 0;
        } else if (!this.isCarrying()) {
            this.carryTicks = 0;
        }
        if (mode == CRY && --this.cryTicks <= 0) {
            this.setMode(IDLE); // cried itself out
            this.parentsReconsider(level);
        }
        if (this.angerTarget != null && this.tickCount % 10 == 0) {
            Entity foe = level.getEntity(this.angerTarget);
            if (foe instanceof Player p && p.isAlive() && !p.isSpectator() && this.distanceToSqr(p) < 64.0 * 64.0 && this.getTarget() != p) {
                this.setTarget(p);
            }
            this.angerTicks -= 10;
            if (this.angerTicks <= 0 || foe instanceof Player dead && !dead.isAlive()) {
                this.standDown(level);
            }
        }
    }

    private static float approach(float v, float target, float step) {
        return v < target ? Math.min(target, v + step) : Math.max(target, v - step);
    }

    /** Client: eases the pose blends towards the current mode (and the body's pitch towards where it flies). */
    private void easePoses() {
        byte mode = this.getMode();
        this.jetO = this.jet;
        this.sleepO = this.sleep;
        this.crouchO = this.crouch;
        this.cryO = this.cry;
        this.pitchO = this.pitch;
        boolean flying = mode == DASH || mode == CLIMB || mode == FLIP || mode == DIVE;
        this.jet = approach(this.jet, flying ? 1.0F : 0.0F, flying ? 0.45F : 0.12F);
        this.sleep = approach(this.sleep, mode == SLEEP ? 1.0F : 0.0F, 0.06F);
        this.crouch = approach(this.crouch, mode == CROUCH ? 1.0F : 0.0F, mode == CROUCH ? 0.2F : 0.3F);
        this.cry = approach(this.cry, mode == CRY ? 1.0F : 0.0F, 0.1F);
        double horizontal = Math.sqrt((this.getX() - this.xo) * (this.getX() - this.xo) + (this.getZ() - this.zo) * (this.getZ() - this.zo));
        float want = switch (mode) {
            case CLIMB -> -1.25F;
            case FLIP, DIVE -> 1.4F;
            case DASH -> (float) Mth.clamp(-Mth.atan2(this.getY() - this.yo, Math.max(0.05, horizontal)), -0.7, 0.7);
            default -> 0.0F;
        };
        this.pitch += (want - this.pitch) * (mode == FLIP ? 0.3F : 0.4F);
    }

    /** Client: three vapour trails behind a flying Swifter, tears from a crying cub, fluff shed on the run. */
    private void clientEffects() {
        byte mode = this.getMode();
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(yaw);
        double fz = Mth.cos(yaw);
        if (this.jet > 0.3F) {
            double cp = Mth.cos(this.pitch);
            double sp = Mth.sin(this.pitch);
            double cy = this.getY() + this.getBbHeight() * 0.55;
            for (int i = -1; i <= 1; i++) {
                double px = this.getX() - fx * cp + fz * i * 0.35;
                double py = cy + sp + (i == 0 ? 0.15 : 0.0);
                double pz = this.getZ() - fz * cp - fx * i * 0.35;
                this.level().addParticle(ParticleTypes.CLOUD, px, py, pz, 0.0, 0.0, 0.0);
            }
            if (this.random.nextInt(2) == 0) {
                this.level().addParticle(ModSwifter.WHITE_FLUFF.get(), this.getRandomX(0.6), this.getRandomY(), this.getRandomZ(0.6), 0.0, 0.0, 0.0);
            }
        }
        if (mode == CRY && this.random.nextInt(3) == 0) {
            double side = this.random.nextBoolean() ? 0.07 : -0.07;
            this.level().addParticle(ParticleTypes.FALLING_WATER, this.getX() + fx * 0.28 + fz * side, this.getY() + this.getBbHeight() * 0.75,
                    this.getZ() + fz * 0.28 - fx * side, 0.0, 0.0, 0.0);
        }
        if (mode == IDLE && this.getKnownMovement().horizontalDistanceSqr() > 0.04 && this.random.nextInt(8) == 0) {
            this.level().addParticle(ModSwifter.WHITE_FLUFF.get(), this.getRandomX(0.5), this.getY() + this.getBbHeight() * 0.6, this.getRandomZ(0.5),
                    0.0, 0.02, 0.0);
        }
    }

    /** The Codex page acts it out: tails swaying, the pounce crouch, the jet pose, a nap curled in its tails. */
    public void codexPose(int t) {
        byte mode = switch ((t / 70) % 4) {
            case 1 -> CROUCH;
            case 2 -> DASH;
            case 3 -> SLEEP;
            default -> IDLE;
        };
        this.entityData.set(MODE, mode);
        this.easePoses();
    }

    // ------------------------------------------------------------------ moves

    private void faceAlong(Vec3 dir) {
        float yaw = (float) (Mth.atan2(dir.z, dir.x) * (180.0 / Math.PI)) - 90.0F;
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }

    private Vec3 forward() {
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
    }

    /** The jet dash: gravity off, the whoosh, a burst of cloud around it. */
    private void startDash() {
        this.setNoGravity(true);
        this.setMode(DASH);
        this.level().broadcastEntityEvent(this, EVENT_DASH);
        this.playSound(ModSwifter.DASH.get(), 1.3F, 0.9F + this.random.nextFloat() * 0.2F);
    }

    private void endFlight() {
        this.setNoGravity(false);
        byte mode = this.getMode();
        if (mode == CROUCH || mode == DASH || mode == CLIMB || mode == FLIP || mode == DIVE) {
            this.setMode(IDLE);
        }
    }

    private void dashHit(LivingEntity target, Vec3 dir, boolean rage) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * (rage ? 1.5F : 1.0F);
        if (target.hurtServer(level, this.damageSources().mobAttack(this), damage)) {
            target.push(dir.x * 1.1, 0.35, dir.z * 1.1);
        }
        level.broadcastEntityEvent(this, EVENT_GRAB);
        this.playSound(ModSwifter.GRAB.get(), 1.0F, 1.2F);
        level.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 8, 0.3, 0.3, 0.3, 0.08);
    }

    /** The slam: down it comes in a ring of wind and dust, the Bulb first. */
    private void slam(ServerLevel level, @Nullable Bulb held) {
        this.endFlight();
        Vec3 fwd = this.forward();
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();
        level.broadcastEntityEvent(this, EVENT_SLAM);
        this.playSound(ModSwifter.SLAM.get(), 1.4F, 0.9F + this.random.nextFloat() * 0.2F);
        this.playSound(ModSwifter.GUST.get(), 1.0F, 1.0F);
        level.sendParticles(ModParticles.RESONANCE_RING.get(), x, y + 0.05, z, 0, 3.5, 0.0, 0.0, 1.0);
        for (int i = 0; i < 20; i++) {
            double a = i * Mth.TWO_PI / 20.0;
            level.sendParticles(ParticleTypes.CLOUD, x + Math.cos(a) * 0.6, y + 0.15, z + Math.sin(a) * 0.6, 0, Math.cos(a), 0.04, Math.sin(a), 0.4);
        }
        BlockState ground = level.getBlockState(this.blockPosition().below());
        if (!ground.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), x, y + 0.1, z, 24, 0.8, 0.1, 0.8, 0.15);
        }
        for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(3.0, 1.0, 3.0),
                e -> e != this && e != held && !(e instanceof Swifter))) {
            Vec3 away = near.position().subtract(this.position()).normalize();
            near.push(away.x * 0.5, 0.25, away.z * 0.5);
        }
        if (held != null) {
            held.stopRiding();
            held.teleportTo(x + fwd.x * 0.6, y, z + fwd.z * 0.6);
            level.sendParticles(ModParticles.GLOW_SPLAT.get(), held.getX(), held.getY() + 0.3, held.getZ(), 8, 0.3, 0.2, 0.3, 0.1);
            held.hurtServer(level, this.damageSources().mobAttack(this), 6.0F + this.random.nextFloat() * 4.0F);
            if (held.isAlive()) {
                held.push(fwd.x * 0.4, 0.5, fwd.z * 0.4); // dazed, it bounces off
            } else {
                this.heal(6.0F);
                this.playSound(ModSwifter.EAT.get(), 1.0F, 1.0F);
            }
        }
    }

    // ------------------------------------------------------------------ the carried Bulb

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().isEmpty() && passenger instanceof Bulb;
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        Vec3 jaw = this.jawPosition();
        moveFunction.accept(passenger, jaw.x, jaw.y - passenger.getBbHeight() * 0.5, jaw.z);
    }

    /** Where the Bulb hangs: in the jaws in front, above the head climbing, below the body diving. */
    private Vec3 jawPosition() {
        Vec3 fwd = this.forward();
        double h = this.getBbHeight();
        return switch (this.getMode()) {
            case CLIMB -> this.position().add(fwd.scale(0.15)).add(0.0, h + 0.4, 0.0);
            case FLIP -> this.position().add(fwd.scale(0.55)).add(0.0, h * 0.9, 0.0);
            case DIVE -> this.position().add(fwd.scale(0.2)).add(0.0, -0.05, 0.0);
            default -> this.position().add(fwd.scale(0.75)).add(0.0, h * 0.55, 0.0);
        };
    }

    // ------------------------------------------------------------------ client events

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EVENT_DASH -> this.dashBurst();
            case EVENT_GRAB -> this.grabAnimation.start(this.tickCount);
            case EVENT_SLAM -> this.slamAnimation.start(this.tickCount);
            case EVENT_CALM -> this.calmAnimation.start(this.tickCount);
            case EVENT_SNARL -> this.snarlAnimation.start(this.tickCount);
            case EVENT_CRY -> {
                for (int i = 0; i < 6; i++) {
                    this.level().addParticle(ParticleTypes.SPLASH, this.getRandomX(0.4), this.getY() + this.getBbHeight() * 0.8, this.getRandomZ(0.4),
                            0.0, 0.1, 0.0);
                }
            }
            default -> super.handleEntityEvent(id);
        }
    }

    /** A ring of cloud bursting out round the body as it breaks into a dash, and a puff of gust behind. */
    private void dashBurst() {
        Vec3 fwd = this.forward();
        double cy = this.getY() + this.getBbHeight() * 0.5;
        for (int i = 0; i < 14; i++) {
            double a = i * Mth.TWO_PI / 14.0;
            double side = Math.cos(a) * 0.6;
            double up = Math.sin(a) * 0.6;
            this.level().addParticle(ParticleTypes.CLOUD, this.getX() + fwd.z * side, cy + up, this.getZ() - fwd.x * side,
                    fwd.z * side * 0.2 - fwd.x * 0.1, up * 0.2, -fwd.x * side * 0.2 - fwd.z * 0.1);
        }
        this.level().addParticle(ParticleTypes.SMALL_GUST, this.getX() - fwd.x * 0.8, cy, this.getZ() - fwd.z * 0.8, 0.0, 0.0, 0.0);
    }

    // ------------------------------------------------------------------ sounds, saving

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return switch (this.getMode()) {
            case CRY -> ModSwifter.CRY.get();
            case SLEEP -> ModSwifter.SLEEP.get();
            default -> this.isAngry() ? ModSwifter.SNARL.get() : ModSwifter.AMBIENT.get();
        };
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSwifter.HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSwifter.DEATH.get();
    }

    @Override
    public void makePoofParticles() {
        KillBurst.pop(this, 0xF4F8FF, 0x9FC2F2, KillBurst.STAR, ParticleTypes.CLOUD);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.storeNullable("Den", BlockPos.CODEC, this.den);
        output.storeNullable("AngerTarget", UUIDUtil.CODEC, this.angerTarget);
        output.putInt("AngerTicks", this.angerTicks);
        output.putInt("HuntCooldown", this.huntCooldown);
        output.putInt("CryTicks", this.isCrying() ? this.cryTicks : 0);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.den = input.read("Den", BlockPos.CODEC).orElse(null);
        this.angerTarget = input.read("AngerTarget", UUIDUtil.CODEC).orElse(null);
        this.angerTicks = input.getIntOr("AngerTicks", 0);
        this.huntCooldown = input.getIntOr("HuntCooldown", 400);
        this.cryTicks = input.getIntOr("CryTicks", 0);
        this.entityData.set(MODE, this.cryTicks > 0 ? CRY : IDLE);
        this.entityData.set(ANGRY, this.angerTarget != null);
        this.setNoGravity(false);
    }

    // ================================================================== goals

    /** A crying cub sits where it is, head bowed, and will not be moved. */
    private final class CryGoal extends Goal {
        CryGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return Swifter.this.isCrying();
        }

        @Override
        public void start() {
            Swifter.this.getNavigation().stop();
        }

        @Override
        public void tick() {
            Swifter.this.getNavigation().stop();
        }
    }

    /**
     * Fighting: circle in, crouch (the anticipation: low, tails up, a wiggle), then a straight jet
     * dash through the target, a skid and a turn. A parent defending its den barely pauses.
     */
    private final class DashAttackGoal extends Goal {
        private int phase;
        private int timer;
        private int repath;
        private boolean hit;
        private Vec3 dir = Vec3.ZERO;

        DashAttackGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = Swifter.this.getTarget();
            return !Swifter.this.isBaby() && target != null && target.isAlive() && !Swifter.this.isCarrying() && !Swifter.this.isPassenger();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            this.phase = 0;
            this.timer = 0;
            this.repath = 0;
            Swifter.this.wakeUp();
            Swifter.this.setAggressive(true);
        }

        @Override
        public void stop() {
            Swifter.this.setAggressive(false);
            Swifter.this.endFlight();
            Swifter.this.getNavigation().stop();
        }

        @Override
        public void tick() {
            Swifter sw = Swifter.this;
            LivingEntity target = sw.getTarget();
            if (target == null) {
                return;
            }
            boolean rage = sw.angerTarget != null;
            double d2 = sw.distanceToSqr(target);
            if (this.phase == 0) {
                sw.getLookControl().setLookAt(target, 30.0F, 30.0F);
                if (--this.repath <= 0) {
                    this.repath = 8;
                    sw.getNavigation().moveTo(target, rage ? 1.45 : 1.3);
                }
                if (--this.timer <= 0 && d2 < 11.0 * 11.0 && sw.onGround() && sw.hasLineOfSight(target)) {
                    this.phase = 1;
                    this.timer = rage ? 7 : 11;
                    sw.getNavigation().stop();
                    sw.setMode(CROUCH);
                    sw.playSound(ModSwifter.SNARL.get(), 1.0F, sw.getVoicePitch());
                }
            } else if (this.phase == 1) {
                Vec3 to = target.getBoundingBox().getCenter().subtract(sw.position().add(0.0, sw.getBbHeight() * 0.5, 0.0));
                sw.faceAlong(to);
                if (--this.timer <= 0) {
                    Vec3 flat = to.lengthSqr() < 1.0E-4 ? sw.forward() : to.normalize();
                    this.dir = new Vec3(flat.x, Mth.clamp(flat.y, -0.35, 0.45), flat.z).normalize();
                    this.timer = (int) Math.min(16.0, Math.sqrt(d2) / DASH_SPEED + 6.0);
                    this.hit = false;
                    this.phase = 2;
                    sw.startDash();
                }
            } else if (this.phase == 2) {
                sw.setDeltaMovement(this.dir.scale(DASH_SPEED));
                sw.faceAlong(this.dir);
                if (!this.hit && sw.getBoundingBox().inflate(0.35).intersects(target.getBoundingBox())) {
                    this.hit = true;
                    sw.dashHit(target, this.dir, rage);
                    this.timer = Math.min(this.timer, 3); // a little overshoot: the follow-through
                }
                if (--this.timer <= 0 || sw.horizontalCollision) {
                    this.phase = 3;
                    this.timer = rage ? 8 + sw.getRandom().nextInt(6) : 16 + sw.getRandom().nextInt(12);
                    sw.endFlight();
                    sw.setDeltaMovement(sw.getDeltaMovement().scale(0.35));
                }
            } else {
                // skid to a stop, shake off, turn round
                sw.getLookControl().setLookAt(target, 30.0F, 30.0F);
                if (--this.timer <= 0) {
                    this.phase = 0;
                    this.timer = rage ? 0 : 10;
                }
            }
        }
    }

    /**
     * The hunt: stalk a Bulb, crouch, pounce, snatch it, rocket ~10 blocks straight up, hang a moment
     * at the top as it flips over, then dive and slam it into the ground.
     */
    private final class HuntGoal extends Goal {
        private static final int STALK = 0;
        private static final int WIND_UP = 1;
        private static final int POUNCE = 2;
        private static final int RISE = 3;
        private static final int APEX = 4;
        private static final int FALL = 5;
        private static final int RECOVER = 6;
        private int phase;
        private int timer;
        private int patience;
        private double climbFrom;
        private boolean caught;
        private boolean done;
        private Vec3 dir = Vec3.ZERO;
        private @Nullable Bulb prey;

        HuntGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            Swifter sw = Swifter.this;
            if (sw.isBaby() || sw.getTarget() != null || sw.huntCooldown > 0 || sw.isAngry() || sw.isNapping() || sw.isInWater()
                    || sw.isPassenger() || sw.getRandom().nextInt(20) != 0) {
                return false;
            }
            Bulb best = null;
            double bestD = 0.0;
            for (Bulb b : sw.level().getEntitiesOfClass(Bulb.class, sw.getBoundingBox().inflate(20.0, 8.0, 20.0), e -> e.isAlive() && !e.isPassenger())) {
                double d = sw.distanceToSqr(b);
                if (best == null || d < bestD) {
                    best = b;
                    bestD = d;
                }
            }
            this.prey = best;
            return best != null;
        }

        @Override
        public boolean canContinueToUse() {
            Swifter sw = Swifter.this;
            if (this.done || sw.getTarget() != null) {
                return false;
            }
            if (this.phase >= RISE) {
                return true; // airborne: the move always plays out
            }
            return this.prey != null && this.prey.isAlive() && !this.prey.isPassenger() && !sw.isInWater() && this.patience > 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            this.phase = STALK;
            this.timer = 0;
            this.patience = 400;
            this.caught = false;
            this.done = false;
            Swifter.this.wakeUp();
        }

        @Override
        public void stop() {
            Swifter sw = Swifter.this;
            Entity held = sw.getFirstPassenger();
            if (held != null) {
                held.stopRiding();
            }
            sw.endFlight();
            sw.getNavigation().stop();
            sw.huntCooldown = this.caught ? 1600 + sw.getRandom().nextInt(1600) : 300 + sw.getRandom().nextInt(300);
            this.prey = null;
        }

        @Override
        public void tick() {
            Swifter sw = Swifter.this;
            Bulb bulb = this.prey;
            this.patience--;
            switch (this.phase) {
                case STALK -> {
                    if (bulb == null) {
                        return;
                    }
                    double d2 = sw.distanceToSqr(bulb);
                    sw.getLookControl().setLookAt(bulb, 30.0F, 30.0F);
                    if (--this.timer <= 0) {
                        this.timer = 10;
                        sw.getNavigation().moveTo(bulb, d2 > 12.0 * 12.0 ? 1.3 : 1.0);
                    }
                    if (d2 < 7.0 * 7.0 && sw.onGround() && sw.hasLineOfSight(bulb)) {
                        this.phase = WIND_UP;
                        this.timer = 14;
                        sw.getNavigation().stop();
                        sw.setMode(CROUCH);
                    }
                }
                case WIND_UP -> {
                    if (bulb == null) {
                        return;
                    }
                    Vec3 to = bulb.getBoundingBox().getCenter().subtract(sw.position().add(0.0, sw.getBbHeight() * 0.5, 0.0));
                    sw.faceAlong(to);
                    if (--this.timer <= 0) {
                        Vec3 flat = to.lengthSqr() < 1.0E-4 ? sw.forward() : to.normalize();
                        this.dir = new Vec3(flat.x, Mth.clamp(flat.y, -0.3, 0.4), flat.z).normalize();
                        this.timer = (int) Math.min(14.0, Math.sqrt(sw.distanceToSqr(bulb)) / DASH_SPEED + 6.0);
                        this.phase = POUNCE;
                        sw.startDash();
                    }
                }
                case POUNCE -> {
                    sw.setDeltaMovement(this.dir.scale(DASH_SPEED));
                    sw.faceAlong(this.dir);
                    if (bulb != null && bulb.isAlive() && !bulb.isPassenger() && sw.getBoundingBox().inflate(0.4).intersects(bulb.getBoundingBox())
                            && bulb.startRiding(sw, true, true)) {
                        this.caught = true;
                        this.phase = RISE;
                        this.timer = 18;
                        this.climbFrom = sw.getY();
                        sw.setMode(CLIMB);
                        sw.level().broadcastEntityEvent(sw, EVENT_GRAB);
                        sw.playSound(ModSwifter.GRAB.get(), 1.0F, 1.0F);
                        sw.playSound(ModSwifter.ROCKET.get(), 1.2F, 1.0F);
                    } else if (--this.timer <= 0 || sw.horizontalCollision) {
                        // missed: skid, shake it off, try again later
                        sw.endFlight();
                        sw.setDeltaMovement(sw.getDeltaMovement().scale(0.3));
                        this.phase = RECOVER;
                        this.timer = 20;
                    }
                }
                case RISE -> {
                    sw.setDeltaMovement(this.dir.x * 0.04, 0.72, this.dir.z * 0.04);
                    if (sw.getY() >= this.climbFrom + 10.0 || sw.verticalCollision || --this.timer <= 0 || !sw.isCarrying()) {
                        this.phase = APEX;
                        this.timer = 7;
                        sw.setMode(FLIP);
                    }
                }
                case APEX -> {
                    // the moment at the top: it slows, hangs, flips over (the anticipation of the slam)
                    sw.setDeltaMovement(sw.getDeltaMovement().scale(0.5));
                    if (--this.timer <= 0) {
                        this.phase = FALL;
                        this.timer = 30;
                        sw.setMode(DIVE);
                        sw.playSound(ModSwifter.DASH.get(), 1.2F, 0.8F);
                    }
                }
                case FALL -> {
                    sw.setDeltaMovement(this.dir.x * 0.05, -1.7, this.dir.z * 0.05);
                    if (sw.onGround() || --this.timer <= 0 || sw.isInWater()) {
                        if (sw.level() instanceof ServerLevel level) {
                            sw.slam(level, sw.getFirstPassenger() instanceof Bulb held ? held : null);
                        } else {
                            sw.endFlight();
                        }
                        this.phase = RECOVER;
                        this.timer = 24;
                    }
                }
                default -> {
                    // the follow-through: it shakes itself off (SwifterModel plays the slam's recovery)
                    if (--this.timer <= 0) {
                        this.done = true;
                    }
                }
            }
        }
    }

    /** Naps, curled up in its three tails, now and then when all is quiet; anyone walking up wakes it (sneak to get close). */
    private final class NapGoal extends Goal {
        private int ticks;

        NapGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            Swifter sw = Swifter.this;
            if (sw.napCooldown > 0 || sw.getTarget() != null || sw.isAngry() || sw.isCrying() || !sw.onGround() || sw.isInWater() || sw.isVehicle()) {
                return false;
            }
            return sw.getRandom().nextInt(sw.isBaby() ? 300 : 600) == 0 && sw.level().getNearestPlayer(sw, 6.0) == null;
        }

        @Override
        public boolean canContinueToUse() {
            Swifter sw = Swifter.this;
            if (this.ticks <= 0 || !sw.isNapping() || sw.getTarget() != null || sw.hurtTime > 0) {
                return false;
            }
            Player near = sw.level().getNearestPlayer(sw, 3.0);
            return near == null || near.isSteppingCarefully();
        }

        @Override
        public void start() {
            Swifter sw = Swifter.this;
            this.ticks = 400 + sw.getRandom().nextInt(800);
            sw.getNavigation().stop();
            sw.setMode(SLEEP);
        }

        @Override
        public void tick() {
            this.ticks--;
            Swifter.this.getNavigation().stop();
        }

        @Override
        public void stop() {
            Swifter sw = Swifter.this;
            if (sw.isNapping()) {
                sw.setMode(IDLE);
            }
            sw.napCooldown = 1200 + sw.getRandom().nextInt(2400);
        }
    }

    /** Never strays far from its den: cubs stay within 8 blocks, the grown-ups within 14. */
    private final class HomeGoal extends Goal {
        HomeGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        private double homeSqr() {
            BlockPos home = Swifter.this.den;
            return home == null ? 0.0 : home.distToCenterSqr(Swifter.this.getX(), Swifter.this.getY(), Swifter.this.getZ());
        }

        @Override
        public boolean canUse() {
            Swifter sw = Swifter.this;
            return sw.den != null && sw.getTarget() == null && !sw.isCrying() && sw.getRandom().nextInt(40) == 0
                    && this.homeSqr() > (sw.isBaby() ? 64.0 : 196.0);
        }

        @Override
        public boolean canContinueToUse() {
            return Swifter.this.den != null && !Swifter.this.getNavigation().isDone() && this.homeSqr() > 16.0;
        }

        @Override
        public void start() {
            BlockPos home = Swifter.this.den;
            if (home != null) {
                Swifter.this.getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 1.0);
            }
        }
    }
}
