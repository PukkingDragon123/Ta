package com.thesift.block;

import com.thesift.block.entity.EchoerDeviceBlockEntity;
import com.thesift.registry.ModEchoer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The Echoer: an ancient soulstone horn that mines with sound. Give it a redstone signal (or use
 * it) and it draws in a breath of light, then fires an echo beam out of its face that shatters
 * the first breakable block in its path.
 *
 * <ul>
 *   <li>Range: a redstone signal sets it - strength 1-15 reaches that many blocks. Used by hand
 *   it fires at its dialled range; sneak-use turns the dial (4, 8 or 16 blocks).</li>
 *   <li>The drops go into a container touching it (the back first), or pop out of its top.</li>
 *   <li>The beam stops at unbreakable blocks and at anything with an inventory or a block entity,
 *   without breaking it.</li>
 * </ul>
 */
public class EchoerDeviceBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty CHARGING = BooleanProperty.create("charging");
    /** The dial for hand use: 0 = 4 blocks, 1 = 8, 2 = 16. */
    public static final IntegerProperty RANGE = IntegerProperty.create("range", 0, 2);
    private static final int[] DIAL = {4, 8, 16};

    public EchoerDeviceBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false).setValue(CHARGING, false)
                .setValue(RANGE, 1));
    }

    public static int dialRange(BlockState state) {
        return DIAL[state.getValue(RANGE)];
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, CHARGING, RANGE);
    }

    /** It faces (and fires) the way you look when you place it. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EchoerDeviceBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModEchoer.ECHOER_DEVICE_BE.get(), EchoerDeviceBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server) {
            if (player.isShiftKeyDown()) {
                BlockState next = state.setValue(RANGE, (state.getValue(RANGE) + 1) % DIAL.length);
                server.setBlock(pos, next, Block.UPDATE_ALL);
                server.playSound(null, pos, net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.BLOCKS, 0.8F,
                        0.8F + next.getValue(RANGE) * 0.3F);
                player.sendOverlayMessage(Component.translatable("message.thesift.echoer_device.range", dialRange(next)));
            } else if (server.getBlockEntity(pos) instanceof EchoerDeviceBlockEntity device) {
                device.charge(server, pos, state, dialRange(state));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            BlockState now = state.setValue(POWERED, powered);
            level.setBlock(pos, now, Block.UPDATE_ALL);
            if (powered && level instanceof ServerLevel server && server.getBlockEntity(pos) instanceof EchoerDeviceBlockEntity device) {
                device.charge(server, pos, now, Math.max(1, server.getBestNeighborSignal(pos)));
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Direction f = state.getValue(FACING);
        double x = pos.getX() + 0.5 + f.getStepX() * 0.55;
        double y = pos.getY() + 0.5 + f.getStepY() * 0.55;
        double z = pos.getZ() + 0.5 + f.getStepZ() * 0.55;
        if (state.getValue(CHARGING)) {
            // light gathering into the horn's mouth
            for (int i = 0; i < 3; i++) {
                double ox = (random.nextDouble() - 0.5) * 1.6;
                double oy = (random.nextDouble() - 0.5) * 1.6;
                double oz = (random.nextDouble() - 0.5) * 1.6;
                level.addParticle(net.minecraft.core.particles.ParticleTypes.SCULK_SOUL, x + ox, y + oy, z + oz, -ox * 0.08, -oy * 0.08, -oz * 0.08);
            }
        } else if (random.nextInt(8) == 0) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.SOUL, x, y, z, f.getStepX() * 0.01, 0.01, f.getStepZ() * 0.01);
        }
    }
}
