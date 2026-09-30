package com.thesift.event;

import com.thesift.music.Resonance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.level.NoteBlockEvent;

/** Listeners on the game event bus: music hooks. */
public final class GameBusEvents {
    private GameBusEvents() {
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
