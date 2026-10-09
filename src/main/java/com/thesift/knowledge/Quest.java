package com.thesift.knowledge;

import com.thesift.TheSift;
import com.thesift.item.MusicSheetItem;
import java.util.Locale;
import java.util.function.Predicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * F3: the Mini Creator's quest lines, in order. Each points at the next big goal of the Sift; the tracker checks the
 * current one every couple of seconds and, when it is met, the Mini Creator pops up to cheer and give the next.
 * Text: {@code quest.thesift.<id>.title/.goal/.line/.done} (tools/knowledge.py, same order).
 */
public enum Quest {
    ARRIVAL(p -> true),
    INSTRUMENT(p -> carries(p, s -> s.is(Quest.INSTRUMENTS))),
    SHEET(p -> carries(p, s -> s.getItem() instanceof MusicSheetItem) || Knowledge.count(p, "song:") > 0),
    PERFORM(p -> Knowledge.count(p, "song:") > 0),
    EUROPHY(p -> Knowledge.has(p, "item:thesift:europhy_table") || Knowledge.has(p, "item:thesift:euphory_altar")),
    LORE(p -> Knowledge.clues(p) >= 3),
    HERALDS(p -> Quest.metHerald(p)),
    SOUL(p -> Knowledge.has(p, "dim:thesift:soul_dimension"));

    public static final TagKey<Item> INSTRUMENTS = TagKey.create(Registries.ITEM, TheSift.id("instruments"));
    /** The Heralds and the Conductor (some still to come): meeting any of them completes the Heralds goal. */
    private static final String[] HERALDS_IDS = {"dictator", "thumper", "strummer", "crocodile", "owl", "octopus", "weaver", "conductor"};

    private final Predicate<ServerPlayer> done;

    Quest(Predicate<ServerPlayer> done) {
        this.done = done;
    }

    public String id() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    /** Knowledge key set when the Mini Creator gives you this goal. */
    public String key() {
        return "quest:" + this.id();
    }

    /** Knowledge key set when you meet it. */
    public String doneKey() {
        return "quest:" + this.id() + ":done";
    }

    public boolean met(ServerPlayer player) {
        return this.done.test(player);
    }

    public boolean hasDoneLine() {
        return this != ARRIVAL && this != SOUL;
    }

    /** The goal the player is on now: the first one given and not yet done (null before the Mini Creator was met). */
    public static Quest current(net.minecraft.world.entity.player.Player player) {
        for (Quest q : values()) {
            if (!Knowledge.has(player, q.doneKey())) {
                return q;
            }
        }
        return SOUL;
    }

    private static boolean carries(ServerPlayer p, Predicate<ItemStack> test) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (test.test(p.getInventory().getItem(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean metHerald(ServerPlayer p) {
        for (String id : HERALDS_IDS) {
            if (Knowledge.has(p, "entity:thesift:" + id)) {
                return true;
            }
        }
        return false;
    }
}
