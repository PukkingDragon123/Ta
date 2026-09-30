package com.thesift.block;

import com.thesift.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HangingMossBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** The pale weeping strands that hang from Lullwood canopies. */
public class HangingLullwoodLeavesBlock extends HangingMossBlock {
    public HangingLullwoodLeavesBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(TIP) && random.nextInt(60) == 0) {
            level.addParticle(ModParticles.LULLWOOD_LEAF.get(), pos.getX() + random.nextDouble(), pos.getY() + 0.1, pos.getZ() + random.nextDouble(),
                    0.0, 0.0, 0.0);
        }
    }
}
