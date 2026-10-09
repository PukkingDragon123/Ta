package com.thesift.registry;

import com.thesift.TheSift;
import com.thesift.block.entity.EurophyTableBlockEntity;
import com.thesift.block.entity.EurophyTableMenu;
import com.thesift.music.SongEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * F1 Materials & Europhy Table: the Europhy Table's block entity and menu, the music that drives it, Bauxite's
 * fuel values and the creatures that steady it. Registered from one line in {@link TheSift}; art, data and text
 * come from tools/materials.py.
 */
public final class ModEurophy {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TheSift.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, TheSift.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EurophyTableBlockEntity>> EUROPHY_TABLE = BLOCK_ENTITIES.register(
            "europhy_table", () -> new BlockEntityType<>(EurophyTableBlockEntity::new, ModBlocks.EUROPHY_TABLE.get()));
    public static final DeferredHolder<MenuType<?>, MenuType<EurophyTableMenu>> EUROPHY_MENU = MENUS.register(
            "europhy_table", () -> new MenuType<EurophyTableMenu>(EurophyTableMenu::new, FeatureFlags.VANILLA_SET));

    /** Living creatures whose calm lets Bauxite settle in the Europhy Table (the Nib now; the Glow Fly later). */
    public static final TagKey<EntityType<?>> BAUXITE_STABILIZERS = TagKey.create(Registries.ENTITY_TYPE, TheSift.id("bauxite_stabilizers"));

    /** Furnace burn times (data/thesift/context_int_provider/cooking/*.json): 60 and 120 items. */
    public static final ResourceKey<ContextIntProvider> BAUXITE_BURN_TIME = ResourceKey.create(Registries.CONTEXT_INT_PROVIDER,
            TheSift.id("cooking/time_bauxite"));
    public static final ResourceKey<ContextIntProvider> STABLE_BAUXITE_BURN_TIME = ResourceKey.create(Registries.CONTEXT_INT_PROVIDER,
            TheSift.id("cooking/time_stable_bauxite"));

    private ModEurophy() {
    }

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        // the table hears every note and every song played near it
        SongEvents.listenNotes(EurophyTableBlockEntity::hearNote);
        SongEvents.listenSongs(EurophyTableBlockEntity::hearSong);
    }
}
