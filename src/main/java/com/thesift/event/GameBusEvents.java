package com.thesift.event;

import com.thesift.music.Resonance;
import com.thesift.registry.ModBlocks;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.NoteBlockEvent;

/** Listeners on the game event bus: log stripping, tilling and music hooks. */
public final class GameBusEvents {
    private static Map<Block, Supplier<? extends Block>> strippables;

    private GameBusEvents() {
    }

    private static Map<Block, Supplier<? extends Block>> strippables() {
        if (strippables == null) {
            strippables = Map.of(
                    ModBlocks.LULLWOOD_LOG.get(), ModBlocks.STRIPPED_LULLWOOD_LOG,
                    ModBlocks.LULLWOOD_WOOD.get(), ModBlocks.STRIPPED_LULLWOOD_WOOD,
                    ModBlocks.WISHWOOD_LOG.get(), ModBlocks.STRIPPED_WISHWOOD_LOG,
                    ModBlocks.WISHWOOD_WOOD.get(), ModBlocks.STRIPPED_WISHWOOD_WOOD);
        }
        return strippables;
    }

    public static void onToolModify(BlockEvent.BlockToolModificationEvent event) {
        BlockState state = event.getState();
        if (event.getItemAbility() == ItemAbilities.AXE_STRIP) {
            Supplier<? extends Block> stripped = strippables().get(state.getBlock());
            if (stripped != null) {
                BlockState result = stripped.get().defaultBlockState();
                if (state.hasProperty(RotatedPillarBlock.AXIS)) {
                    result = result.setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS));
                }
                event.setFinalState(result);
            }
        } else if (event.getItemAbility() == ItemAbilities.HOE_TILL) {
            if ((state.is(ModBlocks.SIFT_GRASS_BLOCK.get()) || state.is(ModBlocks.SIFT_SOIL.get()))
                    && event.getLevel().getBlockState(event.getPos().above()).isAir()) {
                event.setFinalState(Blocks.FARMLAND.defaultBlockState());
            }
        } else if (event.getItemAbility() == ItemAbilities.SHOVEL_FLATTEN) {
            if (state.is(ModBlocks.SIFT_GRASS_BLOCK.get()) && event.getLevel().getBlockState(event.getPos().above()).isAir()) {
                event.setFinalState(Blocks.DIRT_PATH.defaultBlockState());
            }
        }
    }

    /** Note blocks are music too: every note sends a small resonance pulse. */
    public static void onNotePlayed(NoteBlockEvent.Play event) {
        if (event.getLevel() instanceof ServerLevel server) {
            Resonance.pulse(server, event.getPos(), 0.35F, 5);
        }
    }

    /** Jukeboxes emit JUKEBOX_PLAY every second while a song plays. */
    public static void onVanillaGameEvent(VanillaGameEvent event) {
        if (event.getVanillaEvent() == GameEvent.JUKEBOX_PLAY && event.getLevel() instanceof ServerLevel server
                && server.getGameTime() % 40L < 20L) {
            Resonance.pulse(server, BlockPos.containing(event.getEventPosition()), 0.6F, 8);
        }
    }
}
