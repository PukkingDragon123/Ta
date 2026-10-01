package com.thesift.entity.boss;

import com.thesift.block.entity.ConductorsPodiumBlockEntity;
import com.thesift.registry.ModEntities;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Conductor's Mask. It rises out of the Grand Stage while the music falls away, turning slowly
 * in a column of sculk light; the music returns, it spins faster and faster, the light pours into
 * it - and it becomes the Conductor.
 */
public class ConductorMask extends PathfinderMob {
    private static final EntityDataAccessor<Integer> LIFE = SynchedEntityData.defineId(ConductorMask.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(ConductorMask.class, EntityDataSerializers.INT);
    public static final int RISE = 100;
    /** The last ticks before it becomes the Conductor. */
    public static final int TRANSFORM = 60;

    private @Nullable BlockPos stage;
    private double baseY;

    public ConductorMask(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 100.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LIFE, 0);
        builder.define(DURATION, 240);
    }

    public void setStage(BlockPos stage, int duration) {
        this.stage = stage.immutable();
        this.baseY = this.getY();
        this.entityData.set(DURATION, duration);
    }

    public int life() {
        return this.entityData.get(LIFE);
    }

    public int duration() {
        return this.entityData.get(DURATION);
    }

    /** 0..1 through the transformation at the end. */
    public float transform(float partial) {
        return Mth.clamp((this.life() + partial - (this.duration() - TRANSFORM)) / TRANSFORM, 0.0F, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        int life = this.life();
        int dur = this.duration();
        float k = this.transform(0.0F);
        if (this.level() instanceof ServerLevel server) {
            if (this.stage == null) {
                // not raised by a stage (a command or a test): it just hangs there, turning
                this.setDeltaMovement(Vec3.ZERO);
                this.setYRot(this.getYRot() + 1.0F);
                this.yBodyRot = this.getYRot();
                return;
            }
            this.entityData.set(LIFE, life + 1);
            if (life == 0) {
                this.baseY = this.getY();
                this.playSound(ModSounds.CONDUCTOR_MASK_RISE.get(), 4.0F, 0.8F);
            }
            // rise out of the stage, then hover
            double rise = Mth.clamp(life / (double) RISE, 0.0, 1.0);
            double eased = rise * rise * (3 - 2 * rise);
            double y = this.baseY + eased * 6.0 + Math.sin(life * 0.05) * 0.15 * rise;
            this.setPos(this.stage.getX() + 0.5, y, this.stage.getZ() + 0.5);
            this.setDeltaMovement(Vec3.ZERO);
            float spin = 1.0F + k * 18.0F;
            this.setYRot(this.getYRot() + spin);
            this.yBodyRot = this.getYRot();
            this.yHeadRot = this.getYRot();
            if (life == dur - TRANSFORM) {
                this.playSound(ModSounds.CONDUCTOR_MASK_TRANSFORM.get(), 5.0F, 0.7F);
            }
            if (life >= dur) {
                this.becomeConductor(server);
            }
        } else {
            // a column of sculk light; at the end the light pours into the mask
            Level level = this.level();
            for (int i = 0; i < 2 + (int) (k * 8); i++) {
                double a = this.random.nextDouble() * Math.PI * 2;
                double r = 0.4 + this.random.nextDouble() * (1.5 + k * 3.0);
                double px = this.getX() + Math.cos(a) * r;
                double pz = this.getZ() + Math.sin(a) * r;
                double py = this.getY() - 2.0 + this.random.nextDouble() * 5.0;
                if (k > 0.0F) {
                    Vec3 to = this.position().add(0, 1.0, 0).subtract(px, py, pz).scale(0.12);
                    level.addParticle(ParticleTypes.SCULK_SOUL, px, py, pz, to.x, to.y, to.z);
                } else {
                    level.addParticle(ParticleTypes.SCULK_SOUL, px, py, pz, 0, 0.04, 0);
                }
            }
            if (this.random.nextInt(3) == 0) {
                level.addParticle(ModParticles.GLOW_DUST.get(), this.getRandomX(1.0), this.getY() + 1.0, this.getRandomZ(1.0), 0, -0.02, 0);
            }
            if (this.random.nextInt(5) == 0) {
                level.addParticle(ModParticles.SIFT_NOTE.get(), this.getRandomX(1.5), this.getY() + 2.0, this.getRandomZ(1.5), this.random.nextDouble(), 0, 0);
            }
            if (life % 20 == 0) {
                level.addParticle(ModParticles.RESONANCE_RING.get(), this.getX(), this.getY() + 1.0, this.getZ(), 1.5 + k * 3.0, 0.0, 0.0);
            }
        }
    }

    private void becomeConductor(ServerLevel level) {
        Dictator d = ModEntities.DICTATOR.get().create(level, EntitySpawnReason.TRIGGERED);
        if (d != null && this.stage != null) {
            d.snapTo(this.stage.getX() + 0.5, this.stage.getY() + 1.0, this.stage.getZ() + 0.5, this.getYRot(), 0.0F);
            d.setPodium(this.stage);
            Player p = level.getNearestPlayer(this, 48.0);
            if (p != null && !p.isCreative() && !p.isSpectator()) {
                d.setTarget(p);
            }
            level.addFreshEntity(d);
            d.arrive(level);
            if (level.getBlockEntity(this.stage) instanceof ConductorsPodiumBlockEntity podium) {
                podium.bossRaised(d);
            }
        }
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1.0, this.getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY() + 1.0, this.getZ(), 120, 1.5, 2.0, 1.5, 0.15);
        level.sendParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 1.0, this.getZ(), 1, 0, 0, 0, 0);
        for (int i = 0; i < 4; i++) {
            level.sendParticles(ModParticles.RESONANCE_RING.get(), this.getX(), this.stage == null ? this.getY() : this.stage.getY() + 1.1, this.getZ(), 0,
                    3.0 + i * 3.0, 0.0, 0.0, 1.0);
        }
        this.discard();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Life", this.life());
        output.putInt("Duration", this.duration());
        output.putDouble("BaseY", this.baseY);
        if (this.stage != null) {
            output.putInt("StageX", this.stage.getX());
            output.putInt("StageY", this.stage.getY());
            output.putInt("StageZ", this.stage.getZ());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(LIFE, input.getIntOr("Life", 0));
        this.entityData.set(DURATION, input.getIntOr("Duration", 240));
        this.baseY = input.getDoubleOr("BaseY", this.getY());
        if (input.getIntOr("StageY", Integer.MIN_VALUE) != Integer.MIN_VALUE) {
            this.stage = new BlockPos(input.getIntOr("StageX", 0), input.getIntOr("StageY", 0), input.getIntOr("StageZ", 0));
        }
    }
}
