package com.thesift.block;

import net.minecraft.world.level.block.BonemealSource;
import com.thesift.TheSift;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Pink Sift grass. Spreads onto Sift Soil like vanilla grass, puffs pollen when walked over and
 * sprouts Blushgrass, flowers and petals when bonemealed.
 */
public class SiftGrassBlock extends SpreadingSnowyBlock implements BonemealableBlock {
    public static final ResourceKey<Block> SOIL = ResourceKey.create(Registries.BLOCK, TheSift.id("sift_soil"));

    public SiftGrassBlock(BlockBehaviour.Properties properties) {
        super(properties, SOIL);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level.isClientSide() && !entity.isSteppingCarefully() && entity.getKnownMovement().horizontalDistanceSqr() > 0.002
                && level.getRandom().nextInt(3) == 0) {
            RandomSource r = level.getRandom();
            for (int i = 0; i < 2; i++) {
                level.addParticle(ModParticles.FOOTSTEP_PUFF.get(), entity.getX() + (r.nextDouble() - 0.5) * 0.5, pos.getY() + 1.02,
                        entity.getZ() + (r.nextDouble() - 0.5) * 0.5, (r.nextDouble() - 0.5) * 0.04, 0.03, (r.nextDouble() - 0.5) * 0.04);
            }
            if (r.nextInt(3) == 0) {
                level.addParticle(ModParticles.DREAM_POLLEN.get(), entity.getX(), pos.getY() + 1.2, entity.getZ(), 0.0, 0.03, 0.0);
            }
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(90) == 0 && level.getBlockState(pos.above()).isAir()) {
            level.addParticle(ModParticles.DREAM_POLLEN.get(), pos.getX() + random.nextDouble(), pos.getY() + 1.1, pos.getZ() + random.nextDouble(),
                    0.0, 0.01, 0.0);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
        return level.getBlockState(pos.above()).isAir();
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        BlockPos above = pos.above();
        outer:
        for (int i = 0; i < 96; i++) {
            BlockPos p = above;
            for (int j = 0; j < i / 16; j++) {
                p = p.offset(random.nextInt(3) - 1, (random.nextInt(3) - 1) * random.nextInt(3) / 2, random.nextInt(3) - 1);
                if (!level.getBlockState(p.below()).is(this) || level.getBlockState(p).isCollisionShapeFullBlock(level, p)) {
                    continue outer;
                }
            }
            if (!level.getBlockState(p).isAir()) continue;
            BlockState plant;
            int roll = random.nextInt(24);
            if (roll == 0) plant = ModBlocks.DREAMBLOOM.get().defaultBlockState();
            else if (roll == 1) plant = ModBlocks.LULLABY_BELL.get().defaultBlockState();
            else if (roll == 2) plant = ModBlocks.NEBULA_IRIS.get().defaultBlockState();
            else if (roll < 5) plant = ModBlocks.CORAL_BUSH.get().defaultBlockState();
            else plant = ModBlocks.BLUSHGRASS.get().defaultBlockState();
            if (plant.canSurvive(level, p)) {
                level.setBlock(p, plant, Block.UPDATE_ALL);
            }
        }
    }
}
