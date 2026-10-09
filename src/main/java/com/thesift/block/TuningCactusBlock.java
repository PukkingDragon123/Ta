package com.thesift.block;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * W-land: the Tuning Cactus's stem, the Rocky Dunes' other plant. A thin ribbed stalk that joins its neighbours like a
 * chorus plant and follows the chorus rules: it stands on sand (or on more stem), a side shoot hangs on a neighbour that
 * stands, and cut at the foot the whole cactus comes apart. It grows from {@link TuningCactusBudBlock}.
 */
public class TuningCactusBlock extends Block {
    public static final Map<Direction, BooleanProperty> SIDES = new EnumMap<>(Direction.class);
    private static final VoxelShape[] SHAPES = new VoxelShape[64];

    static {
        SIDES.put(Direction.NORTH, BlockStateProperties.NORTH);
        SIDES.put(Direction.EAST, BlockStateProperties.EAST);
        SIDES.put(Direction.SOUTH, BlockStateProperties.SOUTH);
        SIDES.put(Direction.WEST, BlockStateProperties.WEST);
        SIDES.put(Direction.UP, BlockStateProperties.UP);
        SIDES.put(Direction.DOWN, BlockStateProperties.DOWN);
        for (int bits = 0; bits < 64; bits++) {
            VoxelShape s = Block.box(5.0, 5.0, 5.0, 11.0, 11.0, 11.0);
            for (Direction d : Direction.values()) {
                if ((bits & (1 << d.ordinal())) != 0) {
                    s = Shapes.or(s, arm(d));
                }
            }
            SHAPES[bits] = s;
        }
    }

    public TuningCactusBlock(BlockBehaviour.Properties properties) {
        super(properties);
        BlockState s = this.stateDefinition.any();
        for (BooleanProperty p : SIDES.values()) {
            s = s.setValue(p, false);
        }
        this.registerDefaultState(s);
    }

    static VoxelShape arm(Direction d) {
        return switch (d) {
            case NORTH -> Block.box(5.0, 5.0, 0.0, 11.0, 11.0, 5.0);
            case SOUTH -> Block.box(5.0, 5.0, 11.0, 11.0, 11.0, 16.0);
            case WEST -> Block.box(0.0, 5.0, 5.0, 5.0, 11.0, 11.0);
            case EAST -> Block.box(11.0, 5.0, 5.0, 16.0, 11.0, 11.0);
            case UP -> Block.box(5.0, 11.0, 5.0, 11.0, 16.0, 11.0);
            case DOWN -> Block.box(5.0, 0.0, 5.0, 11.0, 5.0, 11.0);
        };
    }

    /** Stem or bud: any part of a Tuning Cactus. */
    public static boolean isCactus(BlockState state) {
        return state.getBlock() instanceof TuningCactusBlock || state.getBlock() instanceof TuningCactusBudBlock;
    }

    /** Whether a part at `pos` reaches towards its neighbour in `d` (other parts, and the ground under its foot). */
    public static boolean reaches(BlockState neighbour, Direction d) {
        return isCactus(neighbour) || (d == Direction.DOWN && RattlethornBlock.desertGround(neighbour));
    }

    /** This stem's state joined up to whatever is around it. */
    public static BlockState connected(BlockGetter level, BlockPos pos, BlockState state) {
        for (Direction d : Direction.values()) {
            if (state.hasProperty(SIDES.get(d))) {
                state = state.setValue(SIDES.get(d), reaches(level.getBlockState(pos.relative(d)), d));
            }
        }
        return state;
    }

    /** The chorus rule: on the ground or on a stem, or hung sideways on a stem that itself stands (never both at once). */
    public static boolean supported(LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        boolean boxed = !level.getBlockState(pos.above()).isAir() && !below.isAir();
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos side = pos.relative(d);
            if (level.getBlockState(side).getBlock() instanceof TuningCactusBlock) {
                if (boxed) {
                    return false;
                }
                BlockState under = level.getBlockState(side.below());
                if (under.getBlock() instanceof TuningCactusBlock || RattlethornBlock.desertGround(under)) {
                    return true;
                }
            }
        }
        return below.getBlock() instanceof TuningCactusBlock || RattlethornBlock.desertGround(below);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.NORTH, BlockStateProperties.EAST, BlockStateProperties.SOUTH, BlockStateProperties.WEST,
                BlockStateProperties.UP, BlockStateProperties.DOWN);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return connected(context.getLevel(), context.getClickedPos(), this.defaultBlockState());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int bits = 0;
        for (Direction d : Direction.values()) {
            if (state.getValue(SIDES.get(d))) {
                bits |= 1 << d.ordinal();
            }
        }
        return SHAPES[bits];
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return supported(level, pos);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            ticks.scheduleTick(pos, this, 1);
        }
        return state.setValue(SIDES.get(direction), reaches(neighbourState, direction));
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }
}
