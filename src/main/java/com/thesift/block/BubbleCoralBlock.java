package com.thesift.block;

import com.thesift.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * W-sea: a Bubble Coral of the Chrome Coral Ocean (rose, amber, lime, azure, violet or pearl) - a clump of glassy coral
 * bubbles on the sea floor. Unlike vanilla coral it never dies: it lives in water, in Chrome, or dry on a shelf, so it is a
 * building block like any other. Holds water or Chrome ({@link SeaLogging}).
 */
public class BubbleCoralBlock extends Block implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = SeaLogging.WATERLOGGED;
    public static final BooleanProperty CHROMELOGGED = SeaLogging.CHROMELOGGED;
    private static final VoxelShape SHAPE = Block.column(12.0, 0.0, 15.0);

    public BubbleCoralBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, false).setValue(CHROMELOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED, CHROMELOGGED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = SeaLogging.inFluidAt(this.defaultBlockState(), context.getLevel(), context.getClickedPos());
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        SeaLogging.tick(state, level, ticks, pos);
        if (this.supportedFrom(state) == direction && !state.canSurvive(level, pos)) {
            return SeaLogging.remains(state);
        }
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    /** The side this coral grows from. */
    protected Direction supportedFrom(BlockState state) {
        return Direction.DOWN;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return SeaLogging.fluid(state);
    }

    @Override
    public boolean canPlaceLiquid(@Nullable LivingEntity user, BlockGetter level, BlockPos pos, BlockState state, Fluid type) {
        return !SeaLogging.chromelogged(state) && SimpleWaterloggedBlock.super.canPlaceLiquid(user, level, pos, state, type);
    }

    /** Its bubbles let go now and then and drift up. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(16) != 0) {
            return;
        }
        double x = pos.getX() + 0.25 + random.nextDouble() * 0.5;
        double y = pos.getY() + 0.3 + random.nextDouble() * 0.4;
        double z = pos.getZ() + 0.25 + random.nextDouble() * 0.5;
        if (SeaLogging.chromelogged(state)) {
            level.addParticle(ModParticles.CHROME_BUBBLE.get(), x, y, z, 0.0, 0.02, 0.0);
        } else if (SeaLogging.waterlogged(state)) {
            level.addParticle(ParticleTypes.BUBBLE, x, y, z, 0.0, 0.04, 0.0);
        }
    }
}
