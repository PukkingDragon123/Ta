package com.thesift.client;

import com.thesift.client.particle.RainbowSnowParticle;
import com.thesift.registry.ModWorldLand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/** W-land, client side: the rainbow snowflake that falls through the White Forest. */
public final class WorldLandClient {
    private WorldLandClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(WorldLandClient::particles);
    }

    private static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModWorldLand.RAINBOW_SNOWFLAKE.get(), RainbowSnowParticle.Provider::new);
    }
}
