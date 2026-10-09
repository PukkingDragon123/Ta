package com.thesift.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * W-deep caves: no grass in caves. The surface rules lay the grass and turf of a surface biome on every floor above
 * the preliminary surface - including the floors of caves just under the ground and of caves inside mountains whose
 * real surface towers over the estimate. Run once per chunk before anything else is decorated, this walks every column
 * down from the top of the ground and turns any {@code cover} block lying on the floor of a roofed pocket (a roof at
 * least {@code roof} blocks thick, a pocket no taller than {@code max_gap}) into {@code floor} stone. Open-sky ground,
 * the land under the floating islands (too far below them) and beaches are left alone.
 */
public record CaveScrubFeature(List<Block> cover, BlockState floor, int roof, int maxGap, int depth) implements Feature {
    public static final MapCodec<CaveScrubFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BuiltInRegistries.BLOCK.byNameCodec().listOf().fieldOf("cover").forGetter(CaveScrubFeature::cover),
            BlockState.CODEC.fieldOf("floor").forGetter(CaveScrubFeature::floor),
            Codec.intRange(1, 16).fieldOf("roof").forGetter(CaveScrubFeature::roof),
            Codec.intRange(1, 64).fieldOf("max_gap").forGetter(CaveScrubFeature::maxGap),
            Codec.intRange(8, 256).fieldOf("depth").forGetter(CaveScrubFeature::depth)
    ).apply(i, CaveScrubFeature::new));

    @Override
    public MapCodec<CaveScrubFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int x0 = origin.getX() & ~15;
        int z0 = origin.getZ() & ~15;
        int minY = level.getMinY() + 1;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        boolean any = false;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx;
                int z = z0 + dz;
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                int bottom = Math.max(minY, top - this.depth);
                int solid = 0;
                int roofAbove = 0;
                int gap = 0;
                for (int y = top; y >= bottom; y--) {
                    p.set(x, y, z);
                    BlockState state = level.getBlockState(p);
                    if (state.getCollisionShape(level, p).isEmpty()) {
                        // open: air, water, a plant - the roof above this pocket is however much rock we just came through
                        if (gap == 0) {
                            roofAbove = solid;
                        }
                        gap++;
                        solid = 0;
                        continue;
                    }
                    if (gap > 0 && roofAbove >= this.roof && gap <= this.maxGap && this.cover.contains(state.getBlock())) {
                        level.setBlock(p, this.floor, Block.UPDATE_CLIENTS);
                        any = true;
                    }
                    gap = 0;
                    solid++;
                }
            }
        }
        return any;
    }
}
