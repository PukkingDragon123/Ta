package com.thesift.client.codex;

import com.thesift.entity.Bulb;
import com.thesift.entity.Harmoner;
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

/**
 * Every written page of the Knowledge Book (it grew out of the Sift Codex), grouped by chapter ribbon. The Songs,
 * Enchantments, Lore and Guide chapters (F3) are built from the songs, the enchantment registry, the lore and the
 * Mini Creator's quests themselves (see KnowledgePages); the Items chapter also lists every recipe you know.
 */
public final class CodexEntries {
    public static final int CREATURES = 0;
    public static final int ITEMS = 1;
    public static final int PLACES = 2;
    public static final int MAGIC = 3;
    public static final int DICTATOR = 4;
    // F3 Knowledge and lore: the chapters built by KnowledgePages
    public static final int SONGS = 5;
    public static final int ENCHANTMENTS = 6;
    public static final int LORE = 7;
    public static final int GUIDE = 8;
    /** Lang keys {@code knowledge.thesift.chapter.<key>} by chapter. */
    public static final String[] CATEGORY_KEYS = {"creatures", "recipes", "structures", "machines", "heralds", "songs", "enchantments", "lore", "guide"};
    /** The order of the ribbons along the top of the book. */
    public static final int[] ORDER = {CREATURES, SONGS, ENCHANTMENTS, LORE, PLACES, ITEMS, MAGIC, DICTATOR, GUIDE};

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

    /** CR3: a fish on its page turns through its colour variants, one every three seconds. */
    private static void fishColours(LivingEntity e, int t) {
        if (e instanceof com.thesift.entity.SiftFish f && t % 60 == 0) {
            f.setVariant((t / 60) % f.variantCount());
        }
    }

    private static List<CodexEntry> build() {
        List<CodexEntry> l = new ArrayList<>();
        // ---- creatures
        l.add(mob(CREATURES, "bulb", ModEntities.BULB, (e, t) -> {
            if (e instanceof Bulb b && t % 60 == 0) b.codexPose(t / 60);
        }));
        l.add(thing(ITEMS, "sculk_bloom", ModItems.SCULK_BLOOM)); // A1: the Bulbs' rare back flower
        l.add(mob(CREATURES, "harmoner", ModEntities.HARMONER, (e, t) -> {
            if (e instanceof Harmoner h && t % 45 == 0) {
                h.setVariant((t / 45) % Harmoner.VARIANTS);
                h.singAnimation.start(h.tickCount);
            }
        }));
        l.add(mob(CREATURES, "sniffer", com.thesift.registry.ModSiftSniffer.SIFT_SNIFFER, (e, t) -> { })); // E1: the Sift Sniffer
        l.add(mob(CREATURES, "enchoer", ModEntities.ENCHOER, (e, t) -> {
            if (e instanceof com.thesift.entity.Enchoer en) {
                // CR1: the speaker-bat beats its wings on the page, and every few seconds its drill whirrs round
                en.flapO = en.flap;
                en.flap += 0.55F;
                en.beatO = 1.0F;
                en.beat = 1.0F;
                en.drillO = en.drill;
                en.drill += t % 120 < 50 ? 0.8F : 0.0F;
            }
        }));
        // A2 Echoer: its household and the meadow wisps
        l.add(mob(CREATURES, "soul_golem", com.thesift.registry.ModEchoer.SOUL_GOLEM, (e, t) -> { }));
        l.add(mob(CREATURES, "nib", com.thesift.registry.ModEchoer.NIB, (e, t) -> { }));
        l.add(mob(CREATURES, "slumbler", ModEntities.SLUMBLER, (e, t) -> {
            if (e instanceof Slumbler s) {
                if (t % 120 == 20) {
                    s.yawnAnimation.start(s.tickCount);
                } else if (t % 120 == 80) {
                    s.spitAnimation.start(s.tickCount); // CR2: and spits a gob of Chrome
                }
            }
        }));
        // CR2: the Slumbler's stingray-like tadpole, gliding and snapping on the page
        l.add(mob(CREATURES, "slumbler_tadpole", com.thesift.registry.ModSlumbler.SLUMBLER_TADPOLE, (e, t) -> {
            if (e instanceof com.thesift.entity.slumbler.SlumblerTadpole tp && t % 50 == 15) tp.biteAnimation.start(tp.tickCount);
        }));
        l.add(mob(CREATURES, "sifter", ModEntities.SIFTER, (e, t) -> {
            if (e instanceof Sifter s && t % 50 == 10) s.bonkAnimation.start(s.tickCount); // CR1: the bell rears and swings
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
            fishColours(e, t); // CR3: the fish show off their colour variants on the page
        }));
        l.add(mob(CREATURES, "kazoo_fish", ModEntities.KAZOO_FISH, CodexEntries::fishColours));
        l.add(mob(CREATURES, "tubafish", ModEntities.TUBAFISH, (e, t) -> {
            fishColours(e, t);
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
        // CR2: the Caravan Queen spits gems, swats and rings her shell on the page; a larva snaps; the ore Caravans fill with eggs
        l.add(mob(CREATURES, "caravan_queen", com.thesift.registry.ModCaravans.CARAVAN_QUEEN, (e, t) -> {
            if (e instanceof com.thesift.entity.caravan.CaravanQueen q) {
                switch (t % 120) {
                    case 10 -> q.spitAnimation.start(q.tickCount);
                    case 60 -> q.swatAnimation.start(q.tickCount);
                    case 95 -> q.tapAnimation.start(q.tickCount);
                    default -> { }
                }
            }
        }));
        l.add(mob(CREATURES, "caravan_larva", com.thesift.registry.ModCaravans.CARAVAN_LARVA, (e, t) -> {
            if (e instanceof com.thesift.entity.caravan.CaravanLarva lv && t % 40 == 10) lv.biteAnimation.start(lv.tickCount);
        }));
        l.add(thing(PLACES, "egg_laden_ore", () -> ModBlocks.EGG_LADEN_ORE.get().asItem()));
        // sea & sky: the blind deep-sea catfish, the two water seas and the cloud garden, sushi
        l.add(mob(CREATURES, "gobbler", com.thesift.registry.ModSeaSky.GOBBLER, (e, t) -> {
            if (e instanceof com.thesift.entity.Gobbler g && t % 70 == 10) g.lungeAnimation.start(g.tickCount);
        }));
        l.add(thing(PLACES, "sea_and_sky", ModItems.ROSE_GLOWKELP));
        l.add(thing(PLACES, "sky_islands", ModItems.SKY_VINE)); // W-sky: the islands, their vines and how to swing
        l.add(thing(ITEMS, "sky_fruit", ModItems.SKYRIND)); // W-sky: Driftfruit and Skyrind
        // CR3 Fish & Coral Organs: the Sculk Ocean's biting schools, and its living organs playing, aiming and firing on the page
        l.add(mob(CREATURES, "sculk_fish", com.thesift.registry.ModSculkSea.SCULK_FISH, (e, t) -> {
            fishColours(e, t);
            if (e instanceof com.thesift.entity.SculkFish f && t % 40 == 20) f.biteAnimation.start(f.tickCount);
        }));
        l.add(mob(CREATURES, "coral_organ", com.thesift.registry.ModSculkSea.CORAL_ORGAN, (e, t) -> {
            if (e instanceof com.thesift.entity.CoralOrgan o) {
                switch (t % 140) {
                    case 10 -> o.chordAnimation.start(o.tickCount);
                    case 70 -> o.chargeAnimation.start(o.tickCount);
                    case 98 -> {
                        o.chargeAnimation.stop();
                        o.fireAnimation.start(o.tickCount);
                    }
                    default -> { }
                }
            }
        }));
        // A2 Swifter & White Forest: the three-tailed cloud fox (acting out its poses), and its pale forest
        l.add(mob(CREATURES, "swifter", com.thesift.registry.ModSwifter.SWIFTER, (e, t) -> {
            if (e instanceof com.thesift.entity.Swifter s) s.codexPose(t);
        }));
        l.add(thing(PLACES, "white_forest", ModItems.WHITE_LULLWOOD_SAPLING));
        // W-land: the Rocky Dunes (rock, sand and its two plants), Rainbow Snow and the White Forest's frost flowers
        l.add(thing(PLACES, "rocky_dunes", ModItems.TUNING_CACTUS_BUD));
        l.add(thing(PLACES, "rainbow_snow", ModItems.RAINBOW_SNOW));
        l.add(thing(MAGIC, "frost_flowers", ModItems.HALO_LILY));
        // A4 cave creatures: the Jailer slams its cell down on the page; a Sculkling giggles and covers its ears
        l.add(mob(CREATURES, "jailer", com.thesift.registry.ModCaveCreatures.JAILER, (e, t) -> {
            if (e instanceof com.thesift.entity.cave.Jailer j && t % 80 == 10) j.slamAnimation.start(j.tickCount);
        }));
        l.add(mob(CREATURES, "sculkling", com.thesift.registry.ModCaveCreatures.SCULKLING, (e, t) -> {
            if (e instanceof com.thesift.entity.cave.Sculkling s && t % 60 == 20) s.giggleAnimation.start(s.tickCount);
        }));
        // CR4: the Jailer's Sculkite; the Cypole croaks, crashes its plates and shoots its tongue on the page
        l.add(thing(ITEMS, "sculkite", ModItems.SCULKITE));
        l.add(mob(CREATURES, "cypole", com.thesift.registry.ModCaveCreatures.CYPOLE, (e, t) -> {
            if (e instanceof com.thesift.entity.swamp.Cypole c) c.codexPose(t);
        }));
        l.add(thing(ITEMS, "sushi", ModItems.SUSHI_PLATTER));
        // ---- items
        l.add(thing(ITEMS, "bubble_gun", ModItems.BUBBLE_GUN));
        l.add(thing(ITEMS, "prism", ModItems.PRISM_GEM)); // C
        l.add(thing(ITEMS, "skysong_gem", ModItems.SKYSONG_GEM));
        l.add(thing(ITEMS, "siftite", ModItems.SIFTITE_PICKAXE));
        // F1 Materials: Bauxite, Magnesite, Galena and the soul materials
        l.add(thing(ITEMS, "bauxite", ModItems.BAUXITE));
        l.add(thing(ITEMS, "magnesite", ModItems.MAGNESITE));
        l.add(thing(ITEMS, "galena", ModItems.GALENA));
        l.add(thing(ITEMS, "soul_materials", ModItems.SOUL_CHUNK));
        l.add(thing(ITEMS, "slingshot", ModItems.SLINGSHOT));
        l.add(thing(ITEMS, "chrome", ModItems.CHROME_BUCKET));
        l.add(thing(ITEMS, "warden_core", ModItems.WARDEN_CORE));
        l.add(thing(ITEMS, "sift_cake", ModItems.SIFT_CAKE));
        l.add(thing(ITEMS, "staff", ModItems.CONDUCTORS_STAFF));
        // songs & instruments (agent D)
        l.add(thing(ITEMS, "songs", ModItems.MUSIC_SHEET_LULLABY));
        l.add(thing(ITEMS, "prism_instruments", ModItems.PRISM_HARP));
        l.add(thing(ITEMS, "band", ModItems.WIND_CHIMES)); // M2 band: every creature has a voice and may join your band
        // M1 instrument play: the four ways to play, and the versions you find or craft
        l.add(thing(ITEMS, "instruments", ModItems.STAR_LUTE));
        l.add(thing(ITEMS, "wind_chimes", ModItems.WIND_CHIMES));
        l.add(thing(ITEMS, "star_lute", ModItems.STAR_LUTE));
        l.add(thing(ITEMS, "serbim_flute", ModItems.SERBIM_FLUTE));
        l.add(thing(ITEMS, "thunder_drums", ModItems.THUNDER_DRUMS));
        l.add(thing(ITEMS, "glass_bells", ModItems.GLASS_BELLS));
        l.add(thing(ITEMS, "prism_chimes", ModItems.PRISM_CHIMES));
        // ---- places
        l.add(thing(PLACES, "portal", ModItems.SIFT_DRUM));
        // W1 World & terrain: the Sculk Swamp and the Sculk Ocean; the old ruins live on as buried relics
        l.add(thing(PLACES, "sculk_swamp", ModItems.BLIGHTWOOD_SAPLING));
        l.add(thing(PLACES, "sculk_ocean", ModItems.SCULK_CORAL_FAN));
        // W-sea: the reef that plays itself, and the coral garden at the bottom of the Chrome
        l.add(thing(PLACES, "brass_coral_reef", ModItems.BRASS_TRUMPET_CORAL_BELL));
        l.add(thing(PLACES, "chrome_coral_ocean", ModItems.ROSE_BUBBLE_CORAL_FAN));
        l.add(thing(PLACES, "relics", ModItems.SUSPICIOUS_CHIME_SAND));
        l.add(thing(PLACES, "sculk_castle", ModItems.CONDUCTORS_PODIUM));
        l.add(thing(PLACES, "sift_sky", ModItems.SKYSONG_GEM)); // B1 Portal & sky FX: rainbows, ribbons, shooting stars
        // W-deep caves: the normal caves and their crystal, the Cave Jungle, the Sculk Caves
        l.add(thing(PLACES, "sift_caves", ModItems.POINTED_AZURE_CRYSTAL));
        l.add(thing(PLACES, "crystal_dripstone", ModItems.ROSE_CRYSTAL_CLUSTER));
        l.add(thing(PLACES, "cave_jungle", ModItems.SHOCKER_PLANT));
        l.add(thing(PLACES, "sculk_caves", ModItems.SCULK_GRASPER));
        // ---- blocks & magic
        l.add(thing(MAGIC, "sift_drum", ModItems.SIFT_DRUM));
        l.add(thing(MAGIC, "europhy_table", ModItems.EUROPHY_TABLE)); // F1: crafting by music (replaces the Euphory Altar)
        l.add(thing(MAGIC, "echoer_device", ModItems.ECHOER_DEVICE)); // A2 Echoer
        l.add(thing(MAGIC, "music", ModItems.SOUL_CHIME));
        l.add(thing(MAGIC, "music_crystal", ModItems.MUSIC_CRYSTAL)); // C
        l.add(thing(MAGIC, "band_table", ModItems.BAND_TABLE)); // F2 Band Table
        l.add(thing(MAGIC, "sift_enchantments", () -> Items.ENCHANTED_BOOK)); // F2 Band Table: the Sift enchantments
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
        // F3 Knowledge and lore: the Mini Creator, and the lands of the Sift
        l.add(mob(CREATURES, "mini_creator", com.thesift.registry.ModKnowledge.MINI_CREATOR, (e, t) -> {
            if (e instanceof com.thesift.entity.MiniCreator m && t % 120 == 30) {
                ((t / 120) % 2 == 0 ? m.talkAnimation : m.celebrateAnimation).start(m.tickCount);
            }
        }));
        l.add(thing(PLACES, "sift_plains", ModItems.CORAL_TURF));
        l.add(thing(PLACES, "wishing_grove", ModItems.WISHWOOD_SAPLING));
        l.add(thing(PLACES, "forest_mountains", ModItems.LULLWOOD_SAPLING));
        l.add(thing(PLACES, "chrome_lakes", ModItems.CHROME_REEDS));
        l.add(thing(PLACES, "rocky_dunes", ModItems.CHIME_SAND));
        l.add(thing(PLACES, "caravans_cavern", ModItems.MUSIC_CRYSTAL));
        l.add(thing(PLACES, "deep_sift", ModItems.HUSHSLATE));
        l.add(thing(PLACES, "drum_pit", ModItems.CANNONBALL));
        return l;
    }
}
