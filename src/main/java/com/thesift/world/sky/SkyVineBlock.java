package com.thesift.world.sky;

import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModParticles;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * W-sky: the Sky Vine - a thick, twisted liana that hangs from the Sky Islands' roots and trees and spans the gaps
 * between them. It joins its neighbours like a chorus plant (an arm towards every vine, root, log, leaf or solid face
 * it holds on to), has no collision, and you can grab it - use it, or jump into it - and swing ({@link SkySwing}).
 * A vine must hang from something or hold on sideways; cut a strand and everything below it falls away, Driftfruit and
 * all. The tip of a hanging strand slowly grows a new Driftfruit.
 */
public class SkyVineBlock extends Block {
    public static final BooleanProperty NORTH = PipeBlock.NORTH;
    public static final BooleanProperty EAST = PipeBlock.EAST;
    public static final BooleanProperty SOUTH = PipeBlock.SOUTH;
    public static final BooleanProperty WEST = PipeBlock.WEST;
    public static final BooleanProperty UP = PipeBlock.UP;
    public static final BooleanProperty DOWN = PipeBlock.DOWN;
    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = PipeBlock.PROPERTY_BY_DIRECTION;
    private static final VoxelShape[] SHAPES = new VoxelShape[64];

    static {
        VoxelShape core = Block.box(5.0, 5.0, 5.0, 11.0, 11.0, 11.0);
        Direction[] dirs = Direction.values();
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape s = core;
            for (int i = 0; i < dirs.length; i++) {
                if ((mask & (1 << i)) != 0) {
                    s = Shapes.or(s, arm(dirs[i]));
                }
            }
            SHAPES[mask] = s.optimize();
        }
    }

    public SkyVineBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false)
                .setValue(WEST, false).setValue(UP, false).setValue(DOWN, false));
    }

    private static VoxelShape arm(Direction d) {
        return switch (d) {
            case NORTH -> Block.box(5.0, 5.0, 0.0, 11.0, 11.0, 5.0);
            case SOUTH -> Block.box(5.0, 5.0, 11.0, 11.0, 11.0, 16.0);
            case WEST -> Block.box(0.0, 5.0, 5.0, 5.0, 11.0, 11.0);
            case EAST -> Block.box(11.0, 5.0, 5.0, 16.0, 11.0, 11.0);
            case UP -> Block.box(5.0, 11.0, 5.0, 11.0, 16.0, 11.0);
            case DOWN -> Block.box(5.0, 0.0, 5.0, 11.0, 5.0, 11.0);
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = 0;
        Direction[] dirs = Direction.values();
        for (int i = 0; i < dirs.length; i++) {
            if (state.getValue(PROPERTY_BY_DIRECTION.get(dirs[i]))) {
                mask |= 1 << i;
            }
        }
        return SHAPES[mask];
    }

    /** What a vine holds on to (or hangs from) on its {@code side}: other vines, roots, logs, leaves, any solid face. */
    public static boolean holdsOn(BlockGetter level, BlockPos neighbourPos, BlockState neighbour, Direction side) {
        if (neighbour.getBlock() instanceof SkyVineBlock || neighbour.is(BlockTags.LOGS) || neighbour.is(BlockTags.LEAVES)) {
            return true;
        }
        if (side == Direction.DOWN && neighbour.getBlock() instanceof DriftfruitBlock) {
            return true;
        }
        return neighbour.isFaceSturdy(level, neighbourPos, side.getOpposite());
    }

    /** The state with an arm towards every neighbour it holds on to. */
    public static BlockState connected(BlockGetter level, BlockPos pos, BlockState state) {
        for (Direction d : Direction.values()) {
            BlockPos n = pos.relative(d);
            state = state.setValue(PROPERTY_BY_DIRECTION.get(d), holdsOn(level, n, level.getBlockState(n), d));
        }
        return state;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = connected(context.getLevel(), context.getClickedPos(), this.defaultBlockState());
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    /** A vine hangs from above or holds on sideways; one that only rests on what is below it falls. */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            if (d != Direction.DOWN) {
                BlockPos n = pos.relative(d);
                if (holdsOn(level, n, level.getBlockState(n), d)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            ticks.scheduleTick(pos, this, 1);
        }
        return state.setValue(PROPERTY_BY_DIRECTION.get(directionToNeighbour), holdsOn(level, neighbourPos, neighbourState, directionToNeighbour));
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    /** Grab it: the client starts swinging (the server learns of it from SkySwing's packet). */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            SkySwing.clientGrab.accept(pos);
        }
        return InteractionResult.SUCCESS;
    }

    /** Only the bare tip of a hanging strand grows anything: a new Driftfruit, now and then. */
    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(UP) && !state.getValue(DOWN);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos below = pos.below();
        if (random.nextInt(48) != 0 || !level.getBlockState(below).isAir() || !level.getBlockState(below.below()).isAir()) {
            return;
        }
        // a fruit needs a strand of at least three vines to hang from, and no other fruit close by
        for (int i = 1; i <= 2; i++) {
            if (!(level.getBlockState(pos.above(i)).getBlock() instanceof SkyVineBlock)) {
                return;
            }
        }
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-3, -3, -3), pos.offset(3, 1, 3))) {
            if (level.getBlockState(p).getBlock() instanceof DriftfruitBlock) {
                return;
            }
        }
        level.setBlock(below, ModBlocks.DRIFTFRUIT.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(pos, state.setValue(DOWN, true), Block.UPDATE_CLIENTS);
        level.playSound(null, below, SoundEvents.CAVE_VINES_PICK_BERRIES, SoundSource.BLOCKS, 0.8F, 0.7F);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(60) == 0 && level.getBlockState(pos.below()).isAir()) {
            level.addParticle(ModParticles.LULLWOOD_LEAF.get(), pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.2,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, -0.02, 0.0);
        }
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
