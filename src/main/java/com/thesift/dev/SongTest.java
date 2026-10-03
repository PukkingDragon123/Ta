package com.thesift.dev;

import com.thesift.TheSift;
import com.thesift.entity.Enchoer;
import com.thesift.music.Instrument;
import com.thesift.music.Notes;
import com.thesift.music.Song;
import com.thesift.music.SongTracker;
import com.thesift.music.SongEvents;
import com.thesift.registry.ModEntities;
import com.thesift.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
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
 * C4 songs: CI checks for the song system. Every song's notes go through the real instrument path
 * ({@link Notes#play}) and the real song tracker: each must be performed on its own instrument,
 * also when played a semitone off with a double tap and a stray note, never on the wrong
 * instrument or without its sheet. Then the Offering is rung on the Wind Chimes beside a waiting
 * Echoer, which must dance - and {@link #finish} checks it gave its reward.
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

    /** The instrument a song is tested on: one of the family it asks for (the Harp for any). */
    static Instrument instrumentFor(Song song) {
        Instrument.Family f = song.instrument();
        if (f == null) {
            return Instrument.HARP;
        }
        return switch (f) {
            case CHIMES -> Instrument.WIND_CHIMES;
            case FLUTE -> Instrument.FLUTE;
            case DRUM -> Instrument.DRUM;
            case STRINGS -> Instrument.GUITAR;
        };
    }

    static Item sheet(Song song) {
        return switch (song) {
            case OFFERING -> ModItems.MUSIC_SHEET_OFFERING.get();
            case NIB -> ModItems.MUSIC_SHEET_NIB.get();
            case GOLEM -> ModItems.MUSIC_SHEET_GOLEM.get();
            case CRYSTAL -> ModItems.MUSIC_SHEET_CRYSTAL.get();
            case WHALE -> ModItems.MUSIC_SHEET_WHALE.get();
            case TIDE -> ModItems.MUSIC_SHEET_TIDE.get();
            case LULLABY -> ModItems.MUSIC_SHEET_LULLABY.get();
        };
    }

    private boolean perform(FakePlayer player, Instrument instrument, int[] notes) {
        this.heard.clear();
        SongTracker.forget(player);
        for (int n : notes) {
            Notes.play(this.sift, player, instrument, n);
        }
        return !this.heard.isEmpty();
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
        for (Song song : Song.values()) {
            player.getInventory().add(new ItemStack(sheet(song)));
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.WIND_CHIMES.get()));

        // every song, note for note, on its own instrument
        for (Song song : Song.values()) {
            boolean ok = this.perform(player, instrumentFor(song), song.notes());
            TheSift.LOGGER.info("SMOKE: song {} on {} -> heard {}", song.id(), instrumentFor(song), this.heard);
            this.check.accept(ok && this.heard.size() == 1 && this.heard.get(0) == song, "songs: " + song.id() + " is performed on " + instrumentFor(song) + " (heard " + this.heard + ")");
        }
        // forgiving: a semitone off either way, a double tap and one stray note
        for (Song song : Song.values()) {
            int[] notes = song.notes();
            List<Integer> seq = new ArrayList<>();
            for (int i = 0; i < notes.length; i++) {
                int off = i % 2 == 1 ? Math.min(Notes.MAX_PITCH, notes[i] + 1) : Math.max(0, notes[i] - 1);
                seq.add(off);
                if (i == 1) {
                    seq.add(off);
                    seq.add(notes[2] < 15 ? 23 : 0);
                }
            }
            boolean ok = this.perform(player, instrumentFor(song), seq.stream().mapToInt(Integer::intValue).toArray());
            this.check.accept(ok && this.heard.contains(song), "songs: " + song.id() + " forgives a semitone off, a double tap and a stray note (heard " + this.heard + ")");
        }
        // the wrong instrument, and no sheet, never perform it
        this.perform(player, Instrument.GUITAR, Song.OFFERING.notes());
        this.check.accept(!this.heard.contains(Song.OFFERING), "songs: the Offering is not performed on the guitar");
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.WIND_CHIMES.get()));
        this.perform(player, Instrument.GUITAR, Song.LULLABY.notes());
        this.check.accept(!this.heard.contains(Song.LULLABY), "songs: no song without its music sheet");

        // the Echoer's ceremony: it waits with an offering; the Offering on the Wind Chimes makes it dance
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
        this.perform(player, Instrument.WIND_CHIMES, Song.OFFERING.notes());
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
