package com.thesift.entity;

import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Tubafish: a huge, round periwinkle pufferfish with an anemone-crowned tuba bell on its back. It drifts lazily through
 * Chrome, burbling low notes - but come too close and it blasts a tuba note, swells to twice its
 * size with its spikes out and blows a storm of bubbles. Touching it while it is puffed hurts.
 * Drops Tuba Bubbles.
 */
public class Tubafish extends SiftFish {
    private static final EntityDataAccessor<Boolean> PUFFED = SynchedEntityData.defineId(Tubafish.class, EntityDataSerializers.BOOLEAN);
    private int puffTicks;
    private int stingCooldown;
    /** Client: smoothed swell, 0 deflated .. 1 fully puffed. */
    public float puff;
    public float puffO;

    public Tubafish(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 16.0).add(Attributes.MOVEMENT_SPEED, 0.4).add(Attributes.ATTACK_DAMAGE, 2.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PUFFED, false);
    }

    public boolean isPuffed() {
        return this.entityData.get(PUFFED);
    }

    @Override
    protected double cruiseSpeed() {
        // slow and stately, but always visibly on the move (at 0.035 it drifted under half a block a second)
        return 0.05;
    }

    @Override
    protected double speedFactor() {
        return this.isPuffed() ? 0.4 : 1.0;
    }

    private boolean threatened(LivingEntity e) {
        if (e == this || !e.isAlive() || e instanceof Tubafish || e instanceof KazooFish) {
            return false;
        }
        if (e instanceof Player p) {
            return !p.isCreative() && !p.isSpectator();
        }
        return e instanceof Enemy || e instanceof FanfareEel;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && this.isAlive()) {
            this.inflate(level);
        }
        return hurt;
    }

    private void inflate(ServerLevel level) {
        this.puffTicks = 120;
        if (!this.isPuffed()) {
            this.entityData.set(PUFFED, true);
            this.playSound(ModSounds.TUBAFISH_PUFF.get(), 1.4F, 0.9F + this.random.nextFloat() * 0.2F);
            level.sendParticles(ParticleTypes.BUBBLE_POP, this.getX(), this.getY() + 0.5, this.getZ(), 30, 0.7, 0.6, 0.7, 0.08);
            level.sendParticles(ModParticles.CHROME_BUBBLE.get(), this.getX(), this.getY() + 0.5, this.getZ(), 24, 0.8, 0.6, 0.8, 0.05);
            level.sendParticles(ModParticles.RESONANCE_RING.get(), this.getX(), this.getY() + 0.5, this.getZ(), 0, 2.2, 0.0, 0.0, 1.0);
            level.sendParticles(ModParticles.SIFT_NOTE.get(), this.getX(), this.getY() + 1.4, this.getZ(), 0, 0.05, 0.0, 0.0, 1.0);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level() instanceof ServerLevel server) {
            if (this.tickCount % 5 == 0 && !server.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(2.5), this::threatened).isEmpty()) {
                this.inflate(server);
            }
            if (this.isPuffed()) {
                if (--this.puffTicks <= 0) {
                    this.entityData.set(PUFFED, false);
                    this.playSound(ModSounds.TUBAFISH_DEFLATE.get(), 1.0F, 1.0F);
                    server.sendParticles(ModParticles.CHROME_BUBBLE.get(), this.getX(), this.getY() + 1.0, this.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
                }
                // bubbles stream from the tuba bell
                if (this.tickCount % 2 == 0) {
                    server.sendParticles(ModParticles.CHROME_BUBBLE.get(), this.getX(), this.getY() + 1.5, this.getZ(), 2, 0.15, 0.05, 0.15, 0.04);
                    server.sendParticles(ParticleTypes.BUBBLE_POP, this.getX(), this.getY() + 1.4, this.getZ(), 1, 0.3, 0.1, 0.3, 0.02);
                }
                if (this.stingCooldown > 0) {
                    this.stingCooldown--;
                } else {
                    for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.35), this::threatened)) {
                        if (e.hurtServer(server, this.damageSources().mobAttack(this), 2.0F)) {
                            e.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
                            this.stingCooldown = 20;
                        }
                    }
                }
            }
        } else {
            this.puffO = this.puff;
            float target = this.isPuffed() ? 1.0F : 0.0F;
            // swells fast with a wobbly overshoot, deflates slowly
            this.puff += (target - this.puff) * (target > this.puff ? 0.35F : 0.06F);
        }
    }

    /** Its glowing freckles twinkle, the tuba burbles bubbles and the anemone sheds glitter. */
    @Override
    protected void clientEffects() {
        double size = 0.5 + 0.4 * this.puff;
        if (this.random.nextInt(5) == 0) {
            this.level().addParticle(ModParticles.STAR_SPARKLE.get(), this.getRandomX(size * 1.6), this.getY() + this.random.nextDouble() * size * 1.6,
                    this.getRandomZ(size * 1.6), 0.0, 0.0, 0.0);
        }
        if (this.random.nextInt(this.isPuffed() ? 2 : 9) == 0) {
            this.level().addParticle(ModParticles.CHROME_BUBBLE.get(), this.getX() + (this.random.nextDouble() - 0.5) * 0.3, this.getY() + 1.1 + this.puff * 0.5,
                    this.getZ() + (this.random.nextDouble() - 0.5) * 0.3, 0.0, 0.04, 0.0);
        }
        if (this.random.nextInt(14) == 0) {
            this.level().addParticle(ModParticles.GLOW_DUST.get(), this.getX(), this.getY() + 1.3 + this.puff * 0.5, this.getZ(), 0.0, 0.01, 0.0);
        }
    }

    @Override
    protected SoundEvent getFlopSound() {
        return ModSounds.TUBAFISH_FLOP.get();
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ModSounds.TUBAFISH_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.TUBAFISH_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.TUBAFISH_DEATH.get();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("PuffTicks", this.puffTicks);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.puffTicks = input.getIntOr("PuffTicks", 0);
        this.entityData.set(PUFFED, this.puffTicks > 0);
    }

    /** It pops like a balloon: a huge spray of bubbles and gold stars. */
    @Override
    public void makePoofParticles() {
        KillBurst.pop(this, 0x78A5E3, 0xF37D84, KillBurst.DROP, ParticleTypes.BUBBLE_POP);
        for (int i = 0; i < 30; i++) {
            this.level().addParticle(ModParticles.CHROME_BUBBLE.get(), this.getRandomX(1.2), this.getRandomY(), this.getRandomZ(1.2),
                    (this.random.nextDouble() - 0.5) * 0.2, this.random.nextDouble() * 0.2, (this.random.nextDouble() - 0.5) * 0.2);
        }
    }

    @Override
    public boolean isPushable() {
        return !this.isPuffed();
    }
}
