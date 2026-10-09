package com.thesift.registry;

import com.mojang.serialization.MapCodec;
import com.thesift.TheSift;
import com.thesift.worldgen.PaleTreeFeature;
import com.thesift.worldgen.RainbowSnowfallFeature;
import com.thesift.worldgen.RockFormationFeature;
import com.thesift.worldgen.TuningCactusFeature;
import java.util.List;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * W-land: the ground of The Sift. The Rocky Dunes' rock formations and Tuning Cacti, the White Forest's weeping White
 * Lullwoods and Rainbow Snow (features), the rainbow snowflake particle and the Tuning Fruit's food. The blocks and items
 * are generated into {@link ModBlocks} / {@link ModItems} from tools/wland.py; the terrain itself is data
 * (thesift:sift/final_density, tools/wland.py).
 */
public final class ModWorldLand {
    public static final DeferredRegister<MapCodec<? extends Feature>> FEATURE_TYPES = DeferredRegister.create(Registries.FEATURE_TYPE, TheSift.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, TheSift.MODID);

    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<RockFormationFeature>> ROCK_FORMATION = FEATURE_TYPES.register(
            "rock_formation", () -> RockFormationFeature.CODEC);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<TuningCactusFeature>> TUNING_CACTUS = FEATURE_TYPES.register(
            "tuning_cactus", () -> TuningCactusFeature.CODEC);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<PaleTreeFeature>> PALE_TREE = FEATURE_TYPES.register(
            "pale_tree", () -> PaleTreeFeature.CODEC);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<RainbowSnowfallFeature>> RAINBOW_SNOWFALL = FEATURE_TYPES.register(
            "rainbow_snowfall", () -> RainbowSnowfallFeature.CODEC);

    /** A flake of Rainbow Snow: it drifts down slowly, turning round the colour wheel as it falls. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RAINBOW_SNOWFLAKE = PARTICLES.register("rainbow_snowflake",
            () -> new SimpleParticleType(false));

    /** Tuning Fruit: sweet, a little fizzy - you hop like a Jaberora for a while. */
    public static final FoodProperties TUNING_FRUIT = new FoodProperties.Builder().nutrition(4).saturationModifier(0.3F).build();
    public static final Consumable TUNING_FRUIT_CONSUMABLE = Consumables.defaultFood()
            .consumeSeconds(1.2F)
            .onConsume(new ApplyStatusEffectsConsumeEffect(List.of(new MobEffectInstance(MobEffects.JUMP_BOOST, 160, 1))))
            .build();

    private ModWorldLand() {
    }

    public static void register(IEventBus bus) {
        FEATURE_TYPES.register(bus);
        PARTICLES.register(bus);
    }
}
