package com.thesift.knowledge;

import com.thesift.registry.ModKnowledge;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * F3 Knowledge &amp; lore: what a player has discovered, kept as a list of keys in a data attachment that is saved
 * with the player, synced to their client and kept through death ({@link ModKnowledge#KNOWLEDGE}).
 *
 * <p>Keys: {@code entity:<id>} (met a creature), {@code item:<id>} (held an item), {@code biome:<id>} / {@code dim:<id>}
 * / {@code structure:<id>} (been there), {@code song:<id>} (played or heard a song), {@code ench:<id>} (held an
 * enchantment), {@code lore:<id>} (read a Lore Book or Scroll), {@code quest:<id>} and {@code quest:<id>:done} (the
 * Mini Creator's goals) and {@code guide:*} (the guide's own bookkeeping).
 *
 * <p>In creative everything in the Knowledge Book counts as discovered ({@link #knows}), but nothing is written down.
 */
public final class Knowledge {
    private static List<String> cachedList = List.of();
    private static Set<String> cachedSet = Set.of();

    private Knowledge() {
    }

    public static List<String> all(Player player) {
        List<String> l = player.getExistingDataOrNull(ModKnowledge.KNOWLEDGE.get());
        return l == null ? List.of() : l;
    }

    /** The player's keys as a set (cached by list identity: the attachment is replaced, never changed, on every unlock). */
    public static synchronized Set<String> set(Player player) {
        List<String> l = all(player);
        if (l != cachedList) {
            cachedList = l;
            cachedSet = new HashSet<>(l);
        }
        return cachedSet;
    }

    /** True if this player has recorded {@code key}. */
    public static boolean has(Player player, String key) {
        return set(player).contains(key);
    }

    /** What the Knowledge Book shows as discovered: recorded, or everything in creative. */
    public static boolean knows(Player player, String key) {
        return player.isCreative() || has(player, key);
    }

    public static int count(Player player, String prefix) {
        int n = 0;
        for (String k : all(player)) {
            if (k.startsWith(prefix)) {
                n++;
            }
        }
        return n;
    }

    /** Lore pieces read: the clues that decipher the ancient script. */
    public static int clues(Player player) {
        return count(player, "lore:");
    }

    /**
     * Records a discovery. Returns true if it is new. Announces it on the action bar unless {@code quiet} (items are
     * announced by the client, which knows which items have a page of their own).
     */
    public static boolean unlock(ServerPlayer player, String key, boolean quiet) {
        List<String> old = all(player);
        if (old.contains(key)) {
            return false;
        }
        List<String> next = new ArrayList<>(old.size() + 1);
        next.addAll(old);
        next.add(key);
        player.setData(ModKnowledge.KNOWLEDGE.get(), next);
        if (!quiet) {
            Component name = KnowledgeNames.of(player, key);
            if (name != null) {
                player.sendOverlayMessage(Component.translatable("knowledge.thesift.unlocked", name));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModKnowledge.UNLOCK.get(), SoundSource.PLAYERS, 0.6F, 1.0F);
            }
        }
        return true;
    }

    public static boolean unlock(ServerPlayer player, String key) {
        return unlock(player, key, false);
    }

    /** Forgets a key (the Mini Creator's bookkeeping, and tests). */
    public static void forget(ServerPlayer player, String key) {
        List<String> old = all(player);
        if (old.contains(key)) {
            List<String> next = new ArrayList<>(old);
            next.remove(key);
            player.setData(ModKnowledge.KNOWLEDGE.get(), next);
        }
    }
}
