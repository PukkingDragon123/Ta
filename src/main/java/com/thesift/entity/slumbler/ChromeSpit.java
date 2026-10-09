package com.thesift.entity.slumbler;

import com.thesift.entity.Slumbler;
import com.thesift.registry.ModChrome;
import com.thesift.registry.ModItems;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSlumbler;
import com.thesift.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
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
 * CR2: a gob of Chrome spat by an angry Slumbler - a wobbling rainbow blob trailing droplets. It
 * stings a little and leaves whoever it hits dizzy with Rainbow Daze; it splashes wherever it lands.
 */
public class ChromeSpit extends ThrowableItemProjectile {
    private static final float DAMAGE = 2.0F;
    private static final int DAZE_TICKS = 200;

    public ChromeSpit(EntityType<? extends ChromeSpit> type, Level level) {
        super(type, level);
    }

    public ChromeSpit(ServerLevel level, LivingEntity owner) {
        super(ModSlumbler.CHROME_SPIT.get(), owner, level, new ItemStack(ModItems.CHROME_PEARL.get()));
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.CHROME_PEARL.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.05;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() && this.tickCount > 1) {
            Vec3 v = this.getDeltaMovement();
            this.level().addParticle(ModParticles.CHROME_DROPLET.get(), this.getX() - v.x * 0.4, this.getY() - v.y * 0.4, this.getZ() - v.z * 0.4,
                    0.0, 0.0, 0.0);
            if (this.tickCount % 2 == 0) {
                this.level().addParticle(ModChrome.CHROME_SPARK.get(), this.getX(), this.getY(), this.getZ(), (this.tickCount * 0.07) % 1.0, 0.02, 0.0);
            }
        }
    }

    /** It flies past other Slumblers. */
    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !(target instanceof Slumbler);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level() instanceof ServerLevel server) {
            Entity e = result.getEntity();
            e.hurtServer(server, this.damageSources().thrown(this, this.getOwner()), DAMAGE);
            if (e instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(ModChrome.RAINBOW_DAZE, DAZE_TICKS, 0, false, true, true), this.getOwner());
            }
        }
    }

    /** A rainbow splash wherever it lands. */
    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level() instanceof ServerLevel server) {
            Vec3 at = result.getLocation();
            server.sendParticles(ModParticles.CHROME_DROPLET.get(), at.x, at.y, at.z, 18, 0.3, 0.2, 0.3, 0.15);
            server.sendParticles(ModChrome.CHROME_SPARK.get(), at.x, at.y + 0.1, at.z, 0, this.random.nextDouble(), 0.05, 0.0, 1.0);
            server.playSound(null, at.x, at.y, at.z, ModSounds.CHROME_SPIT_SPLASH.get(), SoundSource.HOSTILE, 1.0F, 0.9F + this.random.nextFloat() * 0.3F);
            this.discard();
        }
    }
}
