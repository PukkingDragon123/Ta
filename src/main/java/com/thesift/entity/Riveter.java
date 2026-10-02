package com.thesift.entity;

import com.thesift.music.MusicListener;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import com.thesift.registry.ModTags;
import java.util.List;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ShriekParticleOption;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.SpawnUtil;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.warden.WardenSpawnTracker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Riveter: a pale sentinel that hangs from the ceilings of the Deep Sift. It does not fight. When
 * it notices an intruder it screams - and every Warden within earshot comes running. If no Warden
 * is near, its screams wake one from the sculk. Sneak past, or silence it with music.
 *
 * <p>By day it roosts asleep, wrapped in its wings, and only wakes if you come right up to it; by
 * night it is wide awake, chitters with its neighbours and flits out now and then to snap glow
 * dust out of the air before flapping back up to a ceiling. It bobs along to music.
 */
public class Riveter extends Monster implements MusicListener {
    private static final EntityDataAccessor<Boolean> HANGING = SynchedEntityData.defineId(Riveter.class, EntityDataSerializers.BOOLEAN);
    private static final byte EVENT_SCREAM = 110;
    private static final byte EVENT_CHITTER = 111;
    private static final byte EVENT_SNAP = 112;
    private static final byte EVENT_BOB = 113;
    private static final EntityDataAccessor<Boolean> ROOSTING = SynchedEntityData.defineId(Riveter.class, EntityDataSerializers.BOOLEAN);
    public static final int SCREAM_TICKS = 34;

    public final AnimationState screamAnimation = new AnimationState();
    public final AnimationState chitterAnimation = new AnimationState();
    public final AnimationState snapAnimation = new AnimationState();
    private int forageTicks;
    private @Nullable Vec3 forageTarget;
    private int socialCooldown = 300;
    private int bobCooldown;
    public final Spring sway = new Spring(0.03F, 0.02F);
    private int screamCooldown = 60;
    private int lulledTicks;

    public Riveter(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 12.0).add(Attributes.MOVEMENT_SPEED, 0.0).add(Attributes.FOLLOW_RANGE, 16.0);
    }

    public static boolean checkRiveterSpawnRules(EntityType<Riveter> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos,
            net.minecraft.util.RandomSource random) {
        if (!level.getBlockState(pos).isAir()) return false;
        for (int i = 1; i <= 6; i++) {
            BlockState above = level.getBlockState(pos.above(i));
            if (above.isFaceSturdy(level, pos.above(i), Direction.DOWN)) {
                return level.getRawBrightness(pos, 0) <= 6 || above.is(ModTags.Blocks.RIVETER_ROOST);
            }
            if (!above.isAir()) return false;
        }
        return false;
    }

    @Override
    protected void registerGoals() {
        // Riveters hang still; all behaviour lives in customServerAiStep.
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HANGING, true);
        builder.define(ROOSTING, false);
    }

    /** Asleep on its roost (by day). */
    public boolean isRoosting() {
        return this.entityData.get(ROOSTING);
    }

    public boolean isHanging() {
        return this.entityData.get(HANGING);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
            @Nullable SpawnGroupData data) {
        this.snapToCeiling(level);
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    /** Move straight up until our top touches a ceiling. */
    private boolean snapToCeiling(LevelAccessor level) {
        BlockPos p = this.blockPosition();
        for (int i = 0; i < 8; i++) {
            BlockPos above = p.above(i + 1);
            if (level.getBlockState(above).isFaceSturdy(level, above, Direction.DOWN)) {
                double top = above.getY();
                this.setPos(this.getX(), top - this.getBbHeight() - 0.01, this.getZ());
                return true;
            }
        }
        return false;
    }

    private boolean hasCeiling() {
        BlockPos above = BlockPos.containing(this.getX(), this.getY() + this.getBbHeight() + 0.1, this.getZ());
        return this.level().getBlockState(above).isFaceSturdy(this.level(), above, Direction.DOWN);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.socialCooldown > 0) {
            this.socialCooldown--;
        }
        if (this.bobCooldown > 0) {
            this.bobCooldown--;
        }
        if (this.forageTicks > 0) {
            this.forage(level);
            return;
        }
        boolean ceiling = this.hasCeiling();
        this.entityData.set(HANGING, ceiling);
        this.entityData.set(ROOSTING, ceiling && level.isBrightOutside() && this.lulledTicks <= 0);
        if (!ceiling) {
            // Flutter upwards to find a new roost.
            Vec3 v = this.getDeltaMovement();
            this.setDeltaMovement(v.x * 0.8 + (this.random.nextDouble() - 0.5) * 0.05, 0.08, v.z * 0.8 + (this.random.nextDouble() - 0.5) * 0.05);
            if (this.tickCount % 20 == 0) this.snapToCeiling(level);
            return;
        }
        this.setDeltaMovement(Vec3.ZERO);
        if (this.lulledTicks > 0) {
            this.lulledTicks--;
            return;
        }
        if (this.screamCooldown > 0) {
            this.screamCooldown--;
            return;
        }
        // a roosting Riveter is a light sleeper, but a sleeper: only someone right beneath it wakes it
        boolean roosting = this.isRoosting();
        Player intruder = level.getNearestPlayer(this.getX(), this.getY(), this.getZ(), roosting ? 4.0 : 14.0, p -> {
            if (!(p instanceof Player pl) || pl.isCreative() || pl.isSpectator()) return false;
            double d = p.distanceTo(this);
            return (!pl.isShiftKeyDown() || d < (roosting ? 2.0 : 4.0)) && this.hasLineOfSight(pl);
        });
        if (intruder instanceof ServerPlayer player) {
            this.entityData.set(ROOSTING, false);
            this.scream(level, player);
            return;
        }
        if (roosting) {
            return;
        }
        // awake at night: chitter with a neighbour, or flit out to hunt glow dust
        if (this.socialCooldown <= 0 && this.random.nextInt(200) == 0) {
            for (Riveter other : level.getEntitiesOfClass(Riveter.class, this.getBoundingBox().inflate(4.0), o -> o != this && o.isHanging() && !o.isRoosting())) {
                level.broadcastEntityEvent(this, EVENT_CHITTER);
                level.broadcastEntityEvent(other, EVENT_CHITTER);
                this.playSound(ModSounds.RIVETER_AMBIENT.get(), 0.6F, 1.6F + this.random.nextFloat() * 0.2F);
                this.socialCooldown = other.socialCooldown = 400 + this.random.nextInt(400);
                break;
            }
        }
        if (this.random.nextInt(900) == 0) {
            this.forageTicks = 160 + this.random.nextInt(120);
            this.forageTarget = null;
        }
    }

    /** A short hunting flight: flits about below its roost snapping glow dust out of the air. */
    private void forage(ServerLevel level) {
        this.forageTicks--;
        this.entityData.set(HANGING, false);
        this.entityData.set(ROOSTING, false);
        Vec3 target = this.forageTarget;
        if (target == null || this.position().distanceToSqr(target) < 1.0 || this.tickCount % 60 == 0) {
            target = this.position().add((this.random.nextDouble() - 0.5) * 10.0, (this.random.nextDouble() - 0.6) * 3.0, (this.random.nextDouble() - 0.5) * 10.0);
            if (!level.getBlockState(BlockPos.containing(target)).isAir()) {
                target = this.position().add(0.0, -0.5, 0.0);
            }
            this.forageTarget = target;
        }
        Vec3 to = target.subtract(this.position());
        Vec3 v = this.getDeltaMovement().scale(0.85).add(to.normalize().scale(0.035));
        this.setDeltaMovement(v);
        if (v.horizontalDistanceSqr() > 1.0E-4) {
            this.setYRot((float) Math.toDegrees(Math.atan2(v.z, v.x)) - 90.0F);
            this.yBodyRot = this.getYRot();
        }
        if (this.random.nextInt(40) == 0) {
            level.broadcastEntityEvent(this, EVENT_SNAP);
            this.playSound(ModSounds.RIVETER_AMBIENT.get(), 0.4F, 1.9F);
            Vec3 mouth = this.position().add(this.getLookAngle().scale(0.5));
            level.sendParticles(ModParticles.GLOW_DUST.get(), mouth.x, mouth.y + 0.4, mouth.z, 4, 0.15, 0.15, 0.15, 0.01);
        }
    }

    /** A note was played nearby: it bobs along on its roost and forgets to scream. */
    public void hearNote(ServerLevel level, int pitch) {
        this.lulledTicks = Math.max(this.lulledTicks, 80);
        if (this.bobCooldown <= 0) {
            this.bobCooldown = 4;
            level.broadcastEntityEvent(this, EVENT_BOB);
        }
    }

    private void scream(ServerLevel level, ServerPlayer player) {
        this.screamCooldown = 20 * 12;
        level.broadcastEntityEvent(this, EVENT_SCREAM);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.RIVETER_SCREAM.get(), SoundSource.HOSTILE, 3.0F, 0.9F + this.random.nextFloat() * 0.2F);
        level.sendParticles(new ShriekParticleOption(0), this.getX(), this.getY() + 0.3, this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 160, 0), this);

        List<Warden> wardens = level.getEntitiesOfClass(Warden.class, new AABB(this.blockPosition()).inflate(48));
        if (!wardens.isEmpty()) {
            for (Warden w : wardens) {
                if (w.hasEffect(com.thesift.registry.ModEffects.DEAFENED)) continue;
                w.increaseAngerAt(player, 80, true);
                w.getBrain().setMemory(MemoryModuleType.DISTURBANCE_LOCATION, this.blockPosition().below(2));
            }
        } else {
            OptionalInt warning = WardenSpawnTracker.tryWarn(level, this.blockPosition(), player);
            if (warning.isPresent() && warning.getAsInt() >= 3) {
                SpawnUtil.trySpawnMob(EntityTypes.WARDEN, EntitySpawnReason.TRIGGERED, level, this.blockPosition().below(3), 20, 5, 6,
                        SpawnUtil.Strategy.ON_TOP_OF_COLLIDER, false);
            }
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_SCREAM) {
            this.screamAnimation.start(this.tickCount);
            this.sway.kick(0.6F);
        } else if (id == EVENT_CHITTER) {
            this.chitterAnimation.start(this.tickCount);
        } else if (id == EVENT_SNAP) {
            this.snapAnimation.start(this.tickCount);
        } else if (id == EVENT_BOB) {
            this.sway.kick(this.random.nextBoolean() ? 0.25F : -0.25F);
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.sway.setTarget((float) Math.sin(this.tickCount * 0.03) * 0.15F);
            this.sway.tick();
            if (this.random.nextInt(20) == 0) {
                this.level().addParticle(ModParticles.GLOW_DUST.get(), this.getRandomX(0.4), this.getY() + 0.1, this.getRandomZ(0.4), 0, -0.01, 0);
            }
            // dissolving into sculk souls as it shrivels
            if (this.deathTime > 2 && this.random.nextInt(2) == 0) {
                this.level().addParticle(ParticleTypes.SCULK_SOUL, this.getRandomX(0.6), this.getRandomY(), this.getRandomZ(0.6), 0, 0.03, 0);
            }
        }
    }

    @Override
    public void hearMusic(BlockPos source, float strength) {
        this.lulledTicks = (int) (200 * strength) + 60;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        this.sway.kick(0.4F);
        this.forageTicks = 0;
        this.entityData.set(ROOSTING, false);
        if (source.getEntity() instanceof ServerPlayer player && this.screamCooldown < 100) {
            this.screamCooldown = 0;
            this.lulledTicks = 0;
            this.scream(level, player);
        }
        return super.hurtServer(level, source, damage);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ModSounds.RIVETER_AMBIENT.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 300;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.RIVETER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.RIVETER_DEATH.get();
    }

    /** Sculk souls and cyan sparks. */
    @Override
    public void makePoofParticles() {
        KillBurst.pop(this, 0x2EE6D6, 0x1D3B4F, KillBurst.STAR, net.minecraft.core.particles.ParticleTypes.SCULK_SOUL);
    }
}
