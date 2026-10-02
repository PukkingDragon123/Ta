package com.thesift.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * The enormous broken rock pillars of the Deep Dark Ocean. Each rises from the sea floor with a
 * flared root, a star-shaped, jagged cross-section that twists as it climbs, ledges where it
 * steps in, and a crown of splintered spires; it may break the surface. Sculk creeps over every
 * upward face, and here and there a catalyst glows in the rock.
 */
public record JaggedPillarFeature(BlockState body, BlockState accent, BlockState coat, BlockState glow, int minHeight, int maxHeight,
                                  float radius) implements Feature {
    public static final MapCodec<JaggedPillarFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockState.CODEC.fieldOf("body").forGetter(JaggedPillarFeature::body),
            BlockState.CODEC.fieldOf("accent").forGetter(JaggedPillarFeature::accent),
            BlockState.CODEC.fieldOf("coat").forGetter(JaggedPillarFeature::coat),
            BlockState.CODEC.fieldOf("glow").forGetter(JaggedPillarFeature::glow),
            Codec.intRange(4, 160).fieldOf("min_height").forGetter(JaggedPillarFeature::minHeight),
            Codec.intRange(4, 160).fieldOf("max_height").forGetter(JaggedPillarFeature::maxHeight),
            Codec.floatRange(1.0F, 9.0F).fieldOf("radius").forGetter(JaggedPillarFeature::radius)
    ).apply(i, JaggedPillarFeature::new));

    private static final int LOBES = 7;

    @Override
    public MapCodec<JaggedPillarFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int height = this.minHeight + random.nextInt(Math.max(1, this.maxHeight - this.minHeight + 1));
        if (origin.getY() + height + 12 >= level.getMaxY()) {
            height = level.getMaxY() - origin.getY() - 13;
            if (height < this.minHeight) {
                return false;
            }
        }
        float baseR = this.radius * (0.75F + random.nextFloat() * 0.5F);
        float[] amp = new float[LOBES];
        float[] phase = new float[LOBES];
        for (int k = 0; k < LOBES; k++) {
            amp[k] = 0.15F + random.nextFloat() * 0.4F;
            phase[k] = random.nextFloat() * Mth.TWO_PI;
        }
        float twist = (random.nextFloat() - 0.5F) * 0.08F;
        double leanX = (random.nextDouble() - 0.5) * 0.25;
        double leanZ = (random.nextDouble() - 0.5) * 0.25;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        float ledge = 1.0F;
        for (int y = -4; y < height; y++) {
            float t = Math.max(0, y) / (float) height;
            if (y > 0 && random.nextInt(9) == 0) {
                ledge = 0.82F + random.nextFloat() * 0.18F; // the column steps in
            }
            // flared root, a long rough shaft, narrowing to the crown
            float r = baseR * ledge * (1.0F + 0.6F * Math.max(0.0F, 1.0F - t * 8.0F)) * (1.0F - 0.45F * t * t);
            double cx = leanX * y;
            double cz = leanZ * y;
            int ir = (int) Math.ceil(r * 1.6F) + 1;
            for (int dx = -ir; dx <= ir; dx++) {
                for (int dz = -ir; dz <= ir; dz++) {
                    double ddx = dx - cx;
                    double ddz = dz - cz;
                    float a = (float) Mth.atan2(ddz, ddx) + twist * y;
                    float shape = 1.0F;
                    for (int k = 0; k < LOBES; k++) {
                        shape += amp[k] * Math.max(0.0F, Mth.sin(a * (k % 3 + 2) + phase[k])) * 0.5F;
                    }
                    float rr = r * shape;
                    double d2 = ddx * ddx + ddz * ddz;
                    if (d2 > rr * rr) {
                        continue;
                    }
                    p.set(origin.getX() + dx, origin.getY() + y, origin.getZ() + dz);
                    BlockState existing = level.getBlockState(p);
                    if (y < 0 && !existing.isAir() && existing.getFluidState().isEmpty()) {
                        continue; // only fill the gaps under the root
                    }
                    BlockState s = this.body;
                    if ((y + (int) (a * 3.0F)) % 7 == 0 || random.nextInt(9) == 0) {
                        s = this.accent;
                    }
                    if (d2 > (rr - 1.2) * (rr - 1.2) && random.nextInt(140) == 0) {
                        s = this.glow;
                    }
                    level.setBlock(p, s, Block.UPDATE_CLIENTS);
                }
            }
        }
        // the splintered crown: a few leaning spikes rising out of the top
        int spikes = 2 + random.nextInt(3);
        int top = height - 1;
        for (int k = 0; k < spikes; k++) {
            float a = random.nextFloat() * Mth.TWO_PI;
            float off = baseR * 0.45F * random.nextFloat();
            double sx = leanX * top + Mth.cos(a) * off;
            double sz = leanZ * top + Mth.sin(a) * off;
            int len = 4 + random.nextInt(10);
            float sr = 1.0F + random.nextFloat() * baseR * 0.3F;
            for (int j = 0; j < len; j++) {
                float rr = sr * (1.0F - j / (float) len) + 0.35F;
                sx += Mth.cos(a) * 0.25;
                sz += Mth.sin(a) * 0.25;
                int ir = (int) Math.ceil(rr);
                for (int dx = -ir; dx <= ir; dx++) {
                    for (int dz = -ir; dz <= ir; dz++) {
                        if ((dx - (sx - Math.floor(sx))) * (dx - (sx - Math.floor(sx))) + (dz - (sz - Math.floor(sz))) * (dz - (sz - Math.floor(sz))) <= rr * rr) {
                            p.set(origin.getX() + (int) Math.floor(sx) + dx, origin.getY() + top + j, origin.getZ() + (int) Math.floor(sz) + dz);
                            level.setBlock(p, j % 4 == 3 ? this.accent : this.body, Block.UPDATE_CLIENTS);
                        }
                    }
                }
            }
        }
        // sculk creeps over every face that looks up into the water
        int span = (int) Math.ceil(baseR * 2.6F) + 4;
        for (int dx = -span; dx <= span; dx++) {
            for (int dz = -span; dz <= span; dz++) {
                for (int y = height + 14; y >= -4; y--) {
                    p.set(origin.getX() + dx, origin.getY() + y, origin.getZ() + dz);
                    BlockState s = level.getBlockState(p);
                    if (s == this.body || s == this.accent) {
                        BlockState above = level.getBlockState(p.above());
                        if ((above.isAir() || !above.getFluidState().isEmpty()) && random.nextInt(3) != 0) {
                            level.setBlock(p, this.coat, Block.UPDATE_CLIENTS);
                        }
                    }
                }
            }
        }
        return true;
    }
}
