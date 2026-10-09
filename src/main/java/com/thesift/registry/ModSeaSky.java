package com.thesift.registry;

import com.mojang.serialization.MapCodec;
import com.thesift.TheSift;
import com.thesift.entity.Gobbler;
import com.thesift.music.Song;
import com.thesift.music.SongEvents;
import com.thesift.worldgen.JaggedPillarFeature;
import com.thesift.worldgen.SeaFloodFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Sea & sky (agents F + W): the Gobbler, the worldgen of the Magic Kelp Forest, the Deep Dark Ocean
 * and the Sound Garden (real water for the two oceans, the Deep Dark Ocean's rock pillars), and
 * the Gobbler's ears: any music played nearby draws it, and the Lullaby lulls it (CLEAN: the Tide Song is gone).
 */
public final class ModSeaSky {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(TheSift.MODID);
    public static final DeferredRegister<MapCodec<? extends Feature>> FEATURE_TYPES = DeferredRegister.create(Registries.FEATURE_TYPE, TheSift.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<Gobbler>> GOBBLER = ENTITIES.registerEntityType("gobbler", Gobbler::new,
            MobCategory.WATER_CREATURE, b -> b.sized(2.2F, 1.3F).eyeHeight(0.7F).clientTrackingRange(10));

    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<SeaFloodFeature>> SEA_FLOOD = FEATURE_TYPES.register("sea_flood",
            () -> SeaFloodFeature.CODEC);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<JaggedPillarFeature>> JAGGED_PILLAR = FEATURE_TYPES.register(
            "jagged_pillar", () -> JaggedPillarFeature.CODEC);

    /** How long the Lullaby keeps a Gobbler lulled (two minutes), and how far it carries under water. */
    public static final int LULL_TICKS = 2400;
    public static final double LULL_RADIUS = 48.0;

    private ModSeaSky() {}

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        FEATURE_TYPES.register(bus);
        bus.addListener(ModSeaSky::registerAttributes);
        bus.addListener(ModSeaSky::registerSpawnPlacements);
        SongEvents.listenSongs((level, player, at, song) -> {
            if (song == Song.LULLABY) {
                for (Gobbler g : level.getEntitiesOfClass(Gobbler.class, new AABB(at, at).inflate(LULL_RADIUS), Gobbler::isAlive)) {
                    g.calm(LULL_TICKS);
                }
            }
        });
        // a blind hunter hears music through the water: every note played nearby draws it in
        SongEvents.listenNotes((level, player, at, pitch) -> {
            for (Gobbler g : level.getEntitiesOfClass(Gobbler.class, new AABB(at, at).inflate(20.0), Gobbler::isAlive)) {
                g.sense(at, player);
            }
        });
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(GOBBLER.get(), Gobbler.createAttributes().build());
    }

    private static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(GOBBLER.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.OCEAN_FLOOR, ModSeaSky::checkGobbler,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    /** Gobblers only rise out of deep water: at least ten blocks under the surface, with water below too. */
    private static boolean checkGobbler(EntityType<Gobbler> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos,
            RandomSource random) {
        if (EntitySpawnReason.isSpawner(reason)) {
            return true;
        }
        return level.getFluidState(pos).is(Fluids.WATER) && level.getFluidState(pos.below()).is(Fluids.WATER)
                && level.getFluidState(pos.above(2)).is(Fluids.WATER) && pos.getY() < 53;
    }
}
