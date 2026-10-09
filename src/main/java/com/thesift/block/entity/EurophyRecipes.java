package com.thesift.block.entity;

import com.thesift.registry.ModEurophy;
import com.thesift.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jspecify.annotations.Nullable;

/**
 * What the Europhy Table makes. Ingredients sit on its four arms in any order; each recipe names how many
 * different notes in a row (repeats do not count) wind the table up, and whether a living helper creature must
 * be near. A whole song played close by winds it up at once.
 */
public final class EurophyRecipes {
    /** One ingredient: what it accepts and how many of it one craft uses. */
    public record Input(Predicate<ItemStack> test, int count) {
    }

    /** Makes the output from the matched inputs (in recipe order); level is null for the client's preview. */
    @FunctionalInterface
    public interface Result {
        ItemStack make(List<ItemStack> inputs, @Nullable ServerLevel level, int quality);
    }

    public record Recipe(String id, List<Input> inputs, int notes, @Nullable TagKey<EntityType<?>> helper, Result result) {
    }

    /** A recipe and, for each of its inputs, the table slot holding it. */
    public record Match(Recipe recipe, int[] slots) {
        public List<ItemStack> inputs(List<ItemStack> table) {
            List<ItemStack> l = new ArrayList<>();
            for (int s : this.slots) {
                l.add(table.get(s));
            }
            return l;
        }

        public ItemStack preview(List<ItemStack> table) {
            return this.recipe.result().make(this.inputs(table), null, 0);
        }
    }

    /** Siftite: four Siftite Dust and a Copper Ingot. */
    public static final Recipe SIFTITE_INGOT = new Recipe("siftite_ingot",
            List.of(new Input(s -> s.is(ModItems.SIFTITE_DUST.get()), 4), new Input(s -> s.is(Items.COPPER_INGOT), 1)), 8, null,
            (in, level, q) -> new ItemStack(ModItems.SIFTITE_INGOT.get()));
    /** Stable Bauxite: Bauxite settled with Nib Dust, while a Nib (or Glow Fly) is close. */
    public static final Recipe STABLE_BAUXITE = new Recipe("stable_bauxite",
            List.of(new Input(s -> s.is(ModItems.BAUXITE.get()), 1), new Input(s -> s.is(ModItems.NIB_DUST.get()), 1)), 6,
            ModEurophy.BAUXITE_STABILIZERS, (in, level, q) -> new ItemStack(ModItems.STABLE_BAUXITE.get()));
    /** The Euphory Altar's old ritual: an item laid beside a Chrome Pearl comes out enchanted - the more varied the tune, the better. */
    public static final Recipe EUPHORIC_INFUSION = new Recipe("euphoric_infusion",
            List.of(new Input(s -> (s.isEnchantable() || s.is(Items.BOOK)) && !s.isEnchanted(), 1), new Input(s -> s.is(ModItems.CHROME_PEARL.get()), 1)),
            10, null, EurophyRecipes::infuse);

    public static final List<Recipe> ALL = List.of(SIFTITE_INGOT, STABLE_BAUXITE, EUPHORIC_INFUSION);

    private EurophyRecipes() {
    }

    /** The recipe the four ingredient stacks make (every non-empty stack must be used), or null. */
    public static @Nullable Match match(List<ItemStack> table) {
        for (Recipe r : ALL) {
            int[] slots = new int[r.inputs().size()];
            if (assign(r, table, 0, slots, new boolean[table.size()])) {
                return new Match(r, slots);
            }
        }
        return null;
    }

    private static boolean assign(Recipe r, List<ItemStack> table, int i, int[] slots, boolean[] used) {
        if (i == r.inputs().size()) {
            for (int s = 0; s < table.size(); s++) {
                if (!used[s] && !table.get(s).isEmpty()) {
                    return false;
                }
            }
            return true;
        }
        Input in = r.inputs().get(i);
        for (int s = 0; s < table.size(); s++) {
            ItemStack st = table.get(s);
            if (!used[s] && !st.isEmpty() && st.getCount() >= in.count() && in.test().test(st)) {
                used[s] = true;
                slots[i] = s;
                if (assign(r, table, i + 1, slots, used)) {
                    return true;
                }
                used[s] = false;
            }
        }
        return false;
    }

    /** quality: how many different notes the tune used (a whole song counts as 25). */
    private static ItemStack infuse(List<ItemStack> in, @Nullable ServerLevel level, int quality) {
        ItemStack item = in.get(0).copyWithCount(1);
        if (level == null) {
            return item;
        }
        int power = Math.min(40, 10 + quality * 2);
        Optional<HolderSet.Named<Enchantment>> pool = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(EnchantmentTags.IN_ENCHANTING_TABLE);
        Stream<Holder<Enchantment>> stream = pool.map(HolderSet::stream).orElseGet(Stream::empty);
        ItemStack result = EnchantmentHelper.enchantItem(level.getRandom(), item, power, stream);
        // a whole song lifts one enchantment a level past its usual best, as the old altar's euphoric bonus did
        if (quality >= 25) {
            ItemEnchantments ench = EnchantmentHelper.getEnchantmentsForCrafting(result);
            List<Holder<Enchantment>> keys = new ArrayList<>(ench.keySet());
            if (!keys.isEmpty()) {
                Holder<Enchantment> pick = keys.get(level.getRandom().nextInt(keys.size()));
                int lvl = ench.getLevel(pick);
                EnchantmentHelper.updateEnchantments(result, m -> m.set(pick, Math.min(255, lvl + 1)));
            }
        }
        return result;
    }
}
