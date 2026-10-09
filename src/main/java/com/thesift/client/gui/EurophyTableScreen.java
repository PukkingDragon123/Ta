package com.thesift.client.gui;

import com.thesift.TheSift;
import com.thesift.block.entity.EurophyTableBlockEntity;
import com.thesift.block.entity.EurophyTableMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Europhy Table's screen: the dais with its four arm slots and the centre slot, and a little score on the
 * right whose note heads light up as music winds the table up; a bar fills while the output forms. The panel
 * art is textures/gui/container/europhy_table.png (tools/materials.py).
 */
public class EurophyTableScreen extends AbstractContainerScreen<EurophyTableMenu> {
    private static final Identifier TEXTURE = TheSift.id("textures/gui/container/europhy_table.png");
    private static final int[] STAVES = {32, 56};
    private static final int[] MELODY = {7, 4, 1, 5, 2, 8};
    private static final int[] RAINBOW = {0xFFFF9FD8, 0xFFFFE28C, 0xFF8FF0FF, 0xFFB4F59A};
    private static final int INK = 0xFF5B4A33;
    private static final int FAINT = 0xFFB9A57E;
    private static final String[] STATUS = {"idle", "ready", "helper", "full", "forming"};

    public EurophyTableScreen(EurophyTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 186);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int x = this.leftPos;
        int y = this.topPos;
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        long now = mc.level != null ? mc.level.getGameTime() : 0L;
        // the score: one note head per note the recipe asks for, lit as they are played
        int need = Math.min(12, this.menu.need());
        int charge = this.menu.charge();
        for (int i = 0; i < need; i++) {
            int staff = STAVES[i / 6];
            int col = i % 6;
            int nx = x + 140 + col * 5;
            int ny = y + staff + MELODY[col];
            boolean lit = i < charge || this.menu.isForming();
            int c = lit ? RAINBOW[i % RAINBOW.length] : FAINT;
            if (lit) {
                g.fill(nx - 1, ny - 1, nx + 4, ny + 3, (c & 0x00FFFFFF) | 0x50000000);  // a soft glow
                g.fill(nx, ny, nx + 3, ny + 2, c);
            } else {
                g.outline(nx, ny, 3, 2, c);
            }
            boolean high = MELODY[col] <= 4;
            g.fill(nx + (high ? 0 : 2), high ? ny + 1 : ny - 5, nx + (high ? 1 : 3), high ? ny + 7 : ny + 1, lit ? INK : FAINT);
        }
        // forming: the bar under the score fills, and the centre slot's frame pulses
        if (this.menu.isForming()) {
            float f = this.menu.formProgress();
            int w = Mth.ceil(34 * f);
            for (int i = 0; i < w; i++) {
                g.fill(x + 133 + i, y + 70, x + 134 + i, y + 72, RAINBOW[(i / 4 + (int) (now / 2)) % RAINBOW.length]);
            }
            int alpha = (int) (90 + 70 * Mth.sin(now * 0.4F));
            g.outline(x + EurophyTableMenu.OUTPUT_X - 6, y + EurophyTableMenu.OUTPUT_Y - 6, 28, 28, (alpha << 24) | 0xFFD9F7);
            g.outline(x + EurophyTableMenu.OUTPUT_X - 7, y + EurophyTableMenu.OUTPUT_Y - 7, 30, 30, ((alpha / 2) << 24) | 0xB99CF2);
        }
        // what the table is waiting for, top right
        int status = Mth.clamp(this.menu.status(), 0, STATUS.length - 1);
        if (status != EurophyTableBlockEntity.IDLE || this.menu.need() > 0) {
            String s = Component.translatable("europhy.thesift.status." + STATUS[status]).getString();
            int colour = switch (status) {
                case EurophyTableBlockEntity.NEEDS_HELPER, EurophyTableBlockEntity.FULL -> 0xFFB0402A;
                case EurophyTableBlockEntity.FORMING -> 0xFF7B5CC6;
                default -> 0xFF4F7A3A;
            };
            g.text(this.font, s, x + this.imageWidth - 8 - this.font.width(s), y + 6, colour, false);
        }
    }
}
