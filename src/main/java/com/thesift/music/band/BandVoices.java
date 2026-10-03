package com.thesift.music.band;

import com.thesift.entity.Bulb;
import com.thesift.entity.Enchoer;
import com.thesift.entity.Harmoner;
import com.thesift.entity.Nib;
import com.thesift.entity.SkyWhale;
import com.thesift.entity.Slumbler;
import com.thesift.entity.SoulGolem;
import com.thesift.entity.Swifter;
import com.thesift.music.Instrument.Family;
import com.thesift.music.Song;
import com.thesift.registry.ModCaravans;
import com.thesift.registry.ModCaveCreatures;
import com.thesift.registry.ModEchoer;
import com.thesift.registry.ModEntities;
import com.thesift.registry.ModSeaSky;
import com.thesift.registry.ModSwifter;
import java.util.UUID;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/**
 * The Sift's own band: one voice for every creature that is not a boss. Tame and gentle creatures
 * join; the monsters of the Sift have voices too, but only ever answer from where they stand.
 *
 * <table>
 *   <caption>Who plays what</caption>
 *   <tr><th>Creature</th><th>Instrument</th><th>Called by</th><th>Temper</th></tr>
 *   <tr><td>Bulb</td><td>pling bells, an octave up</td><td>chimes, strings; Nibs' Song, Lullaby</td><td>eager</td></tr>
 *   <tr><td>Harmoner</td><td>piccolo (flute, an octave up)</td><td>flute, chimes</td><td>eager</td></tr>
 *   <tr><td>Nib</td><td>wisp bells (bell, an octave up)</td><td>flute, chimes</td><td>eager, flies</td></tr>
 *   <tr><td>Kazoo Fish</td><td>kazoo (bit)</td><td>flute; Tide Song</td><td>eager, swims</td></tr>
 *   <tr><td>Stomper</td><td>thunder drums (bass drum + bass, low)</td><td>drum; Golem Hymn</td><td>shy</td></tr>
 *   <tr><td>Slumbler</td><td>snore drone (didgeridoo, low, every second note)</td><td>strings; Lullaby</td><td>shy</td></tr>
 *   <tr><td>Echoer</td><td>wind chimes a fifth above, amethyst shimmer</td><td>chimes; Golem Hymn</td><td>shy</td></tr>
 *   <tr><td>Swifter</td><td>banjo yips</td><td>strings, drum</td><td>shy</td></tr>
 *   <tr><td>Tubafish</td><td>tuba (trumpet, low)</td><td>drum, flute; Tide Song</td><td>shy, swims</td></tr>
 *   <tr><td>Sky Whale</td><td>whale song (low flute + ghastly coo, every second note)</td><td>flute; Whale Song</td><td>shy, flies</td></tr>
 *   <tr><td>Soul Golem</td><td>soul vibraphone (iron xylophone)</td><td>drum; Golem Hymn</td><td>loyal (owned golems only)</td></tr>
 *   <tr><td>Sifter, Caravan, Jailer, Sculkling, Fanfare Eel, Gobbler</td><td>snare shell, crystal xylophone, jailer's bell, sculk clicks,
 *   fanfare horn, deep bass</td><td>drum; crystal; any; strings; flute; Tide Song</td><td>hostile: never join</td></tr>
 * </table>
 *
 * (The bosses and their summons play in the Conductor's orchestra,
 * not in yours.)
 */
final class BandVoices {
    private BandVoices() {
    }

    static void registerSiftCreatures() {
        // ---- eager: small, social, musical
        BandRegistry.voice(ModEntities.BULB, SoundEvents.NOTE_BLOCK_PLING).transpose(12).volume(0.8F)
                .families(Family.CHIMES, Family.STRINGS).songs(Song.NIB, Song.LULLABY)
                .instrument("pling_bells").colour(0x8FD7FF).when(m -> !((Bulb) m).isSleepingBulb()).register();
        BandRegistry.voice(ModEntities.HARMONER, SoundEvents.NOTE_BLOCK_FLUTE).transpose(12).volume(0.8F)
                .families(Family.FLUTE, Family.CHIMES)
                .instrument("piccolo").colour(0xFFD25A)
                .when(m -> !((Harmoner) m).isGuiding() && !((Harmoner) m).isRoosting())
                .bond(BandVoices::harmonerBond).register();
        BandRegistry.voice(ModEchoer.NIB, SoundEvents.NOTE_BLOCK_BELL).transpose(12).volume(0.6F)
                .layer(SoundEvents.AMETHYST_BLOCK_CHIME, 0.25F)
                .families(Family.FLUTE, Family.CHIMES)
                .movement(BandVoice.Movement.FLY).instrument("wisp_bells").colour(0xD9B8FF)
                .when(m -> ((Nib) m).getState() == Nib.FLYING).register();
        BandRegistry.voice(ModEntities.KAZOO_FISH, SoundEvents.NOTE_BLOCK_BIT).volume(0.7F)
                .families(Family.FLUTE).songs(Song.TIDE)
                .movement(BandVoice.Movement.SWIM).instrument("kazoo").colour(0xFF9A5A).register();

        // ---- shy: wild ones need two songs, tamed ones come at once
        BandRegistry.voice(ModEntities.STOMPER, SoundEvents.NOTE_BLOCK_BASEDRUM).transpose(-12).volume(1.2F)
                .layer(SoundEvents.NOTE_BLOCK_BASS, 0.7F)
                .families(Family.DRUM).songs(Song.GOLEM)
                .temper(BandVoice.Temper.SHY).instrument("thunder_drums").colour(0xC9A27A).register();
        BandRegistry.voice(ModEntities.SLUMBLER, SoundEvents.NOTE_BLOCK_DIDGERIDOO).transpose(-12).every(2).volume(1.1F)
                .families(Family.STRINGS).songs(Song.LULLABY)
                .temper(BandVoice.Temper.SHY).instrument("snore_drone").colour(0x9CC7A0)
                .when(m -> !((Slumbler) m).isSlumbering()).register();
        BandRegistry.voice(ModEntities.ENCHOER, SoundEvents.NOTE_BLOCK_CHIME).transpose(7).volume(0.9F)
                .layer(SoundEvents.AMETHYST_BLOCK_CHIME, 0.35F)
                .families(Family.CHIMES).songs(Song.GOLEM)
                .temper(BandVoice.Temper.SHY).instrument("echo_chimes").colour(0xBFF6FF)
                .when(m -> ((Enchoer) m).getState() == Enchoer.IDLE).register();
        BandRegistry.voice(ModSwifter.SWIFTER, SoundEvents.NOTE_BLOCK_BANJO).volume(0.9F)
                .families(Family.STRINGS, Family.DRUM)
                .temper(BandVoice.Temper.SHY).instrument("banjo").colour(0xF2F2F2)
                .when(m -> !((Swifter) m).isNapping() && !((Swifter) m).isAngry()).register();
        BandRegistry.voice(ModEntities.TUBAFISH, SoundEvents.NOTE_BLOCK_TRUMPET).transpose(-12).volume(1.1F)
                .layer(SoundEvents.NOTE_BLOCK_BASS, 0.4F)
                .families(Family.DRUM, Family.FLUTE).songs(Song.TIDE)
                .temper(BandVoice.Temper.SHY).movement(BandVoice.Movement.SWIM).instrument("tuba").colour(0x5AA9E6).register();
        BandRegistry.voice(ModEntities.SKY_WHALE, SoundEvents.NOTE_BLOCK_FLUTE).transpose(-12).every(2).volume(3.0F)
                .layer(SoundEvents.HAPPY_GHAST_AMBIENT, 0.5F)
                .families(Family.FLUTE).songs(Song.WHALE)
                .temper(BandVoice.Temper.SHY).movement(BandVoice.Movement.FLY).instrument("whale_song").colour(0x7FB2FF)
                .when(m -> !((SkyWhale) m).isAnswering()).register();

        // ---- loyal: only their owner's
        BandRegistry.voice(ModEchoer.SOUL_GOLEM, SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE).volume(0.9F)
                .families(Family.DRUM).songs(Song.GOLEM)
                .temper(BandVoice.Temper.LOYAL).instrument("soul_vibraphone").colour(0x6FE3D2)
                .when(m -> !((SoulGolem) m).isSlumped())
                .bond((m, p) -> ((SoulGolem) m).isOwned() ? BandVoice.Bond.OWN : BandVoice.Bond.WILD).register();

        // ---- hostile: a voice, never a seat
        BandRegistry.voice(ModEntities.SIFTER, SoundEvents.NOTE_BLOCK_SNARE)
                .families(Family.DRUM).temper(BandVoice.Temper.HOSTILE).instrument("snare_shell").colour(0xD9B98A).register();
        BandRegistry.voice(ModCaravans.CARAVAN, SoundEvents.NOTE_BLOCK_XYLOPHONE).volume(0.8F)
                .songs(Song.CRYSTAL).temper(BandVoice.Temper.HOSTILE).instrument("crystal_xylophone").colour(0xE07AF0).register();
        BandRegistry.voice(ModCaveCreatures.JAILER, SoundEvents.NOTE_BLOCK_COW_BELL).transpose(-12)
                .families(Family.DRUM, Family.CHIMES).temper(BandVoice.Temper.HOSTILE).instrument("jailers_bell").colour(0x8A93A6).register();
        BandRegistry.voice(ModCaveCreatures.SCULKLING, SoundEvents.NOTE_BLOCK_HAT).transpose(12)
                .layer(SoundEvents.SCULK_CLICKING, 0.5F)
                .families(Family.STRINGS).temper(BandVoice.Temper.HOSTILE).instrument("sculk_clicks").colour(0x2FB8B0).register();
        BandRegistry.voice(ModEntities.FANFARE_EEL, SoundEvents.NOTE_BLOCK_TRUMPET)
                .families(Family.FLUTE).temper(BandVoice.Temper.HOSTILE).movement(BandVoice.Movement.SWIM)
                .instrument("fanfare_horn").colour(0xFFC23A).register();
        BandRegistry.voice(ModSeaSky.GOBBLER, SoundEvents.NOTE_BLOCK_BASS).transpose(-12).volume(1.3F)
                .layer(SoundEvents.WARDEN_HEARTBEAT, 0.6F)
                .songs(Song.TIDE).temper(BandVoice.Temper.HOSTILE).movement(BandVoice.Movement.SWIM)
                .instrument("deep_bass").colour(0x3E6F8F).register();
    }

    /** Harmoners keep their tamer's id themselves. */
    private static BandVoice.Bond harmonerBond(Mob mob, Player player) {
        Harmoner h = (Harmoner) mob;
        UUID owner = h.getOwnerId();
        if (owner == null) {
            return h.isTame() ? BandVoice.Bond.OWN : BandVoice.Bond.WILD;
        }
        return owner.equals(player.getUUID()) ? BandVoice.Bond.OWN : BandVoice.Bond.OTHER;
    }
}
