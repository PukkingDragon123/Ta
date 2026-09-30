package com.thesift.registry;

import com.mojang.serialization.MapCodec;
import com.thesift.TheSift;
import com.thesift.worldgen.FloatingIslandFeature;
import com.thesift.worldgen.SiftTreeFeature;
import com.thesift.worldgen.SpireFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModFeatures {
    public static final DeferredRegister<MapCodec<? extends Feature>> FEATURE_TYPES = DeferredRegister.create(Registries.FEATURE_TYPE, TheSift.MODID);

    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<SiftTreeFeature>> SIFT_TREE = FEATURE_TYPES.register("sift_tree",
            () -> SiftTreeFeature.CODEC);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<SpireFeature>> SPIRE = FEATURE_TYPES.register("spire",
            () -> SpireFeature.CODEC);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<FloatingIslandFeature>> FLOATING_ISLAND = FEATURE_TYPES.register(
            "floating_island", () -> FloatingIslandFeature.CODEC);

    private ModFeatures() {
    }
}
