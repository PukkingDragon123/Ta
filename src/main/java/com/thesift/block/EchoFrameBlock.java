package com.thesift.block;

import com.thesift.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Echo Frame: deepslate bricks bound with echo shards and sculk. Like the reinforced deepslate of
 * Ancient Cities it can hold open a Sift portal; the arrival portals in the Sift are built of it.
 */
public class EchoFrameBlock extends Block {
    public EchoFrameBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(10) != 0) return;
        for (Direction d : Direction.values()) {
            if (level.getBlockState(pos.relative(d)).is(ModBlocks.SIFT_PORTAL.get())) {
                level.addParticle(ParticleTypes.SCULK_SOUL, pos.getX() + 0.5 + d.getStepX() * 0.6, pos.getY() + 0.5 + d.getStepY() * 0.6,
                        pos.getZ() + 0.5 + d.getStepZ() * 0.6, 0, 0.02, 0);
                return;
            }
        }
    }
}
