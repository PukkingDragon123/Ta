package com.thesift.client;

import com.thesift.TheSift;
import com.thesift.client.model.ModelGeometry;
import com.thesift.client.renderer.NibRenderer;
import com.thesift.client.renderer.SoulGolemRenderer;
import com.thesift.registry.ModEchoer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** A2 Echoer, client side: the Soul Golem and Nib models and renderers. */
public final class EchoerClient {
    public static final ModelLayerLocation SOUL_GOLEM = new ModelLayerLocation(TheSift.id("soul_golem"), "main");
    public static final ModelLayerLocation NIB = new ModelLayerLocation(TheSift.id("nib"), "main");

    private EchoerClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(EchoerClient::registerLayers);
        modBus.addListener(EchoerClient::registerRenderers);
    }

    private static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SOUL_GOLEM, ModelGeometry::soul_golem);
        event.registerLayerDefinition(NIB, ModelGeometry::nib);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEchoer.SOUL_GOLEM.get(), SoulGolemRenderer::new);
        event.registerEntityRenderer(ModEchoer.NIB.get(), NibRenderer::new);
    }
}
