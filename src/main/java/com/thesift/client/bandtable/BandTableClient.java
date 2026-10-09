package com.thesift.client.bandtable;

import com.thesift.TheSift;
import com.thesift.block.BandTableBlock;
import com.thesift.client.model.ModelGeometry;
import com.thesift.registry.ModBandTable;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * F2 Band Table, client side: the table's moving parts and their renderer, and its score screen. (The Sift
 * enchantments' books get their own look from assets/minecraft/items/enchanted_book.json, a select on the stored
 * enchantments - tools/band_table.py.)
 */
public final class BandTableClient {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(TheSift.id("band_table"), "main");

    private BandTableClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(BandTableClient::layers);
        modBus.addListener(BandTableClient::renderers);
        BandTableBlock.clientOpen = BandTableScreen::open;
    }

    private static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LAYER, ModelGeometry::band_table);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBandTable.BAND_TABLE_BE.get(), BandTableRenderer::new);
    }
}
