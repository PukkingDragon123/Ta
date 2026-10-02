package com.thesift.entity;

import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Kazoo Fish: a little teal and orange schooling fish with a kazoo for a snout. Schools follow a
 * leader through the Chrome lakes, buzzing tiny kazoo notes; hit one and the whole school scatters.
 */
public class KazooFish extends SiftFish {
    private @Nullable KazooFish leader;
    private int leaderCheck;
    private int panic;
    private @Nullable Vec3 fleeFrom;

    public KazooFish(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 4.0).add(Attributes.MOVEMENT_SPEED, 0.6);
    }

    @Override
    protected double cruiseSpeed() {
        return 0.09;
    }

    @Override
    protected double speedFactor() {
        return this.panic > 0 ? 2.6 : 1.0;
    }

    public boolean isPanicking() {
        return this.panic > 0;
    }

    @Override
    protected @Nullable Vec3 wantedPosition() {
        if (this.panic > 0) {
            this.panic--;
            if (this.fleeFrom != null) {
                Vec3 away = this.position().subtract(this.fleeFrom);
                if (away.lengthSqr() > 1.0E-4) {
                    return this.position().add(away.normalize().scale(4.0));
                }
            }
            return null;
        }
        if (--this.leaderCheck <= 0) {
            this.leaderCheck = 20 + this.random.nextInt(20);
            this.leader = null;
            List<KazooFish> school = this.level().getEntitiesOfClass(KazooFish.class, this.getBoundingBox().inflate(10.0), Entity::isAlive);
            for (KazooFish f : school) {
                if (f.getId() < this.getId() && (this.leader == null || f.getId() < this.leader.getId())) {
                    this.leader = f;
                }
            }
        }
        if (this.leader != null && this.leader.isAlive() && this.leader.inLiquid()) {
            // swim in formation: each fish keeps its own spot around the leader
            double a = (this.getId() * 2.399) % (Math.PI * 2.0);
            double r = 1.2 + (this.getId() % 3) * 0.5;
            Vec3 spot = this.leader.position().add(Math.cos(a) * r, ((this.getId() % 5) - 2) * 0.25, Math.sin(a) * r);
            Vec3 lead = this.leader.getDeltaMovement().scale(6.0);
            return spot.add(lead);
        }
        return null;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        Entity attacker = source.getEntity();
        Vec3 from = attacker != null ? attacker.position() : this.position().add(this.random.nextGaussian(), 0.0, this.random.nextGaussian());
        for (KazooFish f : level.getEntitiesOfClass(KazooFish.class, this.getBoundingBox().inflate(8.0), Entity::isAlive)) {
            f.panic = 50 + f.random.nextInt(30);
            f.fleeFrom = from;
        }
        return super.hurtServer(level, source, damage);
    }

    @Override
    public void playAmbientSound() {
        super.playAmbientSound();
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ModParticles.SIFT_NOTE.get(), this.getX(), this.getY() + 0.5, this.getZ(), 0, this.random.nextDouble(), 0.0, 0.0, 1.0);
        }
    }

    /** Tiny notes buzz off the kazoo, bubbles trail and the moss sprout twinkles. */
    @Override
    protected void clientEffects() {
        if (this.random.nextInt(70) == 0) {
            this.level().addParticle(ModParticles.SIFT_NOTE.get(), this.getX(), this.getY() + 0.45, this.getZ(), this.random.nextDouble(), 0.0, 0.0);
        }
        if (this.inLiquid() && this.random.nextInt(6) == 0) {
            double bx = Math.sin(Math.toRadians(this.yBodyRot)) * 0.35;
            double bz = -Math.cos(Math.toRadians(this.yBodyRot)) * 0.35;
            this.level().addParticle(ModParticles.CHROME_BUBBLE.get(), this.getX() + bx, this.getY() + 0.2, this.getZ() + bz, 0.0, 0.01, 0.0);
        }
        if (this.random.nextInt(30) == 0) {
            this.level().addParticle(ModParticles.STAR_SPARKLE.get(), this.getX(), this.getY() + 0.5, this.getZ(), 0.0, 0.01, 0.0);
        }
    }

    @Override
    protected SoundEvent getFlopSound() {
        return ModSounds.KAZOO_FISH_FLOP.get();
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ModSounds.KAZOO_FISH_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.KAZOO_FISH_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.KAZOO_FISH_DEATH.get();
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 8;
    }

    /** A pop of notes and bubbles in teal and orange. */
    @Override
    public void makePoofParticles() {
        KillBurst.pop(this, 0x3FD0C4, 0xF87D8D, KillBurst.NOTE, ModParticles.CHROME_BUBBLE.get());
    }
}
