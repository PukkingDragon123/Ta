package com.thesift.registry;

import java.util.List;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.RemoveStatusEffectsConsumeEffect;

/**
 * Sea & sky: the meat of every Sift fish, raw and cooked, and the sushi rolled from it with dried
 * kelp and glowkelp. Each sushi lends a little of its fish to you:
 * <ul>
 *   <li>Kazoo Fish Sushi: breathe underwater.</li>
 *   <li>Tubafish Sushi: see in the dark, and toughen up like a puffed Tubafish.</li>
 *   <li>Fanfare Eel Sushi: glide through the water like an eel.</li>
 *   <li>Gobbler Sushi (with its glowing sculk bladder): cures Sculk Corruption and lets you see in the dark.</li>
 *   <li>Sushi Platter: all of it at once, for a long dive.</li>
 * </ul>
 */
public final class ModSeaFoods {
    public static final FoodProperties TUBAFISH = new FoodProperties.Builder().nutrition(2).saturationModifier(0.1F).build();
    public static final FoodProperties COOKED_TUBAFISH = new FoodProperties.Builder().nutrition(6).saturationModifier(0.7F).build();
    public static final FoodProperties FANFARE_EEL = new FoodProperties.Builder().nutrition(2).saturationModifier(0.2F).build();
    public static final FoodProperties COOKED_FANFARE_EEL = new FoodProperties.Builder().nutrition(6).saturationModifier(0.8F).build();
    public static final FoodProperties GOBBLER_FILLET = new FoodProperties.Builder().nutrition(3).saturationModifier(0.2F).build();
    public static final FoodProperties COOKED_GOBBLER_FILLET = new FoodProperties.Builder().nutrition(9).saturationModifier(0.9F).build();

    public static final FoodProperties KAZOO_FISH_SUSHI = new FoodProperties.Builder().nutrition(5).saturationModifier(0.6F).build();
    public static final Consumable KAZOO_FISH_SUSHI_CONSUMABLE = Consumables.defaultFood().consumeSeconds(1.0F)
            .onConsume(new ApplyStatusEffectsConsumeEffect(List.of(new MobEffectInstance(MobEffects.WATER_BREATHING, 1800, 0))))
            .build();

    public static final FoodProperties TUBAFISH_SUSHI = new FoodProperties.Builder().nutrition(5).saturationModifier(0.6F).build();
    public static final Consumable TUBAFISH_SUSHI_CONSUMABLE = Consumables.defaultFood().consumeSeconds(1.0F)
            .onConsume(new ApplyStatusEffectsConsumeEffect(List.of(
                    new MobEffectInstance(MobEffects.NIGHT_VISION, 2400, 0),
                    new MobEffectInstance(MobEffects.RESISTANCE, 600, 0))))
            .build();

    public static final FoodProperties FANFARE_EEL_SUSHI = new FoodProperties.Builder().nutrition(5).saturationModifier(0.6F).build();
    public static final Consumable FANFARE_EEL_SUSHI_CONSUMABLE = Consumables.defaultFood().consumeSeconds(1.0F)
            .onConsume(new ApplyStatusEffectsConsumeEffect(List.of(
                    new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 900, 0),
                    new MobEffectInstance(MobEffects.WATER_BREATHING, 600, 0))))
            .build();

    public static final FoodProperties GOBBLER_SUSHI = new FoodProperties.Builder().nutrition(7).saturationModifier(0.7F).alwaysEdible().build();
    public static final Consumable GOBBLER_SUSHI_CONSUMABLE = Consumables.defaultFood().consumeSeconds(1.2F)
            .onConsume(new RemoveStatusEffectsConsumeEffect(ModEffects.SCULK_CORRUPTION))
            .onConsume(new ApplyStatusEffectsConsumeEffect(List.of(new MobEffectInstance(MobEffects.NIGHT_VISION, 3600, 0))))
            .build();

    public static final FoodProperties SUSHI_PLATTER = new FoodProperties.Builder().nutrition(14).saturationModifier(1.0F).alwaysEdible().build();
    public static final Consumable SUSHI_PLATTER_CONSUMABLE = Consumables.defaultFood().consumeSeconds(2.4F)
            .onConsume(new RemoveStatusEffectsConsumeEffect(ModEffects.SCULK_CORRUPTION))
            .onConsume(new ApplyStatusEffectsConsumeEffect(List.of(
                    new MobEffectInstance(MobEffects.WATER_BREATHING, 6000, 0),
                    new MobEffectInstance(MobEffects.NIGHT_VISION, 6000, 0),
                    new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 1200, 0),
                    new MobEffectInstance(MobEffects.REGENERATION, 200, 0))))
            .build();

    private ModSeaFoods() {}
}
