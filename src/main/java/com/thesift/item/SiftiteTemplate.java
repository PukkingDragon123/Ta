package com.thesift.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.SmithingTemplateItem;

/** Smithing template that upgrades Copper gear into Siftite gear. Found in Sift ruins. */
public class SiftiteTemplate extends SmithingTemplateItem {
    private static final ChatFormatting DESC = ChatFormatting.BLUE;

    public SiftiteTemplate(Properties properties) {
        super(Component.translatable("item.thesift.smithing_template.siftite_upgrade.applies_to").withStyle(DESC),
                Component.translatable("item.thesift.smithing_template.siftite_upgrade.ingredients").withStyle(DESC),
                Component.translatable("item.thesift.smithing_template.siftite_upgrade.base_slot_description"),
                Component.translatable("item.thesift.smithing_template.siftite_upgrade.additions_slot_description"),
                List.of(slot("helmet"), slot("sword"), slot("chestplate"), slot("pickaxe"), slot("leggings"), slot("axe"), slot("boots"),
                        slot("hoe"), slot("shovel"), slot("spear")),
                List.of(Identifier.withDefaultNamespace("container/slot/ingot")),
                properties);
    }

    private static Identifier slot(String name) {
        return Identifier.withDefaultNamespace("container/slot/" + name);
    }
}
