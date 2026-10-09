package com.thesift.block;

import com.thesift.block.entity.SculkGrasperBlockEntity;
import com.thesift.registry.ModCaves;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * W-deep caves: the Sculk Grasper, a squat sculk pod of the Sculk Caves with a bone-ringed maw and a crown of feelers.
 * When a player comes near it lashes out a tendril, catches hold and drags them in to be chewed
 * ({@link SculkGrasperBlockEntity}); ACTIVE (maw open and glowing) while it strikes and holds. Strike the pod - or shoot
 * it - and it lets go; or struggle free by moving.
 */
public class SculkGrasperBlock extends BaseEntityBlock {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 7.0, 14.0);

    public SculkGrasperBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighbourPos,
            BlockState neighbourState, RandomSource random) {
        return direction == Direction.DOWN && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SculkGrasperBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? createTickerHelper(type, ModCaves.SCULK_GRASPER.get(), SculkGrasperBlockEntity::clientTick)
                : createTickerHelper(type, ModCaves.SCULK_GRASPER.get(), SculkGrasperBlockEntity::serverTick);
    }

    /** A blow to the pod makes it let go of whatever it holds. */
    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (level instanceof ServerLevel server && server.getBlockEntity(pos) instanceof SculkGrasperBlockEntity grasper) {
            grasper.struck(server);
        }
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (level instanceof ServerLevel server && server.getBlockEntity(hit.getBlockPos()) instanceof SculkGrasperBlockEntity grasper) {
            grasper.struck(server);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(state.getValue(ACTIVE) ? 2 : 9) == 0) {
            level.addParticle(ParticleTypes.SCULK_CHARGE_POP, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6, pos.getY() + 0.5,
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6, 0.0, 0.03, 0.0);
        }
    }
}
