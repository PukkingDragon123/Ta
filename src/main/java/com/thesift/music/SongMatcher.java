package com.thesift.music;

import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

/**
 * One player's progress through every song, note by note. The server keeps one per player (the
 * {@link SongTracker}), the client one for the local player (the on-screen guide), and both run
 * the same rules, so the guide always shows what the server will accept.
 *
 * <p>Forgiving on purpose:
 * <ul>
 *   <li>a note counts if it is within {@link #TOLERANCE} semitone of the written one (the look
 *   angle only has to land within about 7 degrees of the right spot);</li>
 *   <li>up to {@link #GAP} ticks (4 s) between two notes;</li>
 *   <li>playing the note you just played again (a double tap) changes nothing;</li>
 *   <li>one stray wrong note per attempt is ignored;</li>
 *   <li>playing the first note of a song again starts it over from there.</li>
 * </ul>
 * Only songs the player knows (carries the sheet of) and whose instrument fits are followed. A
 * shadow progress that ignores the instrument tells the tracker when someone is playing the right
 * notes on the wrong instrument, so it can say so.
 */
public final class SongMatcher {
    /** The longest pause between two notes of one song (4 s). */
    public static final int GAP = 80;
    /** How many semitones a note may be off and still count. */
    public static final int TOLERANCE = 1;
    private static final Song[] SONGS = Song.values();

    private final int[] done = new int[SONGS.length];
    private final boolean[] slipped = new boolean[SONGS.length];
    private final int[] shadow = new int[SONGS.length];
    private final boolean[] shadowSlipped = new boolean[SONGS.length];
    private long last = Long.MIN_VALUE / 2;
    private @Nullable Song wrong;
    /** Server: when the last wrong-instrument hint was shown (game time). */
    public long hintAt = Long.MIN_VALUE / 2;
    /** The last song completed, and when (game time) - for the guide's little flourish. */
    public @Nullable Song lastCompleted;
    public long completedAt = Long.MIN_VALUE / 2;

    /** True if {@code played} is close enough to the written {@code note}. */
    public static boolean matches(int note, int played) {
        return Math.abs(note - played) <= TOLERANCE;
    }

    /**
     * Hears one note at game time {@code now}, played on {@code instrument} (null: not an
     * instrument). {@code known} says which songs the player can perform (carries the sheet of).
     *
     * @return the song this note completed, or null
     */
    public @Nullable Song hear(int pitch, long now, @Nullable Instrument instrument, Predicate<Song> known) {
        if (now - this.last > GAP || now < this.last) {
            this.reset();
        }
        this.last = now;
        this.wrong = null;
        Song completed = null;
        for (Song song : SONGS) {
            int s = song.ordinal();
            if (!known.test(song)) {
                this.done[s] = 0;
                this.shadow[s] = 0;
                continue;
            }
            if (song.accepts(instrument)) {
                if (step(this.done, this.slipped, song, pitch) >= song.length() && completed == null) {
                    completed = song;
                }
            } else {
                this.done[s] = 0;
                int sh = step(this.shadow, this.shadowSlipped, song, pitch);
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
        int d = this.progress(song, now);
        return song.note(d >= song.length() ? 0 : d);
    }

    public void reset() {
        for (int i = 0; i < SONGS.length; i++) {
            this.done[i] = 0;
            this.slipped[i] = false;
            this.shadow[i] = 0;
            this.shadowSlipped[i] = false;
        }
    }

    /** One note against one song's progress; returns the new progress. */
    private static int step(int[] done, boolean[] slipped, Song song, int pitch) {
        int s = song.ordinal();
        int i = Math.min(done[s], song.length() - 1);
        if (matches(song.note(i), pitch)) {
            done[s] = i + 1;
        } else if (i > 0 && matches(song.note(i - 1), pitch)) {
            // the note just played, again: a double tap
            done[s] = i;
        } else if (i > 0 && matches(song.note(0), pitch)) {
            // starting over
            done[s] = 1;
            slipped[s] = false;
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
