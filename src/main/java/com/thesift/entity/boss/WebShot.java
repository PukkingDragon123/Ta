package com.thesift.entity.boss;

import com.thesift.registry.ModEntities;
import com.thesift.world.TemporaryBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** A ball of web spat by the Strummer's spider: it bursts into a patch of cobwebs that melt away. */
public class WebShot extends ThrowableItemProjectile {
    public WebShot(EntityType<? extends WebShot> type, Level level) {
        super(type, level);
    }

    public WebShot(ServerLevel level, LivingEntity owner) {
        super(ModEntities.WEB_SHOT.get(), owner, level, new ItemStack(Items.COBWEB));
    }

    @Override
    protected Item getDefaultItem() {
        return Items.COBWEB;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.02;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.level().addParticle(ParticleTypes.WHITE_ASH, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level() instanceof ServerLevel server && result.getEntity() instanceof LivingEntity le && !MiniBoss.isBandmate(le)) {
            le.hurtServer(server, this.damageSources().thrown(this, this.getOwner()), 3.0F);
            le.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 2));
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level() instanceof ServerLevel server) {
            Vec3 at = result.getLocation();
            BlockPos c = BlockPos.containing(at);
            TemporaryBlocks.webs(server, c, 1, 0.45F, 160);
            TemporaryBlocks.place(server, c, net.minecraft.world.level.block.Blocks.COBWEB.defaultBlockState(), 160);
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.COBWEB.defaultBlockState()), at.x, at.y, at.z, 16, 0.4, 0.4, 0.4, 0.1);
            server.playSound(null, at.x, at.y, at.z, SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.0F, 0.6F);
            this.discard();
        }
    }
}
