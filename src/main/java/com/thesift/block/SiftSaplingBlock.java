package com.thesift.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class SiftSaplingBlock extends SaplingBlock {
    public SiftSaplingBlock(TreeGrower grower, BlockBehaviour.Properties properties) {
        super(grower, properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return SiftPlantBlock.isSiftSoil(state) || super.mayPlaceOn(state, level, pos);
    }
}
