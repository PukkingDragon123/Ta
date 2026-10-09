package com.thesift.dev;

import com.mojang.authlib.GameProfile;
import com.thesift.TheSift;
import com.thesift.block.entity.BandTableBlockEntity;
import com.thesift.enchant.SiftEnchant;
import com.thesift.music.Instrument;
import com.thesift.music.InstrumentPlay;
import com.thesift.music.Notes;
import com.thesift.music.Song;
import com.thesift.music.SongTracker;
import com.thesift.music.band.Bands;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModEntities;
import com.thesift.registry.ModItems;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * F2 Band Table: CI checks that enchanting by playing works, through the very path a player uses - the score's
 * choice ({@link BandTableBlockEntity#choose}) and then every note through {@link InstrumentPlay#serverPlay} with the
 * instrument in hand.
 * <ul>
 *   <li>a clean Song of the Nibs on a guitar enchants iron boots with Melody Steps III, and costs its levels and shards;</li>
 *   <li>the Sculk Requiem on Wind Chimes with two wrong notes still enchants a book, with a weaker Sculk Ward;</li>
 *   <li>three wrong notes and the song falls apart (no enchantment);</li>
 *   <li>Fortissimo is refused without a tamed creature in the band, and given once a tamed Stomper plays in it.</li>
 * </ul>
 * Its own fake player, away from the song and band tests.
 */
final class BandTableTest {
    private static final int X = 64;
    private static final int Z = 64;
    private static final GameProfile PROFILE = new GameProfile(UUID.fromString("5f1a7b2e-0b3c-4d8e-9a6f-2c4b8e1d7f30"), "[SiftBandTable]");

    private final ServerLevel sift;
    private final BiConsumer<Boolean, String> check;

    private BandTableTest(ServerLevel sift, BiConsumer<Boolean, String> check) {
        this.sift = sift;
        this.check = check;
    }

    static void run(ServerLevel sift, BiConsumer<Boolean, String> check) {
        try {
            new BandTableTest(sift, check).go();
        } catch (RuntimeException e) {
            TheSift.LOGGER.warn("SMOKE: band table test threw", e);
            check.accept(false, "band table: the test ran without throwing (" + e + ")");
        }
    }

    private static int find(FakePlayer p, Item item) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(item) && inv.getItem(i) != p.getMainHandItem()) {
                return i;
            }
        }
        return -1;
    }

    private static int count(FakePlayer p, Item item) {
        int n = 0;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(item)) {
                n += inv.getItem(i).getCount();
            }
        }
        return n;
    }

    /** A note the instrument has that is far (3+ semitones) from both {@code want} and the note before it. */
    private static int wrong(Instrument ins, int want, int before) {
        int best = -1;
        int bestD = -1;
        for (int n : ins.notes()) {
            int d = Math.min(Math.abs(n - want), before < 0 ? 99 : Math.abs(n - before));
            if (d > bestD) {
                bestD = d;
                best = n;
            }
        }
        return best;
    }

    /** Plays the song on the instrument in hand, in its rhythm, with a wrong note slipped in before each index in {@code wrongBefore}. */
    private void play(FakePlayer p, Instrument ins, Song song, int... wrongBefore) {
        SongTracker.forget(p);
        double clock = 1000.0;
        for (int i = 0; i < song.length(); i++) {
            for (int w : wrongBefore) {
                if (w == i) {
                    int bad = wrong(ins, song.note(i), i > 0 ? song.note(i - 1) : -1);
                    InstrumentPlay.serverPlay(p, InteractionHand.MAIN_HAND, bad, -1, clock - 2.0, Notes.HEARD);
                }
            }
            int n = ins.nearest(song.note(i));
            boolean ok = InstrumentPlay.serverPlay(p, InteractionHand.MAIN_HAND, n < 0 ? song.note(i) : n, -1, clock, Notes.HEARD);
            this.check.accept(ok, "band table: " + ins + " sounds note " + song.note(i));
            clock += song.rhythmic() ? song.gap(i) : 10.0;
        }
    }

    private void go() {
        int y = this.sift.getHeight(Heightmap.Types.MOTION_BLOCKING, X, Z) + 3;
        for (BlockPos q : BlockPos.betweenClosed(X - 5, y - 1, Z - 5, X + 5, y + 4, Z + 5)) {
            this.sift.setBlock(q, q.getY() == y - 1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        this.sift.setChunkForced(X >> 4, Z >> 4, true);
        BlockPos at = new BlockPos(X, y, Z);
        this.sift.setBlock(at, ModBlocks.BAND_TABLE.get().defaultBlockState(), Block.UPDATE_ALL);
        if (!(this.sift.getBlockEntity(at) instanceof BandTableBlockEntity table)) {
            this.check.accept(false, "band table: the block entity is there");
            return;
        }
        var reg = this.sift.registryAccess();
        FakePlayer p = FakePlayerFactory.get(this.sift, PROFILE);
        p.snapTo(X + 0.5, y, Z - 1.5, 0.0F, 0.0F);
        p.getInventory().clearContent();
        p.giveExperienceLevels(80);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GUITAR.get()));
        p.getInventory().add(new ItemStack(Items.AMETHYST_SHARD, 32));
        p.getInventory().add(new ItemStack(ModItems.MUSIC_SHEET_NIB.get()));
        p.getInventory().add(new ItemStack(Items.IRON_BOOTS));

        // 1. a clean Song of the Nibs on the guitar: Melody Steps III on the boots
        int levels = p.experienceLevel;
        Component why = table.choose(p, find(p, Items.IRON_BOOTS), SiftEnchant.MELODY_STEPS.ordinal());
        this.check.accept(why == null, "band table: Melody Steps can be chosen for iron boots (" + (why == null ? "" : why.getString()) + ")");
        this.check.accept(table.isPerforming() && table.getItem().is(Items.IRON_BOOTS), "band table: the boots lie on the table while it listens");
        this.play(p, Instrument.GUITAR, Song.NIB);
        int lvl = SiftEnchant.MELODY_STEPS.level(table.getItem(), reg);
        TheSift.LOGGER.info("SMOKE: band table, a clean Song of the Nibs -> {} (Melody Steps {}), levels {} -> {}, shards left {}",
                table.getItem(), lvl, levels, p.experienceLevel, count(p, Items.AMETHYST_SHARD));
        this.check.accept(!table.isPerforming() && table.getItem().is(Items.IRON_BOOTS) && lvl == 3,
                "band table: a clean Song of the Nibs gives the boots Melody Steps III (got " + lvl + ")");
        this.check.accept(p.experienceLevel == levels - SiftEnchant.levelsTaken(3) && count(p, Items.AMETHYST_SHARD) == 32 - SiftEnchant.shardsTaken(3),
                "band table: the performance cost its levels and shards");
        table.use(this.sift, p);
        this.check.accept(table.getItem().isEmpty() && count(p, Items.IRON_BOOTS) == 1, "band table: using the table hands the boots back");

        // 2. the Sculk Requiem on Wind Chimes with two wrong notes: a book, but a weaker Sculk Ward
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.WIND_CHIMES.get()));
        p.getInventory().add(new ItemStack(ModItems.MUSIC_SHEET_REQUIEM.get()));
        p.getInventory().add(new ItemStack(Items.BOOK, 2));
        why = table.choose(p, find(p, Items.BOOK), SiftEnchant.SCULK_WARD.ordinal());
        this.check.accept(why == null && table.getItem().is(Items.BOOK) && table.getItem().getCount() == 1,
                "band table: Sculk Ward can be chosen for one book (" + (why == null ? "" : why.getString()) + ")");
        this.play(p, Instrument.WIND_CHIMES, Song.REQUIEM, 2, 5);
        int ward = SiftEnchant.SCULK_WARD.level(table.getItem(), reg);
        TheSift.LOGGER.info("SMOKE: band table, a sloppy Sculk Requiem -> {} (Sculk Ward {})", table.getItem(), ward);
        this.check.accept(table.getItem().is(Items.ENCHANTED_BOOK) && ward >= 1 && ward < 4,
                "band table: a sloppy Requiem still gives a book, with a weaker Sculk Ward (got " + ward + ")");
        table.use(this.sift, p);

        // 3. three wrong notes and the song falls apart
        why = table.choose(p, find(p, Items.BOOK), SiftEnchant.ECHO_STRIKE.ordinal());
        this.check.accept(why == null, "band table: Echo Strike can be chosen for a book (" + (why == null ? "" : why.getString()) + ")");
        this.play(p, Instrument.WIND_CHIMES, Song.REQUIEM, 1, 2, 3);
        this.check.accept(!table.isPerforming() && table.getItem().is(Items.BOOK),
                "band table: three wrong notes and the song falls apart - the book stays plain (" + table.getItem() + ")");
        table.use(this.sift, p);

        // 4. Fortissimo needs one of your tamed creatures playing in your band
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GUITAR.get()));
        p.getInventory().add(new ItemStack(ModItems.MUSIC_SHEET_CANON.get()));
        p.getInventory().add(new ItemStack(ModItems.WIND_CHIMES.get()));
        why = table.choose(p, find(p, ModItems.WIND_CHIMES.get()), SiftEnchant.FORTISSIMO.ordinal());
        this.check.accept(why != null && !table.isPerforming(), "band table: Fortissimo is refused without a tamed creature in the band");
        if (!(ModEntities.STOMPER.get().create(this.sift, EntitySpawnReason.COMMAND) instanceof com.thesift.entity.Stomper pet)) {
            this.check.accept(false, "band table: a Stomper was made");
            return;
        }
        pet.snapTo(X + 2.5, y, Z - 1.5, 0.0F, 0.0F);
        pet.setPersistenceRequired();
        pet.setBaby(true);
        this.sift.addFreshEntity(pet);
        pet.setTame(true, false);
        pet.setOwner(p);
        // a drum song calls it into the band
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CONGA_DRUM.get()));
        p.getInventory().add(new ItemStack(ModItems.MUSIC_SHEET_GOLEM.get()));
        this.play(p, Instrument.DRUM, Song.GOLEM);
        this.check.accept(Bands.isMember(p, pet), "band table: the tamed Stomper joins the band (" + Bands.size(p) + " members)");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GUITAR.get()));
        why = table.choose(p, find(p, ModItems.WIND_CHIMES.get()), SiftEnchant.FORTISSIMO.ordinal());
        this.check.accept(why == null, "band table: with a tamed Stomper in the band Fortissimo can be chosen (" + (why == null ? "" : why.getString()) + ")");
        this.play(p, Instrument.GUITAR, Song.CANON);
        int ff = SiftEnchant.FORTISSIMO.level(table.getItem(), reg);
        this.check.accept(table.getItem().is(ModItems.WIND_CHIMES.get()) && ff == 2,
                "band table: the Enchanter's Canon gives the Wind Chimes Fortissimo II (got " + ff + ")");
        Bands.disband(p);
        pet.discard();
        p.getInventory().clearContent();
    }
}
