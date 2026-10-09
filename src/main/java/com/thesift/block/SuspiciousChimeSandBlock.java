package com.thesift.block;

import com.thesift.registry.ModBlocks;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Archaeology block buried in relic caches (worldgen/RelicCacheFeature). Brushing reveals its loot and leaves Chime Sand. */
public class SuspiciousChimeSandBlock extends BrushableBlock {
    public SuspiciousChimeSandBlock(BlockBehaviour.Properties properties) {
        super(ModBlocks.CHIME_SAND.get(), SoundEvents.BRUSH_SAND, SoundEvents.BRUSH_SAND_COMPLETED, properties);
    }
}
