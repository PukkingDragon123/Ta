package com.thesift.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
 * W-sea: Tube Seaweed of the Brass Coral Reef - a clump of hollow, algae-green tubes that stacks into a column like kelp
 * and slowly grows taller under water (up to {@link #MAX_HEIGHT}); its open tips breathe out strings of bubbles.
 * Smoke or smelt it into Dried Kelp.
 */
public class TubeSeaweedBlock extends Block implements SimpleWaterloggedBlock {
    public static final BooleanProperty TIP = BooleanProperty.create("tip");
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final int MAX_HEIGHT = 9;
    private static final VoxelShape SHAPE = Block.column(12.0, 0.0, 16.0);
    private static final VoxelShape TIP_SHAPE = Block.column(12.0, 0.0, 12.0);

    public TubeSeaweedBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(TIP, true).setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TIP, WATERLOGGED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(TIP) ? TIP_SHAPE : SHAPE;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        FluidState fluid = context.getLevel().getFluidState(pos);
        if (!fluid.is(Fluids.WATER) || !fluid.isSource()) {
            return null; // a sea plant: only under water
        }
        BlockState s = this.defaultBlockState().setValue(WATERLOGGED, true)
                .setValue(TIP, !(context.getLevel().getBlockState(pos.above()).getBlock() instanceof TubeSeaweedBlock));
        return s.canSurvive(context.getLevel(), pos) ? s : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.getBlock() instanceof TubeSeaweedBlock || below.isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (direction == Direction.DOWN && !state.canSurvive(level, pos)) {
            ticks.scheduleTick(pos, this, 1); // a cut clump comes down tube by tube
        }
        if (direction == Direction.UP) {
            state = state.setValue(TIP, !(neighbourState.getBlock() instanceof TubeSeaweedBlock));
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
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(TIP) && state.getValue(WATERLOGGED);
    }

    /** It grows a tube taller now and then, into open water. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) != 0) {
            return;
        }
        BlockPos up = pos.above();
        if (!level.getBlockState(up).is(Blocks.WATER) || !level.getFluidState(up).isSource()) {
            return;
        }
        int height = 1;
        while (height < MAX_HEIGHT && level.getBlockState(pos.below(height)).getBlock() instanceof TubeSeaweedBlock) {
            height++;
        }
        if (height < MAX_HEIGHT) {
            level.setBlock(up, this.defaultBlockState().setValue(TIP, true).setValue(WATERLOGGED, true), Block.UPDATE_ALL);
        }
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    /** The open tips breathe out little strings of bubbles. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(TIP) && state.getValue(WATERLOGGED) && random.nextInt(7) == 0) {
            double x = pos.getX() + 0.3 + random.nextDouble() * 0.4;
            double z = pos.getZ() + 0.3 + random.nextDouble() * 0.4;
            for (int i = 0; i < 1 + random.nextInt(3); i++) {
                level.addParticle(ParticleTypes.BUBBLE, x, pos.getY() + 0.8 + i * 0.12, z, 0.0, 0.07, 0.0);
            }
        }
    }
}
