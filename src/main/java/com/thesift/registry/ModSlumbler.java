package com.thesift.registry;

import com.thesift.TheSift;
import com.thesift.entity.slumbler.ChromeSpit;
import com.thesift.entity.slumbler.SlumblerEggs;
import com.thesift.entity.slumbler.SlumblerTadpole;
import com.thesift.music.Instrument;
import com.thesift.music.SongEvents;
import com.thesift.music.band.BandRegistry;
import com.thesift.music.band.BandVoice;
import java.util.UUID;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * CR2: the Slumbler's family - the gob of Chrome it spits, the clutch of eggs two well-fed Slumblers
 * lay and the stingray-like tadpoles that hatch from it - with the tadpoles' taming by drum music and
 * their voice in a player's band (crash cymbals). The Slumbler itself is in {@link ModEntities}; its
 * sounds are in {@link ModSounds}; plain items (the gill, the tadpole buckets and spawn egg) come
 * from tools/spec.py via tools/slumbler.py. Registered from one line in {@link TheSift}.
 */
public final class ModSlumbler {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(TheSift.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<SlumblerTadpole>> SLUMBLER_TADPOLE = ENTITIES.registerEntityType("slumbler_tadpole",
            SlumblerTadpole::new, MobCategory.WATER_AMBIENT, b -> b.sized(0.6F, 0.25F).eyeHeight(0.15F).clientTrackingRange(6));
    public static final DeferredHolder<EntityType<?>, EntityType<SlumblerEggs>> SLUMBLER_EGGS = ENTITIES.registerEntityType("slumbler_eggs",
            SlumblerEggs::new, MobCategory.MISC, b -> b.sized(0.7F, 0.35F).eyeHeight(0.2F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<ChromeSpit>> CHROME_SPIT = ENTITIES.registerEntityType("chrome_spit",
            ChromeSpit::new, MobCategory.MISC, b -> b.sized(0.35F, 0.35F).clientTrackingRange(4).updateInterval(10));

    /** How far a drum carries to a tadpole that is waiting to be tamed. */
    private static final double DRUM_HEARING = 16.0;

    private ModSlumbler() {
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        modBus.addListener(ModSlumbler::attributes);
        // drum music tames a well-fed tadpole (see SlumblerTadpole#hearNote)
        SongEvents.listenNotes((level, player, at, pitch) -> {
            Instrument played = SongEvents.instrument();
            boolean drum = played != null && played.family() == Instrument.Family.DRUM;
            for (SlumblerTadpole t : level.getEntitiesOfClass(SlumblerTadpole.class, new AABB(at, at).inflate(DRUM_HEARING), SlumblerTadpole::isAlive)) {
                t.hearNote(level, player, drum);
            }
        });
        // a tamed tadpole joins its owner's band on crash cymbals: a snare's crack under the cymbal's splash
        BandRegistry.voice(SLUMBLER_TADPOLE, ModSounds.SLUMBLER_TADPOLE_CRASH).volume(0.8F)
                .layer(SoundEvents.NOTE_BLOCK_SNARE, 0.6F)
                .families(Instrument.Family.DRUM)
                .temper(BandVoice.Temper.LOYAL).movement(BandVoice.Movement.SWIM).instrument("crash_cymbals").colour(0xF7B884)
                .bond((mob, player) -> {
                    UUID owner = ((SlumblerTadpole) mob).getOwnerId();
                    return owner == null ? BandVoice.Bond.WILD : owner.equals(player.getUUID()) ? BandVoice.Bond.OWN : BandVoice.Bond.OTHER;
                }).register();
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(SLUMBLER_TADPOLE.get(), SlumblerTadpole.createAttributes().build());
        event.put(SLUMBLER_EGGS.get(), SlumblerEggs.createAttributes().build());
    }
}
