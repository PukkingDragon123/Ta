package com.thesift.client.codex;

import com.thesift.entity.Bulb;
import com.thesift.entity.Harmoner;
import com.thesift.entity.Riveter;
import com.thesift.entity.Sifter;
import com.thesift.entity.SiftSniffer;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
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
        l.add(mob(CREATURES, "sift_sniffer", ModEntities.SIFT_SNIFFER, (e, t) -> {
            if (e instanceof SiftSniffer s && t == 1) {
                s.setTame(true);
                s.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
            }
        }));
        l.add(mob(CREATURES, "enchoer", ModEntities.ENCHOER, (e, t) -> { }));
        l.add(mob(CREATURES, "slumbler", ModEntities.SLUMBLER, (e, t) -> {
            if (e instanceof Slumbler s && t % 120 == 20) s.yawnAnimation.start(s.tickCount);
        }));
        l.add(mob(CREATURES, "sifter", ModEntities.SIFTER, (e, t) -> {
            if (e instanceof Sifter s && t % 50 == 10) s.chompAnimation.start(s.tickCount);
        }));
        l.add(mob(CREATURES, "riveter", ModEntities.RIVETER, (e, t) -> {
            if (e instanceof Riveter r && t % 90 == 20) r.screamAnimation.start(r.tickCount);
        }));
        // ---- items
        l.add(thing(ITEMS, "siftite", ModItems.SIFTITE_PICKAXE));
        l.add(thing(ITEMS, "slingshot", ModItems.SLINGSHOT));
        l.add(thing(ITEMS, "chrome", ModItems.CHROME_BUCKET));
        l.add(thing(ITEMS, "warden_core", ModItems.WARDEN_CORE));
        l.add(thing(ITEMS, "sift_cake", ModItems.SIFT_CAKE));
        l.add(thing(ITEMS, "baton", ModItems.CONDUCTORS_BATON));
        // ---- places
        l.add(thing(PLACES, "portal", ModItems.SIFT_DRUM));
        l.add(thing(PLACES, "musical_temple", ModItems.HARMONY_STONE));
        l.add(thing(PLACES, "chrome_well", ModItems.CHROME_PEARL));
        l.add(thing(PLACES, "ruins", ModItems.DREAM_JOURNAL_FRAGMENT));
        l.add(thing(PLACES, "deep_shrine", ModItems.ECHO_FRAME));
        l.add(thing(PLACES, "sculk_castle", ModItems.CONDUCTORS_PODIUM));
        // ---- blocks & magic
        l.add(thing(MAGIC, "euphory_altar", ModItems.EUPHORY_ALTAR));
        l.add(thing(MAGIC, "music", ModItems.SOUL_CHIME));
        l.add(thing(MAGIC, "flora", () -> ModBlocks.CORAL_BUSH.get().asItem()));
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
        l.add(mob(DICTATOR, "enforcer", ModEntities.ENFORCER, attack));
        l.add(mob(DICTATOR, "resonator", ModEntities.RESONATOR, attack));
        l.add(mob(DICTATOR, "howler", ModEntities.HOWLER, attack));
        l.add(thing(DICTATOR, "vocals", () -> Items.SCULK_SHRIEKER));
        return l;
    }
}
