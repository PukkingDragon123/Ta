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
    // ---- Slumbler
    public static final DeferredHolder<SoundEvent, SoundEvent> SLUMBLER_AMBIENT = reg("entity.slumbler.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> SLUMBLER_HURT = reg("entity.slumbler.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SLUMBLER_DEATH = reg("entity.slumbler.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> SLUMBLER_YAWN = reg("entity.slumbler.yawn");
    public static final DeferredHolder<SoundEvent, SoundEvent> SLUMBLER_STEP = reg("entity.slumbler.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> SLUMBLER_BITE = reg("entity.slumbler.bite");
    // ---- Sifter
    public static final DeferredHolder<SoundEvent, SoundEvent> SIFTER_AMBIENT = reg("entity.sifter.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIFTER_HURT = reg("entity.sifter.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIFTER_DEATH = reg("entity.sifter.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIFTER_CHOMP = reg("entity.sifter.chomp");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIFTER_LEAP = reg("entity.sifter.leap");
    public static final DeferredHolder<SoundEvent, SoundEvent> SIFTER_STEP = reg("entity.sifter.step");
    // ---- Enchoer
    public static final DeferredHolder<SoundEvent, SoundEvent> ENCHOER_AMBIENT = reg("entity.enchoer.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENCHOER_HURT = reg("entity.enchoer.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENCHOER_DEATH = reg("entity.enchoer.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENCHOER_TRADE = reg("entity.enchoer.trade");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENCHOER_YES = reg("entity.enchoer.yes");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENCHOER_NO = reg("entity.enchoer.no");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENCHOER_HUM = reg("entity.enchoer.hum");
    // ---- Riveter
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
    public static final DeferredHolder<SoundEvent, SoundEvent> THUMPER_SPIN = reg("entity.thumper.spin");
    public static final DeferredHolder<SoundEvent, SoundEvent> THUMPER_DAZED = reg("entity.thumper.dazed");
    public static final DeferredHolder<SoundEvent, SoundEvent> THUMPER_CLANK = reg("entity.thumper.clank");
    public static final DeferredHolder<SoundEvent, SoundEvent> THUMPER_DRUM_HIT = reg("entity.thumper.drum_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> WHISTLER_AMBIENT = reg("entity.whistler.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> WHISTLER_HURT = reg("entity.whistler.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> WHISTLER_DEATH = reg("entity.whistler.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> WHISTLER_CHARGE = reg("entity.whistler.charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> WHISTLER_LOCK = reg("entity.whistler.lock");
    public static final DeferredHolder<SoundEvent, SoundEvent> WHISTLER_BREAK = reg("entity.whistler.break");
    public static final DeferredHolder<SoundEvent, SoundEvent> WHISTLER_SCREECH = reg("entity.whistler.screech");
    public static final DeferredHolder<SoundEvent, SoundEvent> WHISTLER_DIVE = reg("entity.whistler.dive");
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
    public static final DeferredHolder<SoundEvent, SoundEvent> WHISTLING_AMBIENT = reg("entity.whistling.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> WHISTLING_HURT = reg("entity.whistling.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> WHISTLING_DEATH = reg("entity.whistling.death");
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
    public static final DeferredHolder<SoundEvent, SoundEvent> BATON_NOTE = reg("item.conductors_baton.note");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIVETER_AMBIENT = reg("entity.riveter.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIVETER_HURT = reg("entity.riveter.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIVETER_DEATH = reg("entity.riveter.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIVETER_SCREAM = reg("entity.riveter.scream");
    // ---- Music & blocks
    public static final DeferredHolder<SoundEvent, SoundEvent> DRUM_LOW = reg("block.sift_drum.low");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRUM_MID = reg("block.sift_drum.mid");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRUM_HIGH = reg("block.sift_drum.high");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRUM_BOOM = reg("block.sift_drum.boom");
    public static final DeferredHolder<SoundEvent, SoundEvent> ALTAR_CHARGE = reg("block.euphory_altar.charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> ALTAR_ENCHANT = reg("block.euphory_altar.enchant");
    public static final DeferredHolder<SoundEvent, SoundEvent> ALTAR_HUM = reg("block.euphory_altar.hum");
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
    public static final DeferredHolder<SoundEvent, SoundEvent> HARMONY_TONE = reg("block.harmony_stone.tone");
    public static final DeferredHolder<SoundEvent, SoundEvent> HARMONY_UNLOCK = reg("block.harmony_seal.unlock");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHIME_RING = reg("block.soul_chime.ring");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHOIR_LILY_SING = reg("block.choir_lily.sing");
    public static final DeferredHolder<SoundEvent, SoundEvent> SNARE_TRIGGER = reg("block.dream_snare.trigger");
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
    public static final DeferredHolder<SoundEvent, SoundEvent> TUBAFISH_AMBIENT = reg("entity.tubafish.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> TUBAFISH_PUFF = reg("entity.tubafish.puff");
    public static final DeferredHolder<SoundEvent, SoundEvent> TUBAFISH_DEFLATE = reg("entity.tubafish.deflate");
    public static final DeferredHolder<SoundEvent, SoundEvent> TUBAFISH_HURT = reg("entity.tubafish.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> TUBAFISH_DEATH = reg("entity.tubafish.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> TUBAFISH_FLOP = reg("entity.tubafish.flop");
    public static final DeferredHolder<SoundEvent, SoundEvent> BUBBLE_GUN_SHOOT = reg("item.bubble_gun.shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> BUBBLE_POP = reg("entity.bubble.pop");
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
