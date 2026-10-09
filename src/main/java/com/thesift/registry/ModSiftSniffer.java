package com.thesift.registry;

import com.mojang.serialization.Codec;
import com.thesift.TheSift;
import com.thesift.entity.SiftRot;
import com.thesift.entity.SiftSniffer;
import com.thesift.music.band.BandRegistry;
import com.thesift.music.band.BandVoice;
import com.thesift.music.Instrument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * E1 Sniffer &amp; rot: the Sift Sniffer, its fluffy egg's sounds, the digging loot, the band voice
 * (it trumpets through its nose) and the rot that creeps into Sift creatures outside the Sift.
 * The egg block and the spawn egg are generated into {@link ModBlocks} / {@link ModItems} from
 * tools/sift_sniffer.py.
 */
public final class ModSiftSniffer {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(TheSift.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, TheSift.MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, TheSift.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<SiftSniffer>> SIFT_SNIFFER = ENTITIES.registerEntityType("sift_sniffer",
            SiftSniffer::new, MobCategory.CREATURE, b -> b.sized(1.6F, 1.5F).eyeHeight(1.05F).clientTrackingRange(10));

    /** Seconds of rot a Sift creature has gathered outside the Sift (saved, and synced for the renderers). */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> ROT = ATTACHMENTS.register("rot",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT.fieldOf("seconds")).sync(ByteBufCodecs.VAR_INT).build());

    public static final ResourceKey<LootTable> DIGGING_LOOT = ResourceKey.create(Registries.LOOT_TABLE, TheSift.id("gameplay/sift_sniffer_digging"));

    public static final DeferredHolder<SoundEvent, SoundEvent> AMBIENT = reg("entity.sift_sniffer.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> HURT = reg("entity.sift_sniffer.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEATH = reg("entity.sift_sniffer.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> STEP = reg("entity.sift_sniffer.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> EAT = reg("entity.sift_sniffer.eat");
    public static final DeferredHolder<SoundEvent, SoundEvent> SNIFF = reg("entity.sift_sniffer.sniff");
    public static final DeferredHolder<SoundEvent, SoundEvent> DIG = reg("entity.sift_sniffer.dig");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIND = reg("entity.sift_sniffer.find");
    public static final DeferredHolder<SoundEvent, SoundEvent> TRUMPET = reg("entity.sift_sniffer.trumpet");
    public static final DeferredHolder<SoundEvent, SoundEvent> HAPPY = reg("entity.sift_sniffer.happy");
    public static final DeferredHolder<SoundEvent, SoundEvent> PREPARE_RAM = reg("entity.sift_sniffer.prepare_ram");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAM = reg("entity.sift_sniffer.ram");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOOM = reg("entity.sift_sniffer.bloom");
    public static final DeferredHolder<SoundEvent, SoundEvent> EGG_PLOP = reg("block.sift_sniffer_egg.plop");
    public static final DeferredHolder<SoundEvent, SoundEvent> EGG_CRACK = reg("block.sift_sniffer_egg.crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> EGG_HATCH = reg("block.sift_sniffer_egg.hatch");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROT_GROAN = reg("entity.sift_creature.rot");

    private ModSiftSniffer() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(TheSift.id(name)));
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        SOUNDS.register(bus);
        ATTACHMENTS.register(bus);
        bus.addListener(ModSiftSniffer::attributes);
        bus.addListener(ModSiftSniffer::spawnPlacements);
        NeoForge.EVENT_BUS.addListener(SiftRot::onEntityTick);
        // a big gentle brass voice: it trumpets every note through its nose
        BandRegistry.voice(SIFT_SNIFFER, SoundEvents.NOTE_BLOCK_TRUMPET).transpose(-5).volume(1.0F)
                .layer(SoundEvents.NOTE_BLOCK_DIDGERIDOO, 0.35F)
                .families(Instrument.Family.FLUTE, Instrument.Family.DRUM)
                .temper(BandVoice.Temper.SHY).instrument("nose_trumpet").colour(0xF6B8D2)
                .when(m -> !SiftRot.isRotten(m) && !((SiftSniffer) m).isAngry() && ((SiftSniffer) m).getState() == SiftSniffer.IDLE).register();
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(SIFT_SNIFFER.get(), SiftSniffer.createAttributes().build());
    }

    private static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(SIFT_SNIFFER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> (level.getBlockState(pos.below()).is(ModTags.Blocks.SIFT_PLANTABLE)
                        || net.minecraft.world.entity.EntitySpawnReason.isSpawner(reason))
                        && Mob.checkMobSpawnRules(type, level, reason, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
