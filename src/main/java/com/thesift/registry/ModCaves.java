package com.thesift.registry;

import com.mojang.serialization.MapCodec;
import com.thesift.TheSift;
import com.thesift.block.entity.SculkGrasperBlockEntity;
import com.thesift.worldgen.CaveScrubFeature;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * W-deep caves: the Sift's underground. The Sculk Grasper's block entity, the cave-scrub worldgen feature (no grass on
 * roofed cave floors), the acid particles, the acid and grasp damage types, the crystal dripstone tag and the cave biomes'
 * keys. The blocks themselves (crystal dripstone, pointed crystals and clusters in three colours, the Shocker Plant, Acid
 * Weeper and Acid Puddle, Writhing Sculk, Sculk Tendrils and the Sculk Grasper) come from tools/spec.py via tools/caves.py,
 * which also builds the Sift Caves, Cave Jungle and Sculk Caves biomes and places them by depth.
 */
public final class ModCaves {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TheSift.MODID);
    public static final DeferredRegister<MapCodec<? extends Feature>> FEATURE_TYPES = DeferredRegister.create(Registries.FEATURE_TYPE, TheSift.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, TheSift.MODID);

    /** The Sculk Grasper: watches for prey, lunges, holds and reels it in (block/entity/SculkGrasperBlockEntity). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SculkGrasperBlockEntity>> SCULK_GRASPER = BLOCK_ENTITIES.register(
            "sculk_grasper", () -> new BlockEntityType<>(SculkGrasperBlockEntity::new, ModBlocks.SCULK_GRASPER.get()));

    /** Turns grass and turf left on roofed cave floors back into stone (worldgen/CaveScrubFeature). */
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<CaveScrubFeature>> CAVE_SCRUB = FEATURE_TYPES.register("cave_scrub",
            () -> CaveScrubFeature.CODEC);

    /** A drop of acid falling from an Acid Weeper; it splashes where it lands. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ACID_DRIP = PARTICLES.register("acid_drip", () -> new SimpleParticleType(false));
    /** Little bubbles of acid fizzing up off a puddle, or off whatever the acid is eating. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ACID_FIZZ = PARTICLES.register("acid_fizz", () -> new SimpleParticleType(false));

    /** Acid: burns, and eats away at the armour of whatever it touches. */
    public static final ResourceKey<DamageType> ACID = ResourceKey.create(Registries.DAMAGE_TYPE, TheSift.id("acid"));
    /** The Sculk Grasper's grip. */
    public static final ResourceKey<DamageType> SCULK_GRASP = ResourceKey.create(Registries.DAMAGE_TYPE, TheSift.id("sculk_grasp"));

    /** Blocks that pointed crystals grow under. */
    public static final TagKey<Block> CRYSTAL_DRIPSTONE = TagKey.create(Registries.BLOCK, TheSift.id("crystal_dripstone"));

    public static final ResourceKey<Biome> SIFT_CAVES = biome("sift_caves");
    public static final ResourceKey<Biome> CAVE_JUNGLE = biome("cave_jungle");
    public static final ResourceKey<Biome> SCULK_CAVES = biome("sculk_caves");

    private ModCaves() {
    }

    private static ResourceKey<Biome> biome(String name) {
        return ResourceKey.create(Registries.BIOME, TheSift.id(name));
    }

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
        FEATURE_TYPES.register(bus);
        PARTICLES.register(bus);
    }
}
