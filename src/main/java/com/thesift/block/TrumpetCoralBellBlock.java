package com.thesift.block;

import com.thesift.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * W-sea: the flared bell at the end of a Trumpet Coral branch - the living, growing tip of the coral. Under water (or in
 * Chrome) it slowly grows on, leaving tube behind it ({@link TrumpetCoral#grow}); it toots its note on its own, when
 * something swims through it and when tapped. Place one on any face to start a new branch; it is also good to eat (see
 * {@code ModSeaReefs.TRUMPET_BELL_FOOD}).
 */
public class TrumpetCoralBellBlock extends Block implements SimpleWaterloggedBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, TrumpetCoral.MAX_AGE);
    public static final BooleanProperty WATERLOGGED = SeaLogging.WATERLOGGED;
    public static final BooleanProperty CHROMELOGGED = SeaLogging.CHROMELOGGED;
    private static final VoxelShape SHAPE_UP = Block.box(1.0, 0.0, 1.0, 15.0, 12.0, 15.0);
    private static final VoxelShape SHAPE_DOWN = Block.box(1.0, 4.0, 1.0, 15.0, 16.0, 15.0);
    private static final VoxelShape SHAPE_NORTH = Block.box(1.0, 1.0, 4.0, 15.0, 15.0, 16.0);
    private static final VoxelShape SHAPE_SOUTH = Block.box(1.0, 1.0, 0.0, 15.0, 15.0, 12.0);
    private static final VoxelShape SHAPE_EAST = Block.box(0.0, 1.0, 1.0, 12.0, 15.0, 15.0);
    private static final VoxelShape SHAPE_WEST = Block.box(4.0, 1.0, 1.0, 16.0, 15.0, 15.0);
    private TrumpetCoral.Metal metal;

    public TrumpetCoralBellBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP).setValue(AGE, 0).setValue(WATERLOGGED, false)
                .setValue(CHROMELOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, AGE, WATERLOGGED, CHROMELOGGED);
    }

    public TrumpetCoral.Metal metal() {
        if (this.metal == null) {
            this.metal = TrumpetCoral.Metal.of(this);
        }
        return this.metal;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case UP -> SHAPE_UP;
            case DOWN -> SHAPE_DOWN;
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
        };
    }

    // ------------------------------------------------------------------ support

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos back = pos.relative(facing.getOpposite());
        BlockState b = level.getBlockState(back);
        return b.getBlock() instanceof TrumpetCoralPipeBlock || b.getBlock() instanceof TrumpetCoralBlock || b.isFaceSturdy(level, back, facing);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        BlockState state = SeaLogging.inFluidAt(this.defaultBlockState().setValue(FACING, context.getClickedFace()), context.getLevel(), pos);
        return state.canSurvive(context.getLevel(), pos) ? state : null;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        SeaLogging.tick(state, level, ticks, pos);
        if (direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)) {
            return SeaLogging.remains(state);
        }
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    // ------------------------------------------------------------------ water or Chrome, and growing

    @Override
    protected FluidState getFluidState(BlockState state) {
        return SeaLogging.fluid(state);
    }

    @Override
    public boolean canPlaceLiquid(@Nullable LivingEntity user, BlockGetter level, BlockPos pos, BlockState state, Fluid type) {
        return !SeaLogging.chromelogged(state) && SimpleWaterloggedBlock.super.canPlaceLiquid(user, level, pos, state, type);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AGE) < TrumpetCoral.MAX_AGE && SeaLogging.wet(state);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            TrumpetCoral.grow(level, pos, state, random);
        }
    }

    // ------------------------------------------------------------------ music

    private void toot(Level level, BlockPos pos, BlockState state, @Nullable Player player, float volume) {
        if (level instanceof ServerLevel server) {
            TrumpetCoral.toot(server, pos, this.metal(), TrumpetCoral.mouth(pos, state.getValue(FACING)), player, volume);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        this.toot(level, pos, state, player, 1.2F);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        this.toot(level, pos, state, player, 1.2F);
    }

    /** Swim through a bell and it sounds. */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (!(entity instanceof LivingEntity) || entity.getKnownMovement().lengthSqr() < 1.0E-4 || (level.getGameTime() + pos.asLong()) % 8 != 0) {
            return;
        }
        this.toot(level, pos, state, entity instanceof Player p ? p : null, 0.9F);
    }

    /** Under water a bell blows its note by itself now and then, breathing out a few bubbles. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!SeaLogging.wet(state)) {
            return;
        }
        Vec3 mouth = TrumpetCoral.mouth(pos, state.getValue(FACING));
        if (random.nextInt(14) == 0) {
            TrumpetCoral.tootLocally(level, pos, this.metal(), mouth, 0.55F);
        }
        if (random.nextInt(6) == 0) {
            Direction f = state.getValue(FACING);
            if (SeaLogging.waterlogged(state)) {
                level.addParticle(ParticleTypes.BUBBLE, mouth.x + (random.nextDouble() - 0.5) * 0.4, mouth.y, mouth.z + (random.nextDouble() - 0.5) * 0.4,
                        f.getStepX() * 0.05, 0.04 + f.getStepY() * 0.05, f.getStepZ() * 0.05);
            } else {
                level.addParticle(ModParticles.CHROME_BUBBLE.get(), mouth.x, mouth.y, mouth.z, 0.0, 0.02, 0.0);
            }
        }
    }
}
