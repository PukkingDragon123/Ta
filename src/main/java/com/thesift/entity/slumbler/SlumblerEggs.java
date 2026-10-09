package com.thesift.entity.slumbler;

import com.thesift.entity.KillBurst;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSlumbler;
import com.thesift.registry.ModSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * CR2: a clutch of Slumbler eggs - a raft of clear jelly eggs, each with a rainbow yolk curled up
 * inside, floating on the Chrome where the Slumblers laid it (or sitting where it was dropped on
 * land). The little ones turn and twitch more and more as they grow, and after a few minutes the
 * jelly bursts and two to four stingray-like tadpoles ({@link SlumblerTadpole}) swim out.
 */
public class SlumblerEggs extends Mob {
    /** How far along it is, 0-100 (synced every second so the client can wobble the yolks). */
    private static final EntityDataAccessor<Integer> PROGRESS = SynchedEntityData.defineId(SlumblerEggs.class, EntityDataSerializers.INT);
    private static final int HATCH_TICKS = 3600;
    private int age;
    private int hatchTicks = HATCH_TICKS;

    public SlumblerEggs(EntityType<? extends Mob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 4.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PROGRESS, 0);
    }

    /** A fresh clutch, to hatch in three to four minutes. */
    public void laid() {
        this.age = 0;
        this.hatchTicks = HATCH_TICKS + this.random.nextInt(1200);
        this.setPersistenceRequired();
    }

    /** 0 when laid, 1 as it hatches. */
    public float progress() {
        return this.entityData.get(PROGRESS) / 100.0F;
    }

    /** It floats: on the surface of whatever liquid it is in, else it rests where it lies. */
    @Override
    public void tick() {
        super.tick();
        if (this.isInFluidType() && !this.isInLava()) {
            Vec3 v = this.getDeltaMovement();
            this.setDeltaMovement(v.x * 0.8, Math.min(v.y + 0.02, 0.06), v.z * 0.8);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level() instanceof ServerLevel server) {
            this.age++;
            if (this.age % 20 == 0) {
                this.entityData.set(PROGRESS, Mth.clamp(this.age * 100 / Math.max(1, this.hatchTicks), 0, 100));
            }
            if (this.age >= this.hatchTicks) {
                this.hatchOut(server);
            }
        } else if (this.random.nextInt(Math.max(3, 30 - (int) (this.progress() * 22))) == 0) {
            this.level().addParticle(ModParticles.CHROME_BUBBLE.get(), this.getRandomX(0.5), this.getY() + 0.2, this.getRandomZ(0.5), 0.0, 0.02, 0.0);
        }
    }

    /** The jelly bursts and the tadpoles swim out. */
    private void hatchOut(ServerLevel level) {
        int n = 2 + this.random.nextInt(3);
        for (int i = 0; i < n; i++) {
            SlumblerTadpole tadpole = ModSlumbler.SLUMBLER_TADPOLE.get().create(level, EntitySpawnReason.BREEDING);
            if (tadpole == null) {
                continue;
            }
            tadpole.snapTo(this.getRandomX(0.6), this.getY() + 0.1, this.getRandomZ(0.6), this.random.nextFloat() * 360.0F, 0.0F);
            tadpole.setPersistenceRequired();
            level.addFreshEntity(tadpole);
        }
        level.sendParticles(ModParticles.CHROME_DROPLET.get(), this.getX(), this.getY() + 0.3, this.getZ(), 24, 0.4, 0.2, 0.4, 0.12);
        level.sendParticles(ModParticles.CHROME_BUBBLE.get(), this.getX(), this.getY() + 0.2, this.getZ(), 16, 0.4, 0.2, 0.4, 0.04);
        this.playSound(ModSounds.SLUMBLER_EGGS_HATCH.get(), 1.0F, 1.0F + this.random.nextFloat() * 0.2F);
        this.discard();
    }

    // ------------------------------------------------------------------ a clutch stays put

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.SLUMBLER_EGGS_HATCH.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SLUMBLER_EGGS_HATCH.get();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Age", this.age);
        output.putInt("HatchTicks", this.hatchTicks);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.age = input.getIntOr("Age", 0);
        this.hatchTicks = Math.max(20, input.getIntOr("HatchTicks", HATCH_TICKS));
        this.entityData.set(PROGRESS, Mth.clamp(this.age * 100 / this.hatchTicks, 0, 100));
    }

    /** Jelly and droplets. */
    @Override
    public void makePoofParticles() {
        KillBurst.pop(this, 0xBFE9F5, 0xF59AD0, KillBurst.DROP, ModParticles.CHROME_BUBBLE.get());
    }
}
