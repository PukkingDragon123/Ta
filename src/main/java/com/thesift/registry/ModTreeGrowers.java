package com.thesift.registry;

import com.thesift.TheSift;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.levelgen.feature.Feature;

public final class ModTreeGrowers {
    public static final ResourceKey<Feature> LULLWOOD_TREE = key("lullwood_tree");
    public static final ResourceKey<Feature> GRAND_LULLWOOD_TREE = key("grand_lullwood_tree");
    public static final ResourceKey<Feature> WISHWOOD_TREE = key("wishwood_tree");
    public static final ResourceKey<Feature> TALL_WISHWOOD_TREE = key("tall_wishwood_tree");

    public static final TreeGrower LULLWOOD = new TreeGrower("thesift_lullwood",
            WeightedList.of(LULLWOOD_TREE),
            WeightedList.of(GRAND_LULLWOOD_TREE),
            WeightedList.<ResourceKey<Feature>>of(),
            LULLWOOD_TREE);
    public static final TreeGrower WISHWOOD = new TreeGrower("thesift_wishwood",
            WeightedList.<ResourceKey<Feature>>builder().add(WISHWOOD_TREE, 3).add(TALL_WISHWOOD_TREE, 1).build(),
            WeightedList.<ResourceKey<Feature>>of(),
            WeightedList.<ResourceKey<Feature>>of(),
            WISHWOOD_TREE);

    private static ResourceKey<Feature> key(String name) {
        return ResourceKey.create(Registries.FEATURE, TheSift.id(name));
    }

    private ModTreeGrowers() {}
}
