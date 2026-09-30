package com.thesift.music;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** A block that physically reacts when music is played nearby. */
public interface Resonant {
    void onResonate(ServerLevel level, BlockPos pos, BlockState state, float strength);
}
