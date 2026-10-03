package com.thesift.music.band;

/**
 * CR1: a creature that does something of its own whenever its band voice sounds - a bell's clapper
 * strikes, a speaker pumps and its drill whirrs. {@link BandVoice#play} calls it on the server for
 * every note the creature plays: along with its band, in a band's closing chord, or humming or
 * answering a song. The note itself is already playing; this is for the creature's own show.
 */
public interface BandPlayer {
    /** Server side: this creature just sounded {@code pitch} (0-24, as played) at {@code loudness} (1 normal). */
    void playedBandNote(int pitch, float loudness);
}
