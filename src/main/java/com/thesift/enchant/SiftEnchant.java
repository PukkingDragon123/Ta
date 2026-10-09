package com.thesift.enchant;

import com.thesift.TheSift;
import com.thesift.music.Song;
import com.thesift.music.band.BandRegistry;
import com.thesift.music.band.BandVoice;
import com.thesift.music.band.Bands;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jspecify.annotations.Nullable;

/**
 * F2 Band Table: the Sift's own enchantments. They are data-driven 26.3 enchantments
 * ({@code data/thesift/enchantment/<id>.json}, written by tools/band_table.py) that no vanilla table, villager or
 * loot roll ever offers: they are won only at the Music Band Table, by playing the enchantment's {@link #song()} on
 * its instrument (some only with tamed creatures playing in the player's band, {@link #bandNeeded()}). Their
 * effects that data cannot express live in {@link SiftEnchantEffects}.
 *
 * <p>Constants keep their order: the Band Table sends the ordinal over the network.
 */
public enum SiftEnchant {
    /** Boots: every few steps a glowing note is left behind and the ground chimes a rising scale; a little faster. */
    MELODY_STEPS(Song.NIB, 0),
    /** Boots: your steps make no vibrations - Sculk Sensors, Shriekers and Wardens never hear you walk. */
    HUSHED_STEP(Song.LULLABY, 0),
    /** Armour: shrugs off Sculk Corruption and softens the Sculk's blows (sonic booms, the corruption's bite). */
    SCULK_WARD(Song.REQUIEM, 0),
    /** Weapons: every few hits ring out as a sonic echo that strikes the monsters around the target. */
    ECHO_STRIKE(Song.REQUIEM, 0),
    /** Weapons: hits ring with the band - more damage for every creature playing in your band. */
    RESONANCE(Song.HEARTBEAT, 2),
    /** Weapons: hits in quick succession build up, each a note higher and harder. */
    CRESCENDO(Song.HEARTBEAT, 0),
    /** Instruments: songs played on it reach further and last longer. */
    REVERB(Song.CANON, 0),
    /** Instruments: finishing a song blasts nearby monsters back with a wall of sound. */
    FORTISSIMO(Song.CANON, 1);

    private static final SiftEnchant[] ALL = values();
    private final Song song;
    private final int band;
    private final ResourceKey<Enchantment> key;

    SiftEnchant(Song song, int band) {
        this.song = song;
        this.band = band;
        this.key = ResourceKey.create(Registries.ENCHANTMENT, TheSift.id(this.name().toLowerCase(Locale.ROOT)));
    }

    public static @Nullable SiftEnchant byIndex(int i) {
        return i >= 0 && i < ALL.length ? ALL[i] : null;
    }

    /** Lower-case id ({@code thesift:<id>}). */
    public String id() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public ResourceKey<Enchantment> key() {
        return this.key;
    }

    /** The song that must be played at the Band Table to win it. */
    public Song song() {
        return this.song;
    }

    /** How many tamed creatures of the player's own must be playing in their band, near the table. */
    public int bandNeeded() {
        return this.band;
    }

    public Optional<Holder.Reference<Enchantment>> holder(HolderLookup.Provider registries) {
        return registries.lookupOrThrow(Registries.ENCHANTMENT).get(this.key);
    }

    // ------------------------------------------------------------------ costs

    /** Experience levels the player must have to start the song (like the vanilla table's requirement). */
    public static int levelsNeeded(int maxLevel) {
        return 5 + 5 * maxLevel;
    }

    /** Experience levels a performance won at {@code level} costs (a failed one costs nothing but its shards). */
    public static int levelsTaken(int level) {
        return level <= 0 ? 0 : level + 1;
    }

    /** Amethyst shards (the tuning crystals) a performance at {@code level} uses up - a failed one cracks one. */
    public static int shardsTaken(int level) {
        return Math.max(1, level);
    }

    // ------------------------------------------------------------------ queries

    /** The level of this enchantment on an item (or stored on a book), 0 if none. */
    public int level(ItemStack stack, HolderLookup.Provider registries) {
        Optional<Holder.Reference<Enchantment>> h = this.holder(registries);
        return h.isEmpty() || stack.isEmpty() ? 0 : EnchantmentHelper.getEnchantmentsForCrafting(stack).getLevel(h.get());
    }

    /** The highest level of this enchantment on the entity's equipment in the slots it works in, 0 if none. */
    public int level(LivingEntity entity) {
        Optional<Holder.Reference<Enchantment>> h = this.holder(entity.level().registryAccess());
        return h.isEmpty() ? 0 : EnchantmentHelper.getEnchantmentLevel(h.get(), entity);
    }

    /** The levels of this enchantment on all four armour pieces, added up (protection-style). */
    public int armourTotal(LivingEntity entity) {
        Optional<Holder.Reference<Enchantment>> h = this.holder(entity.level().registryAccess());
        if (h.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            total += entity.getItemBySlot(slot).getEnchantments().getLevel(h.get());
        }
        return total;
    }

    /**
     * True if the Band Table can put this enchantment on the stack: a plain book (it becomes an enchanted book), or an
     * item it supports that has no clashing enchantment and not yet the highest level.
     */
    public boolean appliesTo(ItemStack stack, HolderLookup.Provider registries) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.is(Items.BOOK)) {
            return true;
        }
        Optional<Holder.Reference<Enchantment>> h = this.holder(registries);
        if (h.isEmpty() || !h.get().value().canEnchant(stack)) {
            return false;
        }
        ItemEnchantments has = stack.getEnchantments();
        int now = has.getLevel(h.get());
        if (now >= h.get().value().getMaxLevel()) {
            return false;
        }
        return now > 0 || EnchantmentHelper.isEnchantmentCompatible(has.keySet(), h.get());
    }

    /** The highest level the table can give (from the data), 1 if the enchantment is missing. */
    public int maxLevel(HolderLookup.Provider registries) {
        return this.holder(registries).map(h -> h.value().getMaxLevel()).orElse(1);
    }

    /** How many of the player's own tamed creatures are playing in their band within 16 blocks of {@code at}. */
    public static int tamedBandNear(Player player, BlockPos at) {
        int n = 0;
        for (Mob mob : Bands.members(player)) {
            BandVoice voice = BandRegistry.voiceOf(mob);
            if (mob.isAlive() && voice != null && voice.bond(mob, player) == BandVoice.Bond.OWN
                    && mob.distanceToSqr(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5) <= 16.0 * 16.0) {
                n++;
            }
        }
        return n;
    }
}
