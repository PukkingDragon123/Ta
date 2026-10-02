package com.thesift.music;

import com.thesift.item.MusicSheetItem;
import com.thesift.registry.ModParticles;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Server side: keeps every player's latest notes. When a player plays all of a {@link Song}'s
 * notes in order - no more than {@link #GAP} ticks apart - while carrying its Music Sheet, the
 * song is performed: a flourish and a chord ring out and {@link SongEvents#played} fires.
 */
public final class SongTracker {
    /** The longest pause between two notes of one song (2 s). */
    public static final int GAP = 40;
    private static final int KEEP = 16;
    private static final Map<UUID, History> HISTORY = new HashMap<>();
    private static boolean started;

    private SongTracker() {
    }

    /** Hooks the tracker and the songs' own effects into {@link SongEvents} (once, at start-up). */
    public static void init() {
        if (started) {
            return;
        }
        started = true;
        SongEvents.listenNotes(SongTracker::onNote);
        SongEffects.init();
    }

    private static void onNote(ServerLevel level, @Nullable Player player, Vec3 at, int pitch) {
        if (player == null) {
            return;
        }
        long now = level.getGameTime();
        History h = HISTORY.computeIfAbsent(player.getUUID(), u -> new History());
        if (now - h.last > GAP || now < h.last) {
            h.size = 0;
        }
        h.push(pitch);
        h.last = now;
        for (Song song : Song.values()) {
            if (h.endsWith(song) && carriesSheet(player, song)) {
                h.size = 0;
                flourish(level, player, at, song);
                SongEvents.played(level, player, at, song);
                break;
            }
        }
        if (HISTORY.size() > 64) {
            for (Iterator<History> it = HISTORY.values().iterator(); it.hasNext(); ) {
                if (now - it.next().last > GAP) {
                    it.remove();
                }
            }
        }
    }

    /** True if the player has the song's Music Sheet anywhere on them. */
    public static boolean carriesSheet(Player player, Song song) {
        if (isSheet(player.getMainHandItem(), song) || isSheet(player.getOffhandItem(), song)) {
            return true;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (isSheet(player.getInventory().getItem(i), song)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSheet(ItemStack stack, Song song) {
        return stack.getItem() instanceof MusicSheetItem sheet && sheet.song() == song;
    }

    /** The song is complete: a rising chord, a burst of notes and stars, a ripple through the ground. */
    private static void flourish(ServerLevel level, Player player, Vec3 at, Song song) {
        int root = song.note(song.length() - 1);
        int[] chord = {root, root + 4, root + 7, root + 12};
        for (int i = 0; i < chord.length; i++) {
            int n = chord[i] > Notes.MAX_PITCH ? chord[i] - 12 : chord[i];
            level.playSound(null, at.x, at.y, at.z, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 1.2F, Notes.soundPitch(n));
            level.playSound(null, at.x, at.y, at.z, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.7F, Notes.soundPitch(n));
        }
        level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.2F);
        Vec3 c = player.position();
        for (int i = 0; i < 16; i++) {
            double a = i / 16.0 * Math.PI * 2.0;
            level.sendParticles(ModParticles.SIFT_NOTE.get(), c.x + Math.cos(a) * 1.4, c.y + 1.0 + (i % 4) * 0.3, c.z + Math.sin(a) * 1.4,
                    0, i / 16.0, 0.0, 0.0, 1.0);
        }
        level.sendParticles(ModParticles.STAR_SPARKLE.get(), c.x, c.y + 1.2, c.z, 24, 0.8, 0.8, 0.8, 0.06);
        level.sendParticles(ModParticles.RESONANCE_RING.get(), c.x, c.y + 0.1, c.z, 0, 6.0, 0.0, 0.0, 1.0);
        Resonance.pulse(level, BlockPos.containing(c), 0.8F, 8);
        player.sendOverlayMessage(Component.translatable("message.thesift.song.played", Component.translatable("song.thesift." + song.id())));
    }

    private static final class History {
        final int[] notes = new int[KEEP];
        int size;
        long last = Long.MIN_VALUE / 2;

        void push(int pitch) {
            if (this.size == KEEP) {
                System.arraycopy(this.notes, 1, this.notes, 0, KEEP - 1);
                this.size--;
            }
            this.notes[this.size++] = pitch;
        }

        boolean endsWith(Song song) {
            int n = song.length();
            if (this.size < n) {
                return false;
            }
            for (int i = 0; i < n; i++) {
                if (this.notes[this.size - n + i] != song.note(i)) {
                    return false;
                }
            }
            return true;
        }
    }
}
