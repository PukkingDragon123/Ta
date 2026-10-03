package com.thesift.music;

import java.util.Locale;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import org.jspecify.annotations.Nullable;

/**
 * The voice an instrument plays its notes with: a tuned note-block sound, an optional second
 * layer (the drum's thump, the prism's shimmer), the colour of its beam and flourish - and its
 * {@link Family}, which is what a {@link Song} asks for.
 */
public enum Instrument {
    FLUTE(SoundEvents.NOTE_BLOCK_FLUTE, null, 0x7FF5E6, Family.FLUTE),
    PRISM_FLUTE(SoundEvents.NOTE_BLOCK_FLUTE, SoundEvents.NOTE_BLOCK_CHIME, 0xF59AF0, Family.FLUTE),
    DRUM(SoundEvents.NOTE_BLOCK_BASS, SoundEvents.NOTE_BLOCK_BASEDRUM, 0xE8C890, Family.DRUM),
    PRISM_DRUM(SoundEvents.NOTE_BLOCK_BASS, SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE, 0xB9B8FF, Family.DRUM),
    HARP(SoundEvents.NOTE_BLOCK_HARP, SoundEvents.NOTE_BLOCK_CHIME, 0xFFE27A, Family.STRINGS),
    GUITAR(SoundEvents.NOTE_BLOCK_GUITAR, null, 0x29DFEB, Family.STRINGS),
    BELL(SoundEvents.NOTE_BLOCK_BELL, null, 0xFFF4D6, Family.CHIMES),
    /** C4 songs: the Wind Chimes, the Echoer's own voice. */
    WIND_CHIMES(SoundEvents.NOTE_BLOCK_CHIME, null, 0xBFF6FF, Family.CHIMES);

    /**
     * The kinds of instrument a song can ask for. A gem-inlaid instrument plays the songs of its
     * plain kin (the Prism Flute plays flute songs).
     */
    public enum Family {
        /** Crane Flute, Prism Flute. */
        FLUTE,
        /** Conga Drum, Prism Drum. */
        DRUM,
        /** Guitar, Weaver's Guitar, Prism Harp. */
        STRINGS,
        /** Wind Chimes. */
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

    Instrument(Holder<SoundEvent> sound, @Nullable Holder<SoundEvent> layer, int colour, Family family) {
        this.sound = sound;
        this.layer = layer;
        this.colour = colour;
        this.family = family;
    }

    public SoundEvent sound() {
        return this.sound.value();
    }

    public @Nullable SoundEvent layer() {
        return this.layer == null ? null : this.layer.value();
    }

    /** RGB colour of this instrument's beam and flourish. */
    public int colour() {
        return this.colour;
    }

    /** The kind of instrument this is, for the songs that ask for one. */
    public Family family() {
        return this.family;
    }
}
