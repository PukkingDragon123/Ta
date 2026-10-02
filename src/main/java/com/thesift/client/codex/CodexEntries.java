package com.thesift.client.codex;

import com.thesift.entity.Bulb;
import com.thesift.entity.Harmoner;
import com.thesift.entity.Riveter;
import com.thesift.entity.Sifter;
import com.thesift.entity.Slumbler;
import com.thesift.entity.boss.Dictator;
import com.thesift.entity.boss.OrchestraMinion;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModEntities;
import com.thesift.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/** Every page of the Sift Codex, grouped by ribbon. */
public final class CodexEntries {
    public static final int CREATURES = 0;
    public static final int ITEMS = 1;
    public static final int PLACES = 2;
    public static final int MAGIC = 3;
    public static final int DICTATOR = 4;
    public static final String[] CATEGORY_KEYS = {"creatures", "items", "places", "magic", "dictator"};

    private static List<CodexEntry> entries;

    private CodexEntries() {
    }

    public static List<CodexEntry> all() {
        if (entries == null) {
            entries = build();
        }
        return entries;
    }

    public static int firstOf(int category) {
        List<CodexEntry> list = all();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).category() == category) {
                return i;
            }
        }
        return 0;
    }

    private static CodexEntry mob(int cat, String key, Supplier<? extends EntityType<?>> type, BiConsumer<LivingEntity, Integer> anim) {
        return new CodexEntry(cat, key, type, null, anim);
    }

    private static CodexEntry thing(int cat, String key, Supplier<? extends ItemLike> item) {
        return new CodexEntry(cat, key, null, item, null);
    }

    private static List<CodexEntry> build() {
        List<CodexEntry> l = new ArrayList<>();
        // ---- creatures
        l.add(mob(CREATURES, "bulb", ModEntities.BULB, (e, t) -> {
            if (e instanceof Bulb b && t % 60 == 0) b.setVariant((t / 60) % Bulb.VARIANTS);
        }));
        l.add(mob(CREATURES, "harmoner", ModEntities.HARMONER, (e, t) -> {
            if (e instanceof Harmoner h && t % 45 == 0) {
                h.setVariant((t / 45) % Harmoner.VARIANTS);
                h.singAnimation.start(h.tickCount);
            }
        }));
        l.add(mob(CREATURES, "sniffer", () -> net.minecraft.world.entity.EntityTypes.SNIFFER, (e, t) -> { }));
        l.add(mob(CREATURES, "enchoer", ModEntities.ENCHOER, (e, t) -> { }));
        // A2 Echoer: its household and the meadow wisps
        l.add(mob(CREATURES, "soul_golem", com.thesift.registry.ModEchoer.SOUL_GOLEM, (e, t) -> { }));
        l.add(mob(CREATURES, "nib", com.thesift.registry.ModEchoer.NIB, (e, t) -> { }));
        l.add(mob(CREATURES, "slumbler", ModEntities.SLUMBLER, (e, t) -> {
            if (e instanceof Slumbler s && t % 120 == 20) s.yawnAnimation.start(s.tickCount);
        }));
        l.add(mob(CREATURES, "sifter", ModEntities.SIFTER, (e, t) -> {
            if (e instanceof Sifter s && t % 50 == 10) s.chompAnimation.start(s.tickCount);
        }));
        l.add(mob(CREATURES, "riveter", ModEntities.RIVETER, (e, t) -> {
            if (e instanceof Riveter r && t % 90 == 20) r.screamAnimation.start(r.tickCount);
        }));
        // ---- the wild creatures
        l.add(mob(CREATURES, "stomper", ModEntities.STOMPER, (e, t) -> {
            if (e instanceof com.thesift.entity.Stomper s) {
                if (t == 1) {
                    s.setChrome(1.0F);
                }
                switch (t % 160) {
                    case 10 -> s.puffAnimation.start(s.tickCount);
                    case 40 -> s.sprayAnimation.start(s.tickCount);
                    case 100 -> s.drinkAnimation.start(s.tickCount);
                    case 150 -> s.puffAnimation.start(s.tickCount);
                    default -> { }
                }
            }
        }));
        l.add(mob(CREATURES, "sky_whale", ModEntities.SKY_WHALE, (e, t) -> {
            if (e instanceof com.thesift.entity.SkyWhale w && t % 120 == 10) w.singAnimation.start(w.tickCount);
        }));
        l.add(mob(CREATURES, "fanfare_eel", ModEntities.FANFARE_EEL, (e, t) -> {
            if (e instanceof com.thesift.entity.FanfareEel f && t % 50 == 10) f.biteAnimation.start(f.tickCount);
        }));
        l.add(mob(CREATURES, "kazoo_fish", ModEntities.KAZOO_FISH, (e, t) -> { }));
        l.add(mob(CREATURES, "tubafish", ModEntities.TUBAFISH, (e, t) -> {
            if (e instanceof com.thesift.entity.Tubafish f) {
                // puff up and down on the page
                f.puffO = f.puff;
                float target = (t / 60) % 2 == 1 ? 1.0F : 0.0F;
                f.puff += (target - f.puff) * (target > f.puff ? 0.35F : 0.06F);
            }
        }));
        // C: the Caravans, tapping out the Crystal Hymn on the page
        l.add(mob(CREATURES, "caravan", com.thesift.registry.ModCaravans.CARAVAN, (e, t) -> {
            if (e instanceof com.thesift.entity.caravan.Caravan c && t % 16 == 4) c.tapAnimation.start(c.tickCount);
        }));
        // sea & sky: the blind deep-sea catfish, the two water seas and the cloud garden, sushi
        l.add(mob(CREATURES, "gobbler", com.thesift.registry.ModSeaSky.GOBBLER, (e, t) -> {
            if (e instanceof com.thesift.entity.Gobbler g && t % 70 == 10) g.lungeAnimation.start(g.tickCount);
        }));
        l.add(thing(PLACES, "sea_and_sky", ModItems.ROSE_GLOWKELP));
        l.add(thing(ITEMS, "sushi", ModItems.SUSHI_PLATTER));
        // ---- items
        l.add(thing(ITEMS, "bubble_gun", ModItems.BUBBLE_GUN));
        l.add(thing(ITEMS, "prism", ModItems.PRISM_GEM)); // C
        l.add(thing(ITEMS, "skysong_gem", ModItems.SKYSONG_GEM));
        l.add(thing(ITEMS, "siftite", ModItems.SIFTITE_PICKAXE));
        l.add(thing(ITEMS, "slingshot", ModItems.SLINGSHOT));
        l.add(thing(ITEMS, "chrome", ModItems.CHROME_BUCKET));
        l.add(thing(ITEMS, "warden_core", ModItems.WARDEN_CORE));
        l.add(thing(ITEMS, "sift_cake", ModItems.SIFT_CAKE));
        l.add(thing(ITEMS, "baton", ModItems.CONDUCTORS_BATON));
        l.add(thing(ITEMS, "staff", ModItems.CONDUCTORS_STAFF));
        // songs & instruments (agent D)
        l.add(thing(ITEMS, "songs", ModItems.MUSIC_SHEET_LULLABY));
        l.add(thing(ITEMS, "prism_instruments", ModItems.PRISM_HARP));
        // ---- places
        l.add(thing(PLACES, "portal", ModItems.SIFT_DRUM));
        l.add(thing(PLACES, "musical_temple", ModItems.HARMONY_STONE));
        l.add(thing(PLACES, "chrome_well", ModItems.CHROME_PEARL));
        l.add(thing(PLACES, "ruins", ModItems.DREAM_JOURNAL_FRAGMENT));
        l.add(thing(PLACES, "deep_shrine", ModItems.SIFT_GATE_FRAME));
        l.add(thing(PLACES, "sculk_castle", ModItems.CONDUCTORS_PODIUM));
        l.add(thing(PLACES, "caravan_colony", ModItems.PRISM_BLOCK)); // C
        l.add(thing(PLACES, "echoer_hut", ModItems.SOUL_GOLEM_CORE)); // A2 Echoer
        // ---- blocks & magic
        l.add(thing(MAGIC, "sift_drum", ModItems.SIFT_DRUM));
        l.add(thing(MAGIC, "euphory_altar", ModItems.EUPHORY_ALTAR));
        l.add(thing(MAGIC, "echoer_device", ModItems.ECHOER_DEVICE)); // A2 Echoer
        l.add(thing(MAGIC, "music", ModItems.SOUL_CHIME));
        l.add(thing(MAGIC, "music_crystal", ModItems.MUSIC_CRYSTAL)); // C
        l.add(thing(MAGIC, "flora", () -> ModBlocks.CORAL_BUSH.get().asItem()));
        // H: where the Sift's seeds grow, and the potted pitchers that make its soups
        l.add(thing(MAGIC, "sift_gardening", ModItems.ECHO_SEED));
        l.add(thing(MAGIC, "pitcher_planter", ModItems.PITCHER_PLANTER));
        // ---- the Dictator
        l.add(mob(DICTATOR, "dictator", ModEntities.DICTATOR, (e, t) -> {
            if (e instanceof Dictator d) {
                switch (t % 160) {
                    case 10 -> d.slashAnimation.start(d.tickCount);
                    case 50 -> d.summonAnimation.start(d.tickCount);
                    case 90 -> d.crescendoAnimation.start(d.tickCount);
                    default -> { }
                }
            }
        }));
        BiConsumer<LivingEntity, Integer> attack = (e, t) -> {
            if (e instanceof OrchestraMinion m && t % 70 == 15) m.attackAnimation.start(m.tickCount);
        };
        BiConsumer<LivingEntity, Integer> perform = (e, t) -> {
            // the page acts out its attacks one after another
            if (e instanceof com.thesift.entity.boss.MiniBoss b && t % 90 == 5) {
                b.codexPose((t / 90) % 4 + 1);
            }
        };
        l.add(mob(DICTATOR, "thumper", ModEntities.THUMPER, perform));
        l.add(mob(DICTATOR, "strummer", ModEntities.STRUMMER, perform));
        l.add(mob(DICTATOR, "strumling", ModEntities.STRUMLING, attack));
        l.add(mob(DICTATOR, "sculk_parasite", ModEntities.SCULK_PARASITE, attack));
        l.add(thing(DICTATOR, "stage", ModItems.INSTRUMENT_ALTAR));
        l.add(thing(DICTATOR, "sculk_corruption", () -> Items.SCULK_VEIN));
        l.add(thing(DICTATOR, "encore_sigil", ModItems.ENCORE_SIGIL));
        l.add(thing(DICTATOR, "ancient_cannon", ModItems.ANCIENT_CANNON));
        l.add(thing(DICTATOR, "conga_drum", ModItems.CONGA_DRUM));
        l.add(thing(DICTATOR, "crane_flute", ModItems.CRANE_FLUTE));
        l.add(thing(DICTATOR, "guitar", ModItems.GUITAR));
        // the Weaver (E2)
        l.add(thing(DICTATOR, "weaver_guitar", ModItems.WEAVER_GUITAR));
        l.add(thing(DICTATOR, "musical_cobweb", ModItems.MUSICAL_COBWEB));
        l.add(thing(DICTATOR, "vocals", () -> Items.SCULK_SHRIEKER));
        return l;
    }
}
