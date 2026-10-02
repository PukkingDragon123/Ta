package com.thesift.block;

import com.thesift.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * Where each of the Sift's ancient plants is happy. Every seed a Sniffer digs up wants a place of
 * its own, so a garden of all three needs some planning:
 * <ul>
 *   <li>Choir Lilies sing to the sky: a Choir Pod only grows with open sky above it.</li>
 *   <li>Echo Orchids remember the Deep Dark: an Echo Seed only grows in the dark (light 7 or less),
 *       and twice as fast rooted in sculk.</li>
 *   <li>Pitcher Bulbs drink: the bush only fills its pitchers with water or Chrome within 4 blocks.</li>
 * </ul>
 */
public final class PlantHabitat {
    public static final int WATER_REACH = 4;
    public static final int DARK_LIGHT = 7;

    private PlantHabitat() {}

    public static boolean openSky(LevelReader level, BlockPos pos) {
        return level.canSeeSky(pos);
    }

    public static boolean dark(LevelReader level, BlockPos pos) {
        return level.getRawBrightness(pos, 0) <= DARK_LIGHT;
    }

    public static boolean onSculk(LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(Blocks.SCULK);
    }

    /** Water or Chrome within {@link #WATER_REACH} blocks around the soil (like farmland's moisture check). */
    public static boolean nearWater(LevelReader level, BlockPos pos) {
        BlockPos soil = pos.below();
        for (BlockPos p : BlockPos.betweenClosed(soil.offset(-WATER_REACH, 0, -WATER_REACH), soil.offset(WATER_REACH, 1, WATER_REACH))) {
            FluidState fluid = level.getFluidState(p);
            if (fluid.is(FluidTags.WATER) || fluid.getType().isSame(ModFluids.CHROME.get())) {
                return true;
            }
        }
        return false;
    }

    /** Soils the Sift's seeds root in: Sift soils, farmland, and for the damp-loving ones mud, clay and moss. */
    public static boolean dampSoil(BlockState state) {
        return state.is(Blocks.MUD) || state.is(Blocks.CLAY) || state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.MUDDY_MANGROVE_ROOTS);
    }
}
