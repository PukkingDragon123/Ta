package com.thesift.client;

import com.thesift.client.particle.AcidParticle;
import com.thesift.client.renderer.SculkGrasperRenderer;
import com.thesift.registry.ModCaves;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/** W-deep caves on the client: the Sculk Grasper's tendril and feelers, and the acid drops and fizz. */
public final class CavesClient {
    private CavesClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(CavesClient::renderers);
        modBus.addListener(CavesClient::particles);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModCaves.SCULK_GRASPER.get(), SculkGrasperRenderer::new);
    }

    private static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModCaves.ACID_DRIP.get(), sprites -> new AcidParticle.Provider(AcidParticle.Kind.DRIP, sprites));
        event.registerSpriteSet(ModCaves.ACID_FIZZ.get(), sprites -> new AcidParticle.Provider(AcidParticle.Kind.FIZZ, sprites));
    }
}
