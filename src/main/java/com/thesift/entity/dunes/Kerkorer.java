package com.thesift.entity.dunes;

import com.thesift.entity.KillBurst;
import com.thesift.entity.Resting;
import com.thesift.entity.Sifter;
import com.thesift.registry.ModDunes;
import com.thesift.registry.ModItems;
import java.util.EnumSet;
import java.util.List;
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
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
 * The Kerkorer: a giant chameleon of the Rocky Dunes. It lies still until it melts into the sand (the client
 * fades its sand-coloured skin in over the real one), with a valuable stuck to the pad of its tongue as bait.
 * Whatever comes close enough is snapped up - the jaws when it is near, the tongue lashing out and yanking it
 * in when it is a little further - and then chased for a while before the Kerkorer settles and hides again.
 * When it is hungry it stalks the Sifters of the dunes. Every so often it calls its own name across the sand
 * ("ker" - "ko" - "rer"). Jaberoras ride on its back, sleep curled against it and sing prey towards it; it never
 * harms them. Sneaking players and players hidden in a Kerkorer Cloak do not wake it.
 */
public class Kerkorer extends PathfinderMob implements DunesNative, Resting {
    public static final int HIDE = 0;
    public static final int PROWL = 1;
    public static final int SNAP = 2;
    public static final int CHASE = 3;
    /** Ticks of a snap: the head draws back, then strikes on {@link #STRIKE}. */
    public static final int SNAP_TIME = 18;
    public static final int STRIKE = 7;
    /** The jaws reach this far from the mouth; the tongue reaches {@link #TONGUE_RANGE}. */
    public static final double JAW_RANGE = 2.2;
    public static final double TONGUE_RANGE = 5.5;
    /** Sneakers may come this close before it notices them. */
    public static final double SNEAK_RANGE = 1.4;
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(Kerkorer.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> LURE = SynchedEntityData.defineId(Kerkorer.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> TONGUE = SynchedEntityData.defineId(Kerkorer.class, EntityDataSerializers.BOOLEAN);
    private static final byte EVENT_CALL = 101;
    private static final byte EVENT_GLINT = 102;

    public final AnimationState snapAnimation = new AnimationState();
    public final AnimationState callAnimation = new AnimationState();
    public final AnimationState glintAnimation = new AnimationState();
    /** Client: 0 its own colours - 1 the colours of the sand (eased). */
    public float camo;
    public float camoO;
    /** Client: where each turret eye is looking (yaw, pitch; radians), eased towards a new point every so often. */
    public final float[] eyes = new float[4];
    public final float[] eyesO = new float[4];
    private final float[] eyeGoal = new float[4];
    private int stillTicks;
    private int actionTicks;
    private int callIn = 200;
    private int callStep = -1;
    private int huntCooldown = 600;
    private int chaseTicks;
    private int lureSwap = 2400;
    private @Nullable LivingEntity prey;

    public Kerkorer(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 12;
        this.entityData.set(LURE, randomLure(this.random));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, HIDE);
        builder.define(LURE, ItemStack.EMPTY);
        builder.define(TONGUE, false);
    }

    @Override
    public int getNoActionTime() {
        return 0;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new HuntGoal());
        this.goalSelector.addGoal(6, new RandomStrollGoal(this, 0.7, 400) {
            @Override
            public boolean canUse() {
                return Kerkorer.this.getState() == HIDE && Kerkorer.this.stillTicks > 600 && !Kerkorer.this.isVehicle() && super.canUse();
            }
        });
    }

    // ------------------------------------------------------------------ state

    public int getState() {
        return this.entityData.get(STATE);
    }

    private void setState(int s) {
        if (s != this.getState()) {
            this.entityData.set(STATE, s);
            this.actionTicks = 0;
        }
    }

    public ItemStack getLure() {
        return this.entityData.get(LURE);
    }

    /** True while the tongue is out (lashing at something further than the jaws reach). */
    public boolean tongueOut() {
        return this.entityData.get(TONGUE);
    }

    /** It lies still in the sand: the never-frozen check lets it off. */
    @Override
    public boolean isResting() {
        return this.getState() == HIDE;
    }

    public boolean isHiding() {
        return this.getState() == HIDE && this.stillTicks > 40;
    }

    public static ItemStack randomLure(net.minecraft.util.RandomSource random) {
        return switch (random.nextInt(6)) {
            case 0 -> new ItemStack(Items.DIAMOND);
            case 1 -> new ItemStack(Items.GOLDEN_APPLE);
            case 2 -> new ItemStack(ModItems.SIFTITE_INGOT.get());
            case 3 -> new ItemStack(ModItems.CHROME_PEARL.get());
            case 4 -> new ItemStack(Items.ENDER_PEARL);
            default -> new ItemStack(Items.EMERALD);
        };
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
            @Nullable SpawnGroupData data) {
        this.entityData.set(LURE, randomLure(this.random));
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    /** Where its mouth is (the bait sits here; the jaws close here). */
    public Vec3 mouth() {
        return this.position().add(Vec3.directionFromRotation(0.0F, this.yBodyRot).scale(1.45)).add(0.0, 0.55, 0.0);
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return ModDunes.dunesGround(level.getBlockState(pos.below())) ? 10.0F : 0.0F;
    }

    // ------------------------------------------------------------------ passengers: Jaberoras ride on its back

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof Jaberora && this.getPassengers().size() < 2;
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        int i = Math.max(0, this.getPassengers().indexOf(passenger));
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        double back = i == 0 ? -0.05 : -0.6;
        double x = this.getX() - Mth.sin(yaw) * back;
        double z = this.getZ() + Mth.cos(yaw) * back;
        double sink = this.getState() == HIDE ? 0.1 : 0.0;
        moveFunction.accept(passenger, x, this.getY() + 1.2 - sink, z);
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return null; // a Jaberora on its back is a passenger, not a rider
    }

    // ------------------------------------------------------------------ the server tick

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.huntCooldown > 0) {
            this.huntCooldown--;
        }
        boolean moving = this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4 || !this.getNavigation().isDone();
        this.stillTicks = moving ? 0 : this.stillTicks + 1;
        this.actionTicks++;
        if (this.getLure().isEmpty() || --this.lureSwap <= 0) {
            this.entityData.set(LURE, randomLure(this.random));
            this.lureSwap = 2400 + this.random.nextInt(2400);
        }
        switch (this.getState()) {
            case HIDE -> this.tickHide(level);
            case SNAP -> this.tickSnap(level);
            case CHASE -> this.tickChase(level);
            default -> {
            }
        }
        this.tickCall(level);
    }

    private void tickHide(ServerLevel level) {
        if (this.tickCount % 5 != 0) {
            return;
        }
        LivingEntity near = this.nearestPrey(level, TONGUE_RANGE);
        if (near != null) {
            this.startSnap(near);
            return;
        }
        // the bait glints now and then
        if (this.isHiding() && this.random.nextInt(24) == 0) {
            level.broadcastEntityEvent(this, EVENT_GLINT);
            this.playSound(ModDunes.KERKORER_LURE.get(), 0.6F, 1.0F + this.random.nextFloat() * 0.3F);
        }
    }

    /** The closest creature it would snap at: anything alive that is not a dunes friend, not hidden and not a careful sneaker. */
    private @Nullable LivingEntity nearestPrey(ServerLevel level, double range) {
        Vec3 m = this.mouth();
        List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class, new AABB(m, m).inflate(range), this::isPrey);
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (LivingEntity e : list) {
            double d = e.distanceToSqr(m);
            if (e instanceof Player p && p.isShiftKeyDown() && d > SNEAK_RANGE * SNEAK_RANGE) {
                continue;
            }
            if (d < bestD && d < range * range && this.hasLineOfSight(e)) {
                best = e;
                bestD = d;
            }
        }
        return best;
    }

    private boolean isPrey(LivingEntity e) {
        if (e == this || !e.isAlive() || e instanceof Kerkorer || e instanceof Jaberora || e instanceof Reservoir || this.hasPassenger(e)) {
            return false;
        }
        if (e instanceof Player p && (p.isCreative() || p.isSpectator() || this.level().getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL)) {
            return false;
        }
        return !ModDunes.unseen(e) && !(e instanceof net.minecraft.world.entity.monster.Enemy && !(e instanceof Grub));
    }

    private void startSnap(LivingEntity target) {
        this.prey = target;
        this.setTarget(target);
        this.getNavigation().stop();
        this.setState(SNAP);
        this.entityData.set(TONGUE, target.distanceToSqr(this.mouth()) > JAW_RANGE * JAW_RANGE);
        this.ejectPassengers();
        this.playSound(ModDunes.KERKORER_HISS.get(), 0.8F, 0.9F + this.random.nextFloat() * 0.2F);
    }

    private void tickSnap(ServerLevel level) {
        LivingEntity t = this.prey;
        if (t != null && t.isAlive()) {
            this.getLookControl().setLookAt(t, 60.0F, 40.0F);
            this.face(t, 25.0F);
        }
        if (this.actionTicks == STRIKE) {
            this.playSound(this.tongueOut() ? ModDunes.KERKORER_TONGUE.get() : ModDunes.KERKORER_SNAP.get(), 1.2F, 0.9F + this.random.nextFloat() * 0.2F);
            if (t != null && t.isAlive()) {
                Vec3 m = this.mouth();
                double d = Math.sqrt(t.distanceToSqr(m));
                if (d <= JAW_RANGE + 0.6) {
                    this.doHurtTarget(level, t);
                    this.playSound(ModDunes.KERKORER_SNAP.get(), 1.0F, 0.7F);
                } else if (d <= TONGUE_RANGE + 0.8) {
                    // the tongue: it stings a little and yanks its catch to the jaws
                    t.hurtServer(level, this.damageSources().mobAttack(this), 3.0F);
                    Vec3 pull = m.subtract(t.position()).normalize().scale(Math.min(1.4, d * 0.32));
                    t.push(pull.x, 0.25, pull.z);
                }
                // the lunge
                Vec3 dir = t.position().subtract(this.position()).normalize();
                this.push(dir.x * 0.35, 0.1, dir.z * 0.35);
            }
            BlockState ground = this.getBlockStateOn();
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), this.getX(), this.getY() + 0.2, this.getZ(), 26, 0.7, 0.1, 0.7, 0.15);
        }
        if (this.actionTicks == STRIKE + 6) {
            this.entityData.set(TONGUE, false);
        }
        if (this.actionTicks >= SNAP_TIME) {
            this.chaseTicks = 0;
            this.setState(t != null && t.isAlive() ? CHASE : HIDE);
        }
    }

    private void tickChase(ServerLevel level) {
        LivingEntity t = this.getTarget();
        this.chaseTicks++;
        if (t == null || !t.isAlive() || this.chaseTicks > 120 || this.distanceToSqr(t) > 20.0 * 20.0 || ModDunes.unseen(t)) {
            this.setTarget(null);
            this.prey = null;
            this.getNavigation().stop();
            this.setState(HIDE);
            if (t != null && !t.isAlive()) {
                this.huntCooldown = 2400 + this.random.nextInt(1200); // fed
            }
            return;
        }
        this.getLookControl().setLookAt(t, 30.0F, 30.0F);
        if (this.actionTicks % 10 == 0) {
            this.getNavigation().moveTo(t, 1.25);
        }
        // in reach again: another snap
        if (this.actionTicks > 20 && t.distanceToSqr(this.mouth()) < JAW_RANGE * JAW_RANGE * 1.4) {
            this.startSnap(t);
        }
    }

    private void face(Entity e, float maxTurn) {
        double dx = e.getX() - this.getX();
        double dz = e.getZ() - this.getZ();
        float want = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        this.setYRot(Mth.approachDegrees(this.getYRot(), want, maxTurn));
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();
    }

    /** "Ker" - "ko" - "rer": its own name, called across the dunes. */
    private void tickCall(ServerLevel level) {
        if (this.callStep < 0) {
            if (--this.callIn <= 0 && this.getState() != SNAP) {
                this.callStep = 0;
                level.broadcastEntityEvent(this, EVENT_CALL);
            }
            return;
        }
        float pitch = 0.9F + (this.getId() % 5) * 0.04F;
        if (this.callStep == 0) {
            this.playSound(ModDunes.KERKORER_KER.get(), 1.6F, pitch);
        } else if (this.callStep == 6) {
            this.playSound(ModDunes.KERKORER_KO.get(), 1.6F, pitch);
        } else if (this.callStep == 12) {
            this.playSound(ModDunes.KERKORER_RER.get(), 1.8F, pitch);
        }
        if (++this.callStep > 26) {
            this.callStep = -1;
            this.callIn = 600 + this.random.nextInt(900);
        }
    }

    /** A Sifter it is stalking (when hungry), for its Jaberoras to sing at. */
    public @Nullable LivingEntity prey() {
        return this.getState() == HIDE ? null : this.getTarget();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && this.isAlive() && source.getEntity() instanceof LivingEntity attacker && attacker != this && !(attacker instanceof Jaberora)) {
            this.ejectPassengers();
            if (this.getState() == HIDE || this.getState() == PROWL) {
                this.playSound(ModDunes.KERKORER_HISS.get(), 1.2F, 0.8F);
            }
            if (this.getState() != SNAP) {
                this.setTarget(attacker);
                this.prey = attacker;
                this.chaseTicks = 0;
                this.setState(CHASE);
            }
        }
        return hurt;
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target instanceof Jaberora || target instanceof Kerkorer) {
            target = null;
        }
        super.setTarget(target);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        // its bait is real treasure it found - sometimes it is still there
        if (killedByPlayer && !this.getLure().isEmpty() && this.random.nextFloat() < 0.3F) {
            this.spawnAtLocation(level, this.getLure().copy());
        }
    }

    // ------------------------------------------------------------------ hunting Sifters

    /** Hungry, it leaves its hide and stalks the nearest Sifter, slowly, then snaps when it is close enough. */
    private class HuntGoal extends Goal {
        private @Nullable Sifter quarry;
        private int ticks;

        HuntGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Kerkorer k = Kerkorer.this;
            if (k.huntCooldown > 0 || k.getState() != HIDE || k.stillTicks < 200 || k.random.nextInt(40) != 0) {
                return false;
            }
            List<Sifter> s = k.level().getEntitiesOfClass(Sifter.class, k.getBoundingBox().inflate(20.0), Sifter::isAlive);
            this.quarry = s.isEmpty() ? null : s.get(k.random.nextInt(s.size()));
            return this.quarry != null;
        }

        @Override
        public boolean canContinueToUse() {
            Kerkorer k = Kerkorer.this;
            return this.quarry != null && this.quarry.isAlive() && k.getState() == PROWL && this.ticks < 600 && k.distanceToSqr(this.quarry) < 28 * 28;
        }

        @Override
        public void start() {
            this.ticks = 0;
            Kerkorer.this.ejectPassengers();
            Kerkorer.this.setState(PROWL);
        }

        @Override
        public void stop() {
            Kerkorer k = Kerkorer.this;
            if (k.getState() == PROWL) {
                k.setState(HIDE);
                k.huntCooldown = 600;
            }
            k.getNavigation().stop();
        }

        @Override
        public void tick() {
            Kerkorer k = Kerkorer.this;
            Sifter q = this.quarry;
            if (q == null) {
                return;
            }
            this.ticks++;
            k.getLookControl().setLookAt(q, 20.0F, 20.0F);
            // a chameleon's stalk: a few rocking steps, a freeze, a few more
            boolean freeze = (this.ticks / 20) % 3 == 2;
            if (freeze) {
                k.getNavigation().stop();
            } else if (this.ticks % 10 == 0) {
                k.getNavigation().moveTo(q, 0.55);
            }
            if (q.distanceToSqr(k.mouth()) < (TONGUE_RANGE - 0.5) * (TONGUE_RANGE - 0.5) && k.hasLineOfSight(q)) {
                k.startSnap(q);
            }
        }
    }

    // ------------------------------------------------------------------ client

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EVENT_CALL -> this.callAnimation.start(this.tickCount);
            case EVENT_GLINT -> {
                this.glintAnimation.start(this.tickCount);
                Vec3 m = this.mouth();
                for (int i = 0; i < 3; i++) {
                    this.level().addParticle(ParticleTypes.WAX_OFF, m.x + (this.random.nextDouble() - 0.5) * 0.3, m.y + 0.2, m.z
                            + (this.random.nextDouble() - 0.5) * 0.3, 0.0, 0.02, 0.0);
                }
            }
            default -> super.handleEntityEvent(id);
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (STATE.equals(accessor) && this.level().isClientSide() && this.getState() == SNAP) {
            this.snapAnimation.start(this.tickCount);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.clientTick();
        }
    }

    private void clientTick() {
        this.camoO = this.camo;
        boolean still = this.getState() == HIDE && this.getDeltaMovement().horizontalDistanceSqr() < 1.0E-4 && this.walkAnimation.speed() < 0.05F;
        // it takes a couple of seconds to melt into the sand, and flashes its own colours the moment it moves
        this.camo = still ? Math.min(1.0F, this.camo + 0.02F) : Math.max(0.0F, this.camo - 0.12F);
        System.arraycopy(this.eyes, 0, this.eyesO, 0, 4);
        // each turret eye rolls on its own: a new point to look at every so often, reached with a quick swivel
        for (int i = 0; i < 2; i++) {
            if (this.random.nextInt(this.getState() == HIDE ? 30 : 12) == 0) {
                this.eyeGoal[i * 2] = (this.random.nextFloat() - 0.5F) * 1.6F;
                this.eyeGoal[i * 2 + 1] = (this.random.nextFloat() - 0.5F) * 0.9F;
            }
        }
        if (this.getState() == SNAP || this.getState() == CHASE) {
            // both eyes lock forward on the prey
            this.eyeGoal[0] = -0.7F;
            this.eyeGoal[2] = 0.7F;
            this.eyeGoal[1] = 0.0F;
            this.eyeGoal[3] = 0.0F;
        }
        for (int i = 0; i < 4; i++) {
            this.eyes[i] += (this.eyeGoal[i] - this.eyes[i]) * 0.35F;
        }
        if (this.camo > 0.9F && this.random.nextInt(60) == 0) {
            // a little sand slides off its back
            this.level().addParticle(new BlockParticleOption(ParticleTypes.FALLING_DUST, this.getBlockStateOn()), this.getRandomX(0.6),
                    this.getY() + 1.1, this.getRandomZ(0.6), 0.0, 0.0, 0.0);
        }
    }

    /** The Codex page: it hides, glints its bait, calls, then snaps. */
    public void codexPose(int t) {
        if (t % 120 == 10) {
            this.callAnimation.start(this.tickCount);
        }
        if (t % 120 == 60) {
            this.glintAnimation.start(this.tickCount);
        }
        if (t % 120 == 90) {
            this.snapAnimation.start(this.tickCount);
        }
        if (this.getLure().isEmpty()) {
            this.entityData.set(LURE, new ItemStack(Items.DIAMOND));
        }
    }

    // ------------------------------------------------------------------ sounds, save

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return null; // it calls its name instead
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModDunes.KERKORER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModDunes.KERKORER_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(ModDunes.KERKORER_STEP.get(), 0.5F, 0.9F);
    }

    @Override
    public void makePoofParticles() {
        KillBurst.pop(this, 0xA3626C, 0xDAD2E9, KillBurst.STAR, ParticleTypes.WHITE_ASH);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (!this.getLure().isEmpty()) {
            output.store("Lure", ItemStack.CODEC, this.getLure());
        }
        output.putInt("HuntCooldown", this.huntCooldown);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(LURE, input.read("Lure", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        this.huntCooldown = input.getIntOr("HuntCooldown", 600);
    }
}
