package com.thesift.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thesift.entity.Swifter;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModSwifter;
import com.thesift.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * A Swifter den on the White Forest floor - a nest of fluff and twigs with a few Puffblooms
 * around it - and its family: one or two grown Swifters and one to three cubs, all homed to the
 * den (that is what makes the cubs cry and the parents rage when it is broken).
 */
public record SwifterDenFeature(int maxAdults, int maxCubs) implements Feature {
    public static final MapCodec<SwifterDenFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(1, 4).fieldOf("max_adults").forGetter(SwifterDenFeature::maxAdults),
            Codec.intRange(0, 6).fieldOf("max_cubs").forGetter(SwifterDenFeature::maxCubs)
    ).apply(i, SwifterDenFeature::new));

    @Override
    public MapCodec<SwifterDenFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockState here = level.getBlockState(origin);
        if (!(here.isAir() || here.canBeReplaced()) || !level.getFluidState(origin).isEmpty()
                || !level.getBlockState(origin.below()).is(ModTags.Blocks.SIFT_PLANTABLE)) {
            return false;
        }
        level.setBlock(origin, ModBlocks.SWIFTER_DEN.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        BlockState flower = ModBlocks.PUFFBLOOM.get().defaultBlockState();
        for (int n = 0; n < 5; n++) {
            BlockPos p = origin.offset(random.nextInt(7) - 3, 0, random.nextInt(7) - 3);
            if (!p.equals(origin) && level.getBlockState(p).isAir() && flower.canSurvive(level, p)) {
                level.setBlock(p, flower, Block.UPDATE_CLIENTS);
            }
        }
        int adults = 1 + random.nextInt(this.maxAdults);
        int cubs = this.maxCubs > 0 ? 1 + random.nextInt(this.maxCubs) : 0;
        for (int n = 0; n < adults + cubs; n++) {
            Swifter swifter = ModSwifter.SWIFTER.get().create(level.getLevel(), EntitySpawnReason.STRUCTURE);
            if (swifter == null) {
                continue;
            }
            boolean cub = n >= adults;
            double angle = random.nextDouble() * Math.PI * 2.0;
            double r = cub ? 1.2 : 2.2;
            int x = origin.getX() + (int) Math.round(Math.cos(angle) * r);
            int z = origin.getZ() + (int) Math.round(Math.sin(angle) * r);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            swifter.snapTo(x + 0.5, y, z + 0.5, random.nextFloat() * 360.0F, 0.0F);
            swifter.finalizeSpawn(level, level.getCurrentDifficultyAt(origin), EntitySpawnReason.STRUCTURE, null);
            swifter.setBaby(cub);
            swifter.setDen(origin.immutable());
            swifter.setPersistenceRequired();
            level.addFreshEntity(swifter);
        }
        return true;
    }
}
