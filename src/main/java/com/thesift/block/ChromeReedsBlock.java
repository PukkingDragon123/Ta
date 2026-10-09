package com.thesift.block;

import com.thesift.registry.ModFluids;
import com.thesift.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/** Pearly reeds that grow along the shores of Chrome (or water) lakes. */
public class ChromeReedsBlock extends SugarCaneBlock {
    public ChromeReedsBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        if (below.is(this)) {
            return true;
        }
        if (!(SiftPlantBlock.isSiftSoil(below) || below.is(com.thesift.registry.ModBlocks.CHIME_SAND.get()))) {
            return false;
        }
        BlockPos base = pos.below();
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            FluidState fluid = level.getFluidState(base.relative(dir));
            if (fluid.getType().isSame(ModFluids.CHROME.get()) || fluid.is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(40) == 0) {
            level.addParticle(ModParticles.CHROME_DROPLET.get(), pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 0.0, 0.0, 0.0);
        }
    }
}
