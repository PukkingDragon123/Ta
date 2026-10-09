package com.thesift.client.codex;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/** Client-only entry point for opening the Knowledge Book from the item. */
public final class CodexOpener {
    private CodexOpener() {
    }

    public static void open() {
        Minecraft.getInstance().gui.setScreen(new KnowledgeBookScreen());
    }

    /** F3: the title of the Knowledge Book page a discovery key belongs to, if any. */
    public static @Nullable Component pageFor(String key) {
        return KnowledgePages.pageFor(key);
    }
}
