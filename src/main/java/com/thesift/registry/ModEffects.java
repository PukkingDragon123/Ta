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

    private ModEffects() {}
}
