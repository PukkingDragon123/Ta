package com.thesift.block;

import com.thesift.block.entity.SculkSummonerBlockEntity;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModMansion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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
import org.jspecify.annotations.Nullable;

/**
 * MANSION: the Sculk Summoner - a vanilla Sculk Catalyst with a Warden Core settled in its bone claws, beating. Made by
 * using a Warden Core on a Sculk Catalyst ({@link #arm}). {@link #READY} (the catalyst blooms) once it has its sensors and a
 * gate to open; then the Sift Symphony played nearby wakes the gate (see {@link SculkSummonerBlockEntity}). Use it to hear
 * what it still needs; sneak and use it to take the core back. Broken, it gives back its catalyst and its core.
 */
public class SculkSummonerBlock extends BaseEntityBlock {
    public static final BooleanProperty READY = BooleanProperty.create("ready");

    public SculkSummonerBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(READY, false));
    }

    /** A Warden Core used on a Sculk Catalyst: the catalyst becomes a summoner. Returns false if {@code pos} is no catalyst. */
    public static boolean arm(ServerLevel level, BlockPos pos, @Nullable Player player) {
        if (!level.getBlockState(pos).is(Blocks.SCULK_CATALYST)) {
            return false;
        }
        level.setBlock(pos, ModBlocks.SCULK_SUMMONER.get().defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof SculkSummonerBlockEntity summoner) {
            summoner.armed(level, player);
        }
        return true;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(READY);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SculkSummonerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // the client follows a waking gate so the camera can watch it (see GateAwakening)
        return level.isClientSide()
                ? createTickerHelper(type, ModMansion.SCULK_SUMMONER.get(), SculkSummonerBlockEntity::clientTick)
                : createTickerHelper(type, ModMansion.SCULK_SUMMONER.get(), SculkSummonerBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server && server.getBlockEntity(pos) instanceof SculkSummonerBlockEntity summoner) {
            if (player.isShiftKeyDown()) {
                boolean removed = summoner.ejectCore(server);
                player.sendOverlayMessage(Component.translatable(removed ? "message.thesift.summoner.core_removed" : "message.thesift.summoner.core_spent"));
            } else {
                summoner.report(server, player);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // souls rise off the beating core; more of them once it blooms
        if (random.nextInt(state.getValue(READY) ? 2 : 4) == 0) {
            level.addParticle(ParticleTypes.SCULK_SOUL, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.4, pos.getY() + 1.15,
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.4, 0.0, 0.03, 0.0);
        }
        if (state.getValue(READY) && random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.SCULK_CHARGE_POP, pos.getX() + random.nextDouble(), pos.getY() + 1.0, pos.getZ() + random.nextDouble(),
                    0.0, 0.02, 0.0);
        }
    }
}
