package com.thesift.registry;

import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

public final class ModWoodTypes {
    public static final BlockSetType LULLWOOD_SET = BlockSetType.register(new BlockSetType("thesift_lullwood"));
    public static final BlockSetType WISHWOOD_SET = BlockSetType.register(new BlockSetType("thesift_wishwood"));
    public static final WoodType LULLWOOD = WoodType.register(new WoodType("thesift_lullwood", LULLWOOD_SET));
    public static final WoodType WISHWOOD = WoodType.register(new WoodType("thesift_wishwood", WISHWOOD_SET));

    private ModWoodTypes() {}
}
