package com.thesift.music;

import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

/**
 * One player's progress through every song, note by note. The server keeps one per player (the
 * {@link SongTracker}), the client one for the local player (free play's guide), and both
 * run the same rules, so the guide always shows what the server will accept.
 *
 * <p>Forgiving on purpose:
 * <ul>
 *   <li>a note counts if it is within {@link #TOLERANCE} semitone of the written one;</li>
 *   <li>up to {@link #GAP} ticks (4 s) between two notes;</li>
 *   <li>playing the note you just played again (a double tap, a drum roll) changes nothing;</li>
 *   <li>one stray wrong note per attempt is ignored;</li>
 *   <li>playing the first note of a song again starts it over from there;</li>
 *   <li>a song with a rhythm (the drums') wants each note its {@link Song#gap} after the last,
 *   give or take {@link #BEAT_SLACK} ticks or {@link #BEAT_SHARE} of the gap - and one note off
 *   the beat is forgiven too (it uses up the same single slip as a stray note);</li>
 *   <li>a Prism song wants each note in its colour of light; the right note in the wrong light
 *   still counts, but uses up the slip.</li>
 * </ul>
 * Only songs the player knows (carries the sheet of) and whose instrument fits are followed. A
 * shadow progress that ignores the instrument (and the colours) tells the tracker when someone is
 * playing the right notes on the wrong instrument, so it can say so.
 *
 * <p>Times: {@code now} is the game time (for the pauses); {@code clock} is when the note was
 * played on the player's own clock, in ticks with fractions (for rhythm) - free play sends it
 * with every note, so the network's delay never spoils a beat.
 */
public final class SongMatcher {
    /** The longest pause between two notes of one song (4 s). */
    public static final int GAP = 80;
    /** How many semitones a note may be off and still count. */
    public static final int TOLERANCE = 1;
    /** A beat may land this many ticks early or late at least... */
    public static final double BEAT_SLACK = 3.0;
    /** ...or this share of the gap before it, if that is more. */
    public static final double BEAT_SHARE = 0.4;
    private static final Song[] SONGS = Song.values();

    private final int[] done = new int[SONGS.length];
    private final boolean[] slipped = new boolean[SONGS.length];
    private final double[] at = new double[SONGS.length];
    private final int[] shadow = new int[SONGS.length];
    private final boolean[] shadowSlipped = new boolean[SONGS.length];
    private final double[] shadowAt = new double[SONGS.length];
    private long last = Long.MIN_VALUE / 2;
    private @Nullable Song wrong;
    /** Server: when the last wrong-instrument hint was shown (game time). */
    public long hintAt = Long.MIN_VALUE / 2;
    /** The last song completed, and when (game time) - for the guide's little flourish. */
    public @Nullable Song lastCompleted;
    public long completedAt = Long.MIN_VALUE / 2;
    /** How the last note sat against the song it advanced: -1 none, else how far off its beat it was (ticks, rhythmic songs). */
    public double lastOffBeat = -1.0;

    /** True if {@code played} is close enough to the written {@code note}. */
    public static boolean matches(int note, int played) {
        return Math.abs(note - played) <= TOLERANCE;
    }

    /** True if {@code dt} ticks after the last note is close enough to a written {@code gap}. */
    public static boolean onBeat(int gap, double dt) {
        return Math.abs(dt - gap) <= window(gap);
    }

    /** How early or late a note may come on a written {@code gap}, in ticks. */
    public static double window(int gap) {
        return Math.max(BEAT_SLACK, gap * BEAT_SHARE);
    }

    /** Hears one note without a colour, its clock the game time. */
    public @Nullable Song hear(int pitch, long now, @Nullable Instrument instrument, Predicate<Song> known) {
        return this.hear(pitch, -1, now, now, instrument, known);
    }

    /**
     * Hears one note at game time {@code now}, played on {@code instrument} (null: not an
     * instrument) in light {@code colour} (-1 none) at {@code clock}. {@code known} says which songs
     * the player can perform (carries the sheet of).
     *
     * @return the song this note completed, or null
     */
    public @Nullable Song hear(int pitch, int colour, double clock, long now, @Nullable Instrument instrument, Predicate<Song> known) {
        if (now - this.last > GAP || now < this.last) {
            this.reset();
        }
        this.last = now;
        this.wrong = null;
        this.lastOffBeat = -1.0;
        Song completed = null;
        for (Song song : SONGS) {
            int s = song.ordinal();
            if (!known.test(song)) {
                this.done[s] = 0;
                this.shadow[s] = 0;
                continue;
            }
            if (song.accepts(instrument)) {
                int before = this.done[s];
                double was = this.at[s];
                if (step(this.done, this.slipped, this.at, song, pitch, colour, clock, true) >= song.length() && completed == null) {
                    completed = song;
                }
                if (this.done[s] == before + 1 && before > 0 && song.rhythmic()) {
                    this.lastOffBeat = Math.abs(clock - was - song.gap(before - 1));
                }
            } else {
                this.done[s] = 0;
                int sh = step(this.shadow, this.shadowSlipped, this.shadowAt, song, pitch, colour, clock, false);
                if (sh >= 2 && (this.wrong == null || sh > this.shadow[this.wrong.ordinal()])) {
                    this.wrong = song;
                }
                if (sh >= song.length()) {
                    this.shadow[s] = 0;
                    this.shadowSlipped[s] = false;
                }
            }
        }
        if (completed != null) {
            this.reset();
            this.lastCompleted = completed;
            this.completedAt = now;
        }
        return completed;
    }

    /** The song the last note was played for on the wrong instrument (its right notes, at least two so far), or null. */
    public @Nullable Song wrongInstrument() {
        return this.wrong;
    }

    /** True once the pause since the last note is too long for any song to go on. */
    public boolean idle(long now) {
        return now - this.last > GAP || now < this.last;
    }

    /** How many notes of the song have been played so far (0 once the pause since the last note is too long). */
    public int progress(Song song, long now) {
        return this.idle(now) ? 0 : this.done[song.ordinal()];
    }

    /** The note the song wants next (its first again once it is complete). */
    public int nextNote(Song song, long now) {
        return song.note(this.nextIndex(song, now));
    }

    /** The index of the note the song wants next. */
    public int nextIndex(Song song, long now) {
        int d = this.progress(song, now);
        return d >= song.length() ? 0 : d;
    }

    /** The colour of light the song wants next (-1 if it is not a Prism song). */
    public int nextColour(Song song, long now) {
        return song.colour(this.nextIndex(song, now));
    }

    /** The clock of the last note that counted for the song (meaningful while {@link #progress} is above 0). */
    public double lastAt(Song song) {
        return this.at[song.ordinal()];
    }

    /** True if the song's one forgiven slip is already used up. */
    public boolean slipped(Song song) {
        return this.slipped[song.ordinal()];
    }

    /**
     * The note to sound for {@code pitch}: nudged onto the note a song the player is playing wants
     * next when it is already within tolerance (the song furthest along wins, so a song that takes
     * any instrument cannot pull the note off the one being played) - so a forgiven note also
     * sounds right.
     */
    public int tune(int pitch, long now, Instrument instrument, Predicate<Song> known) {
        int best = 0;
        int tuned = pitch;
        for (Song song : SONGS) {
            int d = this.progress(song, now);
            if (d > best && song.accepts(instrument) && known.test(song)) {
                int want = this.nextNote(song, now);
                if (want == pitch) {
                    return pitch;
                }
                if (matches(want, pitch)) {
                    best = d;
                    tuned = want;
                }
            }
        }
        return tuned;
    }

    public void reset() {
        for (int i = 0; i < SONGS.length; i++) {
            this.done[i] = 0;
            this.slipped[i] = false;
            this.shadow[i] = 0;
            this.shadowSlipped[i] = false;
        }
    }

    /** True if the colour is right for note {@code i} (always, for a song without colours or when colours are ignored). */
    private static boolean lit(Song song, int i, int colour, boolean colours) {
        return !colours || !song.prism() || song.colour(i) == colour;
    }

    /** One note against one song's progress; returns the new progress. */
    private static int step(int[] done, boolean[] slipped, double[] at, Song song, int pitch, int colour, double clock, boolean colours) {
        int s = song.ordinal();
        int i = Math.min(done[s], song.length() - 1);
        boolean first = matches(song.note(0), pitch) && lit(song, 0, colour, colours);
        if (matches(song.note(i), pitch)) {
            boolean offLight = !lit(song, i, colour, colours);
            boolean offBeat = i > 0 && song.rhythmic() && !onBeat(song.gap(i - 1), clock - at[s]);
            if (offLight || offBeat) {
                if (slipped[s] || offLight && offBeat) {
                    // a second slip: the song is lost - unless this note can begin it again
                    done[s] = first ? 1 : 0;
                    slipped[s] = false;
                    at[s] = clock;
                    return done[s];
                }
                // the right note in the wrong light, or off the beat, still counts - once; the beat carries on from it
                slipped[s] = true;
            }
            done[s] = i + 1;
            at[s] = clock;
        } else if (i > 0 && matches(song.note(i - 1), pitch)) {
            // the note just played, again: a double tap (or a drum roll) - nothing changes, not even the beat
        } else if (i > 0 && first) {
            // starting over
            done[s] = 1;
            slipped[s] = false;
            at[s] = clock;
        } else if (i > 0 && !slipped[s]) {
            // one stray note is forgiven
            slipped[s] = true;
        } else {
            done[s] = 0;
            slipped[s] = false;
        }
        return done[s];
    }
}
