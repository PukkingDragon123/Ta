package com.thesift.entity;

import com.thesift.registry.ModEntities;
import com.thesift.registry.ModItems;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A big wobbly bubble blown by the Bubble Gun. It floats on a gentle arc and pops on whatever it
 * touches in a shower of bubbles: a little damage, and the target is lifted off its feet for a
 * couple of seconds.
 */
public class BubbleEntity extends ThrowableItemProjectile {
    private static final int LIFETIME = 70;

    public BubbleEntity(EntityType<? extends BubbleEntity> type, Level level) {
        super(type, level);
    }

    public BubbleEntity(ServerLevel level, LivingEntity owner, ItemStack stack) {
        super(ModEntities.BUBBLE.get(), owner, level, stack);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.TUBA_BUBBLE.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.004;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            if (this.tickCount > 1 && this.random.nextInt(2) == 0) {
                this.level().addParticle(ModParticles.CHROME_BUBBLE.get(), this.getX() + (this.random.nextDouble() - 0.5) * 0.3,
                        this.getY() + (this.random.nextDouble() - 0.5) * 0.3, this.getZ() + (this.random.nextDouble() - 0.5) * 0.3, 0, 0.01, 0);
            }
            if (this.random.nextInt(6) == 0) {
                this.level().addParticle(ModParticles.STAR_SPARKLE.get(), this.getX(), this.getY(), this.getZ(), 0, 0, 0);
            }
        } else {
            // bubbles drift: a little wobble and they slow down
            Vec3 v = this.getDeltaMovement();
            this.setDeltaMovement(v.x * 0.97 + Math.sin(this.tickCount * 0.5) * 0.004, v.y * 0.97 + 0.002, v.z * 0.97 + Math.cos(this.tickCount * 0.5) * 0.004);
            if (this.tickCount > LIFETIME && this.level() instanceof ServerLevel server) {
                this.pop(server, this.position());
                this.discard();
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level() instanceof ServerLevel server) {
            if (result.getEntity().hurtServer(server, this.damageSources().thrown(this, this.getOwner()), 2.0F)
                    && result.getEntity() instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 0));
            } else if (result.getEntity() instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 0));
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level() instanceof ServerLevel server && !this.isRemoved()) {
            this.pop(server, result.getLocation());
            this.discard();
        }
    }

    private void pop(ServerLevel level, Vec3 at) {
        level.playSound(null, at.x, at.y, at.z, ModSounds.BUBBLE_POP.get(), SoundSource.PLAYERS, 1.0F, 0.9F + level.getRandom().nextFloat() * 0.4F);
        level.sendParticles(ParticleTypes.BUBBLE_POP, at.x, at.y, at.z, 24, 0.35, 0.35, 0.35, 0.12);
        level.sendParticles(ModParticles.CHROME_BUBBLE.get(), at.x, at.y, at.z, 18, 0.4, 0.4, 0.4, 0.06);
        level.sendParticles(ParticleTypes.SPLASH, at.x, at.y, at.z, 14, 0.3, 0.3, 0.3, 0.1);
        level.sendParticles(ModParticles.STAR_SPARKLE.get(), at.x, at.y, at.z, 10, 0.4, 0.4, 0.4, 0.04);
        level.sendParticles(ModParticles.RESONANCE_RING.get(), at.x, at.y, at.z, 0, 1.4, 0.0, 0.0, 1.0);
    }
}
