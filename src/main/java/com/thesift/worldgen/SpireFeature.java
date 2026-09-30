package com.thesift.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Tall, gently twisting rock spires with coloured strata bands and an optional grassy cap. Used for
 * the hoodoos of the Rocky Dunes and the crags of the Forest Mountains.
 */
public record SpireFeature(BlockState body, BlockState band, Optional<BlockState> cap, int minHeight, int maxHeight, float radius)
        implements Feature {
    public static final MapCodec<SpireFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockState.CODEC.fieldOf("body").forGetter(SpireFeature::body),
            BlockState.CODEC.fieldOf("band").forGetter(SpireFeature::band),
            BlockState.CODEC.optionalFieldOf("cap").forGetter(SpireFeature::cap),
            Codec.intRange(4, 64).fieldOf("min_height").forGetter(SpireFeature::minHeight),
            Codec.intRange(4, 64).fieldOf("max_height").forGetter(SpireFeature::maxHeight),
            Codec.floatRange(1.0F, 7.0F).fieldOf("radius").forGetter(SpireFeature::radius)
    ).apply(i, SpireFeature::new));

    @Override
    public MapCodec<SpireFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int height = this.minHeight + random.nextInt(Math.max(1, this.maxHeight - this.minHeight + 1));
        if (origin.getY() + height >= level.getMaxY()) {
            return false;
        }
        float baseR = this.radius * (0.8F + random.nextFloat() * 0.4F);
        double phase = random.nextDouble() * Math.PI * 2;
        double swayAmp = 0.8 + random.nextDouble() * 1.4;
        int bandEvery = 3 + random.nextInt(3);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        double cx = 0;
        double cz = 0;
        // Sink the base a little so it doesn't float on slopes.
        for (int y = -3; y < height; y++) {
            float t = Math.max(0, y) / (float) height;
            // Bulgy hoodoo profile: wide base, pinched waist, knobbly top.
            float r = baseR * (1.0F - 0.65F * t) + Mth.sin(t * 9.0F + (float) phase) * 0.35F;
            if (y > height - 3) {
                r = Math.max(r, baseR * 0.55F);
            }
            cx = Math.sin(t * 2.2 + phase) * swayAmp * t;
            cz = Math.cos(t * 1.7 + phase) * swayAmp * t;
            boolean isBand = y > 0 && (y % bandEvery == 0);
            int ir = (int) Math.ceil(r) + 1;
            for (int dx = -ir; dx <= ir; dx++) {
                for (int dz = -ir; dz <= ir; dz++) {
                    double ddx = dx - cx;
                    double ddz = dz - cz;
                    double d2 = ddx * ddx + ddz * ddz;
                    if (d2 <= r * r) {
                        p.set(origin.getX() + dx, origin.getY() + y, origin.getZ() + dz);
                        BlockState existing = level.getBlockState(p);
                        if (y < 0 && !existing.isAir() && !existing.canBeReplaced()) {
                            continue;
                        }
                        boolean edge = d2 > (r - 1) * (r - 1);
                        BlockState s = isBand && (edge || random.nextInt(3) == 0) ? this.band : this.body;
                        level.setBlock(p, s, 2);
                    }
                }
            }
        }
        if (this.cap.isPresent()) {
            int top = origin.getY() + height - 1;
            int ir = (int) Math.ceil(baseR);
            for (int dx = -ir; dx <= ir; dx++) {
                for (int dz = -ir; dz <= ir; dz++) {
                    p.set(origin.getX() + dx + (int) Math.round(cx), top, origin.getZ() + dz + (int) Math.round(cz));
                    for (int k = 0; k < 3; k++) {
                        if (!level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()) {
                            level.setBlock(p, this.cap.get(), 2);
                            break;
                        }
                        p.move(0, -1, 0);
                    }
                }
            }
        }
        return true;
    }
}
