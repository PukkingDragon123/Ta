package com.thesift.block;

import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class ChoirLilyCropBlock extends SiftCropBlock {
    public ChoirLilyCropBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected int maxAge() {
        return 3;
    }

    @Override
    protected void bloom(ServerLevel level, BlockPos pos) {
        BlockState lily = ModBlocks.CHOIR_LILY.get().defaultBlockState();
        if (level.isEmptyBlock(pos.above())) {
            DoublePlantBlock.placeAt(level, lily, pos, 3);
            com.thesift.music.Resonance.bloomBurst(level, pos);
        }
    }

    @Override
    protected ItemStack seed() {
        return new ItemStack(ModItems.CHOIR_POD.get());
    }
}
