package com.thesift.block;

import com.thesift.block.entity.BossDenBlockEntity;
import com.thesift.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * RR: the hidden heart of a boss's den (it replaced the Encore Sigils, the Instrument Altars and the Conductor's
 * Podium - no altar blocks any more). Invisible and untouchable, it sits in the air of the den:
 * BOSS 0 the Thumper's Drum Pit, 2 the Weaver's hollow (1 is retired and wakes the Thumper) - the first player to
 * come near wakes the boss, once; BOSS 3 the Grand Stage - a player who brings the Conga Drum, the Crane Flute and
 * the Weaver's Guitar onto it starts the performance and the Conductor rises (see {@link BossDenBlockEntity}).
 */
public class BossDenBlock extends BaseEntityBlock {
    public static final IntegerProperty BOSS = IntegerProperty.create("boss", 0, 3);
    public static final BooleanProperty SPENT = BooleanProperty.create("spent");
    public static final int STAGE = 3;

    public BossDenBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(BOSS, 0).setValue(SPENT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BOSS, SPENT);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BossDenBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (state.getValue(BOSS) == STAGE) {
            return createTickerHelper(type, ModBlockEntities.BOSS_DEN.get(), BossDenBlockEntity::stageTick);
        }
        return level.isClientSide() || state.getValue(SPENT) ? null
                : createTickerHelper(type, ModBlockEntities.BOSS_DEN.get(), BossDenBlockEntity::denTick);
    }
}
