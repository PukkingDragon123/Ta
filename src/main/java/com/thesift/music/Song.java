package com.thesift.music;

import java.util.Locale;

/**
 * The songs of the Sift. Each is written on its own Music Sheet item
 * ({@code thesift:music_sheet_<id>}); playing its notes in order on any instrument (within a few
 * seconds of each other) performs it, and {@link SongEvents} tells everyone listening.
 *
 * <p>Pitches are note-block pitches, 0 (F#3) to 24 (F#5).
 */
public enum Song {
    /** The Echoer's offering song: play it beside an offering to begin a trade. */
    OFFERING(new int[]{6, 10, 13, 18, 13, 10, 6}),
    /** The Nibs' song: the little wisps of the Sound Garden dance and turn to treasure. */
    NIB(new int[]{18, 20, 22, 18, 15, 13}),
    /** Wakes and recharges Soul Golems. */
    GOLEM(new int[]{6, 6, 13, 13, 11, 6}),
    /** The Caravans' crystal hymn: calms a colony (or lures its queen). */
    CRYSTAL(new int[]{13, 17, 20, 17, 13, 8}),
    /** The whale song: Sky Whales answer it. */
    WHALE(new int[]{8, 6, 3, 6, 8, 13}),
    /** The tide song: opens drowned vaults of the deep and calms the Gobbler. */
    TIDE(new int[]{3, 8, 10, 8, 3, 1}),
    /** The lullaby: puts nearby creatures to sleep, opens harmony seals. */
    LULLABY(new int[]{13, 11, 10, 8, 10, 6});

    private final int[] notes;

    Song(int[] notes) {
        this.notes = notes;
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

    /** Lower-case id, as used in item ids ({@code music_sheet_<id>}) and translation keys. */
    public String id() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
