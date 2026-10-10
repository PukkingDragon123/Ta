package com.thesift;

import com.mojang.logging.LogUtils;
import com.thesift.dev.SmokeTest;
import com.thesift.event.GameBusEvents;
import com.thesift.event.ModBusEvents;
import com.thesift.registry.ModBlockEntities;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModCreativeTabs;
import com.thesift.registry.ModEffects;
import com.thesift.registry.ModEntities;
import com.thesift.registry.ModFeatures;
import com.thesift.registry.ModFluids;
import com.thesift.registry.ModItems;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * The Sift - a dreaming dimension of soul, music and healing, the gentle opposite of the Nether.
 */
@Mod(TheSift.MODID)
public class TheSift {
    public static final String MODID = "thesift";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TheSift(IEventBus modBus, ModContainer container) {
        ModSounds.SOUNDS.register(modBus);
        ModEffects.EFFECTS.register(modBus);
        ModFluids.FLUID_TYPES.register(modBus);
        ModFluids.FLUIDS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        com.thesift.registry.ModSiege.register(modBus); // the Thumper's arena: ancient cannons, cannonballs
        com.thesift.registry.ModCaravans.register(modBus); // C: Caravans, music crystals, prism armour, song hooks
        com.thesift.registry.ModSeaSky.register(modBus); // sea & sky: the Gobbler, ocean/cloud worldgen, Lullaby calming
        com.thesift.registry.ModEchoer.register(modBus); // A2 Echoer: Soul Golems, Nibs, The Echoer device, the Echoer's hearth
        com.thesift.registry.ModSwifter.register(modBus); // A2 Swifter & White Forest: the Swifter, its dens, white lullwood, fluff
        com.thesift.registry.ModChrome.register(modBus); // A3 Chrome: Rainbow Daze, Chrome particles, Chime Sand reaction, note bursts
        com.thesift.registry.ModCaveCreatures.register(modBus); // A4 cave creatures: the Jailer and its cell, Sculklings
        com.thesift.registry.ModDunes.register(modBus); // P4-DESERT: Kerkorer, Jaberora, Reservoirs, Grubs, the Kerkorer Cloak
        com.thesift.registry.ModSiftSniffer.register(modBus); // E1 Sniffer & rot: the Sift Sniffer, its fluffy egg, the rot outside the Sift
        com.thesift.registry.ModGateFx.register(modBus); // B1 Portal & sky FX: the portal's sky-window block entity
        com.thesift.registry.ModSculkSwamp.register(modBus); // W1 World & terrain: Sculk Water, biome blending, relic caches, swamp crawlers
        com.thesift.registry.ModRings.register(modBus); // CR1: the Sifter's bell rings and the Echoer's echolocation pings
        com.thesift.registry.ModEurophy.register(modBus); // F1 Materials & Europhy Table: the table's block entity, menu and music
        com.thesift.registry.ModSculkSea.register(modBus); // CR3 Fish & Coral Organs: Sculk Fish, Sculk Coral Organs and their hooks, fish buckets
        com.thesift.registry.ModBandTable.register(modBus); // F2 Band Table: the Music Band Table, its score, the Sift enchantments' effects
        com.thesift.registry.ModSlumbler.register(modBus); // CR2: the Slumbler's Chrome spit, its eggs and its tadpoles
        com.thesift.registry.ModKnowledge.register(modBus); // F3 Knowledge & lore: discoveries, lore books/scrolls, the Mini Creator's quests
        com.thesift.registry.ModCaves.register(modBus); // W-deep caves: the Sculk Grasper, cave scrub worldgen, acid particles
        com.thesift.registry.ModSeaReefs.register(modBus); // W-sea: Brass Coral Reef & Chrome Coral Ocean: Trumpet Coral and reef features, sounds
        com.thesift.world.sky.SkyIslands.register(modBus); // W-sky: Sky Islands features, the swinging rope, swing packets
        com.thesift.registry.ModWorldLand.register(modBus); // W-land: Rocky Dunes formations + Tuning Cactus, White Forest trees + Rainbow Snow
        com.thesift.registry.ModCaveJungle.register(modBus); // P4 Cave Jungle: its creatures, sounds, spawn rules, band voices, foods
        ModParticles.PARTICLES.register(modBus);
        ModFeatures.FEATURE_TYPES.register(modBus);
        ModCreativeTabs.TABS.register(modBus);

        modBus.addListener(ModBusEvents::registerAttributes);
        modBus.addListener(ModBusEvents::registerSpawnPlacements);
        modBus.addListener(ModBusEvents::addBlockEntityBlocks);
        modBus.addListener(ModCreativeTabs::addToVanillaTabs);

        NeoForge.EVENT_BUS.addListener(GameBusEvents::onNotePlayed);
        NeoForge.EVENT_BUS.addListener(GameBusEvents::onVanillaGameEvent);
        NeoForge.EVENT_BUS.addListener(GameBusEvents::onLeftClickBlock);
        NeoForge.EVENT_BUS.addListener(com.thesift.world.TemporaryBlocks::onLevelTick);
        com.thesift.music.SongTracker.init(); // songs (agent D): the song tracker + Lullaby/Whale effects
        com.thesift.music.band.Bands.register(modBus); // M2 band: creatures join your band, play along, follow; band registry
        com.thesift.music.InstrumentPlay.register(modBus); // INS free play: notes played in the world reach the server and the watchers
        com.thesift.registry.ModInstrumentFx.register(modBus); // INS free play: notes, rings and breath flying from instruments
        com.thesift.item.PrismGear.register(); // B4 gear: the Prism Sword reveals nearby monsters
        com.thesift.entity.CreatureLife.register(modBus); // A1 creatures: creatures hear notes, Stomper riders stomp
        com.thesift.entity.boss.BossStages.register(modBus); // B2 Thumper & cutscenes: boss stage cutscene payloads, viewers kept safe

        if (Boolean.getBoolean("thesift.smoketest")) {
            SmokeTest.registerIfEnabled(); // CI only
        }
        LOGGER.info("The Sift is dreaming...");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
