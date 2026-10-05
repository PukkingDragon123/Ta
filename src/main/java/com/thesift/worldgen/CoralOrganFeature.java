package com.thesift.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thesift.entity.CoralOrgan;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModSculkSea;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * CR3 Fish &amp; Coral Organs: a Sculk Coral Organ rooted on the Sculk Ocean floor, in open water no deeper than
 * {@code maxDepth} (so its line can reach a swimmer at the surface), with a little thicket of sculk coral grown
 * up round its foot.
 */
public record CoralOrganFeature(int maxDepth) implements Feature {
    public static final MapCodec<CoralOrganFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(4, 96).fieldOf("max_depth").forGetter(CoralOrganFeature::maxDepth)
    ).apply(i, CoralOrganFeature::new));

    @Override
    public MapCodec<CoralOrganFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (!level.getFluidState(origin).is(FluidTags.WATER) || !level.getBlockState(origin.below()).isFaceSturdy(level, origin.below(), Direction.UP)) {
            return false;
        }
        // room to stand: two blocks of open water over a 3 x 3 patch
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-1, 0, -1), origin.offset(1, 2, 1))) {
            if (!level.getFluidState(p).is(FluidTags.WATER) || !level.getBlockState(p).getCollisionShape(level, p).isEmpty()) {
                return false;
            }
        }
        int depth = 0;
        BlockPos.MutableBlockPos up = origin.mutable();
        while (depth <= this.maxDepth && level.getFluidState(up).is(FluidTags.WATER)) {
            up.move(Direction.UP);
            depth++;
        }
        if (depth > this.maxDepth || depth < 6) {
            return false;
        }
        CoralOrgan organ = ModSculkSea.CORAL_ORGAN.get().create(level.getLevel(), EntitySpawnReason.STRUCTURE);
        if (organ == null) {
            return false;
        }
        organ.snapTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
        organ.finalizeSpawn(level, level.getCurrentDifficultyAt(origin), EntitySpawnReason.STRUCTURE, null);
        organ.setPersistenceRequired();
        level.addFreshEntity(organ);
        // its own reef: sculk coral and fans sprouting round its foot, a coral block or two under them
        BlockState[] growth = {ModBlocks.SCULK_CORAL.get().defaultBlockState(), ModBlocks.SCULK_CORAL_FAN.get().defaultBlockState()};
        for (int n = 0; n < 14; n++) {
            int dx = random.nextInt(7) - 3;
            int dz = random.nextInt(7) - 3;
            if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                continue;
            }
            int x = origin.getX() + dx;
            int z = origin.getZ() + dz;
            BlockPos p = new BlockPos(x, level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z), z);
            if (Math.abs(p.getY() - origin.getY()) > 3 || !level.getFluidState(p).is(FluidTags.WATER) || !level.getBlockState(p).isAir()
                    && !level.getBlockState(p).canBeReplaced()) {
                continue;
            }
            if (random.nextInt(4) == 0 && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)) {
                level.setBlock(p.below(), ModBlocks.SCULK_CORAL_BLOCK.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            BlockState plant = growth[random.nextInt(growth.length)];
            if (plant.canSurvive(level, p)) {
                level.setBlock(p, plant, Block.UPDATE_CLIENTS);
            }
        }
        return true;
    }
}
