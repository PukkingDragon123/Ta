package com.thesift.dev;

import com.thesift.TheSift;
import com.thesift.entity.Enchoer;
import com.thesift.item.SiftInstrumentItem;
import com.thesift.item.MusicSheetItem;
import com.thesift.music.Instrument;
import com.thesift.music.InstrumentPlay;
import com.thesift.music.Notes;
import com.thesift.music.Song;
import com.thesift.music.SongEvents;
import com.thesift.music.SongTracker;
import com.thesift.registry.ModEntities;
import com.thesift.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.jspecify.annotations.Nullable;

/**
 * CI checks for the song system (C4 songs, M1 instrument play). Every note goes through the very
 * path the play screens use - {@link InstrumentPlay#serverPlay} with the instrument item in hand -
 * and on through the real song tracker:
 * <ul>
 *   <li>every instrument version reaches every note of the songs of its family;</li>
 *   <li>every song is performed on every version that plays it - a drum song in its rhythm, a
 *   Prism song in its lights;</li>
 *   <li>it is forgiven a semitone off, a double tap and a stray note; a drum song a sloppy beat
 *   and one late note, a Prism song one wrong light;</li>
 *   <li>never on the wrong instrument, without its sheet, out of rhythm or in the wrong lights;</li>
 *   <li>an instrument refuses a note it cannot sound.</li>
 * </ul>
 * Then the Offering is rung on the Wind Chimes beside a waiting Echoer, which must dance - and
 * {@link #finish} checks it gave its reward.
 */
final class SongTest {
    private static boolean listening;
    private static @Nullable SongTest active;

    private final ServerLevel sift;
    private final BiConsumer<Boolean, String> check;
    private final List<Song> heard = new ArrayList<>();
    private @Nullable Enchoer echoer;

    private SongTest(ServerLevel sift, BiConsumer<Boolean, String> check) {
        this.sift = sift;
        this.check = check;
    }

    static SongTest start(ServerLevel sift, BiConsumer<Boolean, String> check) {
        SongTest t = new SongTest(sift, check);
        active = t;
        if (!listening) {
            listening = true;
            SongEvents.listenSongs((level, player, at, song) -> {
                SongTest a = active;
                if (a != null) {
                    a.heard.add(song);
                }
            });
        }
        try {
            t.run();
        } catch (RuntimeException e) {
            TheSift.LOGGER.warn("SMOKE: song test threw", e);
            check.accept(false, "songs: the song test ran without throwing (" + e + ")");
        }
        return t;
    }

    /** The item that plays an instrument version, or null (blocks' and creatures' voices have none). */
    static @Nullable Item itemFor(Instrument instrument) {
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof SiftInstrumentItem ii && ii.instrument() == instrument) {
                return item;
            }
        }
        return null;
    }

    /** The plain version a song is tested on first: one of the family it asks for (a Prism version for a Prism song). */
    static Instrument instrumentFor(Song song) {
        Instrument.Family f = song.instrument();
        if (song.prism()) {
            return f == Instrument.Family.DRUM ? Instrument.PRISM_DRUM : Instrument.PRISM_FLUTE;
        }
        if (f == null) {
            return Instrument.GUITAR;
        }
        return switch (f) {
            case CHIMES -> Instrument.WIND_CHIMES;
            case FLUTE -> Instrument.FLUTE;
            case DRUM -> Instrument.DRUM;
            case STRINGS -> Instrument.GUITAR;
        };
    }

    static @Nullable Item sheet(Song song) {
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof MusicSheetItem s && s.song() == song) {
                return item;
            }
        }
        return null;
    }

    /** What a player would play for a written note: the playable note within tolerance of it. */
    private static int playable(Instrument ins, int note) {
        int n = ins.nearest(note);
        return n < 0 ? note : n;
    }

    /** A playable note a semitone off the written one, if the instrument has one; else the written one's own. */
    private static int offBy(Instrument ins, int note, int dir) {
        if (ins.canPlay(note + dir)) {
            return note + dir;
        }
        if (ins.canPlay(note - dir)) {
            return note - dir;
        }
        return playable(ins, note);
    }

    /** The clocks a song's notes are played at: in its rhythm (a drum song), else half a second apart. */
    private static double[] onBeat(Song song) {
        double[] clocks = new double[song.length()];
        double t = 1000.0;
        for (int i = 0; i < song.length(); i++) {
            clocks[i] = t;
            t += song.rhythmic() ? song.gap(i) : 10.0;
        }
        return clocks;
    }

    private static int[] lights(Song song) {
        int[] out = new int[song.length()];
        for (int i = 0; i < out.length; i++) {
            out[i] = Math.max(0, song.colour(i));
        }
        return out;
    }

    /**
     * Plays the notes on {@code instrument} (held in the main hand) through the play screens' path.
     *
     * @return true if any song was performed (they are in {@link #heard})
     */
    private boolean perform(FakePlayer player, Instrument instrument, int[] notes, int[] colours, double[] clocks) {
        this.heard.clear();
        SongTracker.forget(player);
        Item item = itemFor(instrument);
        if (item == null) {
            this.check.accept(false, "songs: an item plays " + instrument);
            return false;
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
        for (int i = 0; i < notes.length; i++) {
            if (!InstrumentPlay.serverPlay(player, InteractionHand.MAIN_HAND, notes[i], colours[i], clocks[i], Notes.HEARD)) {
                this.check.accept(false, "songs: " + instrument + " sounds note " + notes[i]);
            }
        }
        return !this.heard.isEmpty();
    }

    private boolean performs(FakePlayer player, Instrument instrument, Song song, int[] notes, int[] colours, double[] clocks) {
        return this.perform(player, instrument, notes, colours, clocks) && this.heard.contains(song);
    }

    private void run() {
        int x = 40, z = -40;
        int y = this.sift.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 3;
        for (BlockPos p : BlockPos.betweenClosed(x - 3, y - 1, z - 3, x + 3, y + 3, z + 3)) {
            this.sift.setBlock(p, p.getY() == y - 1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        this.sift.setChunkForced(x >> 4, z >> 4, true);
        FakePlayer player = FakePlayerFactory.getMinecraft(this.sift);
        player.snapTo(x + 0.5, y, z + 0.5, 0.0F, 0.0F);
        player.getInventory().clearContent();
        // an instrument goes in the hand first: the sheets must not land in the held slot and be replaced
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.WIND_CHIMES.get()));
        for (Song song : Song.values()) {
            Item sheet = sheet(song);
            this.check.accept(sheet != null, "songs: " + song.id() + " has a music sheet");
            if (sheet != null) {
                player.getInventory().add(new ItemStack(sheet));
            }
        }

        // every version reaches every note of its family's songs
        for (Instrument ins : Instrument.values()) {
            if (itemFor(ins) == null) {
                continue;
            }
            for (Song song : Song.values()) {
                if (song.accepts(ins)) {
                    this.check.accept(ins.reaches(song), "songs: " + ins + " reaches every note of " + song.id());
                }
            }
        }

        // every song, note for note (in its rhythm and its lights), on every version that plays it
        for (Song song : Song.values()) {
            for (Instrument ins : Instrument.values()) {
                if (!song.accepts(ins) || itemFor(ins) == null) {
                    continue;
                }
                int[] notes = song.notes();
                for (int i = 0; i < notes.length; i++) {
                    notes[i] = playable(ins, notes[i]);
                }
                boolean ok = this.perform(player, ins, notes, lights(song), onBeat(song));
                TheSift.LOGGER.info("SMOKE: song {} on {} -> heard {}", song.id(), ins, this.heard);
                this.check.accept(ok && this.heard.size() == 1 && this.heard.get(0) == song,
                        "songs: " + song.id() + " is performed on " + ins + " (heard " + this.heard + ")");
            }
        }

        // forgiving: a semitone off either way, a double tap and one stray note
        for (Song song : Song.values()) {
            Instrument ins = instrumentFor(song);
            int[] notes = song.notes();
            double[] beat = onBeat(song);
            int[] light = lights(song);
            List<Integer> seq = new ArrayList<>();
            List<Double> clocks = new ArrayList<>();
            List<Integer> lit = new ArrayList<>();
            int stray = -1;
            for (int n : ins.notes()) {
                if (Math.abs(n - notes[0]) > 1 && Math.abs(n - notes[1]) > 1 && Math.abs(n - notes[2]) > 1) {
                    stray = n;
                    break;
                }
            }
            for (int i = 0; i < notes.length; i++) {
                int off = offBy(ins, notes[i], i % 2 == 1 ? 1 : -1);
                seq.add(off);
                clocks.add(beat[i]);
                lit.add(light[i]);
                if (i == 1 && !song.rhythmic()) {
                    // a double tap and a stray note (a drum song's slips are tried below, with its beat)
                    seq.add(off);
                    clocks.add(beat[i] + 2.0);
                    lit.add(light[i]);
                    if (stray >= 0) {
                        seq.add(stray);
                        clocks.add(beat[i] + 4.0);
                        lit.add(light[i]);
                    }
                }
            }
            boolean ok = this.performs(player, ins, song, seq.stream().mapToInt(Integer::intValue).toArray(),
                    lit.stream().mapToInt(Integer::intValue).toArray(), clocks.stream().mapToDouble(Double::doubleValue).toArray());
            this.check.accept(ok, "songs: " + song.id() + " forgives a semitone off, a double tap and a stray note (heard " + this.heard + ")");
        }

        // rhythm: the Tide Song on the Conga Drum
        Song tide = Song.TIDE;
        int[] tideNotes = tide.notes();
        int[] none = new int[tideNotes.length];
        double[] sloppy = onBeat(tide);
        for (int i = 1; i < sloppy.length; i++) {
            sloppy[i] += i % 2 == 0 ? 1.0 : -1.0;
        }
        this.check.accept(this.performs(player, Instrument.DRUM, tide, tideNotes, none, sloppy),
                "songs: the Tide Song forgives a beat a little early or late (heard " + this.heard + ")");
        double[] late = onBeat(tide);
        for (int i = 3; i < late.length; i++) {
            late[i] += 8.0;
        }
        this.check.accept(this.performs(player, Instrument.DRUM, tide, tideNotes, none, late),
                "songs: the Tide Song forgives one note off the beat (heard " + this.heard + ")");
        double[] flat = new double[tideNotes.length];
        for (int i = 0; i < flat.length; i++) {
            flat[i] = 1000.0 + i * 20.0;
        }
        this.check.accept(!this.performs(player, Instrument.DRUM, tide, tideNotes, none, flat),
                "songs: the Tide Song is not performed without its rhythm (heard " + this.heard + ")");
        double[] together = new double[tideNotes.length];
        java.util.Arrays.fill(together, 1000.0);
        this.check.accept(!this.performs(player, Instrument.DRUM, tide, tideNotes, none, together),
                "songs: the Tide Song is not performed with all its beats at once (heard " + this.heard + ")");

        // lights: the Aurora on a Prism Flute
        Song aurora = Song.AURORA;
        int[] auroraNotes = new int[aurora.length()];
        for (int i = 0; i < auroraNotes.length; i++) {
            auroraNotes[i] = playable(Instrument.PRISM_FLUTE, aurora.note(i));
        }
        int[] oneWrong = lights(aurora);
        oneWrong[2] = (oneWrong[2] + 1) % 4;
        this.check.accept(this.performs(player, Instrument.PRISM_FLUTE, aurora, auroraNotes, oneWrong, onBeat(aurora)),
                "songs: the Aurora forgives one wrong light (heard " + this.heard + ")");
        int[] allRose = new int[aurora.length()];
        this.check.accept(!this.performs(player, Instrument.PRISM_FLUTE, aurora, auroraNotes, allRose, onBeat(aurora)),
                "songs: the Aurora is not performed in the wrong lights (heard " + this.heard + ")");
        this.check.accept(!this.performs(player, Instrument.SERBIM_FLUTE, aurora, auroraNotes, lights(aurora), onBeat(aurora)),
                "songs: the Aurora is not performed on an instrument without lights (heard " + this.heard + ")");

        // the wrong instrument, and no sheet, never perform it; an instrument refuses a note it cannot sound
        int[] offering = new int[Song.OFFERING.length()];
        for (int i = 0; i < offering.length; i++) {
            offering[i] = playable(Instrument.GUITAR, Song.OFFERING.note(i));
        }
        this.perform(player, Instrument.GUITAR, offering, new int[offering.length], onBeat(Song.OFFERING));
        this.check.accept(!this.heard.contains(Song.OFFERING), "songs: the Offering is not performed on the guitar");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GUITAR.get()));
        this.check.accept(!InstrumentPlay.serverPlay(player, InteractionHand.MAIN_HAND, 2, -1, 1000.0, 0),
                "songs: the guitar refuses a note below its lowest string");
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.WIND_CHIMES.get()));
        this.perform(player, Instrument.GUITAR, Song.LULLABY.notes(), new int[Song.LULLABY.length()], onBeat(Song.LULLABY));
        this.check.accept(!this.heard.contains(Song.LULLABY), "songs: no song without its music sheet");

        // the Echoer's ceremony: it waits with an offering; the Offering on the Wind Chimes makes it dance
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.WIND_CHIMES.get()));
        player.getInventory().add(new ItemStack(ModItems.MUSIC_SHEET_OFFERING.get()));
        Enchoer e = ModEntities.ENCHOER.get().create(this.sift, EntitySpawnReason.COMMAND);
        if (e == null) {
            this.check.accept(false, "songs: echoer created");
            return;
        }
        e.snapTo(x + 2.5, y, z + 0.5, 90.0F, 0.0F);
        this.sift.addFreshEntity(e);
        e.acceptOffering(new ItemStack(Items.DIAMOND));
        this.check.accept(e.getState() == Enchoer.WAITING, "songs: the echoer waits for the offering song");
        int[] ring = new int[Song.OFFERING.length()];
        for (int i = 0; i < ring.length; i++) {
            ring[i] = playable(Instrument.WIND_CHIMES, Song.OFFERING.note(i));
        }
        this.perform(player, Instrument.WIND_CHIMES, ring, new int[ring.length], onBeat(Song.OFFERING));
        this.check.accept(this.heard.contains(Song.OFFERING) && e.getState() == Enchoer.DANCING,
                "songs: the Offering on the Wind Chimes makes the waiting echoer dance (state " + e.getState() + ")");
        this.echoer = e;
        player.getInventory().clearContent();
    }

    /** A few seconds later: the Echoer has finished its dance and given its reward. */
    void finish() {
        Enchoer e = this.echoer;
        if (e == null) {
            return;
        }
        int gifts = this.sift.getEntitiesOfClass(ItemEntity.class, e.getBoundingBox().inflate(10.0), ItemEntity::isAlive).size();
        TheSift.LOGGER.info("SMOKE: echoer after the offering song: state {}, {} gifts nearby", e.getState(), gifts);
        this.check.accept(e.getState() != Enchoer.DANCING && e.getState() != Enchoer.WAITING && gifts > 0,
                "songs: the echoer finishes its dance and gives its reward (" + gifts + " items)");
        e.discard();
        active = null;
    }
}
