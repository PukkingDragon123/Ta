package com.thesift.client.codex;

import net.minecraft.client.Minecraft;

/** Client-only entry point for opening the Codex from the item. */
public final class CodexOpener {
    private CodexOpener() {
    }

    public static void open() {
        Minecraft.getInstance().gui.setScreen(new SiftCodexScreen());
    }
}
