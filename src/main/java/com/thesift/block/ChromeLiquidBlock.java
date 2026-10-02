package com.thesift.block;

import com.thesift.effect.RainbowDazeEffect;
import com.thesift.registry.ModFluids;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import com.thesift.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Chrome liquid block. Anything that soaks in it comes out Rainbow Dazed (Chrome no longer
 * heals); its creatures - the music fish and Slumblers - are used to it. The thick, quicksand-like
 * movement lives in {@link ChromeFluidType#move}; ripples, colour and the daze's visions live in
 * client/ChromeClient, and Chrome meeting water in world/ChromeReactions.
 */
public class ChromeLiquidBlock extends LiquidBlock {
    public ChromeLiquidBlock(BlockBehaviour.Properties properties) {
        super(ModFluids.CHROME.get(), properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        super.entityInside(state, level, pos, entity, effectApplier, isPrecise);
        if (entity.isOnFire()) {
            entity.clearFire();
        }
        if (level instanceof ServerLevel server && entity instanceof LivingEntity living && living.isAlive()) {
            if (!living.getType().builtInRegistryHolder().is(ModTags.Entities.CHROME_DWELLERS)) {
                RainbowDazeEffect.soak(living);
            }
            if (living.tickCount % 50 == 0 && server.getRandom().nextInt(3) == 0) {
                server.playSound(null, pos, ModSounds.CHROME_SPLASH.get(), SoundSource.BLOCKS, 0.3F, 0.8F + server.getRandom().nextFloat() * 0.4F);
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!level.getBlockState(pos.above()).isAir()) {
            return;
        }
        if (random.nextInt(12) == 0) {
            level.addParticle(ModParticles.CHROME_DROPLET.get(), pos.getX() + random.nextDouble(), pos.getY() + 0.95, pos.getZ() + random.nextDouble(),
                    0.0, 0.04 + random.nextDouble() * 0.04, 0.0);
        }
        if (random.nextInt(18) == 0) {
            level.addParticle(ModParticles.CHROME_BUBBLE.get(), pos.getX() + random.nextDouble(), pos.getY() + 0.6, pos.getZ() + random.nextDouble(),
                    0.0, 0.01, 0.0);
        }
        if (random.nextInt(260) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ModSounds.CHROME_AMBIENT.get(), SoundSource.BLOCKS,
                    0.35F + random.nextFloat() * 0.2F, 0.8F + random.nextFloat() * 0.4F, false);
        }
        if (random.nextInt(140) == 0) {
            level.addParticle(ModParticles.SIFT_MIST.get(), pos.getX() + random.nextDouble(), pos.getY() + 1.05, pos.getZ() + random.nextDouble(),
                    0.0, 0.002, 0.0);
        }
    }
}
