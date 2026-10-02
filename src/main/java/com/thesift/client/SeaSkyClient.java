package com.thesift.client;

import com.thesift.TheSift;
import com.thesift.client.model.GobblerModel;
import com.thesift.client.model.ModelGeometry;
import com.thesift.client.renderer.GobblerRenderer;
import com.thesift.registry.ModSeaSky;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Sea & sky, client side: the Gobbler's model layer and renderer. */
public final class SeaSkyClient {
    public static final ModelLayerLocation GOBBLER = new ModelLayerLocation(TheSift.id("gobbler"), "main");

    private SeaSkyClient() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(SeaSkyClient::registerLayers);
        modBus.addListener(SeaSkyClient::registerRenderers);
    }

    private static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(GOBBLER, ModelGeometry::gobbler);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModSeaSky.GOBBLER.get(), c -> new GobblerRenderer(c, new GobblerModel(c.bakeLayer(GOBBLER))));
    }
}
