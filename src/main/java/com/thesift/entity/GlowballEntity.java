package com.thesift.entity;

import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModEffects;
import com.thesift.registry.ModEntities;
import com.thesift.registry.ModItems;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A slingshot-flung Glowing Slime Ball. On impact it bursts into a dazzling flash: the area stays
 * lit for a while, creatures are outlined, hostile ones are dazzled and Wardens are Deafened.
 */
public class GlowballEntity extends ThrowableItemProjectile {
    private float power = 1.0F;

    public GlowballEntity(EntityType<? extends GlowballEntity> type, Level level) {
        super(type, level);
    }

    public GlowballEntity(ServerLevel level, LivingEntity owner, ItemStack stack) {
        super(ModEntities.GLOWBALL.get(), owner, level, stack);
    }

    public GlowballEntity(Level level, double x, double y, double z, ItemStack stack) {
        super(ModEntities.GLOWBALL.get(), x, y, z, level, stack);
    }

    public static ItemStack defaultStack() {
        return new ItemStack(ModItems.GLOWING_SLIME_BALL.get());
    }

    public void setPower(float power) {
        this.power = power;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.GLOWING_SLIME_BALL.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() && this.tickCount > 1) {
            Vec3 v = this.getDeltaMovement();
            this.level().addParticle(ModParticles.STAR_SPARKLE.get(), this.getX() - v.x * 0.5, this.getY() - v.y * 0.5, this.getZ() - v.z * 0.5,
                    0, 0, 0);
            if (this.tickCount % 2 == 0) {
                this.level().addParticle(ModParticles.GLOW_SPLAT.get(), this.getX(), this.getY(), this.getZ(), 0, 0, 0);
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level() instanceof ServerLevel server) {
            result.getEntity().hurtServer(server, this.damageSources().thrown(this, this.getOwner()), 1.0F + this.power);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level() instanceof ServerLevel server) {
            this.burst(server, result.getLocation());
            this.discard();
        }
    }

    private void burst(ServerLevel level, Vec3 at) {
        float radius = 3.0F + this.power * 3.0F;
        level.playSound(null, at.x, at.y, at.z, ModSounds.GLOWBALL_BURST.get(), SoundSource.PLAYERS, 1.2F, 0.9F + level.getRandom().nextFloat() * 0.3F);
        level.sendParticles(ModParticles.GLOW_SPLAT.get(), at.x, at.y, at.z, 24, 0.4, 0.4, 0.4, 0.25);
        level.sendParticles(ModParticles.STAR_SPARKLE.get(), at.x, at.y, at.z, 40, radius * 0.4, radius * 0.3, radius * 0.4, 0.05);
        level.sendParticles(ModParticles.RESONANCE_RING.get(), at.x, at.y + 0.1, at.z, 0, radius, 0, 0, 1.0);

        // Lingering light: fill a few open spots around the impact with soft glowing air.
        BlockPos centre = BlockPos.containing(at);
        int placed = 0;
        for (BlockPos p : BlockPos.betweenClosed(centre.offset(-2, -1, -2), centre.offset(2, 2, 2))) {
            if (placed >= 6) break;
            if (level.getRandom().nextInt(3) != 0 && !p.equals(centre)) continue;
            if (level.getBlockState(p).isAir()) {
                level.setBlock(p, ModBlocks.LINGERING_GLOW.get().defaultBlockState(), Block.UPDATE_ALL);
                level.scheduleTick(p.immutable(), ModBlocks.LINGERING_GLOW.get(), 20 * (12 + level.getRandom().nextInt(10)));
                placed++;
            }
        }

        AABB box = new AABB(at, at).inflate(radius);
        int duration = (int) (20 * (6 + 10 * this.power));
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, Entity::isAlive)) {
            if (e instanceof Warden) {
                e.addEffect(new MobEffectInstance(ModEffects.DEAFENED, duration, 0));
            } else if (e instanceof Enemy) {
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                e.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0));
            } else {
                e.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration / 2, 0));
            }
        }
    }
}
