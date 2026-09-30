package com.thesift.registry;

import com.thesift.TheSift;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TheSift.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BLOCKS = TABS.register("blocks", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.thesift.blocks"))
            .icon(() -> new ItemStack(ModItems.SIFT_GRASS_BLOCK.get()))
            .displayItems((params, output) -> {
                add(output, ModTabContents.nature());
                add(output, ModTabContents.blocks());
                add(output, ModTabContents.functional());
            })
            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ITEMS = TABS.register("items", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.thesift.items"))
            .icon(() -> new ItemStack(ModItems.SLINGSHOT.get()))
            .withTabsBefore(BLOCKS.getKey())
            .displayItems((params, output) -> {
                add(output, ModTabContents.items());
                add(output, ModTabContents.tools());
                add(output, ModTabContents.combat());
                add(output, ModTabContents.eggs());
            })
            .build());

    private static void add(CreativeModeTab.Output output, List<Supplier<? extends Item>> items) {
        for (Supplier<? extends Item> item : items) {
            output.accept(item.get());
        }
    }

    /** Also list spawn eggs in the vanilla spawn egg tab. */
    public static void addToVanillaTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            for (Supplier<? extends Item> egg : ModTabContents.eggs()) {
                event.accept(egg.get());
            }
        }
    }

    private ModCreativeTabs() {
    }
}
