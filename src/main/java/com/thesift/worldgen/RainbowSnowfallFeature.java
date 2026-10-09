package com.thesift.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModSwifter;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * W-land: Rainbow Snow settling over the White Forest (top-layer step, like vanilla's freeze_top_layer): drifts one to
 * `max_layers` deep in soft patches across the ground, a dusting on every treetop, bare hollows in between. Runs once
 * per chunk over its 16x16 columns, checking each column's biome.
 */
public record RainbowSnowfallFeature(int maxLayers) implements Feature {
    public static final MapCodec<RainbowSnowfallFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(1, 7).fieldOf("max_layers").forGetter(RainbowSnowfallFeature::maxLayers)
    ).apply(i, RainbowSnowfallFeature::new));

    @Override
    public MapCodec<RainbowSnowfallFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockState snow = ModBlocks.RAINBOW_SNOW.get().defaultBlockState();
        int x0 = origin.getX() & ~15;
        int z0 = origin.getZ() & ~15;
        long seed = level.getSeed() * 31L + 7L;
        boolean any = false;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx;
                int z = z0 + dz;
                p.set(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
                if (!level.getBiome(p).is(ModSwifter.WHITE_FOREST) || !level.isEmptyBlock(p) || !level.getFluidState(p.below()).isEmpty()) {
                    continue;
                }
                BlockState below = level.getBlockState(p.below());
                boolean canopy = below.is(net.minecraft.tags.BlockTags.LEAVES);
                double n = drift(seed, x, z);
                int layers = canopy ? (n > 0.3 ? 1 : 0) : (n < 0.32 ? 0 : 1 + (int) Mth.clamp((n - 0.32) / 0.68 * this.maxLayers, 0, this.maxLayers - 1));
                if (layers <= 0 || !snow.canSurvive(level, p)) {
                    continue;
                }
                level.setBlock(p, snow.setValue(SnowLayerBlock.LAYERS, layers), 2);
                if (below.hasProperty(BlockStateProperties.SNOWY)) {
                    level.setBlock(p.below(), below.setValue(BlockStateProperties.SNOWY, true), 2);
                }
                any = true;
            }
        }
        return any;
    }

    /** Smooth 0..1 value noise (two octaves) so drifts gather in soft patches, the same in every chunk seam. */
    private static double drift(long seed, int x, int z) {
        return 0.65 * value(seed, x / 9.0, z / 9.0) + 0.35 * value(seed + 101L, x / 3.5, z / 3.5);
    }

    private static double value(long seed, double x, double z) {
        int ix = Mth.floor(x);
        int iz = Mth.floor(z);
        double fx = x - ix;
        double fz = z - iz;
        double sx = fx * fx * (3 - 2 * fx);
        double sz = fz * fz * (3 - 2 * fz);
        double a = Mth.lerp(sx, hash(seed, ix, iz), hash(seed, ix + 1, iz));
        double b = Mth.lerp(sx, hash(seed, ix, iz + 1), hash(seed, ix + 1, iz + 1));
        return Mth.lerp(sz, a, b);
    }

    private static double hash(long seed, int x, int z) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL);
        h = (h ^ (h >>> 31)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 29)) * 0x94D049BB133111EBL;
        h ^= h >>> 32;
        return (h & 0xFFFFFFL) / (double) 0x1000000L;
    }
}
