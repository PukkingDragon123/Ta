package com.thesift.block;

import com.thesift.block.entity.EncoreSigilBlockEntity;
import com.thesift.registry.ModBlockEntities;
import com.thesift.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
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
import org.jspecify.annotations.Nullable;

/**
 * An Encore Sigil, set into the floor of an old structure: when someone walks up to it, one of
 * the Conductor's three great players (BOSS: 0 the Thumper, 2 the Strummer; 1 is retired and wakes the Thumper)
 * answers the call and comes out to play. It plays only once; then it goes dark.
 */
public class EncoreSigilBlock extends BaseEntityBlock {
    public static final IntegerProperty BOSS = IntegerProperty.create("boss", 0, 2);
    public static final BooleanProperty SPENT = BooleanProperty.create("spent");

    public EncoreSigilBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(BOSS, 0).setValue(SPENT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BOSS, SPENT);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EncoreSigilBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() || state.getValue(SPENT) ? null
                : createTickerHelper(type, ModBlockEntities.ENCORE_SIGIL.get(), EncoreSigilBlockEntity::serverTick);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(SPENT) && random.nextInt(3) == 0) {
            level.addParticle(ModParticles.SIFT_NOTE.get(), pos.getX() + random.nextDouble(), pos.getY() + 1.1, pos.getZ() + random.nextDouble(),
                    random.nextDouble(), 0, 0);
        }
    }
}
