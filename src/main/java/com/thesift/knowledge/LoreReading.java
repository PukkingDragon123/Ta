package com.thesift.knowledge;

import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * F3: reading a Lore Book or Scroll, from the item or from a placed book. The client opens the reading screen (the
 * client side installs {@link #clientOpen}); the server records the piece ({@link KnowledgeTracker#read}).
 */
public final class LoreReading {
    /** The enchanting-table glyphs. */
    public static final FontDescription ALT = new FontDescription.Resource(Identifier.withDefaultNamespace("alt"));
    public static final Style GLYPHS = Style.EMPTY.withFont(ALT);

    public static volatile Consumer<Lore> clientOpen = l -> {
    };
    /** On the client: whether the local player has read a piece (for tooltips). */
    public static volatile Predicate<String> clientKnows = k -> true;

    private LoreReading() {
    }

    public static void read(Level level, Player player, @Nullable Lore lore) {
        if (lore == null) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("lore.thesift.blank"));
            }
            return;
        }
        if (level.isClientSide()) {
            clientOpen.accept(lore);
        } else if (player instanceof ServerPlayer sp) {
            KnowledgeTracker.read(sp, lore);
        }
    }

    /** The piece's title for a tooltip: in the old script until it has been read, if its writer wrote in it. */
    public static MutableComponent title(Lore lore) {
        MutableComponent t = Component.translatable(lore.titleKey());
        if (lore.origin.script >= 0.9F && !clientKnows.test(lore.key())) {
            return t.withStyle(GLYPHS).withStyle(ChatFormatting.DARK_AQUA);
        }
        return t.withColor(lore.origin.accent & 0xFFFFFF);
    }
}
