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
 * <p>Pitches are note-block pitches, 0 (F#3) to 24 (F#5).
 *
 * <p>M1 instrument play: a drum song also carries its rhythm ({@link #rhythmic()}: how many
 * half-beats each note lasts before the next, {@link #STEP} ticks each), and a Prism song carries a
 * colour of light for every note ({@link #prism()}, colours from {@link PrismLight}) - it can only
 * be played on a Prism instrument, each note in its colour.
 *
 * <p>The notes, rhythms, colours and instruments are mirrored in tools/songs.py (sheet art, lang).
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
    // CLEAN: the Tide Song is gone (SPEC 4); the Gobbler now answers the Lullaby
    /** The lullaby: puts nearby creatures to sleep and lulls the Gobbler. */
    LULLABY(new int[]{13, 11, 10, 8, 10, 6}, Instrument.Family.STRINGS),
    /** M1: the Aurora, a Prism song - every note in its colour of light, on any Prism instrument. */
    AURORA(new int[]{6, 10, 13, 18, 15, 13}, null, new int[0], new int[]{0, 1, 2, 3, 2, 0}),
    // ---- F2 Band Table & songs: a drum song, the Dolphin's symphony and the two enchanting songs
    /** The Heartbeat, a drum song: lub-dub, lub-dub, then a rising roll - the deep pulse under the Sift. Enchants weapons. */
    HEARTBEAT(new int[]{1, 3, 1, 3, 10, 8, 6, 1}, Instrument.Family.DRUM, new int[]{1, 3, 1, 3, 2, 2, 2, 4}, new int[0]),
    /** The Symphony of the Dolphin, whistled on a flute: it opens Clam Chests and sets dolphins dancing (heard via {@link SongEvents}). */
    DOLPHIN(new int[]{10, 15, 13, 18, 15, 10, 13, 18}, Instrument.Family.FLUTE),
    /** The Enchanter's Canon on strings, each phrase answering the last a fifth higher: binds songs into instruments. */
    CANON(new int[]{6, 13, 11, 16, 13, 18, 16, 21}, Instrument.Family.STRINGS),
    /** The Sculk Requiem, rung on chimes: the Ancient Cities' lament, which wards against the Sculk. */
    REQUIEM(new int[]{18, 15, 13, 8, 10, 6, 8, 6}, Instrument.Family.CHIMES);

    /** Ticks in one half-beat of a song's rhythm (120 beats a minute). */
    public static final int STEP = 5;

    private final int[] notes;
    private final Instrument.@Nullable Family instrument;
    /** Half-beats from each note to the next (the last: how long the last note rings); empty without a rhythm. */
    private final int[] beats;
    /** The colour of light of each note ({@link PrismLight}); empty for a song that is not a Prism song. */
    private final int[] colours;

    Song(int[] notes, Instrument.@Nullable Family instrument) {
        this(notes, instrument, new int[0], new int[0]);
    }

    Song(int[] notes, Instrument.@Nullable Family instrument, int[] beats, int[] colours) {
        this.notes = notes;
        this.instrument = instrument;
        this.beats = beats;
        this.colours = colours;
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

    /** The kind of instrument this song must be played on, or null for any instrument (of the right kind for a Prism song). */
    public Instrument.@Nullable Family instrument() {
        return this.instrument;
    }

    /** True if the song has a rhythm: each note must come its {@link #gap} after the one before. */
    public boolean rhythmic() {
        return this.beats.length == this.notes.length;
    }

    /** How many half-beats note {@code i} lasts (1 if the song has no rhythm). */
    public int beat(int i) {
        return this.rhythmic() ? this.beats[i] : 1;
    }

    /** Ticks from note {@code i} to note {@code i + 1} (0 for a song without a rhythm). */
    public int gap(int i) {
        return this.rhythmic() ? this.beats[i] * STEP : 0;
    }

    /** True for a Prism song: every note has its colour of light, and only Prism instruments play it. */
    public boolean prism() {
        return this.colours.length == this.notes.length;
    }

    /** The colour of light of note {@code i} ({@link PrismLight}), or -1 if this is not a Prism song. */
    public int colour(int i) {
        return this.prism() ? this.colours[i] : -1;
    }

    /**
     * True if a note played on {@code played} counts towards this song. Notes from blocks and
     * creatures (no instrument) count only for the songs that take any instrument; a Prism song
     * takes only Prism instruments.
     */
    public boolean accepts(@Nullable Instrument played) {
        if (played == null) {
            return this.instrument == null && !this.prism();
        }
        if (this.prism() && !played.prism()) {
            return false;
        }
        return this.instrument == null || played.family() == this.instrument;
    }

    /** Translation key of what this song is played on ({@code instrument.thesift.<family>}, {@code .prism} or {@code .any}). */
    public String instrumentKey() {
        if (this.instrument != null) {
            return "instrument.thesift." + this.instrument.id();
        }
        return "instrument.thesift." + (this.prism() ? "prism" : "any");
    }

    /** Lower-case id, as used in item ids ({@code music_sheet_<id>}) and translation keys. */
    public String id() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
