package com.thesift.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** W-sea: a Bubble Coral fan standing on the sea floor (its wall-hanging twin is {@link BubbleCoralWallFanBlock}). */
public class BubbleCoralFanBlock extends BubbleCoralBlock {
    private static final VoxelShape SHAPE = Block.column(12.0, 0.0, 4.0);

    public BubbleCoralFanBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
