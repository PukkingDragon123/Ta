package com.thesift.block;

import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class EchoOrchidCropBlock extends SiftCropBlock {
    public EchoOrchidCropBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected int maxAge() {
        return 2;
    }

    @Override
    protected void bloom(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, ModBlocks.ECHO_ORCHID.get().defaultBlockState(), Block.UPDATE_ALL);
        com.thesift.music.Resonance.bloomBurst(level, pos);
    }

    /** Echo Orchids remember the Deep Dark: they only grow out of the light... */
    @Override
    protected boolean likesHabitat(net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return PlantHabitat.dark(level, pos);
    }

    /** ...and root happily in sculk, where they grow twice as fast. */
    @Override
    protected float growthChance(net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return PlantHabitat.onSculk(level, pos) ? 0.5F : 0.25F;
    }

    @Override
    protected boolean mayPlaceOn(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return state.is(net.minecraft.world.level.block.Blocks.SCULK) || state.is(net.minecraft.world.level.block.Blocks.MOSS_BLOCK)
                || super.mayPlaceOn(state, level, pos);
    }

    @Override
    protected ItemStack seed() {
        return new ItemStack(ModItems.ECHO_SEED.get());
    }
}
