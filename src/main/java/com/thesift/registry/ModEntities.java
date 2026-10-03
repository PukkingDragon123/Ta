package com.thesift.registry;

import com.thesift.TheSift;
import com.thesift.entity.Bulb;
import com.thesift.entity.Enchoer;
import com.thesift.entity.GlowballEntity;
import com.thesift.entity.Harmoner;
import com.thesift.entity.Riveter;
import com.thesift.entity.boss.Dictator;
import com.thesift.entity.boss.Strumling;
import com.thesift.entity.boss.Strummer;
import com.thesift.entity.boss.Thumper;
import com.thesift.entity.boss.WebShot;
import com.thesift.entity.Sifter;
import com.thesift.entity.Slumbler;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(TheSift.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<Bulb>> BULB = ENTITIES.registerEntityType("bulb", Bulb::new, MobCategory.CREATURE,
            b -> b.sized(0.4F, 0.42F).eyeHeight(0.18F).clientTrackingRange(8)); // A1: the little 1:1 Bulb
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
    public static final DeferredHolder<EntityType<?>, EntityType<Dictator>> DICTATOR = ENTITIES.registerEntityType("dictator", Dictator::new,
            MobCategory.MONSTER, b -> b.sized(1.4F, 5.6F).eyeHeight(4.9F).clientTrackingRange(16).fireImmune()); // C3: 1.5x taller (the colossus scales this, Dictator.getDefaultDimensions)
    /** Summoned by the Conductor's Staff: circles its summoner and sings buffs over them. */
    public static final DeferredHolder<EntityType<?>, EntityType<com.thesift.entity.SculkHarmoner>> SCULK_HARMONER = ENTITIES.registerEntityType(
            "sculk_harmoner", com.thesift.entity.SculkHarmoner::new, MobCategory.MISC,
            b -> b.sized(0.55F, 0.9F).eyeHeight(0.75F).clientTrackingRange(10).noSummon());
    // the Conductor's three great players, and their young
    public static final DeferredHolder<EntityType<?>, EntityType<Thumper>> THUMPER = ENTITIES.registerEntityType("thumper", Thumper::new,
            MobCategory.MONSTER, b -> b.sized(3.0F, 2.9F).eyeHeight(2.0F).clientTrackingRange(16));
    public static final DeferredHolder<EntityType<?>, EntityType<Strummer>> STRUMMER = ENTITIES.registerEntityType("strummer", Strummer::new,
            MobCategory.MONSTER, b -> b.sized(2.6F, 2.8F).eyeHeight(2.4F).clientTrackingRange(16));
    public static final DeferredHolder<EntityType<?>, EntityType<Strumling>> STRUMLING = ENTITIES.registerEntityType("strumling", Strumling::new,
            MobCategory.MONSTER, b -> b.sized(1.3F, 0.8F).eyeHeight(0.55F).clientTrackingRange(10)); // Weaver: a full-size Sculk Spider
    public static final DeferredHolder<EntityType<?>, EntityType<com.thesift.entity.boss.SculkParasite>> SCULK_PARASITE = ENTITIES.registerEntityType(
            "sculk_parasite", com.thesift.entity.boss.SculkParasite::new, MobCategory.MONSTER,
            b -> b.sized(0.9F, 0.4F).eyeHeight(0.3F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<com.thesift.entity.boss.ConductorMask>> CONDUCTOR_MASK = ENTITIES.registerEntityType(
            "conductor_mask", com.thesift.entity.boss.ConductorMask::new, MobCategory.MISC, b -> b.sized(1.2F, 2.4F).clientTrackingRange(16).fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<WebShot>> WEB_SHOT = ENTITIES.registerEntityType("web_shot", WebShot::new,
            MobCategory.MISC, b -> b.sized(0.4F, 0.4F).clientTrackingRange(6).updateInterval(5));
    public static final DeferredHolder<EntityType<?>, EntityType<GlowballEntity>> GLOWBALL = ENTITIES.registerEntityType("glowball",
            GlowballEntity::new, MobCategory.MISC, b -> b.sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10));

    // ---- the wild creatures
    public static final DeferredHolder<EntityType<?>, EntityType<com.thesift.entity.Stomper>> STOMPER = ENTITIES.registerEntityType("stomper",
            com.thesift.entity.Stomper::new, MobCategory.CREATURE, b -> b.sized(2.3F, 2.3F).eyeHeight(1.5F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<com.thesift.entity.FanfareEel>> FANFARE_EEL = ENTITIES.registerEntityType(
            "fanfare_eel", com.thesift.entity.FanfareEel::new, MobCategory.WATER_CREATURE,
            b -> b.sized(0.7F, 0.5F).eyeHeight(0.3F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<com.thesift.entity.KazooFish>> KAZOO_FISH = ENTITIES.registerEntityType(
            "kazoo_fish", com.thesift.entity.KazooFish::new, MobCategory.WATER_AMBIENT,
            b -> b.sized(0.45F, 0.4F).eyeHeight(0.25F).clientTrackingRange(4));
    public static final DeferredHolder<EntityType<?>, EntityType<com.thesift.entity.Tubafish>> TUBAFISH = ENTITIES.registerEntityType("tubafish",
            com.thesift.entity.Tubafish::new, MobCategory.WATER_CREATURE, b -> b.sized(1.0F, 1.0F).eyeHeight(0.55F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<com.thesift.entity.SkyWhale>> SKY_WHALE = ENTITIES.registerEntityType("sky_whale",
            com.thesift.entity.SkyWhale::new, MobCategory.CREATURE, b -> b.sized(4.0F, 3.0F).eyeHeight(1.8F).clientTrackingRange(16));
    public static final DeferredHolder<EntityType<?>, EntityType<com.thesift.entity.BubbleEntity>> BUBBLE = ENTITIES.registerEntityType("bubble",
            com.thesift.entity.BubbleEntity::new, MobCategory.MISC, b -> b.sized(0.4F, 0.4F).clientTrackingRange(4).updateInterval(10));

    private ModEntities() {
    }
}
