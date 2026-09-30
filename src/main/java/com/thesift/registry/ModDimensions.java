package com.thesift.registry;

import com.thesift.TheSift;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;

public final class ModDimensions {
    public static final ResourceKey<Level> THE_SIFT = ResourceKey.create(Registries.DIMENSION, TheSift.id("the_sift"));
    public static final ResourceKey<DimensionType> THE_SIFT_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, TheSift.id("the_sift"));

    public static final ResourceKey<Biome> SIFT_PLAINS = biome("sift_plains");
    public static final ResourceKey<Biome> FOREST_MOUNTAINS = biome("forest_mountains");
    public static final ResourceKey<Biome> ROCKY_DUNES = biome("rocky_dunes");
    public static final ResourceKey<Biome> CHROME_LAKES = biome("chrome_lakes");
    public static final ResourceKey<Biome> DEEP_SIFT = biome("deep_sift");
    public static final ResourceKey<Biome> WISHING_GROVE = biome("wishing_grove");

    private static ResourceKey<Biome> biome(String name) {
        return ResourceKey.create(Registries.BIOME, TheSift.id(name));
    }

    public static boolean isSift(Level level) {
        return level.dimension() == THE_SIFT;
    }

    private ModDimensions() {}
}
