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

    @Override
    protected ItemStack seed() {
        return new ItemStack(ModItems.ECHO_SEED.get());
    }
}
