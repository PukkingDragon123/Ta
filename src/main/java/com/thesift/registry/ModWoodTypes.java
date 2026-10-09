package com.thesift.registry;

import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

public final class ModWoodTypes {
    public static final BlockSetType LULLWOOD_SET = BlockSetType.register(new BlockSetType("thesift_lullwood"));
    public static final BlockSetType WISHWOOD_SET = BlockSetType.register(new BlockSetType("thesift_wishwood"));
    public static final WoodType LULLWOOD = WoodType.register(new WoodType("thesift_lullwood", LULLWOOD_SET));
    public static final WoodType WISHWOOD = WoodType.register(new WoodType("thesift_wishwood", WISHWOOD_SET));
    // W1 World & terrain: the Sculk Swamp's corrupted Blightwood
    public static final BlockSetType BLIGHTWOOD_SET = BlockSetType.register(new BlockSetType("thesift_blightwood"));
    public static final WoodType BLIGHTWOOD = WoodType.register(new WoodType("thesift_blightwood", BLIGHTWOOD_SET));
    // W-land: the White Forest's pearl-barked White Lullwood
    public static final BlockSetType WHITE_LULLWOOD_SET = BlockSetType.register(new BlockSetType("thesift_white_lullwood"));
    public static final WoodType WHITE_LULLWOOD = WoodType.register(new WoodType("thesift_white_lullwood", WHITE_LULLWOOD_SET));

    private ModWoodTypes() {}
}
