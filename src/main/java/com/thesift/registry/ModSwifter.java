package com.thesift.registry;

import com.mojang.serialization.MapCodec;
import com.thesift.TheSift;
import com.thesift.entity.Swifter;
import com.thesift.worldgen.SwifterDenFeature;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * A2 Swifter &amp; White Forest: the Swifter (a three-tailed cloud fox), the den worldgen that
 * places its families, the white lullwood's tree grower, the drifting fluff particle, the
 * Swifter's sounds and the White Forest's ambience. Its blocks and items are generated into
 * {@link ModBlocks} / {@link ModItems} from tools/swifter.py.
 */
public final class ModSwifter {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(TheSift.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, TheSift.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, TheSift.MODID);
    public static final DeferredRegister<MapCodec<? extends Feature>> FEATURE_TYPES = DeferredRegister.create(Registries.FEATURE_TYPE, TheSift.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<Swifter>> SWIFTER = ENTITIES.registerEntityType("swifter", Swifter::new,
            MobCategory.CREATURE, b -> b.sized(0.8F, 0.8F).eyeHeight(0.6F).clientTrackingRange(10));

    /** A tuft of white fluff that drifts and tumbles slowly down (the forest air, white leaves, a running Swifter). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> WHITE_FLUFF = PARTICLES.register("white_fluff",
            () -> new SimpleParticleType(false));

    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<SwifterDenFeature>> SWIFTER_DEN = FEATURE_TYPES.register(
            "swifter_den", () -> SwifterDenFeature.CODEC);

    public static final DeferredHolder<SoundEvent, SoundEvent> AMBIENT = reg("entity.swifter.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> HURT = reg("entity.swifter.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEATH = reg("entity.swifter.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> SNARL = reg("entity.swifter.snarl");
    public static final DeferredHolder<SoundEvent, SoundEvent> DASH = reg("entity.swifter.dash");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROCKET = reg("entity.swifter.rocket");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRAB = reg("entity.swifter.grab");
    public static final DeferredHolder<SoundEvent, SoundEvent> SLAM = reg("entity.swifter.slam");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUST = reg("entity.swifter.gust");
    public static final DeferredHolder<SoundEvent, SoundEvent> SLEEP = reg("entity.swifter.sleep");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRY = reg("entity.swifter.cry");
    public static final DeferredHolder<SoundEvent, SoundEvent> CALM = reg("entity.swifter.calm");
    public static final DeferredHolder<SoundEvent, SoundEvent> EAT = reg("entity.swifter.eat");
    public static final DeferredHolder<SoundEvent, SoundEvent> FOREST_ADDITIONS = reg("ambient.white_forest.additions");

    public static final ResourceKey<Biome> WHITE_FOREST = ResourceKey.create(Registries.BIOME, TheSift.id("white_forest"));
    public static final ResourceKey<Feature> WHITE_LULLWOOD_TREE = ResourceKey.create(Registries.FEATURE, TheSift.id("white_lullwood_tree"));
    public static final ResourceKey<Feature> GRAND_WHITE_LULLWOOD_TREE = ResourceKey.create(Registries.FEATURE, TheSift.id("grand_white_lullwood_tree"));
    /** The white lullwood sapling's grower (the same shapes as the lullwood, under white leaves). */
    public static final TreeGrower WHITE_LULLWOOD = new TreeGrower("thesift_white_lullwood",
            WeightedList.of(WHITE_LULLWOOD_TREE),
            WeightedList.of(GRAND_WHITE_LULLWOOD_TREE),
            WeightedList.<ResourceKey<Feature>>of(),
            WHITE_LULLWOOD_TREE);

    private ModSwifter() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(TheSift.id(name)));
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        SOUNDS.register(bus);
        PARTICLES.register(bus);
        FEATURE_TYPES.register(bus);
        bus.addListener(ModSwifter::attributes);
        bus.addListener(ModSwifter::spawnPlacements);
        NeoForge.EVENT_BUS.addListener(ModSwifter::onBreak);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(SWIFTER.get(), Swifter.createAttributes().build());
    }

    private static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(SWIFTER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> level.getBlockState(pos.below()).is(ModTags.Blocks.SIFT_PLANTABLE)
                        && Mob.checkMobSpawnRules(type, level, reason, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    /** A den broken by a player: its cubs burst into tears and their parents turn on the breaker. */
    private static void onBreak(BreakBlockEvent event) {
        if (!event.isCanceled() && event.getLevel() instanceof ServerLevel level && event.getState().is(ModBlocks.SWIFTER_DEN.get())) {
            Swifter.denBroken(level, event.getPos(), event.getPlayer());
        }
    }
}
