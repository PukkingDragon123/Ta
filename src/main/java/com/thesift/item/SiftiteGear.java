package com.thesift.item;

import com.thesift.TheSift;
import com.thesift.registry.ModItems;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

/**
 * What makes Siftite tools more than stronger Netherite: they hit with a heavy, booming knockback.
 * (There is no Siftite armour: Siftite is a tool and weapon metal.)
 */
public final class SiftiteGear {
    private static final AttributeModifier KNOCKBACK_WEAPON = new AttributeModifier(TheSift.id("siftite_knockback"), 1.6,
            AttributeModifier.Operation.ADD_VALUE);
    private static final AttributeModifier KNOCKBACK_TOOL = new AttributeModifier(TheSift.id("siftite_knockback"), 1.0,
            AttributeModifier.Operation.ADD_VALUE);

    private SiftiteGear() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(SiftiteGear::onAttributes);
    }

    private static boolean isWeapon(Item item) {
        return item == ModItems.SIFTITE_SWORD.get() || item == ModItems.SIFTITE_AXE.get() || item == ModItems.SIFTITE_SPEAR.get();
    }

    private static boolean isTool(Item item) {
        return item == ModItems.SIFTITE_PICKAXE.get() || item == ModItems.SIFTITE_SHOVEL.get() || item == ModItems.SIFTITE_HOE.get();
    }

    private static void onAttributes(ItemAttributeModifierEvent event) {
        Item item = event.getItemStack().getItem();
        if (isWeapon(item)) {
            event.addModifier(Attributes.ATTACK_KNOCKBACK, KNOCKBACK_WEAPON, EquipmentSlotGroup.MAINHAND);
        } else if (isTool(item)) {
            event.addModifier(Attributes.ATTACK_KNOCKBACK, KNOCKBACK_TOOL, EquipmentSlotGroup.MAINHAND);
        }
    }
}
