package com.thesift.registry;

import com.mojang.serialization.MapCodec;
import com.thesift.TheSift;
import com.thesift.worldgen.SiftReefFeature;
import com.thesift.worldgen.TrumpetCoralFeature;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * W-sea: the Brass Coral Reef and the Chrome Coral Ocean - the worldgen feature types that grow their Trumpet Coral and
 * reefs, the reef's sounds (each metal of Trumpet Coral has its own trumpet voice), the two biomes' keys and the Trumpet
 * Coral Bell as food. The blocks themselves come from tools/spec.py via tools/sea_reefs.py; the Chrome tint and fog live
 * in client/SeaReefsClient; water meeting Chrome is worldgen/SeaFloodFeature's {@code blend}.
 */
public final class ModSeaReefs {
    public static final DeferredRegister<MapCodec<? extends Feature>> FEATURE_TYPES = DeferredRegister.create(Registries.FEATURE_TYPE, TheSift.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, TheSift.MODID);

    /** Instrument-shaped Trumpet Coral growths (worldgen/TrumpetCoralFeature). */
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<TrumpetCoralFeature>> TRUMPET_CORAL = FEATURE_TYPES.register(
            "trumpet_coral", () -> TrumpetCoralFeature.CODEC);
    /** Coral reef shapes: trees, domes, spires, arches and clusters, dressed with corals and fans (worldgen/SiftReefFeature). */
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<SiftReefFeature>> SIFT_REEF = FEATURE_TYPES.register(
            "sift_reef", () -> SiftReefFeature.CODEC);

    public static final DeferredHolder<SoundEvent, SoundEvent> TRUMPET_BRASS = reg("block.trumpet_coral.brass");
    public static final DeferredHolder<SoundEvent, SoundEvent> TRUMPET_SILVER = reg("block.trumpet_coral.silver");
    public static final DeferredHolder<SoundEvent, SoundEvent> TRUMPET_COPPER = reg("block.trumpet_coral.copper");
    public static final DeferredHolder<SoundEvent, SoundEvent> TRUMPET_VERDIGRIS = reg("block.trumpet_coral.verdigris");
    public static final DeferredHolder<SoundEvent, SoundEvent> TRUMPET_GROW = reg("block.trumpet_coral.grow");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_REEF = reg("music.brass_coral_reef");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_CHROME_OCEAN = reg("music.chrome_coral_ocean");
    public static final DeferredHolder<SoundEvent, SoundEvent> REEF_LOOP = reg("ambient.brass_coral_reef.loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> REEF_ADDITIONS = reg("ambient.brass_coral_reef.additions");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHROME_OCEAN_LOOP = reg("ambient.chrome_coral_ocean.loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHROME_OCEAN_ADDITIONS = reg("ambient.chrome_coral_ocean.additions");

    public static final ResourceKey<Biome> BRASS_CORAL_REEF = ResourceKey.create(Registries.BIOME, TheSift.id("brass_coral_reef"));
    public static final ResourceKey<Biome> CHROME_CORAL_OCEAN = ResourceKey.create(Registries.BIOME, TheSift.id("chrome_coral_ocean"));

    /** A Trumpet Coral Bell: crunchy brass coral, with a gulp of the air trapped in its throat. */
    public static final FoodProperties TRUMPET_BELL_FOOD = new FoodProperties.Builder().nutrition(3).saturationModifier(0.4F).build();
    public static final Consumable TRUMPET_BELL_CONSUMABLE = Consumables.defaultFood().consumeSeconds(1.2F)
            .onConsume(new ApplyStatusEffectsConsumeEffect(List.of(new MobEffectInstance(MobEffects.WATER_BREATHING, 300, 0))))
            .build();

    private ModSeaReefs() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(TheSift.id(name)));
    }

    public static void register(IEventBus bus) {
        FEATURE_TYPES.register(bus);
        SOUNDS.register(bus);
    }
}
