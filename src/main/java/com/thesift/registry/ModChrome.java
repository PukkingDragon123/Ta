package com.thesift.registry;

import com.thesift.TheSift;
import com.thesift.effect.RainbowDazeEffect;
import com.thesift.music.SongEvents;
import com.thesift.world.ChromeReactions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * A3 Chrome: everything around the rainbow Chrome fluid that is not a plain block or item (those
 * come from tools/spec.py via tools/chrome.py) - the Rainbow Daze, Chrome's particles, the
 * Chrome-meets-water reaction, the note bursts and which fish fit in a Chrome Bucket.
 */
public final class ModChrome {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, TheSift.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, TheSift.MODID);

    /** Bathing in Chrome: the world swims and shifts colour (felt in client/ChromeClient), fading in and out. */
    public static final DeferredHolder<MobEffect, MobEffect> RAINBOW_DAZE = EFFECTS.register("rainbow_daze",
            () -> new RainbowDazeEffect(MobEffectCategory.NEUTRAL, 0xC48CFF).setBlendDuration(30));

    /** A flat rainbow ring spreading over Chrome's surface (xa = hue, ya = size). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CHROME_RIPPLE = particle("chrome_ripple", false);
    /** A rising rainbow twinkle (xa = hue, ya = upward speed). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CHROME_SPARK = particle("chrome_spark", false);
    /** Never drawn: a note rang out here and the client bursts the Chrome around it into colour (xa = pitch / 24). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CHROME_CHORD = particle("chrome_chord", true);
    /** Rainbow motes drifting round anything Rainbow Dazed. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RAINBOW_MOTE = particle("rainbow_mote", false);

    private ModChrome() {
    }

    private static DeferredHolder<ParticleType<?>, SimpleParticleType> particle(String name, boolean alwaysShow) {
        return PARTICLES.register(name, () -> new SimpleParticleType(alwaysShow));
    }

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
        PARTICLES.register(bus);
        bus.addListener(ModChrome::commonSetup);
        // every note played near Chrome lights its surface up
        SongEvents.listenNotes(ChromeReactions::onNote);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        ChromeReactions.registerFluidInteractions();
    }

    /** A Chrome Bucket holding one fish of {@code type}, or empty if that creature does not fit in one. */
    public static ItemStack fishBucket(EntityType<?> type) {
        if (type == ModEntities.KAZOO_FISH.get()) {
            return new ItemStack(ModItems.CHROME_KAZOO_FISH_BUCKET.get());
        }
        if (type == ModEntities.TUBAFISH.get()) {
            return new ItemStack(ModItems.CHROME_TUBAFISH_BUCKET.get());
        }
        if (type == ModEntities.FANFARE_EEL.get()) {
            return new ItemStack(ModItems.CHROME_FANFARE_EEL_BUCKET.get());
        }
        return ItemStack.EMPTY;
    }
}
