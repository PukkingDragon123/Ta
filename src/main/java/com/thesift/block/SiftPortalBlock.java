package com.thesift.block;

import com.thesift.portal.PortalFrames;
import com.thesift.portal.SiftTeleporter;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The shimmering cyan/pink membrane stretched across an awakened frame. Its block model is only the
 * far-away fallback: up close each cell is drawn as a window onto the Sift's sky (B1).
 */
public class SiftPortalBlock extends Block implements Portal, net.minecraft.world.level.block.EntityBlock {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    private static final VoxelShape X_SHAPE = Block.box(0.0, 0.0, 6.0, 16.0, 16.0, 10.0);
    private static final VoxelShape Z_SHAPE = Block.box(6.0, 0.0, 0.0, 10.0, 16.0, 16.0);

    public SiftPortalBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    /** B1 Portal & sky FX: each cell carries a block entity so the client can draw the sky seen through it. */
    @Override
    public net.minecraft.world.level.block.entity.@Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new com.thesift.block.entity.SiftPortalBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.Z ? Z_SHAPE : X_SHAPE;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
            BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        Direction.Axis axis = state.getValue(AXIS);
        boolean inPlane = direction.getAxis() == Direction.Axis.Y || direction.getAxis() == axis;
        // the gate only collapses when its border is opened up, so it can stand on an Ancient City's deepslate floor
        if (inPlane && !neighborState.is(this) && PortalFrames.isFillable(neighborState)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (entity.canUsePortal(false)) {
            entity.setAsInsidePortal(this, pos);
        }
    }

    @Override
    public int getPortalTransitionTime(ServerLevel level, Entity entity) {
        if (entity instanceof Player player) {
            return player.getAbilities().invulnerable ? 1 : 60;
        }
        return 0;
    }

    @Override
    public @Nullable TeleportTransition getPortalDestination(ServerLevel level, Entity entity, BlockPos pos) {
        return SiftTeleporter.destination(level, entity, pos);
    }

    @Override
    public Portal.Transition getLocalTransition() {
        return Portal.Transition.CONFUSION;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(120) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ModSounds.PORTAL_AMBIENT.get(), SoundSource.BLOCKS, 0.5F,
                    random.nextFloat() * 0.4F + 0.8F, false);
        }
        boolean x = state.getValue(AXIS) == Direction.Axis.X;
        for (int i = 0; i < 2; i++) {
            double px = pos.getX() + random.nextDouble();
            double py = pos.getY() + random.nextDouble();
            double pz = pos.getZ() + random.nextDouble();
            int side = random.nextBoolean() ? 1 : -1;
            if (x) {
                pz = pos.getZ() + 0.5 + 0.3 * side;
            } else {
                px = pos.getX() + 0.5 + 0.3 * side;
            }
            level.addParticle(random.nextInt(3) == 0 ? ModParticles.STAR_SPARKLE.get() : ModParticles.PORTAL_SOUL.get(), px, py, pz,
                    (random.nextDouble() - 0.5) * 0.05, 0.02 + random.nextDouble() * 0.03, (random.nextDouble() - 0.5) * 0.05);
        }
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return ItemStack.EMPTY;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return switch (rotation) {
            case COUNTERCLOCKWISE_90, CLOCKWISE_90 -> state.setValue(AXIS, state.getValue(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
            default -> state;
        };
    }
}
