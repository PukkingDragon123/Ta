package com.thesift.music;

import java.util.Arrays;
import java.util.Locale;
import java.util.TreeSet;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import org.jspecify.annotations.Nullable;

/**
 * Every playable instrument version: its voice (a tuned note-block sound and an optional second
 * layer), the colour of its flourish, its {@link Family} (what a {@link Song} asks for and how it is
 * played), its tier and the notes it can reach.
 *
 * <p>M1 instrument play: each family is played in its own way, on its own screen (client
 * {@code InstrumentScreen}). Normal instruments have no powers; progression is finding the better
 * versions - more notes, a new way to play, and at the top the Prism versions, whose every note is
 * also a coloured light ({@link PrismLight}) that Prism songs ask for.
 *
 * <ul>
 *   <li>STRINGS - pick notes: {@link #layout()} is the open strings, {@link #extra()} the frets per
 *   string (the Prism Harp: one string per note).</li>
 *   <li>FLUTE - breath and fingering: {@link #layout()} is the scale from all holes covered to all
 *   open, {@link #extra()} 1 if it can overblow an octave higher.</li>
 *   <li>DRUM - rhythm: {@link #layout()} is the pads, {@link #extra()} 1 if a held pad rolls.</li>
 *   <li>CHIMES - timing: {@link #layout()} is the chimes, {@link #extra()} 1 if you can blow a gust
 *   that swings them faster.</li>
 * </ul>
 * Constants keep their order (older code may store ordinals); new versions are added at the end.
 */
public enum Instrument {
    /** The Crane Flute: six holes, seven notes. */
    FLUTE(SoundEvents.NOTE_BLOCK_FLUTE, null, 0x7FF5E6, Family.FLUTE, 1, false, new int[]{3, 6, 8, 10, 13, 15, 18}, 0),
    /** The Prism Flute: the Serbim Flute's fingering and overblow, and a light for every note. */
    PRISM_FLUTE(SoundEvents.NOTE_BLOCK_FLUTE, SoundEvents.NOTE_BLOCK_CHIME, 0xF59AF0, Family.FLUTE, 3, true, Layouts.G_MAJOR, 1),
    /** The Conga Drum: two drums, three strokes each. */
    DRUM(SoundEvents.NOTE_BLOCK_BASS, SoundEvents.NOTE_BLOCK_BASEDRUM, 0xE8C890, Family.DRUM, 1, false, new int[]{1, 3, 6, 8, 10, 13}, 0),
    /** The Prism Drum: the Thunder Drums' eight pads and rolls, lit. */
    PRISM_DRUM(SoundEvents.NOTE_BLOCK_BASS, SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE, 0xB9B8FF, Family.DRUM, 3, true, Layouts.EIGHT_PADS, 1),
    /** The Prism Harp: a string for every note, each plucked in a colour of light. */
    HARP(SoundEvents.NOTE_BLOCK_HARP, SoundEvents.NOTE_BLOCK_CHIME, 0xFFE27A, Family.STRINGS, 3, true, Layouts.range(0, 24), 1),
    /** The Guitar: four strings, five frets. */
    GUITAR(SoundEvents.NOTE_BLOCK_GUITAR, null, 0x29DFEB, Family.STRINGS, 1, false, new int[]{6, 11, 16, 21}, 5),
    /** A plain bell for blocks and creatures (no item plays it). */
    BELL(SoundEvents.NOTE_BLOCK_BELL, null, 0xFFF4D6, Family.CHIMES, 1, false, Layouts.range(0, 24), 0),
    /** C4 songs: the Wind Chimes, the Echoer's own voice - seven chimes. */
    WIND_CHIMES(SoundEvents.NOTE_BLOCK_CHIME, null, 0xBFF6FF, Family.CHIMES, 1, false, new int[]{6, 8, 10, 13, 15, 18, 20}, 0),
    // ---- M1 instrument play: the upgraded versions
    /** The Serbim Flute: seven holes and an overblown octave. */
    SERBIM_FLUTE(SoundEvents.NOTE_BLOCK_FLUTE, SoundEvents.NOTE_BLOCK_BELL, 0xA9D8FF, Family.FLUTE, 2, false, Layouts.G_MAJOR, 1),
    /** The Thunder Drums: four drums, eight pads, and a held pad rolls. */
    THUNDER_DRUMS(SoundEvents.NOTE_BLOCK_BASEDRUM, SoundEvents.NOTE_BLOCK_BASS, 0xD8A070, Family.DRUM, 2, false, Layouts.EIGHT_PADS, 1),
    /** The Star Lute: six strings over the whole range, and strummed chords. */
    LUTE(SoundEvents.NOTE_BLOCK_GUITAR, SoundEvents.NOTE_BLOCK_HARP, 0xFFD27A, Family.STRINGS, 2, false, Layouts.SIX_STRINGS, 5),
    /** The Weaver's Guitar (a boss's instrument): plays like the Star Lute. */
    WEAVER_GUITAR(SoundEvents.NOTE_BLOCK_GUITAR, SoundEvents.NOTE_BLOCK_BIT, 0x9FFBFF, Family.STRINGS, 2, false, Layouts.SIX_STRINGS, 5),
    /** The Glass Bells: ten chimes of chime glass, and a gust to swing them. */
    GLASS_BELLS(SoundEvents.NOTE_BLOCK_CHIME, SoundEvents.NOTE_BLOCK_BELL, 0xC8F0FF, Family.CHIMES, 2, false, Layouts.TEN_CHIMES, 1),
    /** The Prism Chimes: the Glass Bells, lit. */
    PRISM_CHIMES(SoundEvents.NOTE_BLOCK_CHIME, SoundEvents.NOTE_BLOCK_PLING, 0xFFB8F0, Family.CHIMES, 3, true, Layouts.TEN_CHIMES, 1);

    /**
     * The kinds of instrument a song can ask for - and the four ways to play. Every version plays
     * the songs of its family (the Prism Flute plays flute songs).
     */
    public enum Family {
        /** Breath and fingering: Crane Flute, Serbim Flute, Prism Flute. */
        FLUTE,
        /** Rhythm: Conga Drum, Thunder Drums, Prism Drum. */
        DRUM,
        /** Picked notes: Guitar, Star Lute, Weaver's Guitar, Prism Harp. */
        STRINGS,
        /** Timing: Wind Chimes, Glass Bells, Prism Chimes. */
        CHIMES;

        /** Lower-case id, for translation keys ({@code instrument.thesift.<id>}). */
        public String id() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    private final Holder<SoundEvent> sound;
    private final @Nullable Holder<SoundEvent> layer;
    private final int colour;
    private final Family family;
    private final int tier;
    private final boolean prism;
    private final int[] layout;
    private final int extra;
    private final int[] notes;

    Instrument(Holder<SoundEvent> sound, @Nullable Holder<SoundEvent> layer, int colour, Family family, int tier, boolean prism, int[] layout,
            int extra) {
        this.sound = sound;
        this.layer = layer;
        this.colour = colour;
        this.family = family;
        this.tier = tier;
        this.prism = prism;
        this.layout = layout;
        this.extra = extra;
        this.notes = reach(family, layout, extra);
    }

    /** Every note a layout reaches, sorted, without repeats. */
    private static int[] reach(Family family, int[] layout, int extra) {
        TreeSet<Integer> set = new TreeSet<>();
        for (int n : layout) {
            if (family == Family.STRINGS) {
                for (int fret = 0; fret < Math.max(1, extra); fret++) {
                    set.add(n + fret);
                }
            } else {
                set.add(n);
                if (family == Family.FLUTE && extra > 0) {
                    set.add(n + 12);
                }
            }
        }
        return set.stream().filter(n -> n >= 0 && n <= Notes.MAX_PITCH).mapToInt(Integer::intValue).toArray();
    }

    public SoundEvent sound() {
        return this.sound.value();
    }

    public @Nullable SoundEvent layer() {
        return this.layer == null ? null : this.layer.value();
    }

    /** RGB colour of this instrument's flourish. */
    public int colour() {
        return this.colour;
    }

    /** The kind of instrument this is, for the songs that ask for one. */
    public Family family() {
        return this.family;
    }

    /** 1 for the plain instruments, 2 for the upgraded ones, 3 for the Prism versions. */
    public int tier() {
        return this.tier;
    }

    /** True for the Prism versions: every note is played in a colour of light, and Prism songs can be played. */
    public boolean prism() {
        return this.prism;
    }

    /** The strings, scale, pads or chimes, low to high (see the class comment). */
    public int[] layout() {
        return this.layout.clone();
    }

    public int layoutSize() {
        return this.layout.length;
    }

    public int layout(int i) {
        return this.layout[i];
    }

    /** Frets per string (strings), overblowing (flute), rolls (drum) or the gust (chimes) - see the class comment. */
    public int extra() {
        return this.extra;
    }

    /** True if this version has its family's extra mechanic: chords, overblowing, rolls or the gust. */
    public boolean hasMechanic() {
        return this.family == Family.STRINGS ? this.tier == 2 : this.extra > 0;
    }

    /** True for the strings that strum chords (the Star Lute and the Weaver's Guitar). */
    public boolean chords() {
        return this.family == Family.STRINGS && this.tier == 2;
    }

    /** Every note this instrument can play, low to high. */
    public int[] notes() {
        return this.notes.clone();
    }

    public int lowest() {
        return this.notes[0];
    }

    public int highest() {
        return this.notes[this.notes.length - 1];
    }

    public int noteCount() {
        return this.notes.length;
    }

    /** True if this instrument can sound that very note. */
    public boolean canPlay(int pitch) {
        return Arrays.binarySearch(this.notes, pitch) >= 0;
    }

    /** True if every note of the song is within the matcher's tolerance of a note this instrument has. */
    public boolean reaches(Song song) {
        for (int i = 0; i < song.length(); i++) {
            if (this.nearest(song.note(i)) < 0) {
                return false;
            }
        }
        return true;
    }

    /** The playable note closest to {@code pitch} that the songs accept for it, or -1. */
    public int nearest(int pitch) {
        int best = -1;
        for (int n : this.notes) {
            if (SongMatcher.matches(pitch, n) && (best < 0 || Math.abs(n - pitch) < Math.abs(best - pitch))) {
                best = n;
            }
        }
        return best;
    }

    /** Lower-case id, for translation keys ({@code instrument.thesift.version.<id>}). */
    public String id() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    /** Shared layouts. */
    private static final class Layouts {
        /** G major from G3 to G4: all seven holes covered down to all open (overblown: G4 to F#5). */
        static final int[] G_MAJOR = {1, 3, 5, 6, 8, 10, 12, 13};
        /** Four drums, a low and a high stroke on each. */
        static final int[] EIGHT_PADS = {1, 3, 6, 8, 10, 13, 15, 18};
        /** Six strings a major third apart: five frets each reach the whole two octaves. */
        static final int[] SIX_STRINGS = {0, 4, 8, 12, 16, 20};
        /** Two octaves of the pentatonic scale. */
        static final int[] TEN_CHIMES = {1, 3, 6, 8, 10, 13, 15, 18, 20, 22};

        private Layouts() {
        }

        static int[] range(int lo, int hi) {
            int[] out = new int[hi - lo + 1];
            for (int i = 0; i < out.length; i++) {
                out[i] = lo + i;
            }
            return out;
        }
    }
}
