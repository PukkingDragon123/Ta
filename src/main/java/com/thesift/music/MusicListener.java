package com.thesift.music;

import net.minecraft.core.BlockPos;

/** An entity that reacts when music is played near it. */
public interface MusicListener {
    void hearMusic(BlockPos source, float strength);
}
