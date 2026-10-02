package com.thesift.block;

import com.thesift.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Glowkelp of the Magic Kelp Forest: tall, softly glowing ribbons in rose, azure or amber that
 * stack into columns like kelp. The top segment carries a luminous bulb. Unlike kelp it is
 * happy out of the water too, so it can be planted anywhere; it is water-loggable.
 */
public class GlowKelpBlock extends Block implements SimpleWaterloggedBlock {
    public static final BooleanProperty TIP = BooleanProperty.create("tip");
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape SHAPE = Block.column(10.0, 0.0, 16.0);

    public GlowKelpBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(TIP, true).setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TIP, WATERLOGGED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        FluidState fluid = context.getLevel().getFluidState(pos);
        BlockState s = this.defaultBlockState().setValue(WATERLOGGED, fluid.is(Fluids.WATER))
                .setValue(TIP, !(context.getLevel().getBlockState(pos.above()).getBlock() instanceof GlowKelpBlock));
        return s.canSurvive(context.getLevel(), pos) ? s : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.getBlock() instanceof GlowKelpBlock || below.isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (direction == Direction.DOWN && !state.canSurvive(level, pos)) {
            // break (and drop) on the next tick, so a cut column comes down segment by segment
            ticks.scheduleTick(pos, this, 1);
        }
        if (direction == Direction.UP) {
            state = state.setValue(TIP, !(neighbourState.getBlock() instanceof GlowKelpBlock));
        }
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    /** Motes of light rise off the bulbs; in water they come with a few glittering bubbles. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(TIP) && random.nextInt(5) == 0) {
            level.addParticle(ModParticles.GLOW_DUST.get(), pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.8,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, 0.015, 0.0);
        }
        if (state.getValue(WATERLOGGED) && random.nextInt(14) == 0) {
            level.addParticle(ModParticles.CHROME_BUBBLE.get(), pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
                    pos.getZ() + random.nextDouble(), 0.0, 0.03, 0.0);
        }
    }
}
