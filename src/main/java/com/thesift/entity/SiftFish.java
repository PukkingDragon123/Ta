package com.thesift.entity;

import com.thesift.registry.ModChrome;
import com.thesift.registry.ModItems;
import com.thesift.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Bucketable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Base for the Sift's music fish. They swim through Chrome as easily as through water: steering
 * is done here directly (no pathfinding), each fish picking points inside the liquid to swim to.
 * Out of the liquid they flop about and slowly dry out. Like vanilla's fish they fit in a bucket -
 * a Chrome Bucket (A3 Chrome: see {@link #canBePickedUpWithBucket} and ModChrome.fishBucket).
 */
public abstract class SiftFish extends PathfinderMob implements Bucketable {
    private static final EntityDataAccessor<Boolean> FROM_BUCKET = SynchedEntityData.defineId(SiftFish.class, EntityDataSerializers.BOOLEAN);
    private @Nullable Vec3 swimTarget;
    private int retarget;
    private int dryTicks;
    private int flopTimer;
    /** Client: smoothed swim effort (0 drifting, 1 darting), for the tail beat. */
    public float effort;
    public float effortO;

    protected SiftFish(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    /** True while the fish is in a liquid it can swim in (Chrome or water, never lava). */
    public boolean inLiquid() {
        return this.isInFluidType() && !this.isInLava();
    }

    public static boolean isSwimmable(Level level, BlockPos pos) {
        FluidState fs = level.getFluidState(pos);
        return !fs.isEmpty() && !fs.is(FluidTags.LAVA);
    }

    /** Cruising speed in blocks per tick. */
    protected abstract double cruiseSpeed();

    protected abstract SoundEvent getFlopSound();

    /** Where this fish wants to be; null to wander. Called on the server every tick while swimming. */
    protected @Nullable Vec3 wantedPosition() {
        return null;
    }

    /** Speed multiplier for this tick (darting, chasing...). */
    protected double speedFactor() {
        return 1.0;
    }

    public void setSwimTarget(@Nullable Vec3 target) {
        this.swimTarget = target;
        this.retarget = 40 + this.random.nextInt(60);
    }

    public @Nullable Vec3 getSwimTarget() {
        return this.swimTarget;
    }

    /** Picks a random point in the liquid around the fish. */
    protected @Nullable Vec3 randomSwimPoint(int range) {
        for (int i = 0; i < 10; i++) {
            BlockPos p = this.blockPosition().offset(this.random.nextInt(range * 2 + 1) - range, this.random.nextInt(7) - 3,
                    this.random.nextInt(range * 2 + 1) - range);
            if (isSwimmable(this.level(), p) && isSwimmable(this.level(), p.above())) {
                return Vec3.atCenterOf(p);
            }
        }
        return null;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public void travel(Vec3 input) {
        if (this.inLiquid()) {
            // the steering in aiStep sets the velocity; glide along with gentle drag
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
        } else {
            super.travel(input);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level() instanceof ServerLevel server) {
            if (this.inLiquid()) {
                this.dryTicks = 0;
                this.steer();
            } else {
                this.flopAbout(server);
            }
        } else {
            this.effortO = this.effort;
            double v = this.getDeltaMovement().length();
            float want = (float) Mth.clamp(v / Math.max(0.01, this.cruiseSpeed() * 2.0), 0.0, 1.0);
            if (!this.inLiquid()) {
                want = 1.0F;
            }
            this.effort += (want - this.effort) * 0.2F;
            if (this.inLiquid() && this.random.nextInt(20) == 0) {
                this.level().addParticle(ModParticles.CHROME_BUBBLE.get(), this.getRandomX(0.5), this.getRandomY(), this.getRandomZ(0.5), 0, 0.02, 0);
            }
            this.clientEffects();
        }
    }

    /** Client: each fish's own whimsical sparkle (bubbles, notes, glow). */
    protected void clientEffects() {
    }

    private void steer() {
        Vec3 want = this.wantedPosition();
        if (want == null) {
            if (this.swimTarget == null || --this.retarget <= 0 || this.position().distanceToSqr(this.swimTarget) < 1.0
                    || this.horizontalCollision) {
                this.setSwimTarget(this.randomSwimPoint(8));
            }
            want = this.swimTarget;
        }
        Vec3 v = this.getDeltaMovement();
        if (want != null) {
            Vec3 to = want.subtract(this.position());
            double dist = to.length();
            if (dist > 0.05) {
                double speed = this.cruiseSpeed() * this.speedFactor() * Math.min(1.0, dist / 1.5 + 0.3);
                Vec3 desired = to.scale(speed / dist);
                v = v.add(desired.subtract(v).scale(0.12));
            }
        }
        // stay under the surface
        if (!isSwimmable(this.level(), BlockPos.containing(this.getX(), this.getY() + this.getBbHeight() + 0.1, this.getZ())) && v.y > -0.01) {
            v = new Vec3(v.x, v.y - 0.01, v.z);
        }
        this.setDeltaMovement(v);
        if (v.horizontalDistanceSqr() > 1.0E-5) {
            float yaw = (float) Math.toDegrees(Math.atan2(v.z, v.x)) - 90.0F;
            float cur = this.getYRot();
            this.setYRot(cur + Mth.clamp(Mth.wrapDegrees(yaw - cur), -12.0F, 12.0F));
            this.yBodyRot = this.getYRot();
            this.yHeadRot = this.getYRot();
            float pitch = (float) -Math.toDegrees(Math.atan2(v.y, Math.sqrt(v.horizontalDistanceSqr())));
            this.setXRot(Mth.clamp(pitch, -45.0F, 45.0F));
        }
    }

    private void flopAbout(ServerLevel level) {
        this.dryTicks++;
        if (this.onGround() && --this.flopTimer <= 0) {
            this.flopTimer = 12 + this.random.nextInt(20);
            this.setDeltaMovement((this.random.nextDouble() - 0.5) * 0.25, 0.35, (this.random.nextDouble() - 0.5) * 0.25);
            this.setYRot(this.random.nextFloat() * 360.0F);
            this.playSound(this.getFlopSound(), 1.0F, 0.9F + this.random.nextFloat() * 0.3F);
            level.sendParticles(ModParticles.CHROME_DROPLET.get(), this.getX(), this.getY() + 0.1, this.getZ(), 4, 0.2, 0.05, 0.2, 0.05);
        }
        if (this.dryTicks > 300 && this.dryTicks % 20 == 0) {
            this.hurtServer(level, this.damageSources().generic(), 1.0F);
        }
    }

    // ---- A3 Chrome: Chrome fish buckets, exactly like vanilla's fish buckets (AbstractFish)

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FROM_BUCKET, false);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("FromBucket", this.fromBucket());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setFromBucket(input.getBooleanOr("FromBucket", false));
    }

    @Override
    public boolean fromBucket() {
        return this.entityData.get(FROM_BUCKET);
    }

    @Override
    public void setFromBucket(boolean fromBucket) {
        this.entityData.set(FROM_BUCKET, fromBucket);
    }

    @Override
    public void saveToBucketTag(ItemStack bucket) {
        Bucketable.saveDefaultDataToBucketTag(this, bucket);
    }

    @Override
    public void loadFromBucketTag(CompoundTag tag) {
        Bucketable.loadDefaultDataFromBucketTag(this, tag);
    }

    @Override
    public ItemStack getBucketItemStack() {
        return ModChrome.fishBucket(this.getType());
    }

    @Override
    public SoundEvent getPickupSound() {
        return SoundEvents.BUCKET_FILL_FISH;
    }

    /** These fish live in Chrome: a Chrome Bucket scoops them up (if the fish has a bucket of its own). */
    @Override
    public boolean canBePickedUpWithBucket(ItemStack stack) {
        return stack.is(ModItems.CHROME_BUCKET.get()) && !this.getBucketItemStack().isEmpty();
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        return Bucketable.bucketMobPickup(player, hand, this).orElse(super.mobInteract(player, hand));
    }

    @Override
    public boolean requiresCustomPersistence() {
        return super.requiresCustomPersistence() || this.fromBucket();
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return !this.fromBucket() && !this.hasCustomName() && !this.isPersistenceRequired();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }
}
