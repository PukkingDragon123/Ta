package com.thesift.client.knowledge;

import com.thesift.TheSift;
import com.thesift.item.MusicSheetItem;
import com.thesift.music.Song;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * F3: a Music Sheet's own artwork in its tooltip (textures/gui/sheets/&lt;song&gt;.png, tools/knowledge_art.py): its
 * notes on the staff, and ink diagrams of the instrument it is played on and the creature that answers it. The game
 * writes the title and the two labels.
 */
public class SheetTooltip implements ClientTooltipComponent {
    public static final int W = 176;
    public static final int H = 112;
    private static final int INK = 0xFF3A2614;
    private final Song song;

    public SheetTooltip(MusicSheetItem.SheetArt art) {
        this.song = art.song();
    }

    public static Identifier art(Song song) {
        return TheSift.id("textures/gui/sheets/" + song.id() + ".png");
    }

    @Override
    public int getHeight(Font font) {
        return H + 4;
    }

    @Override
    public int getWidth(Font font) {
        return W;
    }

    @Override
    public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor g) {
        draw(g, font, this.song, x, y);
    }

    /** The sheet with its title and labels at (x, y), 176 x 112. */
    public static void draw(GuiGraphicsExtractor g, Font font, Song song, int x, int y) {
        g.blit(RenderPipelines.GUI_TEXTURED, art(song), x, y, 0, 0, W, H, W, H);
        Component title = Component.translatable("song.thesift." + song.id());
        g.text(font, title, x + (W - font.width(title)) / 2, y + 8, INK, false);
        Component inst = Component.translatable(song.instrumentKey() + ".short");
        g.text(font, inst, x + 36 - font.width(inst) / 2, y + 103, INK, false);
        Component creature = Component.translatable("knowledge.thesift.song." + song.id() + ".creature");
        g.text(font, creature, x + 118 - font.width(creature) / 2, y + 103, INK, false);
    }
}
