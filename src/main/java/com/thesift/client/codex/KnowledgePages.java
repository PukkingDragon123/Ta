package com.thesift.client.codex;

import com.thesift.TheSift;
import com.thesift.knowledge.Knowledge;
import com.thesift.knowledge.Lore;
import com.thesift.knowledge.Quest;
import com.thesift.music.Song;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.ItemLike;
import org.jspecify.annotations.Nullable;

/**
 * F3: the Knowledge Book's pages, in reading order. Each chapter opens with a contents spread; then come its pages:
 * the written entries ({@link CodexEntries}), a page per song, per Sift enchantment, per piece of lore and per Mini
 * Creator goal, and (in Items and Recipes) every Sift recipe you know, four to a spread.
 *
 * <p>A page is discovered when any of its knowledge keys is ({@link Knowledge}); some pages are known from the start.
 */
final class KnowledgePages {
    enum Kind { CONTENTS, ENTRY, SONG, ENCHANT, LORE, QUEST, RECIPES }

    /** One spread of the book. {@code keys} empty: always known. */
    record Page(Kind kind, int chapter, @Nullable CodexEntry entry, @Nullable Song song, @Nullable Holder<Enchantment> enchant,
            @Nullable Lore lore, @Nullable Quest quest, List<RecipeDisplayEntry> recipes, String[] keys) {
        String id() {
            return switch (this.kind) {
                case CONTENTS -> "contents_" + this.chapter;
                case ENTRY -> this.entry.key();
                case SONG -> "song_" + this.song.id();
                case ENCHANT -> "ench_" + this.enchant.getRegisteredName();
                case LORE -> "lore_" + this.lore.id();
                case QUEST -> "quest_" + this.quest.id();
                case RECIPES -> "recipes_" + this.chapter;
            };
        }
    }

    /** Always-known pages: the basics of getting into and around the Sift. */
    private static final String[] ALWAYS = {"portal", "music", "band", "instruments", "songs", "sift_drum", "warden_core", "sculk_corruption", "vocals",
            "prism_instruments"};
    /** Pages recorded by being somewhere rather than by holding their icon. */
    private static final Map<String, String[]> KEYS = new HashMap<>();

    static {
        String sift = "dim:thesift:the_sift";
        KEYS.put("sea_and_sky", new String[]{"biome:thesift:magic_kelp_forest", "biome:thesift:deep_dark_ocean"});
        KEYS.put("white_forest", new String[]{"biome:thesift:white_forest"});
        KEYS.put("sculk_swamp", new String[]{"biome:thesift:sculk_swamp"});
        KEYS.put("sculk_ocean", new String[]{"biome:thesift:deep_dark_ocean"});
        KEYS.put("relics", new String[]{"item:thesift:suspicious_dreamsand", sift});
        KEYS.put("sculk_castle", new String[]{"structure:thesift:sculk_castle"});
        KEYS.put("stage", new String[]{"structure:thesift:sculk_castle"});
        KEYS.put("caravan_colony", new String[]{"structure:thesift:caravan_colony"});
        KEYS.put("sift_sky", new String[]{sift});
        KEYS.put("flora", new String[]{"biome:thesift:sift_plains", "item:thesift:coral_bush"});
        KEYS.put("chrome", new String[]{"biome:thesift:chrome_lakes", "item:thesift:chrome_bucket"});
        KEYS.put("siftite", new String[]{"item:thesift:siftite_ingot", "item:thesift:siftite_pickaxe"});
        KEYS.put("sushi", new String[]{"item:thesift:sushi_platter", "item:thesift:kazoo_fish_sushi", "item:thesift:tubafish_sushi"});
        for (String b : new String[]{"sift_plains", "wishing_grove", "forest_mountains", "chrome_lakes", "rocky_dunes", "caravans_cavern",
                "deep_sift"}) {
            KEYS.put(b, new String[]{"biome:thesift:" + b});
        }
        { // W-sky: the Sound Garden became the Sky Island biome; its page is "sky_islands"
            KEYS.put("sky_islands", new String[]{"biome:thesift:sky_island"});
        }
        KEYS.put("drum_pit", new String[]{"structure:thesift:thumper_arena"});
    }

    private KnowledgePages() {
    }

    static String[] keysOf(CodexEntry e) {
        for (String a : ALWAYS) {
            if (a.equals(e.key())) {
                return new String[0];
            }
        }
        String[] k = KEYS.get(e.key());
        if (k != null) {
            return k;
        }
        if (e.entity() != null) {
            return new String[]{"entity:" + BuiltInRegistries.ENTITY_TYPE.getKey(e.entity().get())};
        }
        if (e.item() != null) {
            Item item = e.item().get().asItem();
            return new String[]{"item:" + BuiltInRegistries.ITEM.getKey(item)};
        }
        return new String[0];
    }

    static boolean known(Player player, Page p) {
        if (p.keys.length == 0 || player.isCreative()) {
            return true;
        }
        for (String k : p.keys) {
            if (Knowledge.has(player, k)) {
                return true;
            }
        }
        return false;
    }

    /** The kind of key a locked page waits for, for its hint. */
    static String hintKind(Page p) {
        if (p.keys.length == 0) {
            return "item";
        }
        String k = p.keys[0];
        return k.substring(0, Math.max(0, k.indexOf(':')));
    }

    static Component title(Page p) {
        return switch (p.kind) {
            case CONTENTS, RECIPES -> Component.translatable(p.kind == Kind.RECIPES ? "knowledge.thesift.recipes.title"
                    : "knowledge.thesift.chapter." + CodexEntries.CATEGORY_KEYS[p.chapter]);
            case ENTRY -> Component.translatable(p.entry.titleKey());
            case SONG -> Component.translatable("song.thesift." + p.song.id());
            case ENCHANT -> p.enchant.value().description();
            case LORE -> Component.translatable(p.lore.titleKey());
            case QUEST -> Component.translatable("quest.thesift." + p.quest.id() + ".title");
        };
    }

    /** The whole book for this player, chapter by chapter in ribbon order. */
    static List<Page> build(Player player) {
        List<Page> pages = new ArrayList<>();
        for (int chapter : CodexEntries.ORDER) {
            pages.add(new Page(Kind.CONTENTS, chapter, null, null, null, null, null, List.of(), new String[0]));
            for (CodexEntry e : CodexEntries.all()) {
                if (e.category() == chapter) {
                    pages.add(new Page(Kind.ENTRY, chapter, e, null, null, null, null, List.of(), keysOf(e)));
                }
            }
            switch (chapter) {
                case CodexEntries.SONGS -> {
                    for (Song s : Song.values()) {
                        pages.add(new Page(Kind.SONG, chapter, null, s, null, null, null, List.of(),
                                new String[]{"song:" + s.id(), "item:thesift:music_sheet_" + s.id()}));
                    }
                }
                case CodexEntries.ENCHANTMENTS -> {
                    if (player.level() != null) {
                        player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements()
                                .filter(h -> h.key().identifier().getNamespace().equals(TheSift.MODID))
                                .sorted((a, b) -> a.key().identifier().compareTo(b.key().identifier()))
                                .forEach(h -> pages.add(new Page(Kind.ENCHANT, chapter, null, null, h, null, null, List.of(),
                                        new String[]{"ench:" + h.key().identifier()})));
                    }
                }
                case CodexEntries.LORE -> {
                    for (Lore l : Lore.values()) {
                        pages.add(new Page(Kind.LORE, chapter, null, null, null, l, null, List.of(), new String[]{l.key()}));
                    }
                }
                case CodexEntries.GUIDE -> {
                    for (Quest q : Quest.values()) {
                        pages.add(new Page(Kind.QUEST, chapter, null, null, null, null, q, List.of(), new String[]{q.key()}));
                    }
                }
                case CodexEntries.ITEMS -> {
                    List<RecipeDisplayEntry> known = knownRecipes();
                    for (int i = 0; i < known.size(); i += 4) {
                        pages.add(new Page(Kind.RECIPES, chapter, null, null, null, null, null, known.subList(i, Math.min(known.size(), i + 4)),
                                new String[0]));
                    }
                    if (known.isEmpty()) {
                        pages.add(new Page(Kind.RECIPES, chapter, null, null, null, null, null, List.of(), new String[0]));
                    }
                }
                default -> {
                }
            }
        }
        return pages;
    }

    /** Every Sift recipe this player knows (from their recipe book), by result. */
    static List<RecipeDisplayEntry> knownRecipes() {
        Minecraft mc = Minecraft.getInstance();
        List<RecipeDisplayEntry> out = new ArrayList<>();
        if (mc.player == null || mc.level == null) {
            return out;
        }
        ContextMap ctx = SlotDisplayContext.fromLevel(mc.level);
        for (RecipeCollection c : mc.player.getRecipeBook().getCollections()) {
            for (RecipeDisplayEntry e : c.getRecipes()) {
                ItemStack result = e.display().result().resolveForFirstStack(ctx);
                if (!result.isEmpty() && BuiltInRegistries.ITEM.getKey(result.getItem()).getNamespace().equals(TheSift.MODID)) {
                    out.add(e);
                }
            }
        }
        out.sort((a, b) -> BuiltInRegistries.ITEM.getKey(a.display().result().resolveForFirstStack(ctx).getItem())
                .compareTo(BuiltInRegistries.ITEM.getKey(b.display().result().resolveForFirstStack(ctx).getItem())));
        return out;
    }

    /** A known recipe that makes this item, for its entry page. */
    static @Nullable RecipeDisplayEntry recipeFor(ItemLike item) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return null;
        }
        ContextMap ctx = SlotDisplayContext.fromLevel(mc.level);
        for (RecipeCollection c : mc.player.getRecipeBook().getCollections()) {
            for (RecipeDisplayEntry e : c.getRecipes()) {
                if (e.display().result().resolveForFirstStack(ctx).is(item.asItem())) {
                    return e;
                }
            }
        }
        return null;
    }

    /** The page (if any) an item key belongs to, for announcing new discoveries. */
    static @Nullable Component pageFor(String key) {
        for (CodexEntry e : CodexEntries.all()) {
            for (String k : keysOf(e)) {
                if (k.equals(key)) {
                    return Component.translatable(e.titleKey());
                }
            }
        }
        if (key.startsWith("item:thesift:music_sheet_")) {
            return Component.translatable("song.thesift." + key.substring("item:thesift:music_sheet_".length()));
        }
        return null;
    }

    static Identifier ribbonTexture() {
        return TheSift.id("textures/gui/knowledge_book.png");
    }
}
