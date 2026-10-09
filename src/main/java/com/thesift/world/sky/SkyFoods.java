package com.thesift.world.sky;

import java.util.List;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;

/** W-sky: the fruit of the Sky Islands. */
public final class SkyFoods {
    /** The sky banana: filling, and for a moment you drift down like a leaf (Swingers adore them). */
    public static final FoodProperties SKYRIND = new FoodProperties.Builder().nutrition(4).saturationModifier(0.5F).build();
    public static final Consumable SKYRIND_CONSUMABLE = Consumables.defaultFood()
            .consumeSeconds(1.2F)
            .onConsume(new ApplyStatusEffectsConsumeEffect(List.of(new MobEffectInstance(MobEffects.SLOW_FALLING, 140, 0))))
            .build();
    /** A slice of Driftfruit: a springy snack that puts a bounce in your step. */
    public static final FoodProperties DRIFTFRUIT_SLICE = new FoodProperties.Builder().nutrition(3).saturationModifier(0.4F).build();
    public static final Consumable DRIFTFRUIT_SLICE_CONSUMABLE = Consumables.defaultFood()
            .consumeSeconds(1.0F)
            .onConsume(new ApplyStatusEffectsConsumeEffect(List.of(new MobEffectInstance(MobEffects.JUMP_BOOST, 200, 0))))
            .build();

    private SkyFoods() {
    }
}
