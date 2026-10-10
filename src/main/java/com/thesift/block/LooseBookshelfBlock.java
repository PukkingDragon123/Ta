package com.thesift.block;

import com.thesift.registry.ModMansion;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * MANSION: a Loose Bookshelf - a vanilla bookshelf with one red book sticking out a hair. Use it to pull the book. The
 * puzzle of the mansion's secret room: all the loose books within {@link #RANGE} blocks must be pulled from the lowest one
 * up (books on the same shelf in any order); a book pulled out of turn and they all click back. When the last is pulled,
 * every Bookshelf Door near them swings open ({@link SecretBookshelfBlock}). Pulled books slide back by themselves.
 */
public class LooseBookshelfBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty PULLED = BooleanProperty.create("pulled");
    /** How far apart one puzzle's books (and its doors) may be. */
    public static final int RANGE = 6;
    private static final int RESET_TICKS = 400;

    public LooseBookshelfBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PULLED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PULLED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private static List<BlockPos> books(ServerLevel level, BlockPos at) {
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-RANGE, -RANGE, -RANGE), at.offset(RANGE, RANGE, RANGE))) {
            if (level.getBlockState(p).getBlock() instanceof LooseBookshelfBlock) {
                out.add(p.immutable());
            }
        }
        return out;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(PULLED)) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        List<BlockPos> books = books(server, pos);
        boolean inTurn = true;
        for (BlockPos b : books) {
            if (!b.equals(pos) && !server.getBlockState(b).getValue(PULLED) && b.getY() < pos.getY()) {
                inTurn = false;
            }
        }
        if (!inTurn) {
            resetNear(server, pos);
            server.playSound(null, pos, ModMansion.LOOSE_PULL.get(), SoundSource.BLOCKS, 1.0F, 0.5F);
            player.sendOverlayMessage(Component.translatable("message.thesift.secret.locked"));
            return InteractionResult.SUCCESS;
        }
        server.setBlock(pos, state.setValue(PULLED, true), Block.UPDATE_ALL);
        server.scheduleTick(pos, this, RESET_TICKS);
        server.playSound(null, pos, ModMansion.LOOSE_PULL.get(), SoundSource.BLOCKS, 1.0F, 0.9F + 0.1F * books.indexOf(pos));
        boolean all = true;
        for (BlockPos b : books) {
            all &= b.equals(pos) || server.getBlockState(b).getValue(PULLED);
        }
        if (all) {
            boolean opened = false;
            for (BlockPos p : BlockPos.betweenClosed(pos.offset(-RANGE, -RANGE, -RANGE), pos.offset(RANGE, RANGE, RANGE))) {
                if (server.getBlockState(p).getBlock() instanceof SecretBookshelfBlock) {
                    SecretBookshelfBlock.openDoor(server, p.immutable());
                    opened = true;
                }
            }
            if (opened) {
                player.sendOverlayMessage(Component.translatable("message.thesift.secret.opened"));
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Every loose book near {@code at} slides back in. */
    public static void resetNear(ServerLevel level, BlockPos at) {
        for (BlockPos b : books(level, at)) {
            BlockState s = level.getBlockState(b);
            if (s.getValue(PULLED)) {
                level.setBlock(b, s.setValue(PULLED, false), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(PULLED)) {
            level.setBlock(pos, state.setValue(PULLED, false), Block.UPDATE_ALL);
        }
    }
}
