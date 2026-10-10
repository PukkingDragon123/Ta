package com.thesift.registry;

import com.thesift.TheSift;
import com.thesift.block.entity.SculkSummonerBlockEntity;
import com.thesift.entity.mansion.Bard;
import com.thesift.entity.mansion.Hornblower;
import com.thesift.music.Instrument;
import com.thesift.music.SongEvents;
import com.thesift.music.band.BandRegistry;
import com.thesift.music.band.BandVoice;
import com.thesift.worldgen.MansionVaultPiece;
import com.thesift.worldgen.MansionVaultStructure;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * MANSION: the Woodland Mansion's secret and the new way into the Sift.
 *
 * <ul>
 *   <li>the Hornblower and the Bard, two new Pillager types that live in mansions and join raids (the vanilla illager
 *   model and the vanilla pillager's texture, re-dressed - see client/MansionClient);</li>
 *   <li>the mansion's secret room: the structure {@code thesift:mansion_vault}, which shares the mansions' own placement so
 *   every mansion gets one (worldgen/MansionVaultStructure, MansionVaultPiece);</li>
 *   <li>the Sculk Summoner's block entity: a Warden Core in a Sculk Catalyst listens for the Sift Symphony and wakes the gate
 *   (block/entity/SculkSummonerBlockEntity);</li>
 *   <li>the giant horn, the illagers' and the summoner's sounds.</li>
 * </ul>
 * Items and blocks are generated from tools/mansion.py (spec), like the rest of the mod's.
 */
public final class ModMansion {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(TheSift.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, TheSift.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TheSift.MODID);
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, TheSift.MODID);
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, TheSift.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<Hornblower>> HORNBLOWER = ENTITIES.registerEntityType("hornblower", Hornblower::new,
            MobCategory.MONSTER, b -> b.sized(0.6F, 1.95F).eyeHeight(1.6F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<Bard>> BARD = ENTITIES.registerEntityType("bard", Bard::new,
            MobCategory.MONSTER, b -> b.sized(0.6F, 1.95F).eyeHeight(1.6F).clientTrackingRange(8));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SculkSummonerBlockEntity>> SCULK_SUMMONER = BLOCK_ENTITIES.register(
            "sculk_summoner", () -> new BlockEntityType<>(SculkSummonerBlockEntity::new, ModBlocks.SCULK_SUMMONER.get()));

    public static final DeferredHolder<StructureType<?>, StructureType<MansionVaultStructure>> MANSION_VAULT = STRUCTURE_TYPES.register(
            "mansion_vault", ModMansion::vaultType);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> MANSION_VAULT_PIECE = STRUCTURE_PIECES.register(
            "mansion_vault", ModMansion::vaultPiece);
    /** The vault's chest (always the Sift Symphony) and its barrels. */
    public static final ResourceKey<LootTable> VAULT_CHEST = ResourceKey.create(Registries.LOOT_TABLE, TheSift.id("chests/mansion_vault"));
    public static final ResourceKey<LootTable> VAULT_BARREL = ResourceKey.create(Registries.LOOT_TABLE, TheSift.id("chests/mansion_vault_barrel"));

    public static final DeferredHolder<SoundEvent, SoundEvent> HORNBLOWER_AMBIENT = reg("entity.hornblower.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> HORNBLOWER_HURT = reg("entity.hornblower.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> HORNBLOWER_DEATH = reg("entity.hornblower.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> HORNBLOWER_WINDUP = reg("entity.hornblower.windup");
    public static final DeferredHolder<SoundEvent, SoundEvent> BARD_AMBIENT = reg("entity.bard.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> BARD_HURT = reg("entity.bard.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> BARD_DEATH = reg("entity.bard.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> BARD_HEAL = reg("entity.bard.heal");
    public static final DeferredHolder<SoundEvent, SoundEvent> HORN_BLAST = reg("item.giant_goat_horn.blast");
    public static final DeferredHolder<SoundEvent, SoundEvent> HORN_GUST = reg("item.giant_goat_horn.gust");
    public static final DeferredHolder<SoundEvent, SoundEvent> SUMMONER_ARM = reg("block.sculk_summoner.arm");
    public static final DeferredHolder<SoundEvent, SoundEvent> SECRET_OPEN = reg("block.secret_bookshelf.open");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOOSE_PULL = reg("block.loose_bookshelf.pull");

    /**
     * Raids bring Hornblowers and Bards along (META-INF/enumextensions.json adds the two raider types). Held in a class of
     * its own: the enum extension reads these fields while Raid.RaiderType loads, before the registries exist, so the
     * entity types are only looked up when a raid spawns a wave.
     */
    public static final class RaidParams {
        public static final EnumProxy<Raid.RaiderType> HORNBLOWER = new EnumProxy<>(Raid.RaiderType.class,
                (Supplier<EntityType<? extends Raider>>) () -> ModMansion.HORNBLOWER.get(), new int[]{0, 0, 1, 0, 1, 1, 1, 2});
        public static final EnumProxy<Raid.RaiderType> BARD = new EnumProxy<>(Raid.RaiderType.class,
                (Supplier<EntityType<? extends Raider>>) () -> ModMansion.BARD.get(), new int[]{0, 0, 0, 1, 0, 1, 1, 1});

        private RaidParams() {
        }
    }

    private ModMansion() {
    }

    private static StructureType<MansionVaultStructure> vaultType() {
        return () -> MansionVaultStructure.CODEC;
    }

    private static StructurePieceType vaultPiece() {
        return (StructurePieceType.ContextlessType) MansionVaultPiece::new;
    }

    private static DeferredHolder<SoundEvent, SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(TheSift.id(name)));
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        SOUNDS.register(bus);
        BLOCK_ENTITIES.register(bus);
        STRUCTURE_TYPES.register(bus);
        STRUCTURE_PIECES.register(bus);
        bus.addListener(ModMansion::attributes);
        // the summoner listens to every note played near it, and to the songs
        SongEvents.listenNotes(SculkSummonerBlockEntity::hearNote);
        SongEvents.listenSongs(SculkSummonerBlockEntity::hearSong);
        // their voices if a band plays near them: the horn's deep bellow, the Bard's guitar (both are hostile: they jeer)
        BandRegistry.voice(HORNBLOWER, SoundEvents.NOTE_BLOCK_DIDGERIDOO).transpose(-12).every(2).volume(1.4F)
                .families(Instrument.Family.FLUTE, Instrument.Family.DRUM).temper(BandVoice.Temper.HOSTILE).instrument("giant_horn")
                .colour(0xC9A043).register();
        BandRegistry.voice(BARD, SoundEvents.NOTE_BLOCK_GUITAR).volume(1.0F)
                .families(Instrument.Family.STRINGS, Instrument.Family.CHIMES).temper(BandVoice.Temper.HOSTILE).instrument("mansion_guitar")
                .colour(0x2C827F).register();
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(HORNBLOWER.get(), Hornblower.createAttributes().build());
        event.put(BARD.get(), Bard.createAttributes().build());
    }
}
