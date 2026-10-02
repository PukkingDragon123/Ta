package com.thesift.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * The Sift's seas are Chrome; only its two true oceans (the Magic Kelp Forest and the Deep Dark
 * Ocean) are filled with real, swimmable water. Runs once per chunk before anything else is
 * decorated: in every column whose sea-level biome is one of {@code biomes}, the open Chrome from
 * the sea surface down to the sea floor becomes water. Buried Chrome aquifers are left alone.
 */
public record SeaFloodFeature(List<ResourceKey<Biome>> biomes, int seaLevel) implements Feature {
    public static final MapCodec<SeaFloodFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceKey.codec(Registries.BIOME).listOf().fieldOf("biomes").forGetter(SeaFloodFeature::biomes),
            Codec.INT.fieldOf("sea_level").forGetter(SeaFloodFeature::seaLevel)
    ).apply(i, SeaFloodFeature::new));

    @Override
    public MapCodec<SeaFloodFeature> codec() {
        return CODEC;
    }

    private boolean wanted(WorldGenLevel level, BlockPos pos) {
        var biome = level.getBiome(pos);
        for (ResourceKey<Biome> key : this.biomes) {
            if (biome.is(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int x0 = origin.getX() & ~15;
        int z0 = origin.getZ() & ~15;
        BlockState water = Blocks.WATER.defaultBlockState();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        boolean any = false;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                p.set(x0 + dx, this.seaLevel - 8, z0 + dz);
                if (!this.wanted(level, p)) {
                    continue;
                }
                for (int y = this.seaLevel; y > level.getMinY(); y--) {
                    p.setY(y);
                    BlockState s = level.getBlockState(p);
                    FluidState fs = s.getFluidState();
                    if (s.getBlock() instanceof LiquidBlock && !fs.isEmpty() && !fs.is(Fluids.WATER) && !fs.is(Fluids.FLOWING_WATER)
                            && !fs.is(Fluids.LAVA) && !fs.is(Fluids.FLOWING_LAVA)) {
                        level.setBlock(p, water, Block.UPDATE_CLIENTS);
                        any = true;
                    } else if (!s.isAir() && fs.isEmpty()) {
                        break; // the sea floor
                    }
                }
            }
        }
        return any;
    }
}
