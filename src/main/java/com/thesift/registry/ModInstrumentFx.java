package com.thesift.registry;

import com.thesift.TheSift;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * INS free play: what you see when an instrument is played (drawn by client/music/InstrumentParticle).
 * Every one is spawned on the clients that see the player play, at the instrument itself; xa carries
 * the colour (packed RGB), ya the direction the player faces (degrees), za its strength or size.
 */
public final class ModInstrumentFx {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, TheSift.MODID);

    /** An eighth note flying out of the instrument in the song's colour, floating up as it fades. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NOTE = PARTICLES.register("instrument_note",
            () -> new SimpleParticleType(true));
    /** Two beamed notes (a chord, a strum, an accent). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NOTES = PARTICLES.register("instrument_notes",
            () -> new SimpleParticleType(true));
    /** A soft ring of sound spreading from the instrument, upright, facing where the player faces. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RING = PARTICLES.register("sound_ring",
            () -> new SimpleParticleType(true));
    /** A wisp of breath (a flute), or of dust shaken from a drum head. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BREATH = PARTICLES.register("instrument_breath",
            () -> new SimpleParticleType(false));

    private ModInstrumentFx() {
    }

    public static void register(IEventBus bus) {
        PARTICLES.register(bus);
    }
}
