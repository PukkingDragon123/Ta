package com.thesift.block;

import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Sift Gate Frame: Siftite bound around echo shards, with a nether star's light sealed inside.
 * It is the only block a player can craft that holds a Sift portal open, like the reinforced
 * deepslate of Ancient City gates - and it is priced like it. A lit frame breathes souls.
 */
public class SiftGateFrameBlock extends Block {
    public SiftGateFrameBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(8) != 0) return;
        for (Direction d : Direction.values()) {
            if (level.getBlockState(pos.relative(d)).is(ModBlocks.SIFT_PORTAL.get())) {
                double x = pos.getX() + 0.5 + d.getStepX() * 0.6;
                double y = pos.getY() + 0.5 + d.getStepY() * 0.6;
                double z = pos.getZ() + 0.5 + d.getStepZ() * 0.6;
                level.addParticle(random.nextBoolean() ? ParticleTypes.SCULK_SOUL : ModParticles.STAR_SPARKLE.get(), x, y, z, 0, 0.02, 0);
                return;
            }
        }
        // an unlit frame still glints now and then: the star inside
        if (random.nextInt(6) == 0) {
            level.addParticle(ModParticles.STAR_SPARKLE.get(), pos.getX() + random.nextDouble(), pos.getY() + 1.02, pos.getZ() + random.nextDouble(),
                    0, 0.01, 0);
        }
    }
}
