package com.thesift.client.renderer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * E1 the rot overlay of a creature texture: tools/sift_sniffer.py paints a {@code _rot} twin next
 * to every creature texture (the expressions share their base texture's, the silhouette is the
 * same). Looked up once per texture; a texture without a twin simply shows no blotches.
 */
final class RotTextures {
    private static final String[] EXPRESSIONS = {"_blink", "_happy", "_angry", "_hurt", "_sleep", "_dead"};
    private static final Map<Identifier, Optional<Identifier>> CACHE = new HashMap<>();

    private RotTextures() {
    }

    static @Nullable Identifier of(Identifier texture) {
        return CACHE.computeIfAbsent(texture, RotTextures::find).orElse(null);
    }

    private static Optional<Identifier> find(Identifier texture) {
        String path = texture.getPath();
        if (!path.endsWith(".png")) {
            return Optional.empty();
        }
        String stem = path.substring(0, path.length() - 4);
        for (String e : EXPRESSIONS) {
            if (stem.endsWith(e)) {
                stem = stem.substring(0, stem.length() - e.length());
                break;
            }
        }
        Identifier rot = Identifier.fromNamespaceAndPath(texture.getNamespace(), stem + "_rot.png");
        return Minecraft.getInstance().getResourceManager().getResource(rot).isPresent() ? Optional.of(rot) : Optional.empty();
    }
}
