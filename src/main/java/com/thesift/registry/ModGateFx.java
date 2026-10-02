package com.thesift.registry;

import com.thesift.TheSift;
import com.thesift.block.entity.SiftPortalBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** B1 Portal & sky FX: the portal's block entity, which lets the client draw a sky behind the frame. */
public final class ModGateFx {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TheSift.MODID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SiftPortalBlockEntity>> SIFT_PORTAL = BLOCK_ENTITIES.register(
            "sift_portal", () -> new BlockEntityType<>(SiftPortalBlockEntity::new, ModBlocks.SIFT_PORTAL.get()));

    private ModGateFx() {
    }

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }
}
