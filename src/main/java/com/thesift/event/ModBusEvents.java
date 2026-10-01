package com.thesift.event;

import com.thesift.entity.Bulb;
import com.thesift.entity.Enchoer;
import com.thesift.entity.Harmoner;
import com.thesift.entity.Riveter;
import com.thesift.entity.SiftSniffer;
import com.thesift.entity.Sifter;
import com.thesift.entity.Slumbler;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModEntities;
import com.thesift.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

/** Listeners on the mod event bus. */
public final class ModBusEvents {
    private ModBusEvents() {
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.BULB.get(), Bulb.createAttributes().build());
        event.put(ModEntities.SLUMBLER.get(), Slumbler.createAttributes().build());
        event.put(ModEntities.SIFTER.get(), Sifter.createAttributes().build());
        event.put(ModEntities.ENCHOER.get(), Enchoer.createAttributes().build());
        event.put(ModEntities.RIVETER.get(), Riveter.createAttributes().build());
        event.put(ModEntities.HARMONER.get(), Harmoner.createAttributes().build());
        event.put(ModEntities.SIFT_SNIFFER.get(), SiftSniffer.createAttributes().build());
    }

    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(ModEntities.BULB.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ModBusEvents::checkSiftCreature, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.SIFT_SNIFFER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ModBusEvents::checkSiftCreature, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.HARMONER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ModBusEvents::checkSiftCreature, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.ENCHOER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ModBusEvents::checkSiftCreature, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.SLUMBLER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ModBusEvents::checkSlumbler, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.SIFTER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ModBusEvents::checkSifter, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.RIVETER.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ModBusEvents::checkRiveter, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    public static void addBlockEntityBlocks(BlockEntityTypeAddBlocksEvent event) {
        event.modify(BlockEntityTypes.BRUSHABLE_BLOCK, ModBlocks.SUSPICIOUS_DREAMSAND.get());
    }

    private static <T extends Mob> boolean checkSiftCreature(EntityType<T> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos,
            RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        return (below.is(ModTags.Blocks.SIFT_PLANTABLE) || EntitySpawnReason.isSpawner(reason)) && Mob.checkMobSpawnRules(type, level, reason, pos, random);
    }

    private static <T extends Mob> boolean checkSlumbler(EntityType<T> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos,
            RandomSource random) {
        return Mob.checkMobSpawnRules(type, level, reason, pos, random);
    }

    private static <T extends Mob> boolean checkSifter(EntityType<T> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos,
            RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL && Mob.checkMobSpawnRules(type, level, reason, pos, random);
    }

    private static boolean checkRiveter(EntityType<Riveter> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL && Riveter.checkRiveterSpawnRules(type, level, reason, pos, random);
    }
}
