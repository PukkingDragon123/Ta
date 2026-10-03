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
 * <p>The note comes from where the player looks: straight ahead is the middle of the range
 * (F#4, pitch 12), and every {@link #DEG_PER_NOTE} degrees up plays a semitone higher, down
 * lower (pitch 0-24, like a note block). Holding any item in {@code #thesift:instruments} shows
 * the note ladder on screen.
 *
 * <p>Usage from an item's {@code use()} (call it on both sides; the client half feeds the
 * on-screen guide, the server half the song tracker):
 * <pre>{@code Notes.play(level, player, Instrument.GUITAR, Notes.lookPitch(player)); }</pre>
 */
public final class Notes {
    public static final TagKey<Item> INSTRUMENTS = TagKey.create(Registries.ITEM, TheSift.id("instruments"));
    public static final int MAX_PITCH = 24;
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

    /** The note (0-24) for where this entity is looking. */
    public static int lookPitch(Entity e) {
        float up = Mth.clamp(-e.getXRot(), -RANGE_DEG, RANGE_DEG);
        return Mth.clamp(Math.round((up + RANGE_DEG) / (2.0F * RANGE_DEG) * MAX_PITCH), 0, MAX_PITCH);
    }

    /** Where to look for a note: degrees above (positive) or below (negative) the horizon. */
    public static int lookAngle(int pitch) {
        return Math.round((Mth.clamp(pitch, 0, MAX_PITCH) - MAX_PITCH / 2) * DEG_PER_NOTE);
    }

    /** "30° up", "straight ahead", "15° down": where to look to play a note. */
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

    /**
     * A player plays one note: the instrument sounds, a coloured note rises, and on the server
     * {@link SongEvents#note} tells the song tracker and every listener which note and on what.
     */
    public static void play(Level level, Player player, Instrument instrument, int pitch) {
        int p = Mth.clamp(pitch, 0, MAX_PITCH);
        Vec3 at = mouth(player);
        if (level instanceof ServerLevel server) {
            p = SongTracker.tune(player, instrument, p, server.getGameTime());
            float sp = soundPitch(p);
            server.playSound(null, at.x, at.y, at.z, instrument.sound(), SoundSource.PLAYERS, 1.4F, sp);
            SoundEvent layer = instrument.layer();
            if (layer != null) {
                server.playSound(null, at.x, at.y, at.z, layer, SoundSource.PLAYERS, 0.45F, sp);
            }
            server.sendParticles(ModParticles.SIFT_NOTE.get(), at.x, at.y + 0.35, at.z, 0, p / 24.0, 0.0, 0.0, 1.0);
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.NOTE, at.x, at.y + 0.6, at.z, 0, p / 24.0, 0.0, 0.0, 1.0);
            SongEvents.note(server, player, at, p, instrument);
        } else {
            CLIENT.hear(p, level.getGameTime(), instrument, s -> SongTracker.carriesSheet(player, s));
        }
    }

    /** Client side: how many of the song's notes the local player has just played in order. */
    public static int clientProgress(Song song, long now) {
        return CLIENT.progress(song, now);
    }
}
