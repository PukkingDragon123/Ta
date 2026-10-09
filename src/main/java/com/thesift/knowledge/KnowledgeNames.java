package com.thesift.knowledge;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jspecify.annotations.Nullable;

/** F3: the name of a discovery, for the "Knowledge Book: ..." line. Null for keys that are not announced this way. */
public final class KnowledgeNames {
    private KnowledgeNames() {
    }

    public static @Nullable Component of(Player player, String key) {
        int colon = key.indexOf(':');
        if (colon < 0) {
            return null;
        }
        String kind = key.substring(0, colon);
        String rest = key.substring(colon + 1);
        Identifier id = Identifier.tryParse(rest);
        return switch (kind) {
            case "entity" -> id == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(id).map(t -> t.getDescription()).orElse(null);
            case "biome" -> id == null ? null : Component.translatable("biome." + id.getNamespace() + "." + id.getPath());
            case "structure" -> id == null ? null : Component.translatableWithFallback("structure." + id.getNamespace() + "." + id.getPath(), id.getPath());
            case "dim" -> id == null ? null : Component.translatableWithFallback("dimension." + id.getNamespace() + "." + id.getPath(), "The Sift");
            case "lore" -> Component.translatable("lore.thesift." + rest + ".title");
            case "song" -> Component.translatable("song.thesift." + rest);
            case "ench" -> id == null ? null : player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOptional(id)
                    .map(Enchantment::description).orElse(null);
            default -> null;
        };
    }
}
