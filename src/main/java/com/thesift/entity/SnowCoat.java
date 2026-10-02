package com.thesift.entity;

import com.thesift.TheSift;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;

/**
 * Bulbs and Stompers born in the White Forest grow a snowy coat. Every way one comes into the world
 * (natural spawns, spawn eggs, Stomper eggs with no parents to take after) asks here.
 */
public final class SnowCoat {
    /** The White Forest biome (added by the White Forest feature; only its key is needed here). */
    public static final ResourceKey<Biome> WHITE_FOREST = ResourceKey.create(Registries.BIOME, TheSift.id("white_forest"));

    private SnowCoat() {
    }

    public static boolean at(LevelReader level, BlockPos pos) {
        return level.getBiome(pos).is(WHITE_FOREST);
    }
}
