package com.thesift.event;

import com.thesift.entity.Bulb;
import com.thesift.entity.Enchoer;
import com.thesift.entity.Harmoner;
import com.thesift.entity.boss.Dictator;
import com.thesift.entity.boss.Strumling;
import com.thesift.entity.boss.Strummer;
import com.thesift.entity.boss.Thumper;
import com.thesift.entity.Sifter;
import com.thesift.entity.Slumbler;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModEntities;
import com.thesift.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
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
        event.put(ModEntities.HARMONER.get(), Harmoner.createAttributes().build());
        event.put(ModEntities.SCULK_HARMONER.get(), com.thesift.entity.SculkHarmoner.createAttributes().build());
        event.put(ModEntities.DICTATOR.get(), Dictator.createAttributes().build());
        event.put(ModEntities.THUMPER.get(), Thumper.createAttributes().build());
        event.put(ModEntities.CONDUCTOR_MASK.get(), com.thesift.entity.boss.ConductorMask.createAttributes().build());
        event.put(ModEntities.STRUMMER.get(), Strummer.createAttributes().build());
        event.put(ModEntities.STRUMLING.get(), Strumling.createAttributes().build());
        event.put(ModEntities.SCULK_PARASITE.get(), com.thesift.entity.boss.SculkParasite.createAttributes().build());
        // the wild creatures
        event.put(ModEntities.STOMPER.get(), com.thesift.entity.Stomper.createAttributes().build());
        event.put(ModEntities.FANFARE_EEL.get(), com.thesift.entity.FanfareEel.createAttributes().build());
        event.put(ModEntities.KAZOO_FISH.get(), com.thesift.entity.KazooFish.createAttributes().build());
        event.put(ModEntities.SKY_WHALE.get(), com.thesift.entity.SkyWhale.createAttributes().build());
    }

    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(ModEntities.BULB.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ModBusEvents::checkSiftCreature, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // vanilla (Zombified) Sniffers no longer spawn in the Sift - one brought in blooms into a Sift Sniffer (E1, SiftRot)
        event.register(net.minecraft.world.entity.EntityTypes.SNIFFER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ModBusEvents::checkSiftCreature, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // S1 spawning: the Wishing Grove's Allays (vanilla never spawns them naturally, so they had no rules and
        // turned up anywhere, deep in caves and in mid-air) land on the grove's ground like the other creatures
        event.register(net.minecraft.world.entity.EntityTypes.ALLAY, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ModBusEvents::checkSiftCreature, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.HARMONER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ModBusEvents::checkSiftCreature, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.ENCHOER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ModBusEvents::checkSiftCreature, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.SLUMBLER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ModBusEvents::checkSlumbler, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.SIFTER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ModBusEvents::checkSifter, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // the wild creatures
        event.register(ModEntities.STOMPER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ModBusEvents::checkSiftCreature, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.FANFARE_EEL.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.OCEAN_FLOOR,
                ModBusEvents::checkSiftFish, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.KAZOO_FISH.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.OCEAN_FLOOR,
                ModBusEvents::checkSiftFish, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.SKY_WHALE.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ModBusEvents::checkSkyWhale, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    public static void addBlockEntityBlocks(BlockEntityTypeAddBlocksEvent event) {
        event.modify(BlockEntityTypes.BRUSHABLE_BLOCK, ModBlocks.SUSPICIOUS_CHIME_SAND.get());
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

    /**
     * CR1 spawning: the Sifter is a neutral creature of the dunes - it settles on open sand by day or by
     * night (it fears no light), and in pitch dark it may sit on bare rock (the Deep Sift, the caves under
     * the dunes). Elsewhere it only comes from a spawner.
     */
    private static <T extends Mob> boolean checkSifter(EntityType<T> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos,
            RandomSource random) {
        boolean dark = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos) == 0
                && level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos) == 0;
        boolean home = com.thesift.entity.Sifter.isSand(level.getBlockState(pos.below())) || dark;
        return (home || EntitySpawnReason.isSpawner(reason)) && Mob.checkMobSpawnRules(type, level, reason, pos, random);
    }

    /** The music fish spawn inside Chrome (or water) that is at least two blocks deep. */
    private static <T extends Mob> boolean checkSiftFish(EntityType<T> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos,
            RandomSource random) {
        net.minecraft.world.level.material.FluidState here = level.getFluidState(pos);
        return !here.isEmpty() && !here.is(net.minecraft.tags.FluidTags.LAVA) && !level.getFluidState(pos.below()).isEmpty()
                && !level.getFluidState(pos.below()).is(net.minecraft.tags.FluidTags.LAVA);
    }

    /** Sky Whales appear (rarely) out in the open under the sky; they rise to cruising height on spawning. */
    private static <T extends Mob> boolean checkSkyWhale(EntityType<T> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos,
            RandomSource random) {
        if (EntitySpawnReason.isSpawner(reason)) {
            return true;
        }
        return pos.getY() >= level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) && level.getFluidState(pos).isEmpty()
                && random.nextInt(3) == 0;
    }
}
