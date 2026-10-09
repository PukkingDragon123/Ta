package com.thesift.client.knowledge;

import com.thesift.TheSift;
import com.thesift.knowledge.Glyphs;
import com.thesift.knowledge.Knowledge;
import com.thesift.knowledge.Lore;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/**
 * F3 / I1: reading a Lore Book or Scroll, laid out like vanilla's book screen - one page at a time, text 114 wide from
 * (36, 32), "Page X of Y" at the top right, the page arrows at the foot. A book is bound in its writer's leather and
 * paper (the Creator's white and gold, a Pillager's hide, the cult's sculk, the Tide-Keepers' silver, the Soul
 * Dimension's dark blue); a scroll unrolls between its two rollers as the screen opens. Words in the ancient script
 * morph into English as clues are found ({@link GlyphText}); how much is deciphered shows at the top left.
 */
public class LoreScreen extends Screen {
    private static final Identifier PAGES = TheSift.id("textures/gui/lore_pages.png");
    private static final Identifier ARROWS = TheSift.id("textures/gui/knowledge_book.png");
    private static final int TW = 1024;
    private static final int TH = 512;
    private static final int BOOK = 192;
    private static final int TX = 36;
    private static final int TY = 32;
    private static final int TEXT_W = 114;
    private static final int LINES = 12;
    private static final int UNROLL = 12;

    private final Lore lore;
    private final GlyphText title;
    private final GlyphText body;
    private int page;
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

    /** Title lines on the first page (at most two). */
    private int titleLines() {
        return Math.min(2, this.title.layout(this.font, TEXT_W));
    }

    /** Body lines on the first page: below the title and its "from" line. */
    private int firstPageLines() {
        return LINES - this.titleLines() - 2;
    }

    private int pageCount() {
        int lines = this.body.layout(this.font, TEXT_W);
        int rest = Math.max(0, lines - this.firstPageLines());
        return 1 + (rest + LINES - 1) / LINES;
    }

    private void turn(int to) {
        int target = Mth.clamp(to, 0, this.pageCount() - 1);
        if (target != this.page) {
            this.page = target;
            if (this.minecraft.player != null) {
                this.minecraft.player.playSound(SoundEvents.BOOK_PAGE_TURN, 1.0F, this.lore.scroll ? 0.75F : 1.0F);
            }
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 262 || event.key() == 267) {
            this.turn(this.page + 1);
            return true;
        }
        if (event.key() == 263 || event.key() == 266) {
            this.turn(this.page - 1);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        this.turn(this.page - (int) Math.signum(scrollY));
        return true;
    }

    private boolean over(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int x = (this.width - BOOK) / 2;
        int y = 2;
        if (this.page > 0 && this.over(event.x(), event.y(), x + 43, y + 157, 23, 13)) {
            this.turn(this.page - 1);
            return true;
        }
        if (this.page < this.pageCount() - 1 && this.over(event.x(), event.y(), x + 116, y + 157, 23, 13)) {
            this.turn(this.page + 1);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        int o = this.lore.origin.ordinal();
        int x = (this.width - BOOK) / 2;
        int y = 2;
        int ink = this.lore.origin.ink;
        int glyph = this.lore.origin.accent;
        if (this.lore.scroll) {
            // the sheet unrolls from the middle outwards between its two rollers
            float open = Mth.clamp((this.age + a) / UNROLL, 0.0F, 1.0F);
            open = 1.0F - (1.0F - open) * (1.0F - open);
            int h = Math.max(4, (int) (172 * open));
            int top = y + 12 + (172 - h) / 2;
            g.blit(RenderPipelines.GUI_TEXTURED, PAGES, x, top, 192 * o, 192 + 12 + (172 - h) / 2.0F, BOOK, h, TW, TH);
            g.blit(RenderPipelines.GUI_TEXTURED, PAGES, x + 8, top - 8, 0, 384 + 12 * o, 176, 12, TW, TH);
            g.blit(RenderPipelines.GUI_TEXTURED, PAGES, x + 8, top + h - 4, 0, 384 + 12 * o, 176, 12, TW, TH);
            if (open < 1.0F) {
                return;
            }
        } else {
            g.blit(RenderPipelines.GUI_TEXTURED, PAGES, x, y, 192 * o, 0, BOOK, BOOK, TW, TH);
        }
        int pages = this.pageCount();
        this.page = Mth.clamp(this.page, 0, pages - 1);
        Component indicator = Component.translatable("book.pageIndicator", this.page + 1, pages);
        g.text(this.font, indicator, x + 148 - this.font.width(indicator), y + 16, ink & 0x00FFFFFF | 0xA0000000, false);
        if (this.lore.origin.script > 0.0F) {
            int pct = Math.round(this.body.readable() * 100.0F);
            g.text(this.font, Component.translatable("lore.thesift.deciphered", pct), x + TX, y + 16,
                    this.body.morphing() ? 0xFF2FA8A0 : glyph & 0x00FFFFFF | 0xC0000000, false);
        }
        int tx = x + TX;
        int ty = y + TY;
        if (this.page == 0) {
            int tl = this.titleLines();
            this.title.draw(g, this.font, tx, ty, 0, tl, glyph, glyph, a);
            Component from = Component.translatable("knowledge.thesift.lore.from", Component.translatable("lore.thesift.origin." + this.lore.origin.id()));
            g.text(this.font, from, tx + (TEXT_W - this.font.width(from)) / 2, ty + tl * 10, ink & 0x00FFFFFF | 0xA0000000, false);
            this.body.draw(g, this.font, tx, ty + (tl + 2) * 10, 0, this.firstPageLines(), ink, glyph, a);
        } else {
            int first = this.firstPageLines() + (this.page - 1) * LINES;
            this.body.draw(g, this.font, tx, ty, first, LINES, ink, glyph, a);
        }
        if (this.page > 0) {
            boolean hover = this.over(mouseX, mouseY, x + 43, y + 157, 23, 13);
            g.blit(RenderPipelines.GUI_TEXTURED, ARROWS, x + 43, y + 157, hover ? 69 : 46, 192, 23, 13, 256, 256);
        }
        if (this.page < pages - 1) {
            boolean hover = this.over(mouseX, mouseY, x + 116, y + 157, 23, 13);
            g.blit(RenderPipelines.GUI_TEXTURED, ARROWS, x + 116, y + 157, hover ? 23 : 0, 192, 23, 13, 256, 256);
        }
    }
}
