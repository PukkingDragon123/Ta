package com.thesift.block;

import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Puzzle stone found in musical temples. Each stone rings one of four tones and has a hidden KEY.
 * Nearby Glyph Stones show which tone each stone must ring. When every Harmony Stone around a
 * Harmony Seal rings its key, the seal dissolves and the vault opens.
 */
public class HarmonyStoneBlock extends Block {
    public static final IntegerProperty TONE = IntegerProperty.create("tone", 0, 3);
    public static final IntegerProperty KEY = IntegerProperty.create("key", 0, 3);
    public static final int SEARCH = 10;

    public HarmonyStoneBlock(BlockBehaviour.Properties properties) {
        super(properties.lightLevel(s -> 4 + s.getValue(TONE) * 2));
        this.registerDefaultState(this.stateDefinition.any().setValue(TONE, 0).setValue(KEY, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TONE, KEY);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server) {
            int tone = (state.getValue(TONE) + 1) % 4;
            server.setBlock(pos, state.setValue(TONE, tone), Block.UPDATE_ALL);
            float pitch = (float) Math.pow(2.0, (new int[] {0, 4, 7, 12})[tone] / 12.0) * 0.7F;
            server.playSound(null, pos, ModSounds.HARMONY_TONE.get(), SoundSource.BLOCKS, 1.0F, pitch);
            server.sendParticles(ModParticles.SIFT_NOTE.get(), pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 0, tone / 4.0 + 0.05, 0, 0, 1);
            checkSeals(server, pos);
        }
        return InteractionResult.SUCCESS;
    }

    public static void checkSeals(ServerLevel level, BlockPos around) {
        List<BlockPos> seals = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(around.offset(-SEARCH, -SEARCH / 2, -SEARCH), around.offset(SEARCH, SEARCH / 2, SEARCH))) {
            if (level.getBlockState(p).is(ModBlocks.HARMONY_SEAL.get())) {
                seals.add(p.immutable());
            }
        }
        for (BlockPos seal : seals) {
            if (!level.getBlockState(seal).is(ModBlocks.HARMONY_SEAL.get())) continue;
            boolean solved = true;
            int stones = 0;
            for (BlockPos p : BlockPos.betweenClosed(seal.offset(-SEARCH, -SEARCH / 2, -SEARCH), seal.offset(SEARCH, SEARCH / 2, SEARCH))) {
                BlockState s = level.getBlockState(p);
                if (s.is(ModBlocks.HARMONY_STONE.get())) {
                    stones++;
                    if (s.getValue(TONE) != s.getValue(KEY)) {
                        solved = false;
                        break;
                    }
                }
            }
            if (solved && stones > 0) {
                HarmonySealBlock.dissolve(level, seal);
            }
        }
    }
}
