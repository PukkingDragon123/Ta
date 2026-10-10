package com.thesift.music;

import com.thesift.item.MusicSheetItem;
import com.thesift.registry.ModParticles;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
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
 * Server side: follows every player's notes with a {@link SongMatcher}. When a player plays all
 * of a {@link Song}'s notes in order on the instrument it asks for, while carrying its Music
 * Sheet, the song is performed: a flourish and a chord ring out and {@link SongEvents#played}
 * fires. Playing a known song's notes on the wrong instrument earns a hint on the action bar.
 *
 * <p>Every note goes through {@link Notes#play} (free play sends its notes as
 * {@link InstrumentPlay.PlayNote}) and the forgiving {@link SongMatcher}, which also follows a drum
 * song's rhythm and a Prism song's colours.
 */
public final class SongTracker {
    /** The longest pause between two notes of one song. */
    public static final int GAP = SongMatcher.GAP;
    /** Ticks between two wrong-instrument hints. */
    private static final int HINT_EVERY = 60;
    private static final Map<UUID, SongMatcher> PLAYERS = new HashMap<>();
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
        SongMatcher m = PLAYERS.computeIfAbsent(player.getUUID(), u -> new SongMatcher());
        Song song = m.hear(pitch, SongEvents.colour(), SongEvents.clock(), now, SongEvents.instrument(), s -> carriesSheet(player, s));
        if (song != null) {
            flourish(level, player, at, song);
            SongEvents.played(level, player, at, song);
        } else {
            Song wrong = m.wrongInstrument();
            if (wrong != null && now - m.hintAt >= HINT_EVERY) {
                m.hintAt = now;
                player.sendOverlayMessage(Component.translatable("message.thesift.song.wrong_instrument",
                        Component.translatable("song.thesift." + wrong.id()), Component.translatable(wrong.instrumentKey()))
                        .withStyle(ChatFormatting.GOLD));
            }
        }
        if (PLAYERS.size() > 64) {
            for (Iterator<SongMatcher> it = PLAYERS.values().iterator(); it.hasNext(); ) {
                SongMatcher old = it.next();
                if (old != m && old.idle(now)) {
                    it.remove();
                }
            }
        }
    }

    /**
     * Server: a note about to be played on {@code instrument}, nudged onto the note a song the
     * player is playing wants next when it is already within tolerance - so a forgiven note also
     * sounds right.
     */
    public static int tune(Player player, Instrument instrument, int pitch, long now) {
        SongMatcher m = PLAYERS.get(player.getUUID());
        return m == null ? pitch : m.tune(pitch, now, instrument, s -> carriesSheet(player, s));
    }

    /** INS free play: the song this player is in the middle of (two notes or more) or has just finished, or null. */
    public static @Nullable Song leading(Player player, long now) {
        SongMatcher m = PLAYERS.get(player.getUUID());
        return m == null ? null : leading(m, now);
    }

    /** The song a matcher is in the middle of (two notes or more) or has just finished, or null (the client uses it too). */
    public static @Nullable Song leading(SongMatcher m, long now) {
        if (m.lastCompleted != null && now - m.completedAt <= 4) {
            return m.lastCompleted;
        }
        Song best = null;
        int most = 1;
        for (Song s : Song.values()) {
            int n = m.progress(s, now);
            if (n > most) {
                most = n;
                best = s;
            }
        }
        return best;
    }

    /** Forgets a player's progress through every song (tests start from a clean slate). */
    public static void forget(Player player) {
        PLAYERS.remove(player.getUUID());
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
        if (song.prism()) {
            PrismLight.burst(level, c);
        }
        Resonance.pulse(level, BlockPos.containing(c), 0.8F, 8);
        player.sendOverlayMessage(Component.translatable("message.thesift.song.played", Component.translatable("song.thesift." + song.id())));
    }
}
