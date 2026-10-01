package com.thesift.registry;

import com.thesift.TheSift;
import com.thesift.effect.DeafenedEffect;
import com.thesift.effect.EuphoriaEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, TheSift.MODID);

    /** Wardens (and anything else that hunts by sound) lose their hearing. */
    public static final DeferredHolder<MobEffect, MobEffect> DEAFENED = EFFECTS.register("deafened",
            () -> new DeafenedEffect(MobEffectCategory.NEUTRAL, 0x7FE8E0));
    /** A dreamy high from Sift Cake and the Euphory Altar. */
    public static final DeferredHolder<MobEffect, MobEffect> EUPHORIA = EFFECTS.register("euphoria",
            () -> new EuphoriaEffect(MobEffectCategory.BENEFICIAL, 0xF59AD0));

    /** The Conductor's Staff's song: stands still and sings for a few seconds. */
    public static final DeferredHolder<MobEffect, MobEffect> ENTRANCED = EFFECTS.register("entranced",
            () -> new com.thesift.effect.EntrancedEffect(MobEffectCategory.HARMFUL, 0x2EF2E2).addAttributeModifier(
                    net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, TheSift.id("entranced_still"), -1.0,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    /** The Crane Flute's whirl of feathers: turns projectiles, softens blows, clouds your view. */
    public static final DeferredHolder<MobEffect, MobEffect> FEATHER_SHIELD = EFFECTS.register("feather_shield",
            () -> new com.thesift.effect.FeatherShieldEffect(MobEffectCategory.BENEFICIAL, 0xF2F1EA));
    /** The Conductor's curse: a slow wither that darkens your sight the longer it lasts. */
    public static final DeferredHolder<MobEffect, MobEffect> SCULK_CORRUPTION = EFFECTS.register("sculk_corruption",
            () -> new com.thesift.effect.SculkCorruptionEffect(MobEffectCategory.HARMFUL, 0x0B5A5A));

    private ModEffects() {}
}
