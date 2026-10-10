package com.thesift.music;

import com.thesift.TheSift;
import com.thesift.registry.ModParticles;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Playing single notes on a hand-held instrument - the one call every instrument makes.
 *
 * <p>INS free play: holding use raises an instrument into its playing stance and the player plays
 * it right there in the world (client {@code FreePlay}: the number keys, the mouse wheel, attack).
 * Each note sounds at once for the player and goes to the server ({@link InstrumentPlay.PlayNote});
 * the server plays it through {@link #play} for everyone else and on to {@link SongEvents#note},
 * so the song tracker and every listener hear it, and tells the players watching
 * ({@link InstrumentPlay.Shown}) so they see the hands move and the notes fly.
 *
 * <p>Pitches are note-block pitches, 0 (F#3) to 24 (F#5); {@link #lookPitch} (the old look-angle
 * scale) is kept for anything that still picks a note by gaze.
 */
public final class Notes {
    public static final TagKey<Item> INSTRUMENTS = TagKey.create(Registries.ITEM, TheSift.id("instruments"));
    public static final int MAX_PITCH = 24;
    /** {@link #play} flag: the player already heard the note (free play sounded it at once). */
    public static final int HEARD = 1;
    /** {@link #play} flag: strum the major chord on the note (the Weaver's Guitar); only the note itself counts for songs. */
    public static final int CHORD = 2;
    /** {@link #play} flag: struck with weight (an accent, a perfectly timed chime) - a little louder. */
    public static final int STRONG = 4;
    /** INS free play, {@link #play} flag: the clients draw this note themselves (notes flying from the instrument). */
    public static final int SHOWN = 8;
    /** INS free play, play-note flag: a held flute note breathing on - heard and seen, but not a new note for the songs. */
    public static final int SUSTAIN = 16;
    /** The look angle (degrees above/below the horizon) that reaches the top/bottom note. */
    private static final float RANGE_DEG = 60.0F;
    /** Degrees of look angle per semitone. */
    public static final float DEG_PER_NOTE = 2.0F * RANGE_DEG / MAX_PITCH;
    private static final String[] NAMES = {"F#", "G", "G#", "A", "A#", "B", "C", "C#", "D", "D#", "E", "F"};

    /** Client side: the local player's progress through the songs (the same rules as the server's tracker). */
    public static final SongMatcher CLIENT = new SongMatcher();
    /** Client side: the sheet the player pinned to the screen by using it, or null. */
    public static @Nullable Song clientPinned;

    private Notes() {
    }

    /** The note (0-24) for where this entity is looking (the old look-angle scale). */
    public static int lookPitch(Entity e) {
        float up = Mth.clamp(-e.getXRot(), -RANGE_DEG, RANGE_DEG);
        return Mth.clamp(Math.round((up + RANGE_DEG) / (2.0F * RANGE_DEG) * MAX_PITCH), 0, MAX_PITCH);
    }

    /** Where to look for a note on the old look-angle scale: degrees above (positive) or below (negative) the horizon. */
    public static int lookAngle(int pitch) {
        return Math.round((Mth.clamp(pitch, 0, MAX_PITCH) - MAX_PITCH / 2) * DEG_PER_NOTE);
    }

    /** "30° up", "straight ahead", "15° down": where to look for a note on the old look-angle scale. */
    public static Component aim(int pitch) {
        int a = lookAngle(pitch);
        if (a == 0) {
            return Component.translatable("music.thesift.aim.ahead");
        }
        return Component.translatable(a > 0 ? "music.thesift.aim.up" : "music.thesift.aim.down", Math.abs(a));
    }

    /** The sound pitch multiplier for a note (12 = 1.0). */
    public static float soundPitch(int pitch) {
        return (float) Math.pow(2.0, (pitch - 12) / 12.0);
    }

    /** The note's name, e.g. {@code F#4}. */
    public static String name(int pitch) {
        int p = Mth.clamp(pitch, 0, MAX_PITCH);
        int octave = 3 + (p + 6) / 12;
        return NAMES[p % 12] + octave;
    }

    /** The vanilla note-particle colour for a pitch (RGB). */
    public static int colour(int pitch) {
        float f = pitch / 24.0F;
        int r = (int) (Math.max(0.0F, Mth.sin((f + 0.0F) * Mth.TWO_PI) * 0.65F + 0.35F) * 255.0F);
        int g = (int) (Math.max(0.0F, Mth.sin((f + 0.33333334F) * Mth.TWO_PI) * 0.65F + 0.35F) * 255.0F);
        int b = (int) (Math.max(0.0F, Mth.sin((f + 0.6666667F) * Mth.TWO_PI) * 0.65F + 0.35F) * 255.0F);
        return r << 16 | g << 8 | b;
    }

    /** Where a player's instrument sounds from: just in front of their face. */
    public static Vec3 mouth(Player player) {
        return player.getEyePosition().add(player.getLookAngle().scale(0.6)).add(0.0, -0.2, 0.0);
    }

    /** The notes a chord on {@code root} sounds (a major triad, kept within the range). */
    public static int[] chord(int root) {
        int[] out = {root, root + 4, root + 7};
        for (int i = 1; i < out.length; i++) {
            if (out[i] > MAX_PITCH) {
                out[i] -= 12;
            }
        }
        return out;
    }

    /** A player plays one note: no colour, the game time as its clock. */
    public static void play(Level level, Player player, Instrument instrument, int pitch) {
        play(level, player, instrument, pitch, -1, level.getGameTime(), 0);
    }

    /**
     * A player plays one note on {@code instrument}, in light {@code colour} (-1 none; Prism
     * instruments) at {@code clock} (the player's own clock in ticks - for rhythm). On the server
     * the instrument sounds for everyone (but the player, with {@link #HEARD}), a coloured note
     * rises - a Prism note lights the place up - and {@link SongEvents#note} tells the song tracker
     * and every listener. On the client the note feeds the local guide ({@link #CLIENT}) and, with
     * {@link #HEARD}, sounds at once.
     */
    public static void play(Level level, Player player, Instrument instrument, int pitch, int colour, double clock, int flags) {
        int p = Mth.clamp(pitch, 0, MAX_PITCH);
        Vec3 at = mouth(player);
        if (level instanceof ServerLevel server) {
            p = SongTracker.tune(player, instrument, p, server.getGameTime());
            Player except = (flags & HEARD) != 0 ? player : null;
            float vol = (flags & STRONG) != 0 ? 1.7F : 1.4F;
            for (int n : (flags & CHORD) != 0 ? chord(p) : new int[]{p}) {
                float sp = soundPitch(n);
                server.playSound(except, at.x, at.y, at.z, instrument.sound(), SoundSource.PLAYERS, n == p ? vol : vol * 0.6F, sp);
                SoundEvent layer = instrument.layer();
                if (layer != null) {
                    server.playSound(except, at.x, at.y, at.z, layer, SoundSource.PLAYERS, n == p ? 0.45F : 0.25F, sp);
                }
            }
            if ((flags & SHOWN) == 0) {
                server.sendParticles(ModParticles.SIFT_NOTE.get(), at.x, at.y + 0.35, at.z, 0, p / 24.0, 0.0, 0.0, 1.0);
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.NOTE, at.x, at.y + 0.6, at.z, 0, p / 24.0, 0.0, 0.0, 1.0);
            }
            if (colour >= 0 && instrument.prism()) {
                PrismLight.flash(server, player, at, colour);
            }
            if (instrument.family() == Instrument.Family.DRUM) {
                // Stompers love a drum
                com.thesift.entity.Stomper.hearDrum(server, player.position(), 16.0);
            }
            SongEvents.note(server, player, at, p, instrument, colour, clock);
        } else {
            if ((flags & HEARD) != 0) {
                int sounded = CLIENT.tune(p, level.getGameTime(), instrument, s -> SongTracker.carriesSheet(player, s));
                hearLocally(player, instrument, sounded, flags);
            }
            CLIENT.hear(p, colour, clock, level.getGameTime(), instrument, s -> SongTracker.carriesSheet(player, s));
        }
    }

    /** Client: sounds a note for the local player only, at once (free play). */
    public static void hearLocally(Player player, Instrument instrument, int pitch, int flags) {
        float vol = (flags & STRONG) != 0 ? 1.0F : 0.85F;
        for (int n : (flags & CHORD) != 0 ? chord(pitch) : new int[]{pitch}) {
            float sp = soundPitch(n);
            player.playSound(instrument.sound(), n == pitch ? vol : vol * 0.6F, sp);
            SoundEvent layer = instrument.layer();
            if (layer != null) {
                player.playSound(layer, n == pitch ? 0.32F : 0.18F, sp);
            }
        }
    }

    /** Client side: how many of the song's notes the local player has just played in order. */
    public static int clientProgress(Song song, long now) {
        return CLIENT.progress(song, now);
    }
}
