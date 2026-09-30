package com.thesift.block;

import com.thesift.block.entity.EuphoryAltarBlockEntity;
import com.thesift.registry.ModBlockEntities;
import com.thesift.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Euphory Altar: a clockwork enchanting altar powered by music. Place an item on it, surround
 * it with Sift Drums and feed it a Chrome Pearl. The drums drum themselves, the rings spin up and
 * the item is enchanted far beyond what an enchanting table can manage.
 */
public class EuphoryAltarBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Block.column(16.0, 0.0, 12.0);

    public EuphoryAltarBlock(BlockBehaviour.Properties properties) {
        super(properties);
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
        return new EuphoryAltarBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.EUPHORY_ALTAR.get(),
                level.isClientSide() ? EuphoryAltarBlockEntity::clientTick : EuphoryAltarBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof EuphoryAltarBlockEntity altar) {
            InteractionResult r = altar.use(player, hand, stack);
            if (r != InteractionResult.PASS) {
                return r;
            }
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof EuphoryAltarBlockEntity altar) {
            return altar.takeItem(player);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) == 0) {
            level.addParticle(ModParticles.STAR_SPARKLE.get(), pos.getX() + 0.5 + (random.nextDouble() - 0.5), pos.getY() + 1.0 + random.nextDouble(),
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5), 0.0, 0.01, 0.0);
        }
    }
}
