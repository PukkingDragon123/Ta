package com.thesift.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

/**
 * The Sift's seas are Chrome; only its true oceans (the Magic Kelp Forest, the Brass Coral Reef and the Sculk Ocean) are
 * filled with real, swimmable water, and the Sculk Swamp's hollows with Sculk Water.
 * Runs once per chunk before anything else is decorated: in every column whose sea-level biome is
 * one of {@code biomes}, the open Chrome from the sea surface down to the sea floor becomes
 * {@code fluid} (water unless given). Buried Chrome aquifers are left alone.
 *
 * <p>W-sea: with a {@code blend} radius the two liquids no longer meet in a sheer wall along the biome line. Chrome is the
 * heavier liquid, so near the shore between a water sea and a Chrome sea the Chrome sinks under the water: each column gets
 * water from the surface down to a depth set by how much of the climate within {@code blend} blocks is water sea (with a
 * little smooth noise so the edge wanders), and keeps its Chrome below. The water thins out over the Chrome side and the
 * Chrome bed thins out under the water side, a gentle slope instead of a seam.
 */
public record SeaFloodFeature(List<ResourceKey<Biome>> biomes, int seaLevel, BlockState fluid, int blend) implements Feature {
    public static final MapCodec<SeaFloodFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceKey.codec(Registries.BIOME).listOf().fieldOf("biomes").forGetter(SeaFloodFeature::biomes),
            Codec.INT.fieldOf("sea_level").forGetter(SeaFloodFeature::seaLevel),
            BlockState.CODEC.optionalFieldOf("fluid", Blocks.WATER.defaultBlockState()).forGetter(SeaFloodFeature::fluid),
            Codec.intRange(0, 16).optionalFieldOf("blend", 0).forGetter(SeaFloodFeature::blend)
    ).apply(i, SeaFloodFeature::new));

    /** Spacing of the coarse climate grid the blend reads, in blocks. */
    private static final int GRID = 4;

    @Override
    public MapCodec<SeaFloodFeature> codec() {
        return CODEC;
    }

    private boolean wanted(WorldGenLevel level, BlockPos pos) {
        return this.wanted(level.getBiome(pos));
    }

    private boolean wanted(Holder<Biome> biome) {
        for (ResourceKey<Biome> key : this.biomes) {
            if (biome.is(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (this.blend > 0) {
            return this.placeBlended(level, origin);
        }
        int x0 = origin.getX() & ~15;
        int z0 = origin.getZ() & ~15;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        boolean any = false;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                p.set(x0 + dx, this.seaLevel - 8, z0 + dz);
                if (!this.wanted(level, p)) {
                    continue;
                }
                any |= this.flood(level, p, x0 + dx, z0 + dz, Integer.MAX_VALUE);
            }
        }
        return any;
    }

    /**
     * Floods one column from the sea surface down: at most {@code layers} blocks of liquid (the rest keeps its Chrome), and
     * never past the sea floor.
     */
    private boolean flood(WorldGenLevel level, BlockPos.MutableBlockPos p, int x, int z, int layers) {
        BlockState flood = this.fluid;
        Fluid target = flood.getFluidState().getType();
        boolean any = false;
        int done = 0;
        for (int y = this.seaLevel; y > level.getMinY() && done < layers; y--) {
            p.set(x, y, z);
            BlockState s = level.getBlockState(p);
            FluidState fs = s.getFluidState();
            if (s.getBlock() instanceof LiquidBlock && !fs.isEmpty() && !fs.getType().isSame(target) && !fs.is(FluidTags.LAVA)) {
                level.setBlock(p, flood, Block.UPDATE_CLIENTS);
                any = true;
                done++;
            } else if (!s.isAir() && fs.isEmpty()) {
                break; // the sea floor
            } else {
                done++;
            }
        }
        return any;
    }

    /** Depth of open liquid (or air) in a column below the sea surface, down to the sea floor. */
    private int depth(WorldGenLevel level, BlockPos.MutableBlockPos p, int x, int z) {
        int d = 0;
        for (int y = this.seaLevel; y > level.getMinY(); y--) {
            p.set(x, y, z);
            BlockState s = level.getBlockState(p);
            if (!s.isAir() && s.getFluidState().isEmpty()) {
                break;
            }
            d++;
        }
        return d;
    }

    private boolean placeBlended(WorldGenLevel level, BlockPos origin) {
        int x0 = origin.getX() & ~15;
        int z0 = origin.getZ() & ~15;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        // a coarse grid of the climate around the chunk, one cell per biome quart (the same cells, read the same way, from
        // every chunk, so neighbouring chunks agree): is each cell a water sea?
        int margin = this.blend + GRID;
        int gx0 = Math.floorDiv(x0 - margin, GRID);
        int gz0 = Math.floorDiv(z0 - margin, GRID);
        int n = (16 + 2 * margin) / GRID;
        int qy = QuartPos.fromBlock(this.seaLevel - 8);
        boolean[] grid = new boolean[n * n];
        int wet = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (this.wanted(level.getNoiseBiome(gx0 + i, qy, gz0 + j))) {
                    grid[i * n + j] = true;
                    wet++;
                }
            }
        }
        if (wet == 0) {
            return false; // no water sea anywhere near
        }
        boolean all = wet == n * n;
        float r = this.blend;
        boolean any = false;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx;
                int z = z0 + dz;
                float t;
                if (all) {
                    t = 1.0F;
                } else {
                    // the share of water sea in a soft disc round the column
                    float sum = 0.0F;
                    float weight = 0.0F;
                    for (int i = 0; i < n; i++) {
                        float cx = (gx0 + i) * GRID + GRID / 2 - x;
                        for (int j = 0; j < n; j++) {
                            float cz = (gz0 + j) * GRID + GRID / 2 - z;
                            float d = Mth.sqrt(cx * cx + cz * cz);
                            if (d >= r) {
                                continue;
                            }
                            float w = 1.0F - d / r;
                            weight += w;
                            if (grid[i * n + j]) {
                                sum += w;
                            }
                        }
                    }
                    float share = weight > 0.0F ? sum / weight : 0.0F;
                    // smooth noise, so the bed's edge wanders instead of tracing the climate's lines
                    share += (smooth(x, z) - 0.5F) * 0.24F + (smooth(x + 517, z - 211) - 0.5F) * 0.1F;
                    t = Mth.clamp((share - 0.5F) * 1.7F + 0.5F, 0.0F, 1.0F);
                    t = t * t * (3.0F - 2.0F * t);
                }
                if (t <= 0.0F) {
                    continue;
                }
                int layers = Integer.MAX_VALUE;
                if (t < 1.0F) {
                    int depth = this.depth(level, p, x, z);
                    layers = Math.round(t * depth);
                    if (layers <= 0) {
                        continue;
                    }
                }
                any |= this.flood(level, p, x, z, layers);
            }
        }
        return any;
    }

    /** A stable pseudo-random value in [0, 1) for a column. */
    private static float hash(int x, int z, int salt) {
        long h = x * 341873128712L + z * 132897987541L + salt * 42317861L;
        h = (h ^ (h >>> 29)) * 0x5DEECE66DL;
        h ^= h >>> 32;
        h *= 0x9E3779B97F4A7C15L;
        h ^= h >>> 31;
        return (h >>> 40) / (float) (1L << 24);
    }

    /** Smooth value noise in [0, 1) on a 9-block lattice. */
    private static float smooth(int x, int z) {
        int cx = Math.floorDiv(x, 9);
        int cz = Math.floorDiv(z, 9);
        float fx = (x - cx * 9) / 9.0F;
        float fz = (z - cz * 9) / 9.0F;
        fx = fx * fx * (3.0F - 2.0F * fx);
        fz = fz * fz * (3.0F - 2.0F * fz);
        float a = hash(cx, cz, 3);
        float b = hash(cx + 1, cz, 3);
        float c = hash(cx, cz + 1, 3);
        float d = hash(cx + 1, cz + 1, 3);
        float top = a + (b - a) * fx;
        float bottom = c + (d - c) * fx;
        return top + (bottom - top) * fz;
    }
}
