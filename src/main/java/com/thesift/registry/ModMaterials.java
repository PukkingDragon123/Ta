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
 * Siftite: Serbim alloyed into Copper. It sits between iron and diamond, is very enchantable and
 * much faster than copper, echoing the "upgraded copper" idea.
 */
public final class ModMaterials {
    public static final ToolMaterial SIFTITE_TOOL = new ToolMaterial(ModTags.Blocks.INCORRECT_FOR_SIFTITE_TOOL, 1150, 8.5F, 2.5F, 20,
            ModTags.Items.SIFTITE_TOOL_MATERIALS);

    public static final ResourceKey<EquipmentAsset> SIFTITE_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, TheSift.id("siftite"));

    public static final ArmorMaterial SIFTITE_ARMOR = new ArmorMaterial(28,
            Map.of(ArmorType.BOOTS, 3, ArmorType.LEGGINGS, 6, ArmorType.CHESTPLATE, 7, ArmorType.HELMET, 3, ArmorType.BODY, 9),
            22, SoundEvents.ARMOR_EQUIP_COPPER, 1.5F, 0.0F, ModTags.Items.REPAIRS_SIFTITE_ARMOR, SIFTITE_ASSET);

    private ModMaterials() {}
}
