package com.thesift.registry;

import com.thesift.TheSift;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

/**
 * Siftite: Serbim forged with echo shards and netherite. The finest metal there is: tools outlast,
 * outdig and outhit Netherite (and knock things flying, see {@code SiftiteGear}), and the armour
 * is at least as tough, with the Deep Dark's sound-proofing worked into it.
 */
public final class ModMaterials {
    public static final ToolMaterial SIFTITE_TOOL = new ToolMaterial(ModTags.Blocks.INCORRECT_FOR_SIFTITE_TOOL, 2600, 10.5F, 5.0F, 18,
            ModTags.Items.SIFTITE_TOOL_MATERIALS);

    public static final ResourceKey<EquipmentAsset> SIFTITE_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, TheSift.id("siftite"));

    public static final ArmorMaterial SIFTITE_ARMOR = new ArmorMaterial(42,
            Map.of(ArmorType.BOOTS, 3, ArmorType.LEGGINGS, 6, ArmorType.CHESTPLATE, 8, ArmorType.HELMET, 3, ArmorType.BODY, 19),
            18, SoundEvents.ARMOR_EQUIP_NETHERITE, 3.5F, 0.15F, ModTags.Items.REPAIRS_SIFTITE_ARMOR, SIFTITE_ASSET);

    private ModMaterials() {}
}
