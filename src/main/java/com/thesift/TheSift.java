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
        com.thesift.registry.ModSeaSky.register(modBus); // sea & sky: the Gobbler, ocean/cloud worldgen, Tide Song calming
        com.thesift.registry.ModEchoer.register(modBus); // A2 Echoer: Soul Golems, Nibs, The Echoer device, the Echoer's Hut
        com.thesift.registry.ModSwifter.register(modBus); // A2 Swifter & White Forest: the Swifter, its dens, white lullwood, fluff
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
        com.thesift.item.SiftiteGear.register(); // H: Siftite knockback, swim speed and sound-proof armour
        com.thesift.entity.CreatureLife.register(modBus); // A1 creatures: creatures hear notes, Stomper riders stomp

        if (Boolean.getBoolean("thesift.smoketest")) {
            SmokeTest.registerIfEnabled(); // CI only
        }
        LOGGER.info("The Sift is dreaming...");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
