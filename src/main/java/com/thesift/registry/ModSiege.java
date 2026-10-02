package com.thesift.registry;

import com.thesift.TheSift;
import com.thesift.block.entity.AncientCannonBlockEntity;
import com.thesift.entity.siege.Cannonball;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The Thumper's arena: its ancient cannons and the cannonballs they fire (and the boulders it throws back). */
public final class ModSiege {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(TheSift.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TheSift.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<Cannonball>> CANNONBALL = ENTITIES.registerEntityType("cannonball", Cannonball::new,
            MobCategory.MISC, b -> b.sized(0.5F, 0.5F).clientTrackingRange(10).updateInterval(1));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AncientCannonBlockEntity>> ANCIENT_CANNON = BLOCK_ENTITIES.register(
            "ancient_cannon", () -> new BlockEntityType<>(AncientCannonBlockEntity::new, ModBlocks.ANCIENT_CANNON.get()));

    private ModSiege() {}

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        BLOCK_ENTITIES.register(bus);
    }
}
