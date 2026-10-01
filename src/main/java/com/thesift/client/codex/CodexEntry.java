package com.thesift.client.codex;

import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ItemLike;
import org.jspecify.annotations.Nullable;

/**
 * One spread of the Sift Codex. The left page shows the subject - a live, animated creature or a
 * large item - and the right page describes it. Text lives in the lang file under
 * {@code codex.thesift.<key>.title / .tagline / .body}.
 *
 * @param category which ribbon tab the entry sits under
 * @param entity the creature to show, if any
 * @param item the item or block to show when there is no creature
 * @param animator called every tick with the showcase creature and the page's age, to play its
 *        idle, attack and special animations on the page
 */
public record CodexEntry(int category, String key, @Nullable Supplier<? extends EntityType<?>> entity, @Nullable Supplier<? extends ItemLike> item,
        @Nullable BiConsumer<LivingEntity, Integer> animator) {
    public String titleKey() {
        return "codex.thesift." + this.key + ".title";
    }

    public String taglineKey() {
        return "codex.thesift." + this.key + ".tagline";
    }

    public String bodyKey() {
        return "codex.thesift." + this.key + ".body";
    }
}
