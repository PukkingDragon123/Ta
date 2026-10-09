package com.thesift.client.knowledge;

import com.thesift.TheSift;
import com.thesift.knowledge.Glyphs;
import com.thesift.knowledge.Knowledge;
import com.thesift.knowledge.Lore;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/**
 * F3: reading a Lore Book or Scroll. One page in the look of whoever wrote it - the Creator's gilded vellum, a
 * Pillager's stained journal, a cultist page eaten by Sculk, a Tide-Keeper's pearly sheet with its clam, a Soul page
 * of dark blue with glowing runes. Scrolls unroll between their rollers as the screen opens. Words in the ancient
 * script morph into English as clues are found ({@link GlyphText}). Scroll the mouse wheel to read on.
 */
public class LoreScreen extends Screen {
    private static final Identifier PAGES = TheSift.id("textures/gui/lore_pages.png");
    private static final Identifier ROLLERS = TheSift.id("textures/gui/lore_rollers.png");
    private static final int PW = 192;
    private static final int PH = 232;
    private static final int TEXT_W = 150;
    private static final int LINES = 17;
    private static final int UNROLL = 12;

    private final Lore lore;
    private final GlyphText title;
    private final GlyphText body;
    private int scroll;
    private int age;

    private final @org.jspecify.annotations.Nullable Screen parent;

    public LoreScreen(Lore lore) {
        this(lore, null);
    }

    /** Opened from the Knowledge Book: closing it goes back to the book. */
    public LoreScreen(Lore lore, @org.jspecify.annotations.Nullable Screen parent) {
        super(Component.translatable(lore.titleKey()));
        this.parent = parent;
        this.lore = lore;
        this.title = new GlyphText(lore.id() + "_title", Component.translatable(lore.titleKey()).getString(), lore.origin.script);
        this.body = new GlyphText(lore.id(), Component.translatable(lore.bodyKey()).getString(), lore.origin.script);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (this.parent != null) {
            this.minecraft.gui.setScreen(this.parent);
        } else {
            super.onClose();
        }
    }

    @Override
    protected void init() {
        super.init();
        if (this.minecraft.player != null && this.age == 0) {
            this.minecraft.player.playSound(SoundEvents.BOOK_PAGE_TURN, 1.0F, this.lore.scroll ? 0.7F : 1.0F);
        }
    }

    private float level() {
        return this.minecraft.player == null ? 1.0F : Glyphs.level(Knowledge.clues(this.minecraft.player));
    }

    @Override
    public void tick() {
        super.tick();
        this.age++;
        float level = this.level();
        this.title.tick(level);
        this.body.tick(level);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        int lines = this.body.layout(this.font, TEXT_W);
        this.scroll = Mth.clamp(this.scroll - (int) Math.signum(scrollY), 0, Math.max(0, lines - LINES));
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        int o = this.lore.origin.ordinal();
        int x = (this.width - PW) / 2;
        int y = (this.height - PH) / 2;
        float open = Mth.clamp((this.age + a) / UNROLL, 0.0F, 1.0F);
        open = 1.0F - (1.0F - open) * (1.0F - open);
        int ink = this.lore.origin.ink;
        int glyph = this.lore.origin.accent;
        if (this.lore.scroll) {
            // the sheet unrolls from the middle outwards between its two rollers
            int h = Math.max(8, (int) (PH * open));
            int top = y + (PH - h) / 2;
            g.blit(RenderPipelines.GUI_TEXTURED, PAGES, x, top, 200 * o, (PH - h) / 2.0F, PW, h, 1024, 256);
            g.blit(RenderPipelines.GUI_TEXTURED, ROLLERS, x - 8, top - 10, 0, 16 * o, 208, 16, 256, 96);
            g.blit(RenderPipelines.GUI_TEXTURED, ROLLERS, x - 8, top + h - 6, 0, 16 * o, 208, 16, 256, 96);
            if (open < 1.0F) {
                return;
            }
        } else {
            g.fill(x + 3, y + 3, x + PW + 3, y + PH + 3, 0x50000000);
            g.blit(RenderPipelines.GUI_TEXTURED, PAGES, x, y, 200 * o, 0, PW, PH, 1024, 256);
            if (open < 1.0F) {
                g.fill(x, y, x + PW, y + PH, (int) ((1.0F - open) * 200) << 24 | 0x101010);
            }
        }
        int tx = x + (PW - TEXT_W) / 2;
        // the title, centred
        int tw = this.title.layout(this.font, TEXT_W);
        this.title.draw(g, this.font, tx, y + 20, 0, Math.min(2, tw), glyph, glyph, a);
        Component from = Component.translatable("knowledge.thesift.lore.from", Component.translatable("lore.thesift.origin." + this.lore.origin.id()));
        g.centeredText(this.font, from, x + PW / 2, y + 22 + Math.min(2, tw) * 10, ink & 0x00FFFFFF | 0xA0000000);
        int bodyY = y + 38 + Math.min(2, tw) * 10;
        int lines = this.body.layout(this.font, TEXT_W);
        int show = Math.min(LINES - Math.min(2, tw) + 1, (y + PH - 28 - bodyY) / 10);
        this.body.draw(g, this.font, tx, bodyY, this.scroll, show, ink, glyph, a);
        if (this.scroll > 0) {
            g.centeredText(this.font, "▲", x + PW / 2, bodyY - 10, glyph);
        }
        if (this.scroll + show < lines) {
            g.centeredText(this.font, "▼", x + PW / 2, y + PH - 26, glyph);
        }
        int pct = Math.round(this.body.readable() * 100.0F);
        if (this.lore.origin.script > 0.0F) {
            g.centeredText(this.font, Component.translatable("lore.thesift.deciphered", pct), x + PW / 2, y + PH - 16,
                    this.body.morphing() ? 0xFF6FF4EC : ink & 0x00FFFFFF | 0x90000000);
        }
    }
}
