package com.thesift.registry;

import com.thesift.TheSift;
import com.thesift.block.entity.BossDenBlockEntity;
import com.thesift.block.entity.SiftDrumBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TheSift.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SiftDrumBlockEntity>> SIFT_DRUM = BLOCK_ENTITIES.register("sift_drum",
            () -> new BlockEntityType<>(SiftDrumBlockEntity::new, ModBlocks.SIFT_DRUM.get()));
    // F1: the Euphory Altar is gone - the Europhy Table (registry/ModEurophy) took over its ritual
    // RR: the Encore Sigils, Instrument Altars and Conductor's Podium are gone - a hidden Boss Den wakes each boss
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BossDenBlockEntity>> BOSS_DEN = BLOCK_ENTITIES.register(
            "boss_den", () -> new BlockEntityType<>(BossDenBlockEntity::new, ModBlocks.BOSS_DEN.get()));

    private ModBlockEntities() {}
}
