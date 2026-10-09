package com.thesift.block;

import com.thesift.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * W-sea: sea plants that live in water <em>or</em> in Chrome. Vanilla only knows water-logging, so the Sift's corals carry a
 * second flag, {@link #CHROMELOGGED}: a Bubble Coral growing on the floor of the Chrome Coral Ocean holds Chrome the way a
 * vanilla coral holds water (the fluid renders round it, nothing flows into a hole, and breaking it leaves the Chrome
 * behind). Blocks using this declare both {@link #WATERLOGGED} and {@link #CHROMELOGGED} and route their fluid calls here.
 */
public final class SeaLogging {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty CHROMELOGGED = BooleanProperty.create("chromelogged");

    private SeaLogging() {
    }

    public static boolean isChrome(FluidState fluid) {
        return !fluid.isEmpty() && fluid.getType().isSame(ModFluids.CHROME.get());
    }

    /** {@code state} as it should be when placed into {@code fluid}: water-logged in water, Chrome-logged in Chrome. */
    public static BlockState inFluid(BlockState state, FluidState fluid) {
        boolean water = fluid.is(Fluids.WATER) && fluid.isSource();
        if (state.hasProperty(WATERLOGGED)) {
            state = state.setValue(WATERLOGGED, water);
        }
        if (state.hasProperty(CHROMELOGGED)) {
            state = state.setValue(CHROMELOGGED, !water && isChrome(fluid));
        }
        return state;
    }

    /** {@code state}, logged with whatever fluid is at {@code pos} right now. */
    public static BlockState inFluidAt(BlockState state, BlockGetter level, BlockPos pos) {
        return inFluid(state, level.getFluidState(pos));
    }

    public static boolean waterlogged(BlockState state) {
        return state.hasProperty(WATERLOGGED) && state.getValue(WATERLOGGED);
    }

    public static boolean chromelogged(BlockState state) {
        return state.hasProperty(CHROMELOGGED) && state.getValue(CHROMELOGGED);
    }

    /** True if the block holds water or Chrome. */
    public static boolean wet(BlockState state) {
        return waterlogged(state) || chromelogged(state);
    }

    /** The fluid a logged block holds. */
    public static FluidState fluid(BlockState state) {
        if (waterlogged(state)) {
            return Fluids.WATER.getSource(false);
        }
        if (chromelogged(state)) {
            return ModFluids.CHROME.get().getSource(false);
        }
        return Fluids.EMPTY.defaultFluidState();
    }

    /** Keeps a logged block's fluid ticking, like vanilla does for water-logged blocks in updateShape. */
    public static void tick(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos) {
        if (waterlogged(state)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        } else if (chromelogged(state)) {
            ticks.scheduleTick(pos, ModFluids.CHROME.get(), ModFluids.CHROME.get().getTickDelay(level));
        }
    }

    /** What is left when a logged block goes: its fluid's own block (or air). */
    public static BlockState remains(BlockState state) {
        return fluid(state).createLegacyBlock();
    }

    /** Open sea: a still source of water or Chrome that a growing coral may take over. */
    public static boolean openSea(BlockGetter level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        FluidState f = s.getFluidState();
        return s.getBlock() instanceof LiquidBlock && f.isSource() && (f.is(Fluids.WATER) || isChrome(f));
    }
}
