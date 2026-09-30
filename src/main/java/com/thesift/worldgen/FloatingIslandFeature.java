package com.thesift.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * A dreamy chunk of land drifting in the sky: an upside-down, craggy cone with a grassy top, hanging
 * roots of stone and an optional decoration (usually a tree) growing on it.
 */
public record FloatingIslandFeature(BlockState top, BlockState soil, BlockState stone, int minRadius, int maxRadius, int minLift, int maxLift,
        Optional<Holder<Feature>> decoration) implements Feature {
    public static final MapCodec<FloatingIslandFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockState.CODEC.fieldOf("top").forGetter(FloatingIslandFeature::top),
            BlockState.CODEC.fieldOf("soil").forGetter(FloatingIslandFeature::soil),
            BlockState.CODEC.fieldOf("stone").forGetter(FloatingIslandFeature::stone),
            Codec.intRange(2, 12).fieldOf("min_radius").forGetter(FloatingIslandFeature::minRadius),
            Codec.intRange(2, 12).fieldOf("max_radius").forGetter(FloatingIslandFeature::maxRadius),
            Codec.intRange(0, 128).fieldOf("min_lift").forGetter(FloatingIslandFeature::minLift),
            Codec.intRange(0, 128).fieldOf("max_lift").forGetter(FloatingIslandFeature::maxLift),
            Feature.CODEC.optionalFieldOf("decoration").forGetter(FloatingIslandFeature::decoration)
    ).apply(i, FloatingIslandFeature::new));

    @Override
    public MapCodec<FloatingIslandFeature> codec() {
        return CODEC;
    }

    @Override
    public Stream<Holder<Feature>> getSubFeatures() {
        return this.decoration.stream();
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int lift = this.minLift + random.nextInt(Math.max(1, this.maxLift - this.minLift + 1));
        BlockPos center = origin.above(lift);
        int radius = this.minRadius + random.nextInt(Math.max(1, this.maxRadius - this.minRadius + 1));
        int depth = radius + 2 + random.nextInt(radius + 1);
        if (center.getY() + 4 >= level.getMaxY() || center.getY() - depth <= level.getMinY()) {
            return false;
        }
        // Keep clear of terrain: the island should really float.
        for (int y = -depth - 2; y <= 3; y += 2) {
            if (!level.getBlockState(center.above(y)).isAir()) {
                return false;
            }
        }
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        double wobble = random.nextDouble() * Math.PI * 2;
        for (int dx = -radius - 1; dx <= radius + 1; dx++) {
            for (int dz = -radius - 1; dz <= radius + 1; dz++) {
                double ang = Math.atan2(dz, dx);
                double edge = radius + Math.sin(ang * 3 + wobble) * 0.9 + Math.cos(ang * 5 - wobble) * 0.5;
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > edge) {
                    continue;
                }
                double frac = 1.0 - d / edge;
                int colDepth = (int) Math.round(1 + frac * depth + random.nextInt(2));
                for (int y = 0; y >= -colDepth; y--) {
                    p.set(center.getX() + dx, center.getY() + y, center.getZ() + dz);
                    if (!level.getBlockState(p).isAir()) {
                        continue;
                    }
                    BlockState s = y == 0 ? this.top : y > -3 ? this.soil : this.stone;
                    level.setBlock(p, s, 2);
                }
                // Occasional dangling stone "roots".
                if (frac > 0.3 && random.nextInt(9) == 0) {
                    int len = 1 + random.nextInt(3);
                    for (int k = 1; k <= len; k++) {
                        p.set(center.getX() + dx, center.getY() - colDepth - k, center.getZ() + dz);
                        if (level.getBlockState(p).isAir()) {
                            level.setBlock(p, this.stone, 2);
                        }
                    }
                }
            }
        }
        this.decoration.ifPresent(f -> f.value().place(level, generator, random, center.above()));
        return true;
    }
}
