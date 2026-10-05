package com.thesift.entity;

import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Fanfare Eel: a long sculk predator of the Chrome lakes, bone-ribbed and glowing. Its mouth is a trumpet bell, and
 * every bite comes with a blast of sound. It hunts swimmers - players and other fish alike -
 * darting in with its body rippling, then circling off to strike again.
 */
public class FanfareEel extends SiftFish implements Enemy {
    private static final byte EVENT_BITE = 102;
    public final AnimationState biteAnimation = new AnimationState();
    private @Nullable LivingEntity prey;
    private int preyCheck;
    private int biteCooldown;

    public FanfareEel(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.8).add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected double cruiseSpeed() {
        return 0.07;
    }

    /** CR3: abyss teal (the classic), violet and the rare ember. */
    @Override
    public int variantCount() {
        return 3;
    }

    @Override
    protected int[] variantWeights() {
        return new int[]{50, 35, 12};
    }

    @Override
    protected double speedFactor() {
        if (this.prey == null) {
            return 1.0;
        }
        // lunges in fast, then backs off while the bite recharges
        return this.biteCooldown > 0 ? 1.6 : 3.6;
    }

    private boolean isPrey(LivingEntity e) {
        if (!e.isAlive() || !e.isInFluidType() || e == this) {
            return false;
        }
        if (e instanceof Player p) {
            return !p.isCreative() && !p.isSpectator();
        }
        return e instanceof KazooFish || (e instanceof Tubafish t && !t.isPuffed());
    }

    @Override
    protected @Nullable Vec3 wantedPosition() {
        if (--this.preyCheck <= 0) {
            this.preyCheck = 10;
            if (this.prey == null || !this.isPrey(this.prey) || this.distanceToSqr(this.prey) > 20.0 * 20.0) {
                this.prey = null;
                double best = Double.MAX_VALUE;
                for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(12.0), this::isPrey)) {
                    double d = this.distanceToSqr(e);
                    if (d < best && this.hasLineOfSight(e)) {
                        best = d;
                        this.prey = e;
                    }
                }
            }
            this.setAggressive(this.prey != null);
            this.setTarget(this.prey);
        }
        if (this.prey == null) {
            return null;
        }
        Vec3 at = this.prey.getBoundingBox().getCenter();
        if (this.biteCooldown > 0) {
            // circle around the prey until the next strike
            double a = this.tickCount * 0.15 + this.getId();
            return at.add(Math.cos(a) * 3.0, 0.5, Math.sin(a) * 3.0);
        }
        return at;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level() instanceof ServerLevel server) {
            if (this.biteCooldown > 0) {
                this.biteCooldown--;
            }
            double reach = 1.3 + (this.prey != null ? this.prey.getBbWidth() * 0.5 : 0.0);
            if (this.prey != null && this.biteCooldown <= 0 && this.inLiquid() && this.distanceToSqr(this.prey) < reach * reach) {
                this.biteCooldown = 30;
                this.doHurtTarget(server, this.prey);
            }
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        level.broadcastEntityEvent(this, EVENT_BITE);
        this.playSound(ModSounds.FANFARE_EEL_BLAST.get(), 1.3F, 0.9F + this.random.nextFloat() * 0.25F);
        Vec3 dir = this.getLookAngle();
        Vec3 mouth = this.position().add(dir.scale(0.6)).add(0.0, 0.25, 0.0);
        level.sendParticles(ModParticles.RESONANCE_RING.get(), mouth.x, mouth.y, mouth.z, 0, 1.2, 0.0, 0.0, 1.0);
        for (int i = 0; i < 4; i++) {
            level.sendParticles(ModParticles.SIFT_NOTE.get(), mouth.x + dir.x * i * 0.4, mouth.y + 0.2, mouth.z + dir.z * i * 0.4, 0,
                    0.55 + i * 0.1, 0.0, 0.0, 1.0);
        }
        level.sendParticles(ModParticles.CHROME_BUBBLE.get(), mouth.x, mouth.y, mouth.z, 12, 0.3, 0.3, 0.3, 0.08);
        boolean hit = super.doHurtTarget(level, target);
        if (hit) {
            target.push(dir.x * 0.5, 0.15, dir.z * 0.5);
        }
        // recoil back from the bite
        this.setDeltaMovement(this.getDeltaMovement().add(dir.scale(-0.25)));
        return hit;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_BITE) {
            this.biteAnimation.start(this.tickCount);
        } else {
            super.handleEntityEvent(id);
        }
    }

    /** Its glowing lateral line sheds motes, and the bell burbles bubbles. */
    @Override
    protected void clientEffects() {
        float yaw = this.yBodyRot * net.minecraft.util.Mth.DEG_TO_RAD;
        double fx = -net.minecraft.util.Mth.sin(yaw);
        double fz = net.minecraft.util.Mth.cos(yaw);
        if (this.random.nextInt(4) == 0) {
            double back = this.random.nextDouble() * 1.8;
            this.level().addParticle(ModParticles.GLOW_DUST.get(), this.getX() - fx * back, this.getY() + 0.25, this.getZ() - fz * back, 0.0, 0.005, 0.0);
        }
        if (this.inLiquid() && this.random.nextInt(8) == 0) {
            this.level().addParticle(ModParticles.CHROME_BUBBLE.get(), this.getX() + fx * 0.7, this.getY() + 0.3, this.getZ() + fz * 0.7, 0.0, 0.02, 0.0);
        }
        if (this.isAggressive() && this.random.nextInt(25) == 0) {
            this.level().addParticle(ModParticles.SIFT_NOTE.get(), this.getX() + fx * 0.8, this.getY() + 0.5, this.getZ() + fz * 0.8, 0.6, 0.0, 0.0);
        }
    }

    @Override
    protected SoundEvent getFlopSound() {
        return ModSounds.FANFARE_EEL_FLOP.get();
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ModSounds.FANFARE_EEL_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.FANFARE_EEL_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.FANFARE_EEL_DEATH.get();
    }

    /** A blare of notes and brass sparks. */
    @Override
    public void makePoofParticles() {
        KillBurst.pop(this, 0x3FF5E6, 0xE3DDCC, KillBurst.NOTE, ModParticles.GLOW_DUST.get());
    }
}
