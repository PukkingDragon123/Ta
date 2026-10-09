package com.thesift.entity;

import com.thesift.music.Song;
import com.thesift.music.SongEvents;
import com.thesift.registry.ModEchoer;
import com.thesift.registry.ModParticles;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A Nib: a tiny glowing wisp-butterfly of the flower meadows. Nibs flutter about in loose flocks,
 * trailing sparkles, and settle on flowers to rest, slowly opening and closing their wings.
 *
 * <p>Play the Song of the Nibs nearby and every Nib within 12 blocks swirls up around you in a
 * spiral of light and turns into something precious ({@code thesift:gameplay/nib_transform}):
 * mostly Nib Dust, sometimes gold, emeralds, amethyst or an echo shard, rarely a diamond.
 */
public class Nib extends PathfinderMob {
    public static final int FLYING = 0;
    public static final int RESTING = 1;
    public static final int SWIRLING = 2;
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(Nib.class, EntityDataSerializers.INT);
    private static final int SWIRL_TIME = 70;

    private @Nullable Vec3 target;
    private Vec3 swirlCenter = Vec3.ZERO;
    private int stateTime;
    private int restTime;
    private final float phase;
    /** Client: how far it has settled on a flower, and into its swirl (smoothed, for the model). */
    public float restO, rest, swirlO, swirl;

    public Nib(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.phase = this.random.nextFloat() * Mth.TWO_PI;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 2.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FLYING_SPEED, 0.4);
    }

    /** The Song of the Nibs: every Nib within 12 blocks swirls up and turns to treasure. */
    public static void listen() {
        SongEvents.listenSongs((level, player, at, song) -> {
            if (song == Song.NIB) {
                for (Nib n : level.getEntitiesOfClass(Nib.class, new AABB(at, at).inflate(12.0))) {
                    n.swirl(at);
                }
            }
        });
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, FLYING);
    }

    public int getState() {
        return this.entityData.get(STATE);
    }

    private void setState(int s) {
        this.entityData.set(STATE, s);
        this.stateTime = 0;
    }

    public float phase() {
        return this.phase;
    }

    public void swirl(Vec3 center) {
        if (this.getState() != SWIRLING) {
            this.swirlCenter = center;
            this.setState(SWIRLING);
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        this.stateTime++;
        switch (this.getState()) {
            case RESTING -> this.rest(level);
            case SWIRLING -> this.swirling(level);
            default -> this.fly(level);
        }
    }

    private void fly(ServerLevel level) {
        Vec3 pos = this.position();
        if (this.target == null || pos.distanceToSqr(this.target) < 0.5 || this.stateTime % 80 == 0) {
            this.target = this.pickTarget(level);
        }
        Vec3 t = this.target;
        boolean landing = this.restTime > 0;
        if (landing && pos.distanceToSqr(t) < 0.04) {
            this.setDeltaMovement(Vec3.ZERO);
            this.snapTo(t.x, t.y, t.z, this.getYRot(), 0.0F);
            this.setState(RESTING);
            return;
        }
        Vec3 v = this.getDeltaMovement();
        Vec3 steer = t.subtract(pos).normalize().scale(landing ? 0.025 : 0.035);
        // a butterfly's bob, so the line is never straight
        double bob = Mth.sin((this.tickCount + this.phase * 10.0F) * 0.35F) * 0.02;
        v = v.add(steer).add(0.0, bob, 0.0);
        double max = landing ? 0.09 : 0.16;
        if (v.lengthSqr() > max * max) {
            v = v.normalize().scale(max);
        }
        this.setDeltaMovement(v);
        // M3: turn smoothly towards where it flies (it used to snap round and spin on the spot when nearly still)
        if (v.horizontalDistanceSqr() > 1.0E-4) {
            float want = (float) (Mth.atan2(v.z, v.x) * Mth.RAD_TO_DEG) - 90.0F;
            this.setYRot(Mth.approachDegrees(this.getYRot(), want, 15.0F));
        }
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();
    }

    private Vec3 pickTarget(ServerLevel level) {
        BlockPos me = this.blockPosition();
        // sometimes: settle on a flower nearby
        if (this.random.nextInt(4) == 0) {
            for (int i = 0; i < 10; i++) {
                BlockPos p = me.offset(this.random.nextInt(13) - 6, this.random.nextInt(7) - 4, this.random.nextInt(13) - 6);
                if (level.getBlockState(p).is(BlockTags.FLOWERS) && level.getBlockState(p.above()).isAir()) {
                    this.restTime = 100 + this.random.nextInt(300);
                    return new Vec3(p.getX() + 0.5, p.getY() + 0.62, p.getZ() + 0.5);
                }
            }
        }
        this.restTime = 0;
        // otherwise: drift with the flock, a few blocks above the ground
        Vec3 centre = this.position();
        List<Nib> flock = level.getEntitiesOfClass(Nib.class, this.getBoundingBox().inflate(6.0), n -> n != this && n.getState() == FLYING);
        if (!flock.isEmpty()) {
            Vec3 sum = Vec3.ZERO;
            for (Nib n : flock) {
                sum = sum.add(n.position());
            }
            centre = centre.add(sum.scale(1.0 / flock.size())).scale(0.5);
        }
        double x = centre.x + (this.random.nextDouble() - 0.5) * 7.0;
        double z = centre.z + (this.random.nextDouble() - 0.5) * 7.0;
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
        double y = Mth.clamp(centre.y + (this.random.nextDouble() - 0.5) * 2.0, ground + 1.0, ground + 4.5);
        return new Vec3(x, y, z);
    }

    private void rest(ServerLevel level) {
        this.setDeltaMovement(Vec3.ZERO);
        BlockPos below = BlockPos.containing(this.getX(), this.getY() - 0.3, this.getZ());
        boolean onFlower = level.getBlockState(below).is(BlockTags.FLOWERS);
        if (!onFlower || this.stateTime > this.restTime || level.getNearestPlayer(this, 2.0) != null || this.hurtTime > 0) {
            this.restTime = 0;
            this.target = null;
            this.setDeltaMovement(0.0, 0.08, 0.0);
            this.setState(FLYING);
        }
    }

    private void swirling(ServerLevel level) {
        float t = this.stateTime / (float) SWIRL_TIME;
        double r = Mth.lerp(t, 2.5, 0.6);
        double a = this.stateTime * 0.32 + this.phase;
        Vec3 goal = this.swirlCenter.add(Math.cos(a) * r, 0.4 + t * 2.2, Math.sin(a) * r);
        this.setDeltaMovement(goal.subtract(this.position()).scale(0.35));
        if (this.stateTime % 3 == 0) {
            level.sendParticles(ModParticles.STAR_SPARKLE.get(), this.getX(), this.getY(), this.getZ(), 1, 0.05, 0.05, 0.05, 0.0);
        }
        if (this.stateTime >= SWIRL_TIME) {
            Vec3 at = this.position();
            this.dropFromGiftLootTable(level, ModEchoer.NIB_TRANSFORM_LOOT, (l, stack) -> {
                ItemEntity e = new ItemEntity(level, at.x, at.y, at.z, stack);
                e.setDeltaMovement((this.random.nextDouble() - 0.5) * 0.1, 0.15, (this.random.nextDouble() - 0.5) * 0.1);
                e.setDefaultPickUpDelay();
                level.addFreshEntity(e);
            });
            level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 14, 0.15, 0.15, 0.15, 0.08);
            level.sendParticles(ModParticles.RESONANCE_RING.get(), at.x, at.y, at.z, 0, 0.8, 0.0, 0.0, 1.0);
            this.playSound(ModEchoer.NIB_TRANSFORM.get(), 1.0F, 0.9F + this.random.nextFloat() * 0.4F);
            this.discard();
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) {
            this.restO = this.rest;
            this.swirlO = this.swirl;
            this.rest = Mth.clamp(this.rest + (this.getState() == RESTING ? 0.1F : -0.15F), 0.0F, 1.0F);
            this.swirl = Mth.clamp(this.swirl + (this.getState() == SWIRLING ? 0.1F : -0.1F), 0.0F, 1.0F);
            // a trail of sparkles
            if (this.random.nextInt(this.getState() == RESTING ? 12 : 3) == 0) {
                this.level().addParticle(ModParticles.GLOW_DUST.get(), this.getX(), this.getY() + 0.1, this.getZ(), 0, -0.005, 0);
            }
            if (this.getState() == SWIRLING && this.random.nextBoolean()) {
                this.level().addParticle(ModParticles.STAR_SPARKLE.get(), this.getX(), this.getY() + 0.1, this.getZ(), 0, 0, 0);
            }
        }
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ModEchoer.NIB_AMBIENT.get();
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModEchoer.NIB_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModEchoer.NIB_HURT.get();
    }
}
