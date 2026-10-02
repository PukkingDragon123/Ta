package com.thesift.music;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import org.jspecify.annotations.Nullable;

/**
 * The voice an instrument plays its notes with: a tuned note-block sound, an optional second
 * layer (the drum's thump, the prism's shimmer) and the colour of its beam and flourish.
 */
public enum Instrument {
    FLUTE(SoundEvents.NOTE_BLOCK_FLUTE, null, 0x7FF5E6),
    PRISM_FLUTE(SoundEvents.NOTE_BLOCK_FLUTE, SoundEvents.NOTE_BLOCK_CHIME, 0xF59AF0),
    DRUM(SoundEvents.NOTE_BLOCK_BASS, SoundEvents.NOTE_BLOCK_BASEDRUM, 0xE8C890),
    PRISM_DRUM(SoundEvents.NOTE_BLOCK_BASS, SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE, 0xB9B8FF),
    HARP(SoundEvents.NOTE_BLOCK_HARP, SoundEvents.NOTE_BLOCK_CHIME, 0xFFE27A),
    GUITAR(SoundEvents.NOTE_BLOCK_GUITAR, null, 0x29DFEB),
    BELL(SoundEvents.NOTE_BLOCK_BELL, null, 0xFFF4D6);

    private final Holder<SoundEvent> sound;
    private final @Nullable Holder<SoundEvent> layer;
    private final int colour;

    Instrument(Holder<SoundEvent> sound, @Nullable Holder<SoundEvent> layer, int colour) {
        this.sound = sound;
        this.layer = layer;
        this.colour = colour;
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
}
