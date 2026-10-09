package com.thesift.block;

import com.thesift.block.entity.EurophyTableBlockEntity;
import com.thesift.registry.ModEurophy;
import com.thesift.registry.ModParticles;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Europhy Table: a Magnesite plinth (this block's model) carrying a clockwork of Copper and Prism (drawn
 * by EurophyTableRenderer). Use it to open its menu; music played nearby makes it craft.
 */
public class EurophyTableBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(Block.column(16.0, 0.0, 3.0), Block.column(10.0, 3.0, 9.0), Block.column(14.0, 9.0, 13.0));

    public EurophyTableBlock(BlockBehaviour.Properties properties) {
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
        return new EurophyTableBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModEurophy.EUROPHY_TABLE.get(),
                level.isClientSide() ? EurophyTableBlockEntity::clientTick : EurophyTableBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof EurophyTableBlockEntity table) {
            player.openMenu(table);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) == 0) {  // the lens sheds a little light
            level.addParticle(ModParticles.STAR_SPARKLE.get(), pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.3, pos.getY() + 1.35,
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.3, 0.0, -0.01, 0.0);
        }
    }
}
