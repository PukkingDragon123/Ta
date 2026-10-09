package com.thesift.block;

import com.thesift.block.entity.BandTableBlockEntity;
import com.thesift.item.SiftInstrumentItem;
import com.thesift.registry.ModBandTable;
import com.thesift.registry.ModParticles;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * F2 Band Table: the Music Band Table, where the Sift's enchantments are won by playing music.
 *
 * <p>Use it empty to open its score (client {@code BandTableScreen}): pick an item from your bag and an enchantment,
 * and the item is laid on the table. Then play the enchantment's song on its instrument nearby - every note lights one
 * of the glowing notes over the table, a wrong one flashes red. Play it cleanly for the full enchantment; a sloppy
 * performance gives a weaker one, a hopeless one nothing. Use the table again to take your item back (sneak-use stops
 * a performance). See {@link BandTableBlockEntity}.
 */
public class BandTableBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0.0, 0.0, 0.0, 16.0, 12.0, 16.0), Block.box(2.0, 12.0, 2.0, 14.0, 15.0, 14.0));
    /** Client: opens the table's score screen (set by the client at start-up; a no-op on a dedicated server). */
    public static volatile Consumer<BlockPos> clientOpen = pos -> {
    };

    public BandTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** Its front (the drum and the metronome) faces whoever places it. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BandTableBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBandTable.BAND_TABLE_BE.get(),
                level.isClientSide() ? BandTableBlockEntity::clientTick : BandTableBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        // while the song is being played, an instrument in hand plays (its own use opens its play screen)
        if (stack.getItem() instanceof SiftInstrumentItem && level.getBlockEntity(pos) instanceof BandTableBlockEntity table && table.isPerforming()) {
            return InteractionResult.PASS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof BandTableBlockEntity table)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            table.use(server, player);
        } else if (!table.isPerforming() && table.getItem().isEmpty()) {
            clientOpen.accept(pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) == 0) {
            level.addParticle(ModParticles.SIFT_NOTE.get(), pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 1.0 + random.nextDouble() * 0.3,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, random.nextDouble(), 0.0, 0.0);
        }
        if (random.nextInt(9) == 0) {
            level.addParticle(ModParticles.STAR_SPARKLE.get(), pos.getX() + 0.5 + (random.nextDouble() - 0.5), pos.getY() + 0.9 + random.nextDouble() * 0.6,
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5), 0.0, 0.01, 0.0);
        }
    }
}
