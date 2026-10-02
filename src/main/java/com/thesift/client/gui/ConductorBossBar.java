package com.thesift.client.gui;

import com.thesift.TheSift;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

/**
 * The Conductor's own health bar (and, in the same layout, each of his three great players'): a sculk-steel frame with gold trim, ticks where his phases
 * change, a glowing cyan fill that pulses faster as he weakens, and his devil's head beside it.
 */
public final class ConductorBossBar {
    /** Each boss's name key, and its bar. */
    private static final java.util.Map<String, Identifier> BARS = java.util.Map.of(
            "entity." + TheSift.MODID + ".dictator", TheSift.id("textures/gui/conductor_bar.png"),
            "entity." + TheSift.MODID + ".thumper", TheSift.id("textures/gui/thumper_bar.png"),
            "entity." + TheSift.MODID + ".thumper.titan", TheSift.id("textures/gui/thumper_titan_bar.png"),
            "entity." + TheSift.MODID + ".whistler", TheSift.id("textures/gui/whistler_bar.png"),
            "entity." + TheSift.MODID + ".strummer", TheSift.id("textures/gui/strummer_bar.png"));

    private ConductorBossBar() {
    }

    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        LerpingBossEvent boss = event.getBossEvent();
        if (!(boss.getName().getContents() instanceof TranslatableContents t) || !BARS.containsKey(t.getKey())) {
            return;
        }
        Identifier texture = BARS.get(t.getKey());
        event.setCanceled(true);
        GuiGraphicsExtractor g = event.getGuiGraphics();
        Minecraft mc = Minecraft.getInstance();
        int cx = event.getX() + 91;
        int x = cx - 107;
        int y = event.getY() - 5;
        float hp = Mth.clamp(boss.getProgress(), 0.0F, 1.0F);
        g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, 214, 22, 428, 44, 512, 64);
        g.blit(RenderPipelines.GUI_TEXTURED, texture, x + 16, y + 10, 0, 54, 182, 5, 364, 10, 512, 64);
        int w = Math.round(182 * hp);
        if (w > 0) {
            g.blit(RenderPipelines.GUI_TEXTURED, texture, x + 16, y + 10, 0, 44, w, 5, w * 2, 10, 512, 64);
            // the leading edge flickers brighter, faster the weaker he gets
            float speed = 0.15F + (1.0F - hp) * 0.45F;
            if (mc.level != null && Mth.sin(mc.level.getGameTime() * speed) > 0.0F) {
                g.fill(x + 16 + w - 1, y + 10, x + 16 + w, y + 15, 0xFFE8FFFC);
            }
        }
        g.blit(RenderPipelines.GUI_TEXTURED, texture, x - 10, y - 3, 448, 0, 16, 16, 32, 32, 512, 64);
        Component name = boss.getName();
        g.text(mc.font, name, cx - mc.font.width(name) / 2, y - 8, 0xFFE8FFFC, true);
        event.setIncrement(32);
    }
}
