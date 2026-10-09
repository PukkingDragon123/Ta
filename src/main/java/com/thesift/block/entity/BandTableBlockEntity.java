package com.thesift.block.entity;

import com.thesift.enchant.SiftEnchant;
import com.thesift.music.Instrument;
import com.thesift.music.Notes;
import com.thesift.music.Resonance;
import com.thesift.music.Song;
import com.thesift.music.SongEvents;
import com.thesift.music.SongMatcher;
import com.thesift.music.SongTracker;
import com.thesift.registry.ModBandTable;
import com.thesift.registry.ModParticles;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * F2 Band Table: the Music Band Table's state - the item laid on it, and the performance under way.
 *
 * <p>A performance ({@link #choose}) asks for one {@link SiftEnchant}'s song. Every note the performer plays on an
 * instrument within {@link #REACH} blocks ({@link #hearNote}, a {@link SongEvents} listener) is judged against the
 * next note of the song: the right note (a semitone off is forgiven, a little) moves the song on - a drum song must
 * also keep its beat - and anything else is a mistake ({@link #MAX_MISTAKES} and the song falls apart). Repeating the
 * note just played changes nothing. When the last note lands the table weighs the playing ({@link #score}): clean
 * playing wins the enchantment's full level, sloppy playing a weaker one, poor playing nothing. Only then is the price
 * paid: experience levels and amethyst shards (a failed song cracks one shard).
 */
public class BandTableBlockEntity extends BlockEntity {
    /** How near the table the notes must be played. */
    public static final double REACH = 16.0;
    /** A performance without a note for this long is abandoned (nothing paid). */
    public static final int TIMEOUT = 20 * 30;
    /** This many wrong notes and the song falls apart. */
    public static final int MAX_MISTAKES = 3;
    /** Below this the performance wins nothing. */
    public static final float FAIL_BELOW = 0.45F;
    /** At or above this the performance wins the full level. */
    public static final float FULL_AT = 0.9F;
    private static final List<BandTableBlockEntity> PERFORMING = new ArrayList<>();

    private ItemStack item = ItemStack.EMPTY;
    /** The enchantment being played for ({@link SiftEnchant} ordinal), -1 when idle. */
    private int enchant = -1;
    private @Nullable UUID performer;
    private int progress;
    private int mistakes;
    private int offBeats;
    private int near;
    private double lastClock;
    private long lastNoteAt;
    private long hintAt = Long.MIN_VALUE / 2;
    // shown by the renderer (synced)
    private long hitAt = -1000L;
    private long missAt = -1000L;
    private long doneAt = -1000L;
    private int doneLevel;
    private int doneEnchant = -1;
    private int lastPitch = 12;

    // client-side animation (the renderer reads these)
    public int age;
    public float open;
    public float openO;
    public float flip;
    public float flipO;
    public float swing;
    public float swingO;
    public float swingSpeed;

    public BandTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBandTable.BAND_TABLE_BE.get(), pos, state);
    }

    // ------------------------------------------------------------------ queries (both sides)

    public ItemStack getItem() {
        return this.item;
    }

    public boolean isPerforming() {
        return this.enchant >= 0;
    }

    public @Nullable SiftEnchant performing() {
        return SiftEnchant.byIndex(this.enchant);
    }

    public int progress() {
        return this.progress;
    }

    public int mistakes() {
        return this.mistakes;
    }

    public long hitAt() {
        return this.hitAt;
    }

    public long missAt() {
        return this.missAt;
    }

    public long doneAt() {
        return this.doneAt;
    }

    /** The enchantment the last finished performance was for (its notes fade away over the table), or null. */
    public @Nullable SiftEnchant lastPlayed() {
        return SiftEnchant.byIndex(this.doneEnchant);
    }

    /** The level the last finished performance won (0: it failed). */
    public int doneLevel() {
        return this.doneLevel;
    }

    public int lastPitch() {
        return this.lastPitch;
    }

    /**
     * How well the song has been played so far, 0..1: a wrong note costs 0.2, a note off the beat 0.12, a note a
     * semitone off 0.03.
     */
    public float score() {
        return Mth.clamp(1.0F - 0.2F * this.mistakes - 0.12F * this.offBeats - 0.03F * this.near, 0.0F, 1.0F);
    }

    /** The level a performance scoring {@code score} wins, for an enchantment that goes up to {@code max} (0: nothing). */
    public static int levelFor(float score, int max) {
        if (score < FAIL_BELOW) {
            return 0;
        }
        if (score >= FULL_AT) {
            return max;
        }
        return Mth.clamp(Math.round(max * score), 1, Math.max(1, max - 1));
    }

    // ------------------------------------------------------------------ interaction (server)

    /** Using the table: a performer's sneak-use stops the song; otherwise its state, or the item handed back. */
    public void use(ServerLevel level, Player player) {
        SiftEnchant e = this.performing();
        if (e != null) {
            if (player.getUUID().equals(this.performer) && player.isShiftKeyDown()) {
                this.end(level, false);
                player.sendOverlayMessage(Component.translatable("message.thesift.band_table.stopped"));
            } else {
                player.sendOverlayMessage(Component.translatable("message.thesift.band_table.playing", Component.translatable("song.thesift." + e.song().id()),
                        Component.translatable(e.song().instrumentKey()), this.progress, e.song().length()).withStyle(ChatFormatting.AQUA));
            }
            return;
        }
        if (!this.item.isEmpty()) {
            if (!player.getInventory().add(this.item)) {
                Block.popResource(level, this.worldPosition.above(), this.item);
            }
            this.item = ItemStack.EMPTY;
            level.playSound(null, this.worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6F, 1.3F);
            this.sync();
        }
    }

    /**
     * Server: the player picked an enchantment for the item in their inventory slot {@code slot} on the table's score.
     * Checks everything (the sheet, levels, shards, the band), lays the item on the table and starts the song.
     *
     * @return null if the song has begun, else why not
     */
    public @Nullable Component choose(Player player, int slot, int enchantIndex) {
        if (!(this.level instanceof ServerLevel level)) {
            return Component.literal("client");
        }
        SiftEnchant e = SiftEnchant.byIndex(enchantIndex);
        Inventory inv = player.getInventory();
        if (e == null || slot < 0 || slot >= inv.getContainerSize()) {
            return Component.translatable("message.thesift.band_table.cannot");
        }
        if (this.isPerforming() || !this.item.isEmpty()) {
            return Component.translatable("message.thesift.band_table.busy");
        }
        if (player.distanceToSqr(Vec3.atCenterOf(this.worldPosition)) > 8.0 * 8.0) {
            return Component.translatable("message.thesift.band_table.too_far");
        }
        ItemStack stack = inv.getItem(slot);
        Optional<Holder.Reference<Enchantment>> h = e.holder(level.registryAccess());
        if (h.isEmpty() || !e.appliesTo(stack, level.registryAccess())) {
            return Component.translatable("message.thesift.band_table.cannot");
        }
        if (!SongTracker.carriesSheet(player, e.song())) {
            return Component.translatable("message.thesift.band_table.needs_sheet", Component.translatable("song.thesift." + e.song().id()));
        }
        int max = h.get().value().getMaxLevel();
        if (!player.hasInfiniteMaterials()) {
            if (player.experienceLevel < SiftEnchant.levelsNeeded(max)) {
                return Component.translatable("message.thesift.band_table.needs_levels", SiftEnchant.levelsNeeded(max));
            }
            if (count(player, Items.AMETHYST_SHARD) < SiftEnchant.shardsTaken(max)) {
                return Component.translatable("message.thesift.band_table.needs_shards", SiftEnchant.shardsTaken(max));
            }
        }
        int band = SiftEnchant.tamedBandNear(player, this.worldPosition);
        if (band < e.bandNeeded()) {
            return Component.translatable("message.thesift.band_table.needs_band", e.bandNeeded(), band);
        }
        this.item = stack.split(1);
        this.enchant = e.ordinal();
        this.performer = player.getUUID();
        this.progress = 0;
        this.mistakes = 0;
        this.offBeats = 0;
        this.near = 0;
        this.lastNoteAt = level.getGameTime();
        if (!PERFORMING.contains(this)) {
            PERFORMING.add(this);
        }
        Vec3 c = Vec3.atCenterOf(this.worldPosition);
        level.playSound(null, this.worldPosition, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.8F, 1.3F);
        level.playSound(null, this.worldPosition, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 1.0F, Notes.soundPitch(e.song().note(0)));
        level.sendParticles(ModParticles.RESONANCE_RING.get(), c.x, c.y + 0.3, c.z, 0, 1.4, 0.0, 0.0, 1.0);
        player.sendOverlayMessage(Component.translatable("message.thesift.band_table.begin", Component.translatable("song.thesift." + e.song().id()),
                Component.translatable(e.song().instrumentKey())).withStyle(ChatFormatting.AQUA));
        this.sync();
        return null;
    }

    private static int count(Player player, net.minecraft.world.item.Item what) {
        int n = 0;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(what)) {
                n += s.getCount();
            }
        }
        return n;
    }

    private static void take(Player player, net.minecraft.world.item.Item what, int n) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize() && n > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(what)) {
                int k = Math.min(n, s.getCount());
                s.shrink(k);
                n -= k;
            }
        }
    }

    // ------------------------------------------------------------------ hearing the song

    /** {@link SongEvents} note listener (registered once by {@link ModBandTable}): every table being played for hears it. */
    public static void hearNote(ServerLevel level, @Nullable Player player, Vec3 at, int pitch) {
        Instrument ins = SongEvents.instrument();
        if (player == null || ins == null || PERFORMING.isEmpty()) {
            return;
        }
        for (BandTableBlockEntity t : List.copyOf(PERFORMING)) {
            if (t.isRemoved() || !t.isPerforming()) {
                PERFORMING.remove(t);
                continue;
            }
            if (t.level == level && player.getUUID().equals(t.performer) && at.distanceToSqr(Vec3.atCenterOf(t.worldPosition)) <= REACH * REACH) {
                t.hear(level, player, pitch, ins, SongEvents.clock());
            }
        }
    }

    private void hear(ServerLevel level, Player player, int pitch, Instrument ins, double clock) {
        SiftEnchant e = this.performing();
        if (e == null) {
            return;
        }
        Song song = e.song();
        long now = level.getGameTime();
        this.lastNoteAt = now;
        if (!song.accepts(ins)) {
            if (now - this.hintAt > 60) {
                this.hintAt = now;
                player.sendOverlayMessage(Component.translatable("message.thesift.song.wrong_instrument", Component.translatable("song.thesift." + song.id()),
                        Component.translatable(song.instrumentKey())).withStyle(ChatFormatting.GOLD));
            }
            return;
        }
        int i = Math.min(this.progress, song.length() - 1);
        int want = song.note(i);
        Vec3 c = Vec3.atCenterOf(this.worldPosition);
        this.lastPitch = pitch;
        if (SongMatcher.matches(want, pitch)) {
            if (i > 0 && song.rhythmic() && !SongMatcher.onBeat(song.gap(i - 1), clock - this.lastClock)) {
                this.offBeats++;
            }
            if (pitch != want) {
                this.near++;
            }
            this.progress = i + 1;
            this.lastClock = clock;
            this.hitAt = now;
            // the table answers: its note rises from the songbook in its colour
            level.sendParticles(ModParticles.SIFT_NOTE.get(), c.x, c.y + 0.9, c.z, 0, want / 24.0, 0.0, 0.0, 1.0);
            level.playSound(null, this.worldPosition, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.5F, Notes.soundPitch(want));
            if (this.progress >= song.length()) {
                this.finish(level, player);
                return;
            }
        } else if (i > 0 && SongMatcher.matches(song.note(i - 1), pitch)) {
            // the note just played, again: a double tap changes nothing
            return;
        } else {
            this.mistakes++;
            this.missAt = now;
            level.sendParticles(ParticleTypes.SMOKE, c.x, c.y + 0.8, c.z, 4, 0.2, 0.1, 0.2, 0.01);
            level.playSound(null, this.worldPosition, SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), SoundSource.BLOCKS, 0.6F, 0.6F);
            player.sendOverlayMessage(Component.translatable("message.thesift.band_table.wrong", Notes.name(want), MAX_MISTAKES - this.mistakes)
                    .withStyle(ChatFormatting.RED));
            if (this.mistakes >= MAX_MISTAKES) {
                this.finish(level, player);
                return;
            }
        }
        this.sync();
    }

    /** The song is over (or has fallen apart): weigh it, pay, enchant. */
    private void finish(ServerLevel level, Player player) {
        SiftEnchant e = this.performing();
        Optional<Holder.Reference<Enchantment>> h = e == null ? Optional.empty() : e.holder(level.registryAccess());
        if (e == null || h.isEmpty() || this.item.isEmpty()) {
            this.end(level, false);
            return;
        }
        int max = h.get().value().getMaxLevel();
        int lvl = this.mistakes >= MAX_MISTAKES ? 0 : levelFor(this.score(), max);
        if (!player.hasInfiniteMaterials()) {
            int shards = SiftEnchant.shardsTaken(lvl);
            if (count(player, Items.AMETHYST_SHARD) < shards || player.experienceLevel < SiftEnchant.levelsTaken(lvl)) {
                lvl = 0;
                player.sendSystemMessage(Component.translatable("message.thesift.band_table.cannot_pay").withStyle(ChatFormatting.RED));
            }
            take(player, Items.AMETHYST_SHARD, SiftEnchant.shardsTaken(lvl));
            if (lvl > 0) {
                player.giveExperienceLevels(-SiftEnchant.levelsTaken(lvl));
            }
        }
        Vec3 c = Vec3.atCenterOf(this.worldPosition);
        if (lvl > 0) {
            if (this.item.is(Items.BOOK)) {
                this.item = EnchantmentHelper.createBook(new EnchantmentInstance(h.get(), lvl));
            } else {
                this.item.enchant(h.get(), lvl);
            }
            // a rising chord, a fountain of notes and stars, a ring through the floor
            int root = e.song().note(e.song().length() - 1);
            for (int n : Notes.chord(root)) {
                level.playSound(null, this.worldPosition, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 1.0F, Notes.soundPitch(n));
            }
            level.playSound(null, this.worldPosition, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.2F, 0.9F);
            level.playSound(null, this.worldPosition, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.2F, 1.4F);
            for (int k = 0; k < 12; k++) {
                double a = k / 12.0 * Math.PI * 2.0;
                level.sendParticles(ModParticles.SIFT_NOTE.get(), c.x + Math.cos(a) * 0.9, c.y + 0.9 + (k % 3) * 0.25, c.z + Math.sin(a) * 0.9, 0, k / 12.0,
                        0.0, 0.0, 1.0);
            }
            level.sendParticles(ModParticles.STAR_SPARKLE.get(), c.x, c.y + 1.0, c.z, 30, 0.5, 0.5, 0.5, 0.12);
            level.sendParticles(ModParticles.RESONANCE_RING.get(), c.x, c.y - 0.4, c.z, 0, 4.0, 0.0, 0.0, 1.0);
            Resonance.pulse(level, this.worldPosition, 0.8F, 8);
            String grade = lvl >= max ? "flawless" : "shaky";
            player.sendOverlayMessage(Component.translatable("message.thesift.band_table." + grade, Enchantment.getFullname(h.get(), lvl))
                    .withStyle(lvl >= max ? ChatFormatting.GOLD : ChatFormatting.YELLOW));
        } else {
            level.playSound(null, this.worldPosition, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1.0F, 0.5F);
            level.playSound(null, this.worldPosition, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 0.9F, 0.8F);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y + 0.7, c.z, 8, 0.25, 0.15, 0.25, 0.01);
            player.sendOverlayMessage(Component.translatable("message.thesift.band_table.failed").withStyle(ChatFormatting.RED));
        }
        this.doneLevel = lvl;
        this.doneEnchant = e.ordinal();
        this.end(level, true);
    }

    private void end(ServerLevel level, boolean finished) {
        this.enchant = -1;
        this.performer = null;
        PERFORMING.remove(this);
        if (finished) {
            this.doneAt = level.getGameTime();
        }
        this.sync();
    }

    // ------------------------------------------------------------------ ticking

    public static void serverTick(Level level, BlockPos pos, BlockState state, BandTableBlockEntity table) {
        if (!table.isPerforming() || !(level instanceof ServerLevel server)) {
            return;
        }
        if (!PERFORMING.contains(table)) {
            PERFORMING.add(table); // loaded from disk mid-song
        }
        long now = server.getGameTime();
        Player p = table.performer == null ? null : server.getPlayerByUUID(table.performer);
        if (now - table.lastNoteAt > TIMEOUT || p == null || p.distanceToSqr(Vec3.atCenterOf(pos)) > 32.0 * 32.0 || table.item.isEmpty()) {
            if (p != null) {
                p.sendOverlayMessage(Component.translatable("message.thesift.band_table.stopped"));
            }
            table.end(server, false);
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, BandTableBlockEntity table) {
        table.age++;
        table.openO = table.open;
        table.flipO = table.flip;
        table.swingO = table.swing;
        boolean playing = table.isPerforming();
        // the songbook opens wide while a song is played and its pages turn with every note
        float targetOpen = playing ? 1.0F : (table.item.isEmpty() ? 0.35F : 0.7F);
        table.open += (targetOpen - table.open) * 0.1F;
        long now = level.getGameTime();
        if (now - table.hitAt < 2) {
            table.flip += 0.5F;
        } else {
            table.flip += (Math.round(table.flip) - table.flip) * 0.2F;
        }
        // the chimes and the metronome swing harder while music is played
        float target = playing ? 0.22F : 0.06F;
        table.swingSpeed += (target - table.swingSpeed) * 0.05F;
        table.swing += table.swingSpeed;
        if (playing && level.getRandom().nextInt(3) == 0) {
            double a = level.getRandom().nextDouble() * Math.PI * 2.0;
            level.addParticle(ModParticles.STAR_SPARKLE.get(), pos.getX() + 0.5 + Math.cos(a) * 0.7, pos.getY() + 1.0, pos.getZ() + 0.5 + Math.sin(a) * 0.7,
                    0.0, 0.03, 0.0);
        }
    }

    // ------------------------------------------------------------------ sync & save

    private void sync() {
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.item.isEmpty()) {
            output.store("Item", ItemStack.CODEC, this.item);
        }
        output.putInt("Enchant", this.enchant);
        if (this.performer != null) {
            output.store("Performer", UUIDUtil.CODEC, this.performer);
        }
        output.putInt("Progress", this.progress);
        output.putInt("Mistakes", this.mistakes);
        output.putInt("OffBeats", this.offBeats);
        output.putInt("Near", this.near);
        output.putLong("HitAt", this.hitAt);
        output.putLong("MissAt", this.missAt);
        output.putLong("DoneAt", this.doneAt);
        output.putInt("DoneLevel", this.doneLevel);
        output.putInt("DoneEnchant", this.doneEnchant);
        output.putInt("LastPitch", this.lastPitch);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.item = input.read("Item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        this.enchant = input.getIntOr("Enchant", -1);
        this.performer = input.read("Performer", UUIDUtil.CODEC).orElse(null);
        this.progress = input.getIntOr("Progress", 0);
        this.mistakes = input.getIntOr("Mistakes", 0);
        this.offBeats = input.getIntOr("OffBeats", 0);
        this.near = input.getIntOr("Near", 0);
        this.hitAt = input.getLongOr("HitAt", -1000L);
        this.missAt = input.getLongOr("MissAt", -1000L);
        this.doneAt = input.getLongOr("DoneAt", -1000L);
        this.doneLevel = input.getIntOr("DoneLevel", 0);
        this.doneEnchant = input.getIntOr("DoneEnchant", -1);
        this.lastPitch = input.getIntOr("LastPitch", 12);
        if (this.enchant >= 0 && this.performer == null) {
            this.enchant = -1;
        }
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        PERFORMING.remove(this);
        if (!this.item.isEmpty() && this.level != null) {
            Block.popResource(this.level, pos, this.item);
        }
        super.preRemoveSideEffects(pos, state);
    }
}
