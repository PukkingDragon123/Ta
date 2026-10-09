package com.thesift.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * W-land: the Rocky Dunes' smaller rock formations, striped in the same layer-cake bands as the terrain's rock (the
 * `layers` list repeats upwards, a few blocks a band):
 * <ul>
 *   <li>{@code hoodoo} - a slim, waisted stem carrying a broad, darker cap stone (the reference's mushroom spires);</li>
 *   <li>{@code arch} - a natural arch over the sand, legs sunk in the dune;</li>
 *   <li>{@code balanced} - a squat pillar with a pale boulder balanced on top.</li>
 * </ul>
 * Everything stays within 12 blocks of the origin, so no write reaches past the neighbouring chunks.
 */
public record RockFormationFeature(List<BlockState> layers, BlockState cap, String style, int minHeight, int maxHeight) implements Feature {
    public static final MapCodec<RockFormationFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockState.CODEC.listOf().fieldOf("layers").forGetter(RockFormationFeature::layers),
            BlockState.CODEC.fieldOf("cap").forGetter(RockFormationFeature::cap),
            Codec.STRING.fieldOf("style").forGetter(RockFormationFeature::style),
            Codec.intRange(2, 32).fieldOf("min_height").forGetter(RockFormationFeature::minHeight),
            Codec.intRange(2, 32).fieldOf("max_height").forGetter(RockFormationFeature::maxHeight)
    ).apply(i, RockFormationFeature::new));

    @Override
    public MapCodec<RockFormationFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockState ground = level.getBlockState(origin.below());
        if (this.layers.isEmpty() || !level.getFluidState(origin).isEmpty() || ground.isAir() || !ground.getFluidState().isEmpty()) {
            return false;
        }
        int height = this.minHeight + random.nextInt(Math.max(1, this.maxHeight - this.minHeight + 1));
        if (origin.getY() + height + 6 >= level.getMaxY()) {
            return false;
        }
        int bandOffset = random.nextInt(this.layers.size() * 3);
        switch (this.style) {
            case "arch" -> this.arch(level, random, origin, height, bandOffset);
            case "balanced" -> this.balanced(level, random, origin, height, bandOffset);
            default -> this.hoodoo(level, random, origin, height, bandOffset);
        }
        return true;
    }

    private BlockState band(int y, int offset) {
        return this.layers.get(Math.floorMod((y + offset) / 3, this.layers.size()));
    }

    private void put(WorldGenLevel level, BlockPos p, BlockState s, boolean sink) {
        BlockState old = level.getBlockState(p);
        if (sink ? old.getDestroySpeed(level, p) >= 0.0F : (old.isAir() || old.canBeReplaced())) {
            level.setBlock(p, s, 2);
        }
    }

    /** A disc of rock at height y (radius r, squashed by the random wobble). */
    private void disc(WorldGenLevel level, BlockPos c, double r, BlockState s, boolean sink) {
        int ir = (int) Math.ceil(r);
        for (int dx = -ir; dx <= ir; dx++) {
            for (int dz = -ir; dz <= ir; dz++) {
                if (dx * dx + dz * dz <= r * r + 0.3) {
                    this.put(level, c.offset(dx, 0, dz), s, sink);
                }
            }
        }
    }

    private void hoodoo(WorldGenLevel level, RandomSource random, BlockPos origin, int height, int bandOffset) {
        double base = 1.6 + random.nextDouble() * 0.8;
        double phase = random.nextDouble() * Math.PI * 2;
        for (int y = -3; y < height; y++) {
            float t = Math.max(0, y) / (float) height;
            // flared foot, a pinched waist two thirds up, then a slight swell under the cap
            double r = base * (1.15 - 0.55 * Mth.sin((float) (t * Math.PI * 0.75))) + 0.25 * Mth.sin((float) (y * 0.9 + phase));
            BlockPos c = origin.offset(0, y, 0);
            this.disc(level, c, Math.max(0.9, r), this.band(c.getY(), bandOffset), y < 0);
        }
        double capR = base + 1.6 + random.nextDouble() * 1.2;
        int capH = 2 + random.nextInt(2);
        for (int y = 0; y < capH; y++) {
            double r = capR - (y == capH - 1 ? 0.9 : 0.0) - (y == 0 ? 0.4 : 0.0);
            this.disc(level, origin.offset(0, height + y, 0), r, this.cap, false);
        }
    }

    private void arch(WorldGenLevel level, RandomSource random, BlockPos origin, int height, int bandOffset) {
        boolean alongX = random.nextBoolean();
        double half = 4.0 + random.nextDouble() * 2.5;
        double thick = 1.3 + random.nextDouble() * 0.5;
        int steps = 48;
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            double a = -half + 2 * half * t;
            double y = height * Math.sin(Math.PI * t);
            int ax = (int) Math.round(alongX ? a : 0);
            int az = (int) Math.round(alongX ? 0 : a);
            BlockPos c = origin.offset(ax, (int) Math.round(y), az);
            // the span thickens towards the legs, which run down into the sand
            double r = thick + 0.8 * (1.0 - Math.sin(Math.PI * t));
            int legDown = (t < 0.18 || t > 0.82) ? 4 : 0;
            for (int dy = -legDown; dy <= 0; dy++) {
                BlockPos p = c.offset(0, dy, 0);
                this.sphereSlice(level, p, r, alongX, bandOffset, dy < 0 || p.getY() <= origin.getY());
            }
        }
        // a cap of the paler stone along the top of the span
        for (int i = 8; i <= steps - 8; i++) {
            double t = i / (double) steps;
            double a = -half + 2 * half * t;
            int y = (int) Math.round(height * Math.sin(Math.PI * t) + thick);
            this.put(level, origin.offset((int) Math.round(alongX ? a : 0), y, (int) Math.round(alongX ? 0 : a)), this.cap, false);
        }
    }

    private void sphereSlice(WorldGenLevel level, BlockPos c, double r, boolean alongX, int bandOffset, boolean sink) {
        int ir = (int) Math.ceil(r);
        for (int dy = -ir; dy <= ir; dy++) {
            for (int dw = -ir; dw <= ir; dw++) {
                if (dy * dy + dw * dw <= r * r + 0.2) {
                    BlockPos p = alongX ? c.offset(0, dy, dw) : c.offset(dw, dy, 0);
                    this.put(level, p, this.band(p.getY(), bandOffset), sink);
                }
            }
        }
    }

    private void balanced(WorldGenLevel level, RandomSource random, BlockPos origin, int height, int bandOffset) {
        double r = 1.0 + random.nextDouble() * 0.6;
        for (int y = -2; y < height; y++) {
            BlockPos c = origin.offset(0, y, 0);
            this.disc(level, c, r + (y < 1 ? 0.7 : 0.0), this.band(c.getY(), bandOffset), y < 0);
        }
        double rx = 2.2 + random.nextDouble() * 1.3;
        double ry = 1.6 + random.nextDouble() * 0.8;
        double rz = 2.0 + random.nextDouble() * 1.3;
        BlockPos top = origin.offset(random.nextInt(3) - 1, height + (int) Math.ceil(ry) - 1, random.nextInt(3) - 1);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    if ((dx * dx) / (rx * rx) + (dy * dy) / (ry * ry) + (dz * dz) / (rz * rz) <= 1.0) {
                        this.put(level, top.offset(dx, dy, dz), this.cap, false);
                    }
                }
            }
        }
    }
}
