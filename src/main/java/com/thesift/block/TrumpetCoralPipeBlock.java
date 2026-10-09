package com.thesift.block;

import com.thesift.registry.ModParticles;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * W-sea: a length of Trumpet Coral tube. Like a chorus plant it joins every neighbouring piece of its family (and the sea
 * floor under it); a bell joins it when the bell grows out of it. Tap it and it sounds its note. Holds water or Chrome.
 */
public class TrumpetCoralPipeBlock extends Block implements SimpleWaterloggedBlock {
    public static final Map<Direction, BooleanProperty> SIDES = PipeBlock.PROPERTY_BY_DIRECTION;
    public static final BooleanProperty WATERLOGGED = SeaLogging.WATERLOGGED;
    public static final BooleanProperty CHROMELOGGED = SeaLogging.CHROMELOGGED;
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final VoxelShape[] SHAPES = makeShapes();
    private TrumpetCoral.Metal metal;

    public TrumpetCoralPipeBlock(BlockBehaviour.Properties properties) {
        super(properties);
        BlockState s = this.stateDefinition.any().setValue(WATERLOGGED, false).setValue(CHROMELOGGED, false);
        for (Direction d : DIRECTIONS) {
            s = s.setValue(SIDES.get(d), false);
        }
        this.registerDefaultState(s);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PipeBlock.NORTH, PipeBlock.EAST, PipeBlock.SOUTH, PipeBlock.WEST, PipeBlock.UP, PipeBlock.DOWN, WATERLOGGED, CHROMELOGGED);
    }

    public TrumpetCoral.Metal metal() {
        if (this.metal == null) {
            this.metal = TrumpetCoral.Metal.of(this);
        }
        return this.metal;
    }

    private static VoxelShape[] makeShapes() {
        VoxelShape core = Block.box(5.0, 5.0, 5.0, 11.0, 11.0, 11.0);
        // in Direction order: down, up, north, south, west, east
        VoxelShape[] arms = {Block.box(5.0, 0.0, 5.0, 11.0, 5.0, 11.0), Block.box(5.0, 11.0, 5.0, 11.0, 16.0, 11.0),
                Block.box(5.0, 5.0, 0.0, 11.0, 11.0, 5.0), Block.box(5.0, 5.0, 11.0, 11.0, 11.0, 16.0),
                Block.box(0.0, 5.0, 5.0, 5.0, 11.0, 11.0), Block.box(11.0, 5.0, 5.0, 16.0, 11.0, 11.0)};
        VoxelShape[] out = new VoxelShape[64];
        for (int m = 0; m < 64; m++) {
            VoxelShape s = core;
            for (int i = 0; i < 6; i++) {
                if ((m & (1 << i)) != 0) {
                    s = Shapes.or(s, arms[i]);
                }
            }
            out[m] = s;
        }
        return out;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int m = 0;
        for (int i = 0; i < 6; i++) {
            if (state.getValue(SIDES.get(DIRECTIONS[i]))) {
                m |= 1 << i;
            }
        }
        return SHAPES[m];
    }

    // ------------------------------------------------------------------ joining up

    /** Whether a tube at {@code pos} joins whatever is on its {@code dir} side. */
    public static boolean connects(BlockGetter level, BlockPos pos, Direction dir) {
        BlockPos np = pos.relative(dir);
        BlockState n = level.getBlockState(np);
        Block b = n.getBlock();
        if (b instanceof TrumpetCoralPipeBlock || b instanceof TrumpetCoralBlock) {
            return true;
        }
        if (b instanceof TrumpetCoralBellBlock) {
            return n.getValue(TrumpetCoralBellBlock.FACING) == dir; // the bell grew out of this tube
        }
        return dir == Direction.DOWN && n.isFaceSturdy(level, np, Direction.UP); // rooted in the sea floor
    }

    /** {@code state} joined to everything around {@code pos}. */
    public static BlockState withConnections(BlockGetter level, BlockPos pos, BlockState state) {
        for (Direction d : DIRECTIONS) {
            state = state.setValue(SIDES.get(d), connects(level, pos, d));
        }
        return state;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        return withConnections(context.getLevel(), pos, SeaLogging.inFluidAt(this.defaultBlockState(), context.getLevel(), pos));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        SeaLogging.tick(state, level, ticks, pos);
        return state.setValue(SIDES.get(direction), connects(level, pos, direction));
    }

    // ------------------------------------------------------------------ water or Chrome

    @Override
    protected FluidState getFluidState(BlockState state) {
        return SeaLogging.fluid(state);
    }

    @Override
    public boolean canPlaceLiquid(@Nullable LivingEntity user, BlockGetter level, BlockPos pos, BlockState state, Fluid type) {
        return !SeaLogging.chromelogged(state) && SimpleWaterloggedBlock.super.canPlaceLiquid(user, level, pos, state, type);
    }

    // ------------------------------------------------------------------ music

    private void toot(Level level, BlockPos pos, @Nullable Player player) {
        if (level instanceof ServerLevel server) {
            TrumpetCoral.toot(server, pos, this.metal(), Vec3.atCenterOf(pos), player, 1.0F);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        this.toot(level, pos, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        this.toot(level, pos, player);
    }

    /** Now and then a bubble escapes a joint in the tube. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(24) != 0) {
            return;
        }
        double x = pos.getX() + 0.35 + random.nextDouble() * 0.3;
        double y = pos.getY() + 0.5 + random.nextDouble() * 0.4;
        double z = pos.getZ() + 0.35 + random.nextDouble() * 0.3;
        if (SeaLogging.waterlogged(state)) {
            level.addParticle(ParticleTypes.BUBBLE, x, y, z, 0.0, 0.05, 0.0);
        } else if (SeaLogging.chromelogged(state)) {
            level.addParticle(ModParticles.CHROME_BUBBLE.get(), x, y, z, 0.0, 0.02, 0.0);
        }
    }
}
