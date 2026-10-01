package com.thesift.registry;

import com.thesift.TheSift;
import com.thesift.entity.Bulb;
import com.thesift.entity.Enchoer;
import com.thesift.entity.GlowballEntity;
import com.thesift.entity.Harmoner;
import com.thesift.entity.Riveter;
import com.thesift.entity.SiftSniffer;
import com.thesift.entity.Sifter;
import com.thesift.entity.Slumbler;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(TheSift.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<Bulb>> BULB = ENTITIES.registerEntityType("bulb", Bulb::new, MobCategory.CREATURE,
            b -> b.sized(0.75F, 0.85F).eyeHeight(0.45F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<Slumbler>> SLUMBLER = ENTITIES.registerEntityType("slumbler", Slumbler::new,
            MobCategory.CREATURE, b -> b.sized(1.7F, 0.95F).eyeHeight(0.7F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Sifter>> SIFTER = ENTITIES.registerEntityType("sifter", Sifter::new, MobCategory.MONSTER,
            b -> b.sized(0.9F, 1.2F).eyeHeight(0.95F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<Enchoer>> ENCHOER = ENTITIES.registerEntityType("enchoer", Enchoer::new,
            MobCategory.CREATURE, b -> b.sized(1.0F, 2.5F).eyeHeight(1.8F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Riveter>> RIVETER = ENTITIES.registerEntityType("riveter", Riveter::new,
            MobCategory.MONSTER, b -> b.sized(0.8F, 1.9F).eyeHeight(0.4F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<Harmoner>> HARMONER = ENTITIES.registerEntityType("harmoner", Harmoner::new,
            MobCategory.CREATURE, b -> b.sized(0.55F, 0.9F).eyeHeight(0.75F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<SiftSniffer>> SIFT_SNIFFER = ENTITIES.registerEntityType("sift_sniffer",
            SiftSniffer::new, MobCategory.CREATURE, b -> b.sized(1.9F, 1.75F).eyeHeight(1.05F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<GlowballEntity>> GLOWBALL = ENTITIES.registerEntityType("glowball",
            GlowballEntity::new, MobCategory.MISC, b -> b.sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10));

    private ModEntities() {
    }
}
