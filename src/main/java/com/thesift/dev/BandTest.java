package com.thesift.dev;

import com.thesift.TheSift;
import com.thesift.music.Instrument;
import com.thesift.music.Notes;
import com.thesift.music.Song;
import com.thesift.music.SongTracker;
import com.thesift.music.band.Bands;
import com.thesift.registry.ModEntities;
import com.thesift.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.jspecify.annotations.Nullable;

/**
 * M2 band: CI checks for the band. Songs go through the real instrument path ({@link Notes#play})
 * and the real song tracker beside a Bulb (strings and chimes), a wild Stomper and a tamed baby
 * Stomper (drums) and a Sifter (a monster that knows the drum):
 * <ul>
 *   <li>an incompatible song (the Whale Song on the flute) leaves the Bulb where it is, and a
 *   compatible one (the Lullaby on the guitar) recruits it;</li>
 *   <li>a drum song brings the tamed Stomper at once, the wild (shy) one only the second time, and
 *   the Sifter never;</li>
 *   <li>the band plays along with the player's next note, and makes their songs stronger;</li>
 *   <li>a few seconds later the members are still in the band, and they leave once the player walks
 *   far away ({@link #tick}).</li>
 * </ul>
 */
final class BandTest {
    private static final int X = -60;
    private static final int Z = 60;

    private final ServerLevel sift;
    private final BiConsumer<Boolean, String> check;
    private final List<Mob> spawned = new ArrayList<>();
    private @Nullable FakePlayer player;
    private @Nullable Mob bulb;
    private @Nullable Mob wild;
    private @Nullable Mob pet;
    private int ticks;
    private boolean done;

    private BandTest(ServerLevel sift, BiConsumer<Boolean, String> check) {
        this.sift = sift;
        this.check = check;
    }

    static BandTest start(ServerLevel sift, BiConsumer<Boolean, String> check) {
        BandTest t = new BandTest(sift, check);
        try {
            t.run();
        } catch (RuntimeException e) {
            TheSift.LOGGER.warn("SMOKE: band test threw", e);
            check.accept(false, "band: the band test ran without throwing (" + e + ")");
            t.done = true;
        }
        return t;
    }

    private @Nullable Mob spawn(EntityType<? extends Mob> type, double x, double y, double z) {
        Mob mob = type.create(this.sift, EntitySpawnReason.COMMAND);
        if (mob == null) {
            this.check.accept(false, "band: created " + type);
            return null;
        }
        mob.snapTo(x, y, z, 0.0F, 0.0F);
        mob.setPersistenceRequired();
        this.sift.addFreshEntity(mob);
        this.spawned.add(mob);
        return mob;
    }

    private void perform(FakePlayer p, Instrument instrument, Song song) {
        SongTracker.forget(p);
        for (int n : song.notes()) {
            Notes.play(this.sift, p, instrument, n);
        }
    }

    private void run() {
        int y = this.sift.getHeight(Heightmap.Types.MOTION_BLOCKING, X, Z) + 3;
        for (BlockPos p : BlockPos.betweenClosed(X - 8, y - 1, Z - 8, X + 8, y + 4, Z + 8)) {
            this.sift.setBlock(p, p.getY() == y - 1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        for (int cx = (X - 8) >> 4; cx <= (X + 8) >> 4; cx++) {
            for (int cz = (Z - 8) >> 4; cz <= (Z + 8) >> 4; cz++) {
                this.sift.setChunkForced(cx, cz, true);
            }
        }
        FakePlayer p = FakePlayerFactory.getMinecraft(this.sift);
        this.player = p;
        Bands.disband(p); // a clean slate: the song test's songs may have called creatures of their own
        p.snapTo(X + 0.5, y, Z + 0.5, 0.0F, 0.0F);
        p.getInventory().clearContent();
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GUITAR.get()));
        p.getInventory().add(new ItemStack(ModItems.MUSIC_SHEET_WHALE.get()));
        p.getInventory().add(new ItemStack(ModItems.MUSIC_SHEET_LULLABY.get()));
        p.getInventory().add(new ItemStack(ModItems.MUSIC_SHEET_TIDE.get()));

        Mob b = this.spawn(ModEntities.BULB.get(), X + 3.5, y, Z + 0.5);
        Mob sifter = this.spawn(ModEntities.SIFTER.get(), X - 4.5, y, Z + 0.5);
        Mob w = this.spawn(ModEntities.STOMPER.get(), X + 0.5, y, Z + 5.5);
        Mob baby = this.spawn(ModEntities.STOMPER.get(), X + 0.5, y, Z - 4.5);
        if (b == null || sifter == null || w == null || !(baby instanceof com.thesift.entity.Stomper tame)) {
            this.done = true;
            return;
        }
        this.bulb = b;
        this.wild = w;
        this.pet = baby;
        tame.setBaby(true);
        tame.setTame(true, false);
        tame.setOwner(p);

        // an incompatible song leaves the Bulb alone; a compatible one calls it
        this.perform(p, Instrument.FLUTE, Song.WHALE);
        this.check.accept(!Bands.isMember(p, b), "band: an incompatible song (the Whale Song on the flute) does not recruit a Bulb");
        this.perform(p, Instrument.GUITAR, Song.LULLABY);
        this.check.accept(Bands.isMember(p, b), "band: a compatible song (the Lullaby on the guitar) recruits a nearby Bulb");

        // drums: the tamed Stomper comes at once, the wild one only the second time, the Sifter never
        this.perform(p, Instrument.DRUM, Song.TIDE);
        this.check.accept(Bands.isMember(p, baby), "band: a tamed Stomper joins its owner's band at the first drum song");
        this.check.accept(!Bands.isMember(p, w), "band: a wild (shy) Stomper only listens to the first drum song");
        this.perform(p, Instrument.DRUM, Song.TIDE);
        this.check.accept(Bands.isMember(p, w), "band: the wild Stomper joins at the second drum song");
        this.check.accept(!Bands.isMember(p, sifter), "band: a hostile Sifter never joins");

        // the band plays along, and makes songs stronger
        int before = Bands.notesPlayed(p);
        Notes.play(this.sift, p, Instrument.GUITAR, 12);
        TheSift.LOGGER.info("SMOKE: band of {} after the songs: {} members, {} notes played along, power {}", p.getName().getString(), Bands.size(p),
                Bands.notesPlayed(p) - before, Bands.power(p));
        this.check.accept(Bands.notesPlayed(p) >= before + 3, "band: the band plays along with the player's note (" + (Bands.notesPlayed(p) - before) + " voices)");
        this.check.accept(Bands.power(p) >= 1.0F + 3 * Bands.POWER_PER_MEMBER - 0.001F, "band: three members make songs stronger (power " + Bands.power(p) + ")");
    }

    /** Called every tick by the mechanics test: the band stays together, then breaks up when the player walks off. */
    void tick() {
        FakePlayer p = this.player;
        if (this.done || p == null) {
            return;
        }
        this.ticks++;
        if (this.ticks == 60) {
            boolean kept = this.bulb != null && this.wild != null && this.pet != null && Bands.isMember(p, this.bulb) && Bands.isMember(p, this.wild)
                    && Bands.isMember(p, this.pet);
            this.check.accept(kept, "band: the members still play in the band three seconds later (" + Bands.size(p) + " members)");
            // the player walks far away
            p.snapTo(p.getX() + 90.0, p.getY(), p.getZ(), 0.0F, 0.0F);
        } else if (this.ticks == 64) {
            this.check.accept(Bands.size(p) == 0, "band: the members leave when the player goes far away (" + Bands.size(p) + " left)");
            Bands.disband(p);
            p.snapTo(X + 0.5, p.getY(), Z + 0.5, 0.0F, 0.0F);
            p.getInventory().clearContent();
            for (Mob m : this.spawned) {
                m.discard();
            }
            this.done = true;
        }
    }
}
