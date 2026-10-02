package com.thesift.client.gui;

import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

/**
 * The boss-bar hook registered by TheSiftClient. Every custom bar (the Conductor, the Thumper, the Weaver, and the vanilla
 * Ender Dragon, Wither and raids) is now drawn by {@link SiftBossBars}.
 */
public final class ConductorBossBar {
    private ConductorBossBar() {
    }

    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        SiftBossBars.render(event);
    }
}
