package com.thesift.client;

import com.thesift.TheSift;
import com.thesift.client.model.ModelGeometry;
import com.thesift.client.renderer.SlumblerEggsRenderer;
import com.thesift.client.renderer.SlumblerTadpoleRenderer;
import com.thesift.registry.ModSlumbler;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** CR2: the Slumbler's family on the client - its tadpoles, its clutch of eggs and the Chrome it spits. */
public final class SlumblerClient {
    public static final ModelLayerLocation SLUMBLER_TADPOLE = new ModelLayerLocation(TheSift.id("slumbler_tadpole"), "main");
    public static final ModelLayerLocation SLUMBLER_EGGS = new ModelLayerLocation(TheSift.id("slumbler_eggs"), "main");

    private SlumblerClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(SlumblerClient::layers);
        modBus.addListener(SlumblerClient::renderers);
    }

    private static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SLUMBLER_TADPOLE, ModelGeometry::slumbler_tadpole);
        event.registerLayerDefinition(SLUMBLER_EGGS, ModelGeometry::slumbler_eggs);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModSlumbler.SLUMBLER_TADPOLE.get(), SlumblerTadpoleRenderer::new);
        event.registerEntityRenderer(ModSlumbler.SLUMBLER_EGGS.get(), SlumblerEggsRenderer::new);
        // the spit is a wobbling gob of Chrome: a Chrome Pearl's rainbow, full bright, trailing droplets
        event.registerEntityRenderer(ModSlumbler.CHROME_SPIT.get(), ctx -> new ThrownItemRenderer<>(ctx, 1.4F, true));
    }
}
