package com.thesift.block;

import com.thesift.registry.ModBlocks;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Archaeology block buried around Sift ruins. Brushing reveals it's loot and leaves Dreamsand. */
public class SuspiciousDreamsandBlock extends BrushableBlock {
    public SuspiciousDreamsandBlock(BlockBehaviour.Properties properties) {
        super(ModBlocks.DREAMSAND.get(), SoundEvents.BRUSH_SAND, SoundEvents.BRUSH_SAND_COMPLETED, properties);
    }
}
