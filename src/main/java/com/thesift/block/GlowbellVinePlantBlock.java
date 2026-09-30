package com.thesift.block;

import net.minecraft.world.level.block.BonemealSource;
import com.thesift.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

public class GlowbellVinePlantBlock extends GrowingPlantBodyBlock implements BonemealableBlock {
    private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 16.0);

    public GlowbellVinePlantBlock(BlockBehaviour.Properties properties) {
        super(properties.lightLevel(s -> s.getValue(GlowbellVineBlock.BELL) ? 13 : 2), Direction.DOWN, SHAPE, false);
        this.registerDefaultState(this.stateDefinition.any().setValue(GlowbellVineBlock.BELL, false));
    }

    @Override
    protected GrowingPlantHeadBlock getHeadBlock() {
        return ModBlocks.GLOWBELL_VINE.get();
    }

    @Override
    protected BlockState updateHeadAfterConvertedFromBody(BlockState bodyState, BlockState headState) {
        return headState.setValue(GlowbellVineBlock.BELL, bodyState.getValue(GlowbellVineBlock.BELL));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GlowbellVineBlock.BELL);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return GlowbellVineBlock.ring(state, level, pos);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        GlowbellVineBlock.spawnBellParticles(state, level, pos, random);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
        return !state.getValue(GlowbellVineBlock.BELL);
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        level.setBlock(pos, state.setValue(GlowbellVineBlock.BELL, true), Block.UPDATE_CLIENTS);
    }
}
