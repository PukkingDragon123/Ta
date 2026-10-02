package com.thesift.registry;

import java.util.List;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;

/**
 * Pitcher Nectar from a potted Pitcher Planter, and the three soups cooked from it. Each soup pairs
 * the nectar with plants from one corner of the Sift and does something quite different:
 * <ul>
 *   <li>Lullaby Soup (lullaby bell, choir pod): mends you and wraps you in golden hearts.</li>
 *   <li>Echo Chowder (echo orchid, glowcap): for the deep - you see in the dark and dig quicker.</li>
 *   <li>Chrome Bisque (chrome reeds, glowing slime): you breathe and glide through water.</li>
 * </ul>
 */
public final class ModSoups {
    public static final FoodProperties PITCHER_NECTAR = new FoodProperties.Builder().nutrition(2).saturationModifier(0.4F).alwaysEdible().build();
    public static final Consumable PITCHER_NECTAR_CONSUMABLE = Consumables.defaultDrink().consumeSeconds(0.8F).build();

    public static final FoodProperties LULLABY_SOUP = new FoodProperties.Builder().nutrition(7).saturationModifier(0.8F).build();
    public static final Consumable LULLABY_SOUP_CONSUMABLE = Consumables.defaultFood()
            .onConsume(new ApplyStatusEffectsConsumeEffect(List.of(
                    new MobEffectInstance(MobEffects.REGENERATION, 300, 0),
                    new MobEffectInstance(MobEffects.ABSORPTION, 2400, 1))))
            .build();

    public static final FoodProperties ECHO_CHOWDER = new FoodProperties.Builder().nutrition(8).saturationModifier(0.7F).build();
    public static final Consumable ECHO_CHOWDER_CONSUMABLE = Consumables.defaultFood()
            .onConsume(new ApplyStatusEffectsConsumeEffect(List.of(
                    new MobEffectInstance(MobEffects.NIGHT_VISION, 3600, 0),
                    new MobEffectInstance(MobEffects.HASTE, 2400, 1))))
            .build();

    public static final FoodProperties CHROME_BISQUE = new FoodProperties.Builder().nutrition(6).saturationModifier(0.6F).build();
    public static final Consumable CHROME_BISQUE_CONSUMABLE = Consumables.defaultFood()
            .onConsume(new ApplyStatusEffectsConsumeEffect(List.of(
                    new MobEffectInstance(MobEffects.WATER_BREATHING, 3600, 0),
                    new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 900, 0))))
            .build();

    private ModSoups() {}
}
