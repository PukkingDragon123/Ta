package com.thesift.world.sky;

import com.thesift.block.SiftGrassBlock;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * W-sky: Sky Grass, the cyan turf of the Sky Islands. Thick and springy: a fall onto it hurts half as much, with a puff
 * of mist. Spreads onto Sift Soil like any Sift grass; bonemeal brings up Cirrus Grass, the odd Fluffbush and Chime Bell.
 */
public class SkyGrassBlock extends SiftGrassBlock {
    public SkyGrassBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        entity.causeFallDamage(fallDistance, 0.5F, level.damageSources().fall());
        if (level.isClientSide() && fallDistance > 2.0) {
            RandomSource r = level.getRandom();
            for (int i = 0; i < 6; i++) {
                level.addParticle(ModParticles.SIFT_MIST.get(), entity.getRandomX(0.8), pos.getY() + 1.05, entity.getRandomZ(0.8),
                        (r.nextDouble() - 0.5) * 0.08, 0.02, (r.nextDouble() - 0.5) * 0.08);
            }
        }
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        BlockPos above = pos.above();
        outer:
        for (int i = 0; i < 80; i++) {
            BlockPos p = above;
            for (int j = 0; j < i / 16; j++) {
                p = p.offset(random.nextInt(3) - 1, (random.nextInt(3) - 1) * random.nextInt(3) / 2, random.nextInt(3) - 1);
                if (!level.getBlockState(p.below()).is(this) || level.getBlockState(p).isCollisionShapeFullBlock(level, p)) {
                    continue outer;
                }
            }
            if (!level.getBlockState(p).isAir()) {
                continue;
            }
            int roll = random.nextInt(32);
            BlockState plant;
            if (roll == 0) {
                plant = ModBlocks.FLUFFBUSH.get().defaultBlockState();
            } else if (roll == 1) {
                plant = ModBlocks.CHIME_BELL.get().defaultBlockState();
            } else {
                plant = ModBlocks.CIRRUS_GRASS.get().defaultBlockState();
            }
            if (plant.canSurvive(level, p)) {
                level.setBlock(p, plant, Block.UPDATE_ALL);
            }
        }
    }
}
