package com.thesift.registry;

import java.util.List;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;

public final class ModFoods {
    public static final FoodProperties GLOWING_SLIME_BALL = new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).alwaysEdible().build();
    public static final Consumable GLOWING_SLIME_BALL_CONSUMABLE = Consumables.defaultFood()
            .consumeSeconds(1.0F)
            .onConsume(new ApplyStatusEffectsConsumeEffect(List.of(
                    new MobEffectInstance(MobEffects.GLOWING, 200, 0),
                    new MobEffectInstance(MobEffects.REGENERATION, 60, 0))))
            .build();

    public static final FoodProperties PITCHER_BULB = new FoodProperties.Builder().nutrition(3).saturationModifier(0.4F).build();

    public static final FoodProperties DREAM_STEW = new FoodProperties.Builder().nutrition(8).saturationModifier(0.7F).build();
    public static final Consumable DREAM_STEW_CONSUMABLE = Consumables.defaultFood()
            .onConsume(new ApplyStatusEffectsConsumeEffect(List.of(
                    new MobEffectInstance(MobEffects.NIGHT_VISION, 1200, 0),
                    new MobEffectInstance(MobEffects.REGENERATION, 100, 0))))
            .build();

    public static final FoodProperties GLOWCAP_SKEWER = new FoodProperties.Builder().nutrition(5).saturationModifier(0.6F).build();

    private ModFoods() {}
}
