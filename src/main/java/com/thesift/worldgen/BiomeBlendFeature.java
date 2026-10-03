package com.thesift.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * W1 World &amp; terrain: soft biome edges. Surface rules pick a ground cover per column from the
 * biome right there, so two biomes used to meet in a hard line of turf against sand or mud. Run
 * once per chunk (before any vegetation), this looks around every surface column for the nearest
 * neighbouring biome with a different ground cover and, with a chance that falls off with the
 * distance and is broken up by smooth value noise, swaps the column's top (and the soil under it)
 * for the neighbour's - a dithered, patchy band some {@code radius} blocks wide on both sides of
 * every border, instead of a cut.
 *
 * <p>{@code palettes}: biomes sharing one ground cover, its {@code top} block and the {@code under}
 * soil beneath. Only columns still showing their own biome's top are touched.
 */
public record BiomeBlendFeature(List<Palette> palettes, int radius) implements Feature {
    public record Palette(List<ResourceKey<Biome>> biomes, BlockState top, BlockState under) {
        public static final Codec<Palette> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceKey.codec(Registries.BIOME).listOf().fieldOf("biomes").forGetter(Palette::biomes),
                BlockState.CODEC.fieldOf("top").forGetter(Palette::top),
                BlockState.CODEC.fieldOf("under").forGetter(Palette::under)
        ).apply(i, Palette::new));
    }

    public static final MapCodec<BiomeBlendFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Palette.CODEC.listOf().fieldOf("palettes").forGetter(BiomeBlendFeature::palettes),
            Codec.intRange(3, 14).fieldOf("radius").forGetter(BiomeBlendFeature::radius)
    ).apply(i, BiomeBlendFeature::new));

    /** Eight compass directions, the diagonals shortened to keep the rings round. */
    private static final float[] DX = {1.0F, 0.71F, 0.0F, -0.71F, -1.0F, -0.71F, 0.0F, 0.71F};
    private static final float[] DZ = {0.0F, 0.71F, 1.0F, 0.71F, 0.0F, -0.71F, -1.0F, -0.71F};

    @Override
    public MapCodec<BiomeBlendFeature> codec() {
        return CODEC;
    }

    private int paletteAt(WorldGenLevel level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        for (int p = 0; p < this.palettes.size(); p++) {
            for (ResourceKey<Biome> key : this.palettes.get(p).biomes()) {
                if (biome.is(key)) {
                    return p;
                }
            }
        }
        return -1;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int x0 = origin.getX() & ~15;
        int z0 = origin.getZ() & ~15;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos();
        // quick look first: a chunk well inside one ground cover has nothing to blend
        int seaLevel = generator.getSeaLevel();
        int first = -2;
        boolean mixed = false;
        for (int gx = -this.radius; gx <= 15 + this.radius && !mixed; gx += 5) {
            for (int gz = -this.radius; gz <= 15 + this.radius && !mixed; gz += 5) {
                q.set(x0 + gx, seaLevel, z0 + gz);
                int pi = this.paletteAt(level, q);
                if (first == -2) {
                    first = pi;
                } else if (pi != first) {
                    mixed = true;
                }
            }
        }
        if (!mixed) {
            return false;
        }
        boolean any = false;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx;
                int z = z0 + dz;
                int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
                if (y <= level.getMinY() + 4) {
                    continue;
                }
                p.set(x, y, z);
                int own = this.paletteAt(level, p);
                if (own < 0) {
                    continue;
                }
                Palette mine = this.palettes.get(own);
                if (!level.getBlockState(p).is(mine.top().getBlock())) {
                    continue;
                }
                // the nearest neighbouring ground cover, ring by ring
                int other = -1;
                int dist = 0;
                for (int r = 2; r <= this.radius && other < 0; r += 2) {
                    for (int k = 0; k < 8; k++) {
                        q.set(x + Math.round(DX[k] * r), y, z + Math.round(DZ[k] * r));
                        int near = this.paletteAt(level, q);
                        if (near >= 0 && near != own) {
                            other = near;
                            dist = r;
                            break;
                        }
                    }
                }
                if (other < 0) {
                    continue;
                }
                float edge = 1.0F - (dist - 1.0F) / (this.radius + 1.0F);
                float chance = edge * 0.6F + (smooth(x, z) - 0.5F) * 0.95F + (smooth(x + 911, z - 377) - 0.5F) * 0.35F;
                if (hash(x, z, 7) >= chance) {
                    continue;
                }
                Palette theirs = this.palettes.get(other);
                level.setBlock(p, theirs.top(), Block.UPDATE_CLIENTS);
                for (int d = 1; d <= 3; d++) {
                    p.setY(y - d);
                    if (!level.getBlockState(p).is(mine.under().getBlock())) {
                        break;
                    }
                    level.setBlock(p, theirs.under(), Block.UPDATE_CLIENTS);
                }
                any = true;
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

    /** Smooth value noise in [0, 1) with an 8-block lattice: the blotches the blend is broken into. */
    private static float smooth(int x, int z) {
        int cx = Math.floorDiv(x, 8);
        int cz = Math.floorDiv(z, 8);
        float fx = (x - cx * 8) / 8.0F;
        float fz = (z - cz * 8) / 8.0F;
        fx = fx * fx * (3.0F - 2.0F * fx);
        fz = fz * fz * (3.0F - 2.0F * fz);
        float a = hash(cx, cz, 1);
        float b = hash(cx + 1, cz, 1);
        float c = hash(cx, cz + 1, 1);
        float d = hash(cx + 1, cz + 1, 1);
        float top = a + (b - a) * fx;
        float bottom = c + (d - c) * fx;
        return top + (bottom - top) * fz;
    }
}
