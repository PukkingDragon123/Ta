package com.thesift.block;

import com.thesift.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FlowerBedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Fallen pink dream petals carpeting the plains; now and then one lifts off on the breeze. */
public class DriftPetalsBlock extends FlowerBedBlock {
    public DriftPetalsBlock(BlockBehaviour.Properties properties) {
        super(properties, 3);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return SiftPlantBlock.isSiftSoil(state) || super.mayPlaceOn(state, level, pos);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(80) == 0) {
            level.addParticle(ModParticles.WISHWOOD_LEAF.get(), pos.getX() + random.nextDouble(), pos.getY() + 0.1, pos.getZ() + random.nextDouble(),
                    0.02, 0.06, 0.02);
        }
    }
}
