package com.thesift.music;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The hub every instrument and every song-hearer goes through (server side).
 *
 * <ul>
 *   <li>Instruments call {@link #note} for every note a player plays.</li>
 *   <li>The song tracker listens to notes and, when a player completes a {@link Song} they know
 *   (they carry its Music Sheet), calls {@link #played}.</li>
 *   <li>Anything that reacts to songs - creatures, blocks, puzzles, trades - registers a
 *   {@link SongListener} once at start-up and checks the distance itself.</li>
 * </ul>
 */
public final class SongEvents {
    /** Hears single notes (e.g. Soul Golems recharge on any music). */
    @FunctionalInterface
    public interface NoteListener {
        void onNote(ServerLevel level, @Nullable Player player, Vec3 at, int pitch);
    }

    /** Hears whole songs. */
    @FunctionalInterface
    public interface SongListener {
        void onSong(ServerLevel level, @Nullable Player player, Vec3 at, Song song);
    }

    private static final List<NoteListener> NOTE_LISTENERS = new ArrayList<>();
    private static final List<SongListener> SONG_LISTENERS = new ArrayList<>();

    private SongEvents() {
    }

    public static void listenNotes(NoteListener l) {
        NOTE_LISTENERS.add(l);
    }

    public static void listenSongs(SongListener l) {
        SONG_LISTENERS.add(l);
    }

    /** A note was played at {@code at} (pitch 0-24), by a player or by a block. */
    public static void note(ServerLevel level, @Nullable Player player, Vec3 at, int pitch) {
        for (NoteListener l : NOTE_LISTENERS) {
            l.onNote(level, player, at, pitch);
        }
    }

    /** A whole song was performed at {@code at}. */
    public static void played(ServerLevel level, @Nullable Player player, Vec3 at, Song song) {
        for (SongListener l : SONG_LISTENERS) {
            l.onSong(level, player, at, song);
        }
    }
}
