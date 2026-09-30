package com.thesift.registry;

import com.thesift.TheSift;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.levelgen.feature.Feature;

public final class ModTreeGrowers {
    public static final ResourceKey<Feature> LULLWOOD_TREE = key("lullwood_tree");
    public static final ResourceKey<Feature> GRAND_LULLWOOD_TREE = key("grand_lullwood_tree");
    public static final ResourceKey<Feature> WISHWOOD_TREE = key("wishwood_tree");
    public static final ResourceKey<Feature> TALL_WISHWOOD_TREE = key("tall_wishwood_tree");

    public static final TreeGrower LULLWOOD = new TreeGrower("thesift_lullwood",
            WeightedList.of(new Weighted<>(LULLWOOD_TREE, 1)),
            WeightedList.of(new Weighted<>(GRAND_LULLWOOD_TREE, 1)),
            WeightedList.of(),
            LULLWOOD_TREE);
    public static final TreeGrower WISHWOOD = new TreeGrower("thesift_wishwood",
            WeightedList.of(new Weighted<>(WISHWOOD_TREE, 3), new Weighted<>(TALL_WISHWOOD_TREE, 1)),
            WeightedList.of(),
            WeightedList.of(),
            WISHWOOD_TREE);

    private static ResourceKey<Feature> key(String name) {
        return ResourceKey.create(Registries.FEATURE, TheSift.id(name));
    }

    private ModTreeGrowers() {}
}
