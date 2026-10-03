package com.thesift.music;

import java.util.Locale;
import org.jspecify.annotations.Nullable;

/**
 * The songs of the Sift. Each is written on its own Music Sheet item
 * ({@code thesift:music_sheet_<id>}) and asks for one kind of instrument ({@link #instrument()},
 * or any instrument when that is null). Playing its notes in order on that instrument while
 * carrying the sheet performs it (see {@link SongMatcher} for how forgiving that is), and
 * {@link SongEvents} tells everyone listening.
 *
 * <p>Pitches are note-block pitches, 0 (F#3) to 24 (F#5); 12 (F#4) is played looking straight
 * ahead, and every 5 degrees up or down is one semitone (see {@link Notes#lookPitch}).
 *
 * <p>The notes and instruments are mirrored in tools/songs.py (sheet art and tooltips' lang).
 */
public enum Song {
    /** The Echoer's offering song, rung on the Wind Chimes beside an Echoer waiting with your offering. */
    OFFERING(new int[]{6, 10, 13, 18, 13, 10, 6}, Instrument.Family.CHIMES),
    /** The Nibs' song: the little wisps of the Sound Garden dance and turn to treasure. */
    NIB(new int[]{18, 20, 22, 18, 15, 13}, Instrument.Family.STRINGS),
    /** Wakes and recharges Soul Golems - any instrument will do. */
    GOLEM(new int[]{6, 6, 13, 13, 11, 6}, null),
    /** The Caravans' crystal hymn: calms a colony (or lures its queen). */
    CRYSTAL(new int[]{13, 17, 20, 17, 13, 8}, Instrument.Family.CHIMES),
    /** The whale song: Sky Whales answer it. */
    WHALE(new int[]{8, 6, 3, 6, 8, 13}, Instrument.Family.FLUTE),
    /** The tide song: opens drowned vaults of the deep and calms the Gobbler. */
    TIDE(new int[]{3, 8, 10, 8, 3, 1}, Instrument.Family.DRUM),
    /** The lullaby: puts nearby creatures to sleep, opens harmony seals. */
    LULLABY(new int[]{13, 11, 10, 8, 10, 6}, Instrument.Family.STRINGS);

    private final int[] notes;
    private final Instrument.@Nullable Family instrument;

    Song(int[] notes, Instrument.@Nullable Family instrument) {
        this.notes = notes;
        this.instrument = instrument;
    }

    public int[] notes() {
        return this.notes.clone();
    }

    public int length() {
        return this.notes.length;
    }

    public int note(int i) {
        return this.notes[i];
    }

    /** The kind of instrument this song must be played on, or null for any instrument. */
    public Instrument.@Nullable Family instrument() {
        return this.instrument;
    }

    /**
     * True if a note played on {@code played} counts towards this song. Notes from blocks and
     * creatures (no instrument) count only for the songs that take any instrument.
     */
    public boolean accepts(@Nullable Instrument played) {
        return this.instrument == null || played != null && played.family() == this.instrument;
    }

    /** Translation key of what this song is played on ({@code instrument.thesift.<family>} or {@code .any}). */
    public String instrumentKey() {
        return "instrument.thesift." + (this.instrument == null ? "any" : this.instrument.id());
    }

    /** Lower-case id, as used in item ids ({@code music_sheet_<id>}) and translation keys. */
    public String id() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
