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
