package com.thesift.client;

import com.thesift.client.gui.EurophyTableScreen;
import com.thesift.client.model.ModelGeometry;
import com.thesift.client.renderer.EurophyTableRenderer;
import com.thesift.registry.ModEurophy;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** F1: the Europhy Table on the client - its clockwork model, renderer and screen. Registered from one line in TheSiftClient. */
public final class EurophyClient {
    private EurophyClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(EurophyClient::layers);
        modBus.addListener(EurophyClient::renderers);
        modBus.addListener(EurophyClient::screens);
    }

    private static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(EurophyTableRenderer.LAYER, ModelGeometry::europhy_table);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModEurophy.EUROPHY_TABLE.get(), EurophyTableRenderer::new);
    }

    private static void screens(RegisterMenuScreensEvent event) {
        event.register(ModEurophy.EUROPHY_MENU.get(), EurophyTableScreen::new);
    }
}
