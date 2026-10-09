package com.thesift.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * W-deep caves: a Sculk Tendril of the Sculk Caves - a column of ribbed sculk hanging from the roof (or reaching up from
 * the floor), swaying with no wind to move it (its textures are animated) and ending in a hooked, glowing bud. Columns
 * stack like vines; the last block is the tip. They can be climbed.
 */
public class SculkTendrilBlock extends Block {
    public static final BooleanProperty HANGING = BlockStateProperties.HANGING;
    public static final BooleanProperty TIP = BooleanProperty.create("tip");
    private static final VoxelShape BODY = Block.column(8.0, 0.0, 16.0);
    private static final VoxelShape TIP_HANGING = Block.column(8.0, 3.0, 16.0);
    private static final VoxelShape TIP_STANDING = Block.column(8.0, 0.0, 13.0);

    public SculkTendrilBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(HANGING, true).setValue(TIP, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HANGING, TIP);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!state.getValue(TIP)) {
            return BODY;
        }
        return state.getValue(HANGING) ? TIP_HANGING : TIP_STANDING;
    }

    /** The side it hangs or stands from. */
    private static Direction support(BlockState state) {
        return state.getValue(HANGING) ? Direction.UP : Direction.DOWN;
    }

    private boolean sameColumn(BlockState state, BlockState other) {
        return other.is(this) && other.getValue(HANGING) == state.getValue(HANGING);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction s = support(state);
        BlockPos at = pos.relative(s);
        BlockState held = level.getBlockState(at);
        return this.sameColumn(state, held) || held.isFaceSturdy(level, at, s.getOpposite());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighbourPos,
            BlockState neighbourState, RandomSource random) {
        Direction s = support(state);
        if (direction == s && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (direction == s.getOpposite()) {
            return state.setValue(TIP, !this.sameColumn(state, neighbourState));
        }
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        LevelReader level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        boolean hanging = context.getClickedFace() != Direction.UP;
        for (boolean h : new boolean[]{hanging, !hanging}) {
            BlockState state = this.defaultBlockState().setValue(HANGING, h);
            if (state.canSurvive(level, pos)) {
                BlockState next = level.getBlockState(pos.relative(support(state).getOpposite()));
                return state.setValue(TIP, !this.sameColumn(state, next));
            }
        }
        return null;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(TIP) && random.nextInt(12) == 0) {
            double y = pos.getY() + (state.getValue(HANGING) ? 0.15 : 0.85);
            level.addParticle(ParticleTypes.SCULK_SOUL, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.3, y,
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.3, 0.0, state.getValue(HANGING) ? -0.01 : 0.02, 0.0);
        }
    }
}
