package com.thesift.registry;

import com.thesift.TheSift;
import com.thesift.block.entity.ConductorsPodiumBlockEntity;
import com.thesift.block.entity.EuphoryAltarBlockEntity;
import com.thesift.block.entity.SiftDrumBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TheSift.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SiftDrumBlockEntity>> SIFT_DRUM = BLOCK_ENTITIES.register("sift_drum",
            () -> new BlockEntityType<>(SiftDrumBlockEntity::new, ModBlocks.SIFT_DRUM.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EuphoryAltarBlockEntity>> EUPHORY_ALTAR = BLOCK_ENTITIES.register("euphory_altar",
            () -> new BlockEntityType<>(EuphoryAltarBlockEntity::new, ModBlocks.EUPHORY_ALTAR.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ConductorsPodiumBlockEntity>> CONDUCTORS_PODIUM = BLOCK_ENTITIES.register(
            "conductors_podium", () -> new BlockEntityType<>(ConductorsPodiumBlockEntity::new, ModBlocks.CONDUCTORS_PODIUM.get()));

    private ModBlockEntities() {}
}
