package com.thesift.client;

import com.thesift.TheSift;
import com.thesift.client.model.ModelGeometry;
import com.thesift.client.renderer.SiftSnifferRenderer;
import com.thesift.registry.ModSiftSniffer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** E1 Sniffer &amp; rot, client side: the Sift Sniffer's model and renderer (the rot itself is drawn by every Sift mob renderer). */
public final class SiftSnifferClient {
    public static final ModelLayerLocation SIFT_SNIFFER = new ModelLayerLocation(TheSift.id("sift_sniffer"), "main");

    private SiftSnifferClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(SiftSnifferClient::layers);
        modBus.addListener(SiftSnifferClient::renderers);
    }

    private static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SIFT_SNIFFER, ModelGeometry::sift_sniffer);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModSiftSniffer.SIFT_SNIFFER.get(), SiftSnifferRenderer::new);
    }
}
