package com.thesift.registry;

import com.mojang.serialization.Codec;
import com.thesift.TheSift;
import com.thesift.block.entity.EchoerDeviceBlockEntity;
import com.thesift.block.entity.EchoerHutHeartBlockEntity;
import com.thesift.entity.Enchoer;
import com.thesift.entity.Nib;
import com.thesift.entity.SoulGolem;
import com.thesift.music.Song;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * A2 Echoer: everything around the Echoer's hearth - the Soul Golems who live there, the Nibs of the
 * flower meadows, The Echoer (a mining-beam device), the hearthstone, their sounds, tags and loot.
 * The Echoer itself keeps its old registration ({@code thesift:enchoer} in {@link ModEntities}).
 */
public final class ModEchoer {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(TheSift.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TheSift.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, TheSift.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<SoulGolem>> SOUL_GOLEM = ENTITIES.registerEntityType("soul_golem", SoulGolem::new,
            MobCategory.CREATURE, b -> b.sized(0.6F, 0.75F).eyeHeight(0.55F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Nib>> NIB = ENTITIES.registerEntityType("nib", Nib::new,
            MobCategory.AMBIENT, b -> b.sized(0.35F, 0.3F).eyeHeight(0.15F).clientTrackingRange(8));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EchoerDeviceBlockEntity>> ECHOER_DEVICE_BE = BLOCK_ENTITIES.register(
            "echoer_device", () -> new BlockEntityType<>(EchoerDeviceBlockEntity::new, ModBlocks.ECHOER_DEVICE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EchoerHutHeartBlockEntity>> ECHOER_HUT_HEART_BE = BLOCK_ENTITIES.register(
            "echoer_hut_heart", () -> new BlockEntityType<>(EchoerHutHeartBlockEntity::new, ModBlocks.ECHOER_HUT_HEART.get()));

    public static final DeferredHolder<SoundEvent, SoundEvent> GOLEM_AMBIENT = reg("entity.soul_golem.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOLEM_HURT = reg("entity.soul_golem.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOLEM_DEATH = reg("entity.soul_golem.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOLEM_STEP = reg("entity.soul_golem.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOLEM_DIG = reg("entity.soul_golem.dig");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOLEM_FIND = reg("entity.soul_golem.find");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOLEM_SLUMP = reg("entity.soul_golem.slump");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOLEM_RECHARGE = reg("entity.soul_golem.recharge");
    public static final DeferredHolder<SoundEvent, SoundEvent> NIB_AMBIENT = reg("entity.nib.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> NIB_HURT = reg("entity.nib.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> NIB_TRANSFORM = reg("entity.nib.transform");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEVICE_CHARGE = reg("block.echoer_device.charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEVICE_FIRE = reg("block.echoer_device.fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEVICE_FIZZLE = reg("block.echoer_device.fizzle");
    // RR: the Echoer Drill hears a beat, and bites into the rock
    public static final DeferredHolder<SoundEvent, SoundEvent> DEVICE_BEAT = reg("block.echoer_device.beat");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEVICE_BITE = reg("block.echoer_device.bite");
    /** CR1 Echoer: its echolocation pings (a sonar click or a chime blip from its speakers). */
    public static final DeferredHolder<SoundEvent, SoundEvent> ENCHOER_CHIMES = reg("entity.enchoer.chimes");
    /** M3 Echoer (a deer spirit): its endless song (heard 48 blocks off), the glint under each hoof as it skips on the air, a gift rising. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ENCHOER_SING = reg("entity.enchoer.sing");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENCHOER_STEP = reg("entity.enchoer.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENCHOER_GIFT = reg("entity.enchoer.gift");

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, TheSift.MODID);
    /** M3: the game time from which a player may be given the Echoers' next gift (one every Enchoer.GIFT_COOLDOWN ticks; kept through death). */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> NEXT_GIFT = ATTACHMENTS.register("echoer_next_gift",
            () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG.fieldOf("time")).copyOnDeath().build());
    public static final ResourceKey<LootTable> GOLEM_DIG_LOOT = loot("gameplay/soul_golem_dig");
    public static final ResourceKey<LootTable> NIB_TRANSFORM_LOOT = loot("gameplay/nib_transform");

    private ModEchoer() {
    }

    /** M3: the gifts an Echoer gives for a song ({@code thesift:gameplay/echoer_gift/<song>}; each may add a rare one). */
    public static ResourceKey<LootTable> giftTable(Song song) {
        return loot("gameplay/echoer_gift/" + song.id());
    }

    private static DeferredHolder<SoundEvent, SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(TheSift.id(name)));
    }

    private static ResourceKey<LootTable> loot(String path) {
        return ResourceKey.create(Registries.LOOT_TABLE, TheSift.id(path));
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        BLOCK_ENTITIES.register(bus);
        SOUNDS.register(bus);
        ATTACHMENTS.register(bus);
        bus.addListener(ModEchoer::attributes);
        bus.addListener(ModEchoer::spawnPlacements);
        // the songs: any song (the Echoer's gifts), the Golem Hymn and every stray note (Soul Golems), the Nibs' song
        Enchoer.listen();
        SoulGolem.listen();
        Nib.listen();
        EchoerDeviceBlockEntity.listen(); // RR: the Echoer Drill hears notes and note blocks as beats
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(SOUL_GOLEM.get(), SoulGolem.createAttributes().build());
        event.put(NIB.get(), Nib.createAttributes().build());
    }

    private static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(NIB.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> level.getBlockState(pos.below()).is(ModTags.Blocks.SIFT_PLANTABLE)
                        && Mob.checkMobSpawnRules(type, level, reason, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(SOUL_GOLEM.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
