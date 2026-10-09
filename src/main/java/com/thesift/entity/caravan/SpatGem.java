package com.thesift.entity.caravan;

import com.thesift.registry.ModCaravans;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * CR2: a gem spat by the Caravan Queen - a real gem (amethyst, lapis, quartz, now and then an emerald,
 * a Prism gem or even a diamond) flying in a glittering arc. It hurts whoever it hits, and then it
 * drops where it lands: the gem is yours to pick up. It flies past her own caravans.
 */
public class SpatGem extends ThrowableItemProjectile {
    private static final float DAMAGE = 5.0F;

    public SpatGem(EntityType<? extends SpatGem> type, Level level) {
        super(type, level);
    }

    public SpatGem(ServerLevel level, LivingEntity owner, ItemStack gem) {
        super(ModCaravans.SPAT_GEM.get(), owner, level, gem);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.AMETHYST_SHARD;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.04;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() && this.tickCount > 1) {
            Vec3 v = this.getDeltaMovement();
            this.level().addParticle(ModParticles.STAR_SPARKLE.get(), this.getX() - v.x * 0.5, this.getY() - v.y * 0.5, this.getZ() - v.z * 0.5,
                    0.0, 0.0, 0.0);
        }
    }

    /** It flies past the caravans it was spat for. */
    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !(target instanceof Caravan) && !(target instanceof CaravanLarva);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level() instanceof ServerLevel server) {
            result.getEntity().hurtServer(server, this.damageSources().thrown(this, this.getOwner()), DAMAGE);
        }
    }

    /** The gem clinks down and stays there, a gem like any other. */
    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level() instanceof ServerLevel server) {
            Vec3 at = result.getLocation();
            ItemStack gem = this.getItem().copy();
            server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromNonEmptyStack(gem)), at.x, at.y, at.z, 8, 0.15, 0.15, 0.15,
                    0.1);
            server.sendParticles(ModParticles.STAR_SPARKLE.get(), at.x, at.y, at.z, 6, 0.2, 0.2, 0.2, 0.02);
            server.playSound(null, at.x, at.y, at.z, ModSounds.SPAT_GEM_HIT.get(), SoundSource.HOSTILE, 0.9F, 0.9F + this.random.nextFloat() * 0.3F);
            Vec3 back = this.getDeltaMovement().scale(-0.08);
            ItemEntity drop = new ItemEntity(server, at.x, at.y + 0.1, at.z, gem, back.x, 0.15, back.z);
            drop.setDefaultPickUpDelay();
            server.addFreshEntity(drop);
            this.discard();
        }
    }
}
