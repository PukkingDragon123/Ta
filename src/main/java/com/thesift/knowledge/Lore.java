package com.thesift.knowledge;

import com.thesift.registry.ModKnowledge;
import java.util.Locale;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * F3: the story of the Sift, piece by piece. Each piece is written in a Lore Book or a Lore Scroll of its origin's
 * look, and its text is {@code lore.thesift.<id>.title/.body/.found} in the lang file (tools/knowledge.py, which must
 * list the same ids, origins and kinds). Some of it is in the ancient script ({@link Glyphs}).
 */
public enum Lore {
    CREATOR_RIFTS(Origin.CREATOR, false),
    CREATOR_SONGS(Origin.CREATOR, true),
    CREATOR_HALL(Origin.CREATOR, false),
    CREATOR_KIN(Origin.CREATOR, true),
    PILLAGER_PURGE(Origin.PILLAGER, false),
    PILLAGER_RIFT(Origin.PILLAGER, true),
    PILLAGER_CAMP(Origin.PILLAGER, false),
    PILLAGER_STRANDED(Origin.PILLAGER, true),
    CULTIST_DEAL(Origin.CULTIST, false),
    CULTIST_HERALDS(Origin.CULTIST, true),
    CULTIST_CONDUCTOR(Origin.CULTIST, true),
    OCEAN_REEF(Origin.OCEAN, true),
    OCEAN_OCTOPUS(Origin.OCEAN, false),
    SOUL_SCULK(Origin.SOUL, false),
    SOUL_CREATOR(Origin.SOUL, true);

    /**
     * Who wrote it, and how much of it is in the ancient script: the Creator and the Soul Dimension write in it
     * entirely, the cult and the Tide-Keepers in part, the Pillagers only where they copied the carvings.
     */
    public enum Origin {
        CREATOR(0.92F, 0xFF3A2A12, 0xFF9A7015),
        PILLAGER(0.12F, 0xFF3A2412, 0xFF7A3A1A),
        CULTIST(0.6F, 0xFF10302E, 0xFF1F8A8A),
        OCEAN(0.5F, 0xFF22384E, 0xFFB8505A),
        SOUL(1.0F, 0xFFCFE8FF, 0xFF5FF0FF);

        public final float script;
        public final int ink;
        public final int accent;

        Origin(float script, int ink, int accent) {
            this.script = script;
            this.ink = ink;
            this.accent = accent;
        }

        public String id() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    public final Origin origin;
    public final boolean scroll;

    Lore(Origin origin, boolean scroll) {
        this.origin = origin;
        this.scroll = scroll;
    }

    public String id() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public String key() {
        return "lore:" + this.id();
    }

    public String titleKey() {
        return "lore.thesift." + this.id() + ".title";
    }

    public String bodyKey() {
        return "lore.thesift." + this.id() + ".body";
    }

    public String foundKey() {
        return "lore.thesift." + this.id() + ".found";
    }

    /** The book or scroll this piece is found in. */
    public ItemStack stack() {
        ItemStack s = ModKnowledge.loreStack(this.origin.id(), this.scroll);
        s.set(ModKnowledge.LORE.get(), this.id());
        return s;
    }

    public static @Nullable Lore byId(@Nullable String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        for (Lore l : values()) {
            if (l.id().equals(id)) {
                return l;
            }
        }
        return null;
    }

    /** The piece written in this stack, if any. */
    public static @Nullable Lore of(ItemStack stack) {
        return byId(stack.get(ModKnowledge.LORE.get()));
    }
}
