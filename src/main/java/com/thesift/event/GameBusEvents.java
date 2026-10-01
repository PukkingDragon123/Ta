package com.thesift.event;

import com.thesift.block.SiftDrumBlock;
import com.thesift.music.Resonance;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.NoteBlockEvent;

/** Listeners on the game event bus: music hooks. */
public final class GameBusEvents {
    /** Fastest a held mouse button can drum: a roll of five beats a second. */
    private static final int DRUM_ROLL_TICKS = 4;
    private static final Map<UUID, Long> LAST_DRUM_HIT = new HashMap<>();

    private GameBusEvents() {
    }

    /**
     * Left-clicking a Sift Drum plays it instead of breaking it (in creative too); sneak to break
     * it. Fires on both sides: cancelling on the client stops the local break, cancelling on the
     * server stops the real one, and the server plays the beat.
     */
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (!(level.getBlockState(event.getPos()).getBlock() instanceof SiftDrumBlock) || player.isShiftKeyDown()) {
            return;
        }
        event.setCanceled(true);
        if (level instanceof ServerLevel server && event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START) {
            // a held button re-sends "start" every tick; let that be a drum roll, not a buzz
            long now = server.getGameTime();
            Long last = LAST_DRUM_HIT.get(player.getUUID());
            if (last == null || now - last >= DRUM_ROLL_TICKS || now < last) {
                LAST_DRUM_HIT.put(player.getUUID(), now);
                SiftDrumBlock.strike(server, event.getPos(), player);
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
