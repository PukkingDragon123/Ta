package com.thesift.client;

import com.thesift.TheSift;
import com.thesift.client.model.ModelGeometry;
import com.thesift.client.particle.CymbalRingParticle;
import com.thesift.client.renderer.CypoleRenderer;
import com.thesift.client.renderer.JailerRenderer;
import com.thesift.client.renderer.SculklingRenderer;
import com.thesift.registry.ModCaveCreatures;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/**
 * CR4's creatures, client side: the Jailer and Sculkling models and renderers (the jail cell itself is
 * drawn by the Jailer), and the Cypole with its tongue and its shockwave ring.
 */
public final class CaveCreaturesClient {
    public static final ModelLayerLocation JAILER = new ModelLayerLocation(TheSift.id("jailer"), "main");
    public static final ModelLayerLocation SCULKLING = new ModelLayerLocation(TheSift.id("sculkling"), "main");
    public static final ModelLayerLocation CYPOLE = new ModelLayerLocation(TheSift.id("cypole"), "main");

    private CaveCreaturesClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(CaveCreaturesClient::registerLayers);
        modBus.addListener(CaveCreaturesClient::registerRenderers);
        modBus.addListener(CaveCreaturesClient::registerParticles);
    }

    private static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(JAILER, ModelGeometry::jailer);
        event.registerLayerDefinition(SCULKLING, ModelGeometry::sculkling);
        event.registerLayerDefinition(CYPOLE, ModelGeometry::cypole);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModCaveCreatures.JAILER.get(), JailerRenderer::new);
        event.registerEntityRenderer(ModCaveCreatures.SCULKLING.get(), SculklingRenderer::new);
        event.registerEntityRenderer(ModCaveCreatures.JAIL_CELL.get(), NoopRenderer::new);
        event.registerEntityRenderer(ModCaveCreatures.CYPOLE.get(), CypoleRenderer::new);
    }

    private static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModCaveCreatures.CYMBAL_RING.get(), CymbalRingParticle.Provider::new);
    }
}
