package com.thesift.registry;

import com.thesift.TheSift;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, TheSift.MODID);

    // ---- Bulb
    public static final DeferredHolder<SoundEvent, SoundEvent> BULB_AMBIENT = reg("entity.bulb.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> BULB_HURT = reg("entity.bulb.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> BULB_DEATH = reg("entity.bulb.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> BULB_HOP = reg("entity.bulb.hop");
    public static final DeferredHolder<SoundEvent, SoundEvent> BULB_SQUISH = reg("entity.bulb.squish");
    public static final DeferredHolder<SoundEvent, SoundEvent> BULB_HAPPY = reg("entity.bulb.happy");
    public static final DeferredHolder<SoundEvent, SoundEvent> BULB_LAY = reg("entity.bulb.lay");
    public static final DeferredHolder<SoundEvent, SoundEvent> BULB_EAT = reg("entity.bulb.eat");
    // A1 Bulb & Stomper: the Bulb's flower swap, the Sculk Bloom, the Stomper's garden shake
    public static final DeferredHolder<SoundEvent, SoundEvent> BULB_NIBBLE = reg("entity.bulb.nibble");
    public static final DeferredHolder<SoundEvent, SoundEvent> BULB_PLUCK = reg("entity.bulb.pluck");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCULK_BLOOM_PUFF = reg("block.sculk_bloom.puff");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCULK_BLOOM_CALM = reg("block.sculk_bloom.calm");
    public static final DeferredHolder<SoundEvent, SoundEvent> STOMPER_SHAKE = reg("entity.stomper.shake");
    // ---- Slumbler
    public static final DeferredHolder<SoundEvent, SoundEvent> SLUMBLER_AMBIENT = reg("entity.slumbler.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> SLUMBLER_HURT = reg("entity.slumbler.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SLUMBLER_DEATH = reg("entity.slumbler.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> SLUMBLER_YAWN = reg("entity.slumbler.yawn");
    public static final DeferredHolder<SoundEvent, SoundEvent> SLUMBLER_STEP = reg("entity.slumbler.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> SLUMBLER_BITE = reg("entity.slumbler.bite");
    // CR2: the Slumbler eats fish, spits Chrome, shakes itself dry and lays eggs; its eggs and tadpoles
    public static final DeferredHolder<SoundEvent, SoundEvent> SLUMBLER_EAT = reg("entity.slumbler.eat");
    public static final DeferredHolder<SoundEvent, SoundEvent> SLUMBLER_SPIT = reg("entity.slumbler.spit");
    public static final DeferredHolder<SoundEvent, SoundEvent> SLUMBLER_SHAKE = reg("entity.slumbler.shake");
    public static final DeferredHolder<SoundEvent, SoundEvent> SLUMBLER_LAY = reg("entity.slumbler.lay");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHROME_SPIT_SPLASH = reg("entity.chrome_spit.splash");
    public static final DeferredHolder<SoundEvent, SoundEvent> SLUMBLER_EGGS_HATCH = reg("entity.slumbler_eggs.hatch");
    public static final DeferredHolder<SoundEvent, SoundEvent> SLUMBLER_TADPOLE_BITE = reg("entity.slumbler_tadpole.bite");
    public static final DeferredHolder<SoundEvent, SoundEvent> SLUMBLER_TADPOLE_HURT = reg("entity.slumbler_tadpole.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SLUMBLER_TADPOLE_DEATH = reg("entity.slumbler_tadpole.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> SLUMBLER_TADPOLE_FLOP = reg("entity.slumbler_tadpole.flop");
    public static final DeferredHolder<SoundEvent, SoundEvent> SLUMBLER_TADPOLE_CRASH = reg("entity.slumbler_tadpole.crash");
    // ---- Sifter
    public static final DeferredHolder<SoundEvent, SoundEvent> SIFTER_AMBIENT = reg("entity.sifter.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIFTER_HURT = reg("entity.sifter.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIFTER_DEATH = reg("entity.sifter.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIFTER_CHOMP = reg("entity.sifter.chomp");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIFTER_LEAP = reg("entity.sifter.leap");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIFTER_STEP = reg("entity.sifter.step");
    // CR1: the living bell - its clapper's tink on the lip, and a full ring when it is struck hard
    public static final DeferredHolder<SoundEvent, SoundEvent> SIFTER_TINK = reg("entity.sifter.tink");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIFTER_RING = reg("entity.sifter.ring");
    // ---- Enchoer
    public static final DeferredHolder<SoundEvent, SoundEvent> ENCHOER_AMBIENT = reg("entity.enchoer.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENCHOER_HURT = reg("entity.enchoer.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENCHOER_DEATH = reg("entity.enchoer.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENCHOER_TRADE = reg("entity.enchoer.trade");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENCHOER_YES = reg("entity.enchoer.yes");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENCHOER_NO = reg("entity.enchoer.no");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENCHOER_HUM = reg("entity.enchoer.hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> HARMONER_AMBIENT = reg("entity.harmoner.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> HARMONER_SING = reg("entity.harmoner.sing");
    public static final DeferredHolder<SoundEvent, SoundEvent> HARMONER_HURT = reg("entity.harmoner.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> HARMONER_DEATH = reg("entity.harmoner.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> DICTATOR_AMBIENT = reg("entity.dictator.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> DICTATOR_HURT = reg("entity.dictator.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> DICTATOR_DEATH = reg("entity.dictator.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> DICTATOR_ROAR = reg("entity.dictator.roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> DICTATOR_BLINK = reg("entity.dictator.blink");
    public static final DeferredHolder<SoundEvent, SoundEvent> DICTATOR_SUMMON = reg("entity.dictator.summon");
    public static final DeferredHolder<SoundEvent, SoundEvent> DICTATOR_CRESCENDO = reg("entity.dictator.crescendo");
    public static final DeferredHolder<SoundEvent, SoundEvent> THUMPER_AMBIENT = reg("entity.thumper.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> THUMPER_HURT = reg("entity.thumper.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> THUMPER_DEATH = reg("entity.thumper.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> THUMPER_WINDUP = reg("entity.thumper.windup");
    public static final DeferredHolder<SoundEvent, SoundEvent> THUMPER_SLAM = reg("entity.thumper.slam");
    public static final DeferredHolder<SoundEvent, SoundEvent> THUMPER_ROAR = reg("entity.thumper.roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> THUMPER_DAZED = reg("entity.thumper.dazed");
    public static final DeferredHolder<SoundEvent, SoundEvent> THUMPER_CLANK = reg("entity.thumper.clank");
    public static final DeferredHolder<SoundEvent, SoundEvent> STRUMMER_AMBIENT = reg("entity.strummer.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> STRUMMER_HURT = reg("entity.strummer.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> STRUMMER_DEATH = reg("entity.strummer.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> STRUMMER_SLASH = reg("entity.strummer.slash");
    public static final DeferredHolder<SoundEvent, SoundEvent> STRUMMER_SPIT = reg("entity.strummer.spit");
    public static final DeferredHolder<SoundEvent, SoundEvent> STRUMMER_HISS = reg("entity.strummer.hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> STRUMMER_LAND = reg("entity.strummer.land");
    public static final DeferredHolder<SoundEvent, SoundEvent> STRUMMER_CHORD = reg("entity.strummer.chord");
    public static final DeferredHolder<SoundEvent, SoundEvent> STRUMMER_DRAW = reg("entity.strummer.draw");
    public static final DeferredHolder<SoundEvent, SoundEvent> STRUMMER_PLUCK = reg("entity.strummer.pluck");
    public static final DeferredHolder<SoundEvent, SoundEvent> STRUMLING_AMBIENT = reg("entity.strumling.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> STRUMLING_HURT = reg("entity.strumling.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> STRUMLING_DEATH = reg("entity.strumling.death");
    // ---- the Sculk Parasite
    public static final DeferredHolder<SoundEvent, SoundEvent> SCULK_PARASITE_AMBIENT = reg("entity.sculk_parasite.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCULK_PARASITE_HURT = reg("entity.sculk_parasite.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCULK_PARASITE_DEATH = reg("entity.sculk_parasite.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCULK_PARASITE_STEP = reg("entity.sculk_parasite.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCULK_PARASITE_HISS = reg("entity.sculk_parasite.hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCULK_PARASITE_BURST = reg("entity.sculk_parasite.burst");
    public static final DeferredHolder<SoundEvent, SoundEvent> CONDUCTOR_MASK_RISE = reg("entity.conductor_mask.rise");
    public static final DeferredHolder<SoundEvent, SoundEvent> CONDUCTOR_MASK_TRANSFORM = reg("entity.conductor_mask.transform");
    public static final DeferredHolder<SoundEvent, SoundEvent> CONGA_DRUM_BOOM = reg("item.conga_drum.boom");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRANE_FLUTE_PLAY = reg("item.crane_flute.play");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUITAR_STRUM = reg("item.guitar.strum");
    public static final DeferredHolder<SoundEvent, SoundEvent> STAFF_NOTE = reg("item.conductors_staff.note"); // CLEAN: was the Baton's note
    // ---- Music & blocks
    public static final DeferredHolder<SoundEvent, SoundEvent> DRUM_LOW = reg("block.sift_drum.low");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRUM_MID = reg("block.sift_drum.mid");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRUM_HIGH = reg("block.sift_drum.high");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRUM_BOOM = reg("block.sift_drum.boom");
    // F1: the Europhy Table (replaces the Euphory Altar) and Bauxite (tools/materials.py)
    public static final DeferredHolder<SoundEvent, SoundEvent> EUROPHY_CHARGE = reg("block.europhy_table.charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> EUROPHY_CRAFT = reg("block.europhy_table.craft");
    public static final DeferredHolder<SoundEvent, SoundEvent> EUROPHY_HUM = reg("block.europhy_table.hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> EUROPHY_NOTE = reg("block.europhy_table.note");
    public static final DeferredHolder<SoundEvent, SoundEvent> EUROPHY_FIZZLE = reg("block.europhy_table.fizzle");
    public static final DeferredHolder<SoundEvent, SoundEvent> BAUXITE_HISS = reg("block.bauxite.hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> PORTAL_AMBIENT = reg("block.sift_portal.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> PORTAL_ACTIVATE = reg("block.sift_portal.activate");
    public static final DeferredHolder<SoundEvent, SoundEvent> PORTAL_TRAVEL = reg("block.sift_portal.travel");
    public static final DeferredHolder<SoundEvent, SoundEvent> RHYTHM_CALL = reg("event.rhythm.call");
    public static final DeferredHolder<SoundEvent, SoundEvent> RHYTHM_GOOD = reg("event.rhythm.good");
    public static final DeferredHolder<SoundEvent, SoundEvent> RHYTHM_FAIL = reg("event.rhythm.fail");
    public static final DeferredHolder<SoundEvent, SoundEvent> RHYTHM_ROUND = reg("event.rhythm.round");
    /** The rising whoosh under a gate's awakening. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GATE_SWELL = reg("event.gate.swell");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHROME_AMBIENT = reg("block.chrome.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHROME_SPLASH = reg("block.chrome.splash");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHIME_RING = reg("block.soul_chime.ring");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRUMBLE = reg("block.crumbling_dreamstone.crumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> SLINGSHOT_SHOOT = reg("item.slingshot.shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> SLINGSHOT_PULL = reg("item.slingshot.pull");
    public static final DeferredHolder<SoundEvent, SoundEvent> GLOWBALL_BURST = reg("entity.glowball.burst");
    public static final DeferredHolder<SoundEvent, SoundEvent> WARDEN_CORE_PULSE = reg("item.warden_core.pulse");
    // ---- Stomper
    public static final DeferredHolder<SoundEvent, SoundEvent> STOMPER_AMBIENT = reg("entity.stomper.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> STOMPER_HURT = reg("entity.stomper.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> STOMPER_DEATH = reg("entity.stomper.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> STOMPER_STEP = reg("entity.stomper.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> STOMPER_TRUMPET = reg("entity.stomper.trumpet");
    public static final DeferredHolder<SoundEvent, SoundEvent> STOMPER_SPRAY = reg("entity.stomper.spray");
    public static final DeferredHolder<SoundEvent, SoundEvent> STOMPER_DRINK = reg("entity.stomper.drink");
    public static final DeferredHolder<SoundEvent, SoundEvent> STOMPER_STOMP = reg("entity.stomper.stomp");
    public static final DeferredHolder<SoundEvent, SoundEvent> STOMPER_PUFF = reg("entity.stomper.puff");
    public static final DeferredHolder<SoundEvent, SoundEvent> STOMPER_HAPPY = reg("entity.stomper.happy");
    public static final DeferredHolder<SoundEvent, SoundEvent> STOMPER_LAY = reg("entity.stomper.lay");
    public static final DeferredHolder<SoundEvent, SoundEvent> STOMPER_HATCH = reg("entity.stomper.hatch");
    // ---- music fish
    public static final DeferredHolder<SoundEvent, SoundEvent> FANFARE_EEL_AMBIENT = reg("entity.fanfare_eel.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> FANFARE_EEL_BLAST = reg("entity.fanfare_eel.blast");
    public static final DeferredHolder<SoundEvent, SoundEvent> FANFARE_EEL_HURT = reg("entity.fanfare_eel.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> FANFARE_EEL_DEATH = reg("entity.fanfare_eel.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> FANFARE_EEL_FLOP = reg("entity.fanfare_eel.flop");
    public static final DeferredHolder<SoundEvent, SoundEvent> KAZOO_FISH_AMBIENT = reg("entity.kazoo_fish.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> KAZOO_FISH_HURT = reg("entity.kazoo_fish.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> KAZOO_FISH_DEATH = reg("entity.kazoo_fish.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> KAZOO_FISH_FLOP = reg("entity.kazoo_fish.flop");
    // ---- Sky Whale
    public static final DeferredHolder<SoundEvent, SoundEvent> SKY_WHALE_AMBIENT = reg("entity.sky_whale.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> SKY_WHALE_SONG = reg("entity.sky_whale.song");
    public static final DeferredHolder<SoundEvent, SoundEvent> SKY_WHALE_MOO = reg("entity.sky_whale.moo");
    public static final DeferredHolder<SoundEvent, SoundEvent> SKY_WHALE_HURT = reg("entity.sky_whale.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SKY_WHALE_DEATH = reg("entity.sky_whale.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> SKY_WHALE_SPIT = reg("entity.sky_whale.spit");
    public static final DeferredHolder<SoundEvent, SoundEvent> SKY_WHALE_FLAP = reg("entity.sky_whale.flap");
    // ---- Ambience
    public static final DeferredHolder<SoundEvent, SoundEvent> AMBIENT_SIFT_LOOP = reg("ambient.sift.loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMBIENT_SIFT_ADDITIONS = reg("ambient.sift.additions");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMBIENT_SIFT_MOOD = reg("ambient.sift.mood");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMBIENT_DEEP_LOOP = reg("ambient.deep_sift.loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_SIFT = reg("music.sift");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_DEEP = reg("music.deep_sift");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_DISC_LULLABY = reg("music_disc.lullaby");
    // ---- C: Caravans and their music crystals
    public static final DeferredHolder<SoundEvent, SoundEvent> CARAVAN_AMBIENT = reg("entity.caravan.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> CARAVAN_HURT = reg("entity.caravan.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> CARAVAN_DEATH = reg("entity.caravan.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> CARAVAN_STEP = reg("entity.caravan.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> CARAVAN_ALARM = reg("entity.caravan.alarm");
    public static final DeferredHolder<SoundEvent, SoundEvent> CARAVAN_BUILD = reg("entity.caravan.build");
    // CR2: the territorial warning and the eggs a Caravan lays in an ore socket
    public static final DeferredHolder<SoundEvent, SoundEvent> CARAVAN_WARN = reg("entity.caravan.warn");
    public static final DeferredHolder<SoundEvent, SoundEvent> CARAVAN_LAY_EGGS = reg("entity.caravan.lay_eggs");
    // CR2: the Caravan Queen, the gems she spits and the larvae that hatch from egg-laden ore
    public static final DeferredHolder<SoundEvent, SoundEvent> CARAVAN_QUEEN_AMBIENT = reg("entity.caravan_queen.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> CARAVAN_QUEEN_HURT = reg("entity.caravan_queen.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> CARAVAN_QUEEN_DEATH = reg("entity.caravan_queen.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> CARAVAN_QUEEN_STEP = reg("entity.caravan_queen.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> CARAVAN_QUEEN_SPIT = reg("entity.caravan_queen.spit");
    public static final DeferredHolder<SoundEvent, SoundEvent> CARAVAN_QUEEN_SWAT = reg("entity.caravan_queen.swat");
    public static final DeferredHolder<SoundEvent, SoundEvent> CARAVAN_QUEEN_FEED = reg("entity.caravan_queen.feed");
    public static final DeferredHolder<SoundEvent, SoundEvent> CARAVAN_QUEEN_SETTLE = reg("entity.caravan_queen.settle");
    public static final DeferredHolder<SoundEvent, SoundEvent> CARAVAN_QUEEN_ROAR = reg("entity.caravan_queen.roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> CARAVAN_QUEEN_BURST = reg("entity.caravan_queen.burst");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPAT_GEM_HIT = reg("entity.spat_gem.hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> CARAVAN_LARVA_AMBIENT = reg("entity.caravan_larva.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> CARAVAN_LARVA_HURT = reg("entity.caravan_larva.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> CARAVAN_LARVA_DEATH = reg("entity.caravan_larva.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> CARAVAN_LARVA_STEP = reg("entity.caravan_larva.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> CARAVAN_LARVA_EMERGE = reg("entity.caravan_larva.emerge");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_CRYSTAL_CHIME = reg("block.music_crystal.chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_CRYSTAL_SHATTER = reg("block.music_crystal.shatter");

    // ---- sea & sky (F + W): the Gobbler, and the music and ambience of the three sea and sky biomes
    public static final DeferredHolder<SoundEvent, SoundEvent> GOBBLER_AMBIENT = reg("entity.gobbler.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOBBLER_HURT = reg("entity.gobbler.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOBBLER_DEATH = reg("entity.gobbler.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOBBLER_FLOP = reg("entity.gobbler.flop");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOBBLER_SNIFF = reg("entity.gobbler.sniff");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOBBLER_LUNGE = reg("entity.gobbler.lunge");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOBBLER_GULP = reg("entity.gobbler.gulp");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOBBLER_SPIT = reg("entity.gobbler.spit");
    public static final DeferredHolder<SoundEvent, SoundEvent> GOBBLER_CALM = reg("entity.gobbler.calm");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_KELP = reg("music.magic_kelp_forest");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_DEEP_OCEAN = reg("music.deep_dark_ocean");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_SOUND_GARDEN = reg("music.sound_garden");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMBIENT_KELP_LOOP = reg("ambient.magic_kelp_forest.loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMBIENT_KELP_ADDITIONS = reg("ambient.magic_kelp_forest.additions");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMBIENT_DEEP_OCEAN_LOOP = reg("ambient.deep_dark_ocean.loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMBIENT_DEEP_OCEAN_ADDITIONS = reg("ambient.deep_dark_ocean.additions");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMBIENT_DEEP_OCEAN_MOOD = reg("ambient.deep_dark_ocean.mood");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMBIENT_GARDEN_LOOP = reg("ambient.sound_garden.loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMBIENT_GARDEN_ADDITIONS = reg("ambient.sound_garden.additions");
    // ---- end sea & sky

    // ---- sound types
    public static final DeferredSoundType DREAMSTONE = new DeferredSoundType(1.0F, 1.1F,
            () -> net.minecraft.sounds.SoundEvents.DEEPSLATE_BRICKS_BREAK, () -> net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_STEP,
            () -> net.minecraft.sounds.SoundEvents.DEEPSLATE_BRICKS_PLACE, () -> net.minecraft.sounds.SoundEvents.DEEPSLATE_BRICKS_HIT,
            () -> net.minecraft.sounds.SoundEvents.DEEPSLATE_BRICKS_FALL);

    public static final ResourceKey<JukeboxSong> LULLABY_SONG = ResourceKey.create(Registries.JUKEBOX_SONG, TheSift.id("lullaby"));

    private static DeferredHolder<SoundEvent, SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(TheSift.id(name)));
    }

    public static Holder<SoundEvent> holder(DeferredHolder<SoundEvent, SoundEvent> h) {
        return h;
    }

    private ModSounds() {}
}
