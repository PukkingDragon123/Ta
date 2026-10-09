package com.thesift.registry;

import net.minecraft.world.item.ToolMaterial;

/**
 * Siftite: Serbim forged with echo shards and netherite. The finest metal there is: tools outlast,
 * outdig and outhit Netherite (and knock things flying, see {@code SiftiteGear}). It is never
 * forged into armour.
 */
public final class ModMaterials {
    public static final ToolMaterial SIFTITE_TOOL = new ToolMaterial(ModTags.Blocks.INCORRECT_FOR_SIFTITE_TOOL, 2600, 10.5F, 5.0F, 18,
            ModTags.Items.SIFTITE_TOOL_MATERIALS);

    private ModMaterials() {}
}
