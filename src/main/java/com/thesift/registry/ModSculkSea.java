package com.thesift.registry;

import com.mojang.serialization.MapCodec;
import com.thesift.TheSift;
import com.thesift.entity.CoralHook;
import com.thesift.entity.CoralOrgan;
import com.thesift.entity.SculkFish;
import com.thesift.music.Instrument;
import com.thesift.music.Song;
import com.thesift.music.SongEvents;
import com.thesift.music.band.BandRegistry;
import com.thesift.music.band.BandVoice;
import com.thesift.worldgen.CoralOrganFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * CR3 Fish &amp; Coral Organs: the new life of the Sculk Ocean - schools of glowing, biting Sculk Fish, and
 * the Sculk Coral Organ, a living reef that plays eerie chords and harpoons swimmers with a hooked line -
 * with their sounds, spawn rules, band voices and the organ's worldgen; and which water bucket each
 * music fish is carried in. Plain items (spawn eggs, the fish buckets) come from tools/spec.py via
 * tools/sculk_sea.py.
 */
public final class ModSculkSea {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(TheSift.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, TheSift.MODID);
    public static final DeferredRegister<MapCodec<? extends Feature>> FEATURE_TYPES = DeferredRegister.create(Registries.FEATURE_TYPE, TheSift.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<SculkFish>> SCULK_FISH = ENTITIES.registerEntityType("sculk_fish", SculkFish::new,
            MobCategory.WATER_AMBIENT, b -> b.sized(0.5F, 0.45F).eyeHeight(0.25F).clientTrackingRange(6).notInPeaceful());
    /** Rooted and persistent: placed by worldgen, never spawned naturally, so it takes no room in any mob cap. */
    public static final DeferredHolder<EntityType<?>, EntityType<CoralOrgan>> CORAL_ORGAN = ENTITIES.registerEntityType("coral_organ",
            CoralOrgan::new, MobCategory.MISC, b -> b.sized(1.4F, 2.0F).eyeHeight(1.1F).clientTrackingRange(10).notInPeaceful());
    public static final DeferredHolder<EntityType<?>, EntityType<CoralHook>> CORAL_HOOK = ENTITIES.registerEntityType("coral_hook", CoralHook::new,
            MobCategory.MISC, b -> b.sized(0.9F, 0.9F).clientTrackingRange(10).updateInterval(1).noSummon().fireImmune());

    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<CoralOrganFeature>> CORAL_ORGAN_FEATURE = FEATURE_TYPES.register(
            "coral_organ", () -> CoralOrganFeature.CODEC);

    public static final DeferredHolder<SoundEvent, SoundEvent> SCULK_FISH_AMBIENT = reg("entity.sculk_fish.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCULK_FISH_BITE = reg("entity.sculk_fish.bite");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCULK_FISH_HURT = reg("entity.sculk_fish.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCULK_FISH_DEATH = reg("entity.sculk_fish.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCULK_FISH_FLOP = reg("entity.sculk_fish.flop");
    public static final DeferredHolder<SoundEvent, SoundEvent> ORGAN_PIPE = reg("entity.coral_organ.pipe");
    public static final DeferredHolder<SoundEvent, SoundEvent> ORGAN_REED = reg("entity.coral_organ.reed");
    public static final DeferredHolder<SoundEvent, SoundEvent> ORGAN_DRONE = reg("entity.coral_organ.drone");
    public static final DeferredHolder<SoundEvent, SoundEvent> ORGAN_CHARGE = reg("entity.coral_organ.charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> ORGAN_FIRE = reg("entity.coral_organ.fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> ORGAN_REEL = reg("entity.coral_organ.reel");
    public static final DeferredHolder<SoundEvent, SoundEvent> ORGAN_CLAMP = reg("entity.coral_organ.clamp");
    public static final DeferredHolder<SoundEvent, SoundEvent> ORGAN_HURT = reg("entity.coral_organ.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> ORGAN_DEATH = reg("entity.coral_organ.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOOK_HIT = reg("entity.coral_hook.hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOOK_STRAIN = reg("entity.coral_hook.strain");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOOK_SNAP = reg("entity.coral_hook.snap");

    /** How far a played note carries through the water to the Sculk Fish (they come to see what made it). */
    public static final double NOTE_RANGE = 24.0;

    private ModSculkSea() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(TheSift.id(name)));
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        SOUNDS.register(bus);
        FEATURE_TYPES.register(bus);
        bus.addListener(ModSculkSea::attributes);
        bus.addListener(ModSculkSea::spawnPlacements);
        // the Sculk Fish hear through the water: any note played nearby brings the school to look for whoever played it
        SongEvents.listenNotes((level, player, at, pitch) -> {
            for (SculkFish f : level.getEntitiesOfClass(SculkFish.class, new AABB(at, at).inflate(NOTE_RANGE), SculkFish::isAlive)) {
                f.hear(at);
            }
        });
        // band voices: neither ever joins a band, but both answer the player's music
        BandRegistry.voice(SCULK_FISH, SoundEvents.NOTE_BLOCK_XYLOPHONE).transpose(12).volume(0.6F)
                .layer(SoundEvents.SCULK_CLICKING, 0.35F)
                .families(Instrument.Family.DRUM).temper(BandVoice.Temper.HOSTILE).movement(BandVoice.Movement.SWIM)
                .instrument("chattering_teeth").colour(0x3FF5E6).register();
        BandRegistry.voice(CORAL_ORGAN, SoundEvents.NOTE_BLOCK_FLUTE).transpose(-12).volume(1.4F)
                .layer(SoundEvents.NOTE_BLOCK_DIDGERIDOO, 0.5F)
                .songs(Song.LULLABY).temper(BandVoice.Temper.HOSTILE) // CLEAN: was the Tide Song
                .instrument("coral_organ").colour(0x2FB8B0).register();
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(SCULK_FISH.get(), SculkFish.createAttributes().build());
        event.put(CORAL_ORGAN.get(), CoralOrgan.createAttributes().build());
    }

    private static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(SCULK_FISH.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.OCEAN_FLOOR, ModSculkSea::checkSculkFish,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    /** Sculk Fish school in any water at least two blocks deep (the sea, the swamp's Sculk Water), never in peaceful. */
    private static boolean checkSculkFish(EntityType<SculkFish> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos,
            RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        if (EntitySpawnReason.isSpawner(reason)) {
            return true;
        }
        FluidState here = level.getFluidState(pos);
        FluidState below = level.getFluidState(pos.below());
        return !here.isEmpty() && !here.is(FluidTags.LAVA) && !below.isEmpty() && !below.is(FluidTags.LAVA);
    }

    /** The plain water bucket a music fish is scooped into, or empty if it has none (the Gobbler, the Sculk Fish). */
    public static ItemStack waterBucket(EntityType<?> type) {
        if (type == ModEntities.KAZOO_FISH.get()) {
            return new ItemStack(ModItems.KAZOO_FISH_BUCKET.get());
        }
        if (type == ModEntities.TUBAFISH.get()) {
            return new ItemStack(ModItems.TUBAFISH_BUCKET.get());
        }
        if (type == ModEntities.FANFARE_EEL.get()) {
            return new ItemStack(ModItems.FANFARE_EEL_BUCKET.get());
        }
        return ItemStack.EMPTY;
    }
}
