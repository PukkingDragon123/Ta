package com.thesift.block;

import com.thesift.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Soft glowing Deep Sift moss. Footsteps squeeze little clouds of glow dust out of it. */
public class LumenMossBlock extends Block {
    public LumenMossBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(24) == 0 && level.getBlockState(pos.above()).isAir()) {
            level.addParticle(ModParticles.GLOW_DUST.get(), pos.getX() + random.nextDouble(), pos.getY() + 1.05, pos.getZ() + random.nextDouble(),
                    0.0, 0.004, 0.0);
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level.isClientSide() && entity.getKnownMovement().horizontalDistanceSqr() > 0.002 && level.getRandom().nextInt(2) == 0) {
            RandomSource r = level.getRandom();
            level.addParticle(ModParticles.GLOW_DUST.get(), entity.getX() + (r.nextDouble() - 0.5) * 0.6, pos.getY() + 1.05,
                    entity.getZ() + (r.nextDouble() - 0.5) * 0.6, 0.0, 0.03, 0.0);
        }
        super.stepOn(level, pos, state, entity);
    }
}
