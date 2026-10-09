package com.thesift.block;

import com.thesift.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * W-deep caves: a crystal cluster (rose, azure or amber) crusting the crystal floors and ceilings of the Sift Caves -
 * vanilla's amethyst cluster in Sift crystal: it grows from any face, chimes when hit and glitters.
 */
public class CrystalClusterBlock extends AmethystClusterBlock {
    public CrystalClusterBlock(BlockBehaviour.Properties properties) {
        super(7.0F, 10.0F, properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(10) == 0) {
            Direction facing = state.getValue(FACING);
            double x = pos.getX() + 0.5 + facing.getStepX() * 0.2 + (random.nextDouble() - 0.5) * 0.5;
            double y = pos.getY() + 0.5 + facing.getStepY() * 0.2 + (random.nextDouble() - 0.5) * 0.5;
            double z = pos.getZ() + 0.5 + facing.getStepZ() * 0.2 + (random.nextDouble() - 0.5) * 0.5;
            level.addParticle(ModParticles.STAR_SPARKLE.get(), x, y, z, 0.0, 0.0, 0.0);
        }
    }
}
