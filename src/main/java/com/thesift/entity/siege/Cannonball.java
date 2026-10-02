package com.thesift.entity.siege;

import com.thesift.entity.boss.MiniBoss;
import com.thesift.entity.boss.Thumper;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModItems;
import com.thesift.registry.ModSiege;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A heavy iron ball fired by an Ancient Cannon: it arcs high and drops hard. The Thumper's shell
 * shrugs off everything else, but a cannonball landing on one of its open, glowing sculk vents
 * truly hurts it. The Thumper throws the same kind of shot back - boulders torn out of its arena
 * floor (shown as a block of cobbled dreamstone), which never hurt it or its kind.
 */
public class Cannonball extends ThrowableItemProjectile {
    public static final float BALL_DAMAGE = 10.0F;
    public static final float BOULDER_DAMAGE = 7.0F;

    public Cannonball(EntityType<? extends Cannonball> type, Level level) {
        super(type, level);
    }

    /** An iron cannonball, fired from a cannon (owner: whoever lit it, or nobody). */
    public static Cannonball ball(ServerLevel level, Vec3 at, @org.jspecify.annotations.Nullable LivingEntity owner) {
        Cannonball b = new Cannonball(ModSiege.CANNONBALL.get(), level);
        b.setItem(new ItemStack(ModItems.CANNONBALL.get()));
        b.setPos(at.x, at.y, at.z);
        if (owner != null) {
            b.setOwner(owner);
        }
        return b;
    }

    /** A boulder hurled by the Thumper. */
    public static Cannonball boulder(ServerLevel level, Vec3 at, LivingEntity thrower) {
        Cannonball b = new Cannonball(ModSiege.CANNONBALL.get(), level);
        b.setItem(new ItemStack(ModBlocks.CRUMBLING_DREAMSTONE.get().asItem()));
        b.setPos(at.x, at.y, at.z);
        b.setOwner(thrower);
        return b;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.CANNONBALL.get();
    }

    public boolean isBoulder() {
        return !this.getItem().is(ModItems.CANNONBALL.get());
    }

    /** Heavy: it climbs, hangs, and comes down steeply. */
    @Override
    protected double getDefaultGravity() {
        return 0.06;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            if (this.isBoulder()) {
                this.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, ModBlocks.CRUMBLING_DREAMSTONE.get().defaultBlockState()),
                        this.getX(), this.getY(), this.getZ(), 0, 0, 0);
            } else {
                this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.1, this.getZ(), 0, 0.01, 0);
                if (this.tickCount % 3 == 0) {
                    this.level().addParticle(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY(), this.getZ(), 0, 0.02, 0);
                }
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(this.level() instanceof ServerLevel server) || !(result.getEntity() instanceof LivingEntity le)) {
            return;
        }
        if (this.isBoulder()) {
            if (!MiniBoss.isBandmate(le)) {
                le.hurtServer(server, this.damageSources().thrown(this, this.getOwner()), BOULDER_DAMAGE);
                Vec3 v = this.getDeltaMovement().multiply(1, 0, 1).normalize();
                le.push(v.x * 0.8, 0.4, v.z * 0.8);
            }
            return;
        }
        if (le instanceof Thumper th) {
            th.cannonHit(server, this);
            return;
        }
        if (le.hurtServer(server, this.damageSources().thrown(this, this.getOwner()), BALL_DAMAGE)) {
            Vec3 v = this.getDeltaMovement().multiply(1, 0, 1).normalize();
            le.push(v.x * 1.2, 0.5, v.z * 1.2);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!(this.level() instanceof ServerLevel server)) {
            return;
        }
        Vec3 at = result.getLocation();
        BlockState dust = this.isBoulder() ? ModBlocks.CRUMBLING_DREAMSTONE.get().defaultBlockState()
                : server.getBlockState(BlockPos.containing(at.subtract(this.getDeltaMovement().normalize().scale(-0.3))));
        if (dust.isAir()) {
            dust = ModBlocks.CRUMBLING_DREAMSTONE.get().defaultBlockState();
        }
        server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, dust), at.x, at.y, at.z, this.isBoulder() ? 40 : 24, 0.5, 0.3, 0.5, 0.25);
        server.sendParticles(ParticleTypes.POOF, at.x, at.y, at.z, 6, 0.3, 0.2, 0.3, 0.05);
        if (this.isBoulder()) {
            server.playSound(null, at.x, at.y, at.z, SoundEvents.DEEPSLATE_BRICKS_BREAK, SoundSource.HOSTILE, 2.0F, 0.6F);
            // the shards still sting whoever stands where it lands
            for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(1.6))) {
                if (!MiniBoss.isBandmate(e) && result.getType() == HitResult.Type.BLOCK) {
                    e.hurtServer(server, this.damageSources().thrown(this, this.getOwner()), BOULDER_DAMAGE * 0.5F);
                }
            }
        } else {
            server.playSound(null, at.x, at.y, at.z, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.2F, 0.5F);
        }
        this.discard();
    }
}
