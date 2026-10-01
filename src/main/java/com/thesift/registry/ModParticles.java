package com.thesift.registry;

import com.thesift.TheSift;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, TheSift.MODID);

    /** Slow, wobbling cyan souls that drift upwards through the whole dimension. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DRIFTING_SOUL = reg("drifting_soul", true);
    /** Sparkling pearl droplets that pop out of Chrome. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CHROME_DROPLET = reg("chrome_droplet", false);
    /** Tiny bubbles rising from Chrome lakes. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CHROME_BUBBLE = reg("chrome_bubble", false);
    /** Pink pollen motes floating over the plains. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DREAM_POLLEN = reg("dream_pollen", false);
    /** Coloured music notes (colour passed through xSpeed like vanilla notes). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SIFT_NOTE = reg("sift_note", true);
    /** Flat expanding ring laid on the ground: a visible music pulse. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RESONANCE_RING = reg("resonance_ring", true);
    /** Glowing dust hanging in caves. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GLOW_DUST = reg("glow_dust", false);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LULLWOOD_LEAF = reg("lullwood_leaf", false);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> WISHWOOD_LEAF = reg("wishwood_leaf", false);
    /** Large, soft ground-hugging mist. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SIFT_MIST = reg("sift_mist", false);
    /** Four pointed twinkling star. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> STAR_SPARKLE = reg("star_sparkle", false);
    /** Spiralling portal soul, pulled towards the portal centre. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PORTAL_SOUL = reg("portal_soul", true);
    /** Little puffs kicked up by footsteps on Sift soils. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FOOTSTEP_PUFF = reg("footstep_puff", false);
    /** Wobbly glowing slime splat used by the slingshot burst. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GLOW_SPLAT = reg("glow_splat", true);
    /** A shooting "wishing star" streak (spawned high in the sky). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> WISHING_STAR = reg("wishing_star", true);
    /** Flat jelly splotches Bulbs leave where they land. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SLIME_TRAIL = reg("slime_trail", false);
    /** Notes a guiding Harmoner leaves behind; xa/ya/za carry its colour. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GUIDE_NOTE = reg("guide_note", true);
    /** Sleepy spores released by Dream Snares. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SLEEP_SPORE = reg("sleep_spore", false);

    /**
     * Bouncy little stars, notes, hearts and droplets that burst out of a Sift mob when it is
     * killed: xa carries the colour (0xRRGGBB), ya the shape (0 star, 1 note, 2 heart, 3 drop) and
     * za how hard it is thrown.
     */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> KILL_STAR = reg("kill_star", true);

    private static DeferredHolder<ParticleType<?>, SimpleParticleType> reg(String name, boolean alwaysShow) {
        return PARTICLES.register(name, () -> new SimpleParticleType(alwaysShow));
    }

    private ModParticles() {}
}
