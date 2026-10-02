package com.thesift.client;

import com.thesift.TheSift;
import com.thesift.client.model.ModelGeometry;
import com.thesift.client.renderer.JailerRenderer;
import com.thesift.client.renderer.SculklingRenderer;
import com.thesift.registry.ModCaveCreatures;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** A4 cave creatures, client side: the Jailer and Sculkling models and renderers (the jail cell itself is drawn by the Jailer). */
public final class CaveCreaturesClient {
    public static final ModelLayerLocation JAILER = new ModelLayerLocation(TheSift.id("jailer"), "main");
    public static final ModelLayerLocation SCULKLING = new ModelLayerLocation(TheSift.id("sculkling"), "main");

    private CaveCreaturesClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(CaveCreaturesClient::registerLayers);
        modBus.addListener(CaveCreaturesClient::registerRenderers);
    }

    private static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(JAILER, ModelGeometry::jailer);
        event.registerLayerDefinition(SCULKLING, ModelGeometry::sculkling);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModCaveCreatures.JAILER.get(), JailerRenderer::new);
        event.registerEntityRenderer(ModCaveCreatures.SCULKLING.get(), SculklingRenderer::new);
        event.registerEntityRenderer(ModCaveCreatures.JAIL_CELL.get(), NoopRenderer::new);
    }
}
