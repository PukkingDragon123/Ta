package com.thesift.client;

import com.thesift.TheSift;
import com.thesift.client.model.CoralOrganModel;
import com.thesift.client.model.ModelGeometry;
import com.thesift.client.model.SculkFishModel;
import com.thesift.client.renderer.CoralHookRenderer;
import com.thesift.client.renderer.CoralOrganRenderer;
import com.thesift.client.renderer.SiftFishRenderer;
import com.thesift.registry.ModSculkSea;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** CR3 Fish &amp; Coral Organs, client side: the Sculk Fish, the Sculk Coral Organ and its hooked line. */
public final class SculkSeaClient {
    public static final ModelLayerLocation SCULK_FISH = new ModelLayerLocation(TheSift.id("sculk_fish"), "main");
    public static final ModelLayerLocation CORAL_ORGAN = new ModelLayerLocation(TheSift.id("coral_organ"), "main");
    /** The Sculk Fish's colour variants, in the order of SiftFish.getVariant() (textures painted by tools/fish_art.py). */
    public static final String[] SCULK_FISH_VARIANTS = {"sculk_fish", "sculk_fish_pale", "sculk_fish_deep"};

    private SculkSeaClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(SculkSeaClient::registerLayers);
        modBus.addListener(SculkSeaClient::registerRenderers);
    }

    private static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SCULK_FISH, ModelGeometry::sculk_fish);
        event.registerLayerDefinition(CORAL_ORGAN, ModelGeometry::coral_organ);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModSculkSea.SCULK_FISH.get(), c -> new SiftFishRenderer<>(c, new SculkFishModel(c.bakeLayer(SCULK_FISH)),
                "sculk_fish", SCULK_FISH_VARIANTS, 0.25F, 1.0F)); // S2: real size, hand-painted (tools/waterfolk.py)
        event.registerEntityRenderer(ModSculkSea.CORAL_ORGAN.get(), c -> new CoralOrganRenderer(c, new CoralOrganModel(c.bakeLayer(CORAL_ORGAN))));
        event.registerEntityRenderer(ModSculkSea.CORAL_HOOK.get(), CoralHookRenderer::new);
    }
}
