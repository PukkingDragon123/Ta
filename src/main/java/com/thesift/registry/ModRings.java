package com.thesift.registry;

import com.thesift.TheSift;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * CR1: rings of sound you can see (drawn by client/particle/RingParticle) - a struck Sifter's ring
 * rippling over the sand and the Echoer's echolocation pings.
 */
public final class ModRings {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, TheSift.MODID);

    /** A warm gold ring rippling out flat from a struck bell (xa = how far it spreads, in blocks; ya = how bright, 0..1). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BELL_RING = PARTICLES.register("bell_ring",
            () -> new SimpleParticleType(false));
    /** An upright ring of sound flying out along its velocity (xa, ya, za) and widening as it slows: an echolocation ping. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ECHO_RING = PARTICLES.register("echo_ring",
            () -> new SimpleParticleType(false));

    private ModRings() {
    }

    public static void register(IEventBus bus) {
        PARTICLES.register(bus);
    }
}
