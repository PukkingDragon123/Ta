package com.thesift.client;

import com.thesift.client.model.ModelGeometry;
import com.thesift.client.renderer.EchoerDrillRenderer;
import com.thesift.registry.ModEchoer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** RR: the Echoer Drill on the client - its gun model and renderer. Registered from one line in TheSiftClient. */
public final class EchoerDrillClient {
    private EchoerDrillClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(EchoerDrillClient::layers);
        modBus.addListener(EchoerDrillClient::renderers);
    }

    private static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(EchoerDrillRenderer.LAYER, ModelGeometry::echoer_drill);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModEchoer.ECHOER_DEVICE_BE.get(), EchoerDrillRenderer::new);
    }
}
