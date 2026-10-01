package com.thesift.entity;

import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AnimationState;
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
 * Called up by the Conductor's Staff: a songbird of sculk that circles its summoner for twenty
 * seconds, singing. Every second its song washes over the player - strength, speed, regeneration
 * and resistance - in a spiral of glowing notes, then it bursts back into the music it came from.
 */
public class SculkHarmoner extends PathfinderMob {
    public static final int LIFETIME = 400;
    private static final byte EVENT_SING = 61;

    public final AnimationState singAnimation = new AnimationState();
    private @Nullable UUID owner;
    private int life = LIFETIME;
    private final float orbitPhase;

    public SculkHarmoner(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.orbitPhase = this.random.nextFloat() * Mth.TWO_PI;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.4).add(Attributes.FLYING_SPEED, 0.8);
    }

    public void setOwner(Player player) {
        this.owner = player.getUUID();
    }

    public float lifeFraction() {
        return this.life / (float) LIFETIME;
    }

    private @Nullable Player ownerPlayer() {
        return this.owner == null ? null : this.level().getPlayerByUUID(this.owner);
    }

    @Override
    public void tick() {
        super.tick();
        Player p = this.ownerPlayer();
        if (this.level() instanceof ServerLevel server) {
            if (this.owner == null) {
                // nobody summoned it (spawned by command): it just hovers and sings
                this.setDeltaMovement(0.0, Mth.sin(this.tickCount * 0.15F) * 0.02, 0.0);
                if (this.tickCount % 40 == 1) {
                    this.level().broadcastEntityEvent(this, EVENT_SING);
                }
                return;
            }
            if (p == null || !p.isAlive() || --this.life <= 0) {
                this.discard();
                server.sendParticles(ModParticles.RESONANCE_RING.get(), this.getX(), this.getY(), this.getZ(), 0, 2.5, 0.0, 0.0, 1.0);
                server.sendParticles(ModParticles.SIFT_NOTE.get(), this.getX(), this.getY(), this.getZ(), 12, 0.4, 0.4, 0.4, 0.0);
                return;
            }
            // circle the summoner's head
            float a = this.tickCount * 0.09F + this.orbitPhase;
            Vec3 target = p.position().add(Mth.cos(a) * 2.0, 2.2 + Mth.sin(this.tickCount * 0.15F) * 0.35, Mth.sin(a) * 2.0);
            Vec3 to = target.subtract(this.position());
            this.setDeltaMovement(to.scale(0.25));
            this.setYRot((float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90.0F);
            this.yBodyRot = this.getYRot();
            if (this.tickCount % 40 == 1) {
                this.level().broadcastEntityEvent(this, EVENT_SING);
                server.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.HARMONER_SING.get(), SoundSource.PLAYERS, 1.0F,
                        0.85F + this.random.nextFloat() * 0.3F);
            }
            if (this.tickCount % 20 == 0) {
                // the song: buffs and a spiral of notes up around the player
                p.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 50, 0, true, true), this);
                p.addEffect(new MobEffectInstance(MobEffects.SPEED, 50, 0, true, true), this);
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 50, 0, true, true), this);
                p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 50, 0, true, true), this);
                server.sendParticles(ModParticles.RESONANCE_RING.get(), p.getX(), p.getY() + 0.05, p.getZ(), 0, 1.6, 0.0, 0.0, 1.0);
                for (int i = 0; i < 12; i++) {
                    double ang = i * Mth.TWO_PI / 12.0 + this.tickCount * 0.1;
                    server.sendParticles(ModParticles.GUIDE_NOTE.get(), p.getX() + Math.cos(ang) * 0.9, p.getY() + i * 0.17, p.getZ() + Math.sin(ang) * 0.9,
                            0, 0.18, 0.95, 0.9, 1.0);
                }
            }
        } else {
            if (this.random.nextInt(3) == 0) {
                this.level().addParticle(ModParticles.GUIDE_NOTE.get(), this.getX(), this.getY() + 0.3, this.getZ(), 0.18, 0.95, 0.9);
            }
            if (this.random.nextInt(4) == 0) {
                this.level().addParticle(ModParticles.GLOW_DUST.get(), this.getRandomX(0.5), this.getRandomY(), this.getRandomZ(0.5), 0, 0, 0);
            }
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_SING) {
            this.singAnimation.start(this.tickCount);
        } else {
            super.handleEntityEvent(id);
        }
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
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Life", this.life);
        if (this.owner != null) {
            output.putString("Owner", this.owner.toString());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.life = input.getIntOr("Life", LIFETIME);
        String o = input.getStringOr("Owner", "");
        if (!o.isEmpty()) {
            this.owner = UUID.fromString(o);
        }
    }
}
