package com.thesift.client.codex;

import com.thesift.TheSift;
import com.thesift.client.codex.KnowledgePages.Kind;
import com.thesift.client.codex.KnowledgePages.Page;
import com.thesift.client.knowledge.GlyphText;
import com.thesift.client.knowledge.LoreScreen;
import com.thesift.client.knowledge.SheetTooltip;
import com.thesift.knowledge.Glyphs;
import com.thesift.knowledge.Knowledge;
import com.thesift.knowledge.LoreReading;
import com.thesift.knowledge.Quest;
import com.thesift.music.Notes;
import com.thesift.music.Song;
import com.thesift.music.band.BandRegistry;
import com.thesift.music.band.BandVoice;
import com.thesift.registry.ModKnowledge;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import org.jspecify.annotations.Nullable;

/**
 * F3 Knowledge &amp; lore: the Knowledge Book. A navy, gold-cornered tome with nine ribbon bookmarks: Creatures, Songs,
 * Enchantments, Lore, Places, Items &amp; Recipes, Music &amp; Machines, Heralds &amp; Bosses and the Guide's Notes. Each
 * chapter opens on a contents spread (what you have discovered, and how much); its pages follow. Creatures stand alive
 * on their page beside written field notes; songs show their sheet; lore is written partly in the ancient script, which
 * clears as you find more of it; item pages show the recipe if you know it. Pages you have not discovered yet are
 * sealed, their titles in glyphs, with a hint. Turn pages with the arrows, the arrow keys or a click on a page; scroll
 * long text with the wheel.
 */
public class KnowledgeBookScreen extends Screen {
    private static final Identifier TEXTURE = TheSift.id("textures/gui/knowledge_book.png");
    private static final int TW = 512;
    private static final int TH = 512;
    private static final int BW = 312;
    private static final int BH = 196;
    private static final int PW = 128;
    private static final int FLIP_TICKS = 11;
    private static final int SHOWCASE_ID = Integer.MAX_VALUE - 2000;
    private static final int INK = 0xFF3A2614;
    private static final int INK_SOFT = 0xFF7A6448;
    private static final int[] ACCENT = {0xFF3F7A3A, 0xFF8A5A1A, 0xFF4E566A, 0xFF1F6F6F, 0xFF7A1F2C, 0xFF9A3A6E, 0xFF543A96, 0xFF8A6A1E, 0xFF6A5A3A};

    private List<Page> pages = List.of();
    private int index;
    private int shownIndex = -1;
    private int flipFrom = -1;
    private int flipTicks;
    private int flipDir = 1;
    private int pageAge;
    private int scroll;
    private int age;
    private @Nullable LivingEntity showcase;
    private @Nullable GlyphText loreTitle;
    private @Nullable GlyphText loreBody;
    private final CodexFx fx = new CodexFx();

    private @Nullable String startAt;

    public KnowledgeBookScreen() {
        super(Component.translatable("knowledge.thesift.title"));
    }

    /** Opens the book at the page with this id (an entry key, {@code contents_<chapter>}, {@code song_<id>}, {@code lore_<id>}, ...). */
    public KnowledgeBookScreen(String pageId) {
        this();
        this.startAt = pageId;
    }

    private int left() {
        return (this.width - BW) / 2;
    }

    private int top() {
        return (this.height - BH) / 2 + 8;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        if (this.pages.isEmpty() && this.minecraft.player != null) {
            this.pages = KnowledgePages.build(this.minecraft.player);
            for (int i = 0; this.startAt != null && i < this.pages.size(); i++) {
                if (this.pages.get(i).id().equals(this.startAt)) {
                    this.index = i;
                }
            }
            this.index = Mth.clamp(this.index, 0, this.pages.size() - 1);
            this.minecraft.player.playSound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F);
        }
    }

    private Player player() {
        return this.minecraft.player;
    }

    private boolean known(Page p) {
        return KnowledgePages.known(this.player(), p);
    }

    // ------------------------------------------------------------------ paging

    private void turnTo(int target) {
        if (this.pages.isEmpty()) {
            return;
        }
        target = Math.floorMod(target, this.pages.size());
        if (target == this.index) {
            return;
        }
        this.flipDir = target > this.index ? 1 : -1;
        this.flipFrom = this.index;
        this.flipTicks = FLIP_TICKS;
        this.index = target;
        this.player().playSound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 0.9F + this.player().getRandom().nextFloat() * 0.25F);
    }

    private int contentsOf(int chapter) {
        for (int i = 0; i < this.pages.size(); i++) {
            if (this.pages.get(i).kind() == Kind.CONTENTS && this.pages.get(i).chapter() == chapter) {
                return i;
            }
        }
        return 0;
    }

    private void prepare() {
        if (this.shownIndex == this.index || this.pages.isEmpty()) {
            return;
        }
        this.shownIndex = this.index;
        this.pageAge = 0;
        this.scroll = 0;
        Page p = this.pages.get(this.index);
        this.fx.reset(this.theme(p));
        this.showcase = null;
        this.loreTitle = null;
        this.loreBody = null;
        EntityType<?> type = p.kind() == Kind.ENTRY && p.entry().entity() != null ? p.entry().entity().get()
                : p.kind() == Kind.QUEST ? ModKnowledge.MINI_CREATOR.get() : null;
        if (type != null && this.known(p) && this.minecraft.level != null) {
            Entity made = type.create(this.minecraft.level, EntitySpawnReason.LOAD);
            if (made instanceof LivingEntity living) {
                // never added to a level, so it needs an id of its own before a renderer asks for one
                living.setId(SHOWCASE_ID - this.index);
                this.showcase = living;
            }
        }
        if (p.kind() == Kind.LORE) {
            this.loreTitle = new GlyphText(p.lore().id() + "_title", Component.translatable(p.lore().titleKey()).getString(), p.lore().origin.script);
            this.loreBody = new GlyphText(p.lore().id(), Component.translatable(p.lore().bodyKey()).getString(), p.lore().origin.script);
        }
    }

    private CodexFx.Theme theme(Page p) {
        return switch (p.kind()) {
            case ENTRY -> CodexFx.theme(p.entry().key());
            case SONG -> CodexFx.Theme.MUSIC;
            case ENCHANT -> CodexFx.Theme.CHROME;
            case LORE -> switch (p.lore().origin) {
                case CULTIST, SOUL -> CodexFx.Theme.SCULK;
                case OCEAN -> CodexFx.Theme.SKY;
                default -> CodexFx.Theme.NONE;
            };
            case QUEST -> CodexFx.Theme.SKY;
            default -> p.chapter() == CodexEntries.DICTATOR ? CodexFx.Theme.BOSS : CodexFx.Theme.NONE;
        };
    }

    @Override
    public void tick() {
        super.tick();
        this.prepare();
        this.age++;
        if (this.flipTicks > 0) {
            this.flipTicks--;
            if (this.flipTicks == FLIP_TICKS / 2) {
                this.player().playSound(SoundEvents.BOOK_PAGE_TURN, 0.45F, 1.35F);
            }
        } else {
            this.fx.tick(this.minecraft.player, BH);
        }
        this.pageAge++;
        if (this.pages.isEmpty()) {
            return;
        }
        Page p = this.pages.get(this.index);
        if (this.showcase != null) {
            this.showcase.tickCount++;
            if (p.kind() == Kind.ENTRY && p.entry().animator() != null) {
                p.entry().animator().accept(this.showcase, this.pageAge);
            }
        }
        float level = Glyphs.level(Knowledge.clues(this.player()));
        if (this.loreTitle != null && this.loreBody != null && this.known(p)) {
            this.loreTitle.tick(level);
            this.loreBody.tick(level);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 262) { // right arrow
            this.turnTo(this.index + 1);
            return true;
        }
        if (event.key() == 263) { // left arrow
            this.turnTo(this.index - 1);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        this.scroll = Math.max(0, this.scroll - (int) Math.signum(scrollY));
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        int l = this.left();
        int t = this.top();
        for (int i = 0; i < CodexEntries.ORDER.length; i++) {
            int rx = l + 24 + i * 28;
            if (mx >= rx && mx < rx + 22 && my >= t - 24 && my < t) {
                this.turnTo(this.contentsOf(CodexEntries.ORDER[i]));
                return true;
            }
        }
        if (my >= t + BH - 24 && my < t + BH - 12) {
            if (mx >= l + 14 && mx < l + 34) {
                this.turnTo(this.index - 1);
                return true;
            }
            if (mx >= l + BW - 34 && mx < l + BW - 14) {
                this.turnTo(this.index + 1);
                return true;
            }
        }
        if (this.flipTicks == 0 && !this.pages.isEmpty()) {
            Page p = this.pages.get(this.index);
            if (p.kind() == Kind.CONTENTS) {
                int hit = this.contentsHit(p, l, t, mx, my);
                if (hit >= 0) {
                    this.turnTo(hit);
                    return true;
                }
            }
            if (p.kind() == Kind.LORE && this.known(p) && mx >= l + 12 && mx < l + BW / 2 && my >= t + 12 && my < t + BH - 26) {
                this.minecraft.gui.setScreen(new LoreScreen(p.lore(), this));
                return true;
            }
        }
        if (mx >= l && mx < l + BW && my >= t && my < t + BH) {
            this.turnTo(this.index + (mx < l + BW / 2.0 ? -1 : 1));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        if (this.pages.isEmpty()) {
            return;
        }
        this.prepare();
        int l = this.left();
        int t = this.top();
        Page page = this.pages.get(this.index);

        // the ribbons: the open chapter's stands taller
        for (int i = 0; i < CodexEntries.ORDER.length; i++) {
            int ch = CodexEntries.ORDER[i];
            boolean sel = ch == page.chapter();
            int rx = l + 24 + i * 28;
            boolean hover = mouseX >= rx && mouseX < rx + 22 && mouseY >= t - 24 && mouseY < t;
            g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, rx, t - (sel ? 24 : hover ? 22 : 19), 24 * ch, sel ? 228 : 200, 22, 26, TW, TH);
            if (hover) {
                g.setTooltipForNextFrame(this.font, Component.translatable("knowledge.thesift.chapter." + CodexEntries.CATEGORY_KEYS[ch]), mouseX, mouseY);
            }
        }
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, l, t, 0, 0, BW, BH, TW, TH);

        float flip = this.flipTicks > 0 ? Mth.clamp(1.0F - (this.flipTicks - a) / FLIP_TICKS, 0.0F, 1.0F) : 1.0F;
        boolean flipping = this.flipTicks > 0 && this.flipFrom >= 0 && this.flipFrom < this.pages.size();
        if (!flipping) {
            this.fx.render(g, l, t, BW, BH, a);
            this.drawLeft(g, l + 18, t + 14, page, mouseX, mouseY, true, a);
            this.drawRight(g, l + BW / 2 + 10, t + 14, page, mouseX, mouseY, a);
        } else {
            Page from = this.pages.get(this.flipFrom);
            boolean forward = this.flipDir > 0;
            this.drawLeft(g, l + 18, t + 14, forward ? from : page, mouseX, mouseY, false, a);
            this.drawRight(g, l + BW / 2 + 10, t + 14, forward ? page : from, mouseX, mouseY, a);
            this.drawCurl(g, l, t, flip, forward, from, page, mouseX, mouseY, a);
        }

        boolean hoverPrev = mouseX >= l + 14 && mouseX < l + 34 && mouseY >= t + BH - 24 && mouseY < t + BH - 12;
        boolean hoverNext = mouseX >= l + BW - 34 && mouseX < l + BW - 14 && mouseY >= t + BH - 24 && mouseY < t + BH - 12;
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, l + 15, t + BH - 23, 20, hoverPrev ? 268 : 256, 18, 10, TW, TH);
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, l + BW - 33, t + BH - 23, 0, hoverNext ? 268 : 256, 18, 10, TW, TH);
        String pageNo = (this.index + 1) + " / " + this.pages.size();
        g.text(this.font, pageNo, l + (BW - this.font.width(pageNo)) / 2, t + BH - 22, INK_SOFT, false);
    }

    /** The turning leaf (see the old Codex): it curls over the spine, shaded, with a rolled lip of light. */
    private void drawCurl(GuiGraphicsExtractor g, int l, int t, float flip, boolean forward, Page from, Page to, int mouseX, int mouseY, float a) {
        float e = (1.0F - Mth.cos(flip * Mth.PI)) * 0.5F;
        float c = Mth.cos(e * Mth.PI);
        float lift = Mth.sin(e * Mth.PI);
        boolean firstHalf = c > 0.0F;
        boolean onRight = forward == firstHalf;
        float k = Math.max(0.03F, Math.abs(c));
        int spine = l + BW / 2;
        int pw = BW / 2 - 12;
        int w = Math.max(1, Math.round(pw * k));
        int drawX = onRight ? spine : spine - w;
        int edge = onRight ? spine + w : spine - w;
        int top = t + 10;
        int ph = BH - 22;
        float sy = 1.0F + 0.035F * lift;
        float cy = top + ph / 2.0F;
        int y0 = Math.round(cy - ph * sy / 2.0F);
        int y1 = Math.round(cy + ph * sy / 2.0F);
        int lip = Math.round(5.0F * lift);
        int sw = Math.round(3 + 14 * lift);
        for (int i = 0; i < sw; i++) {
            int sx = onRight ? edge + lip + i : edge - lip - 1 - i;
            if (sx < l + 12 || sx >= l + BW - 12) {
                continue;
            }
            int sa = (int) (80.0F * lift * (1.0F - i / (float) sw));
            g.fill(sx, top + 3, sx + 1, top + ph - 1, sa << 24 | 0x1A1008);
        }
        g.nextStratum();
        g.pose().pushMatrix();
        g.pose().translate(drawX, cy);
        g.pose().scale(k, sy);
        g.pose().translate(0.0F, -cy);
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, top, onRight ? BW / 2 : 12, 10, pw, ph, TW, TH);
        if (onRight) {
            this.drawRight(g, 10, t + 14, firstHalf ? from : to, -1000, -1000, a);
        } else {
            this.drawLeft(g, 6, t + 14, firstHalf ? from : to, -1000, -1000, false, a);
        }
        g.nextStratum();
        for (int i = 0; i < pw; i += 4) {
            float f = onRight ? i / (float) pw : 1.0F - i / (float) pw;
            int la = (int) (lift * (18.0F + 80.0F * f * f) * (firstHalf ? 1.0F : 0.7F));
            g.fill(i, top, Math.min(pw, i + 4), top + ph, la << 24 | 0x2B1D10);
        }
        int gx = onRight ? 0 : pw - 6;
        g.fillGradient(gx, top, gx + 6, top + ph, 0x40000000, 0x30000000);
        g.pose().popMatrix();
        for (int i = 0; i < lip; i++) {
            int lx = onRight ? edge + i : edge - 1 - i;
            float m = 1.0F - Math.abs((i + 0.5F) / lip - 0.5F) * 2.0F;
            int r = (int) Mth.lerp(m, 0xB0, 0xFB);
            int gg = (int) Mth.lerp(m, 0x9C, 0xF5);
            int bb = (int) Mth.lerp(m, 0x70, 0xE6);
            g.fill(lx, y0 + 1, lx + 1, y1 - 1, 0xFF000000 | r << 16 | gg << 8 | bb);
        }
        int outer = onRight ? edge + lip : edge - lip - 1;
        g.fill(outer, y0 + 2, outer + 1, y1 - 2, 0x70000000);
        g.fill(onRight ? edge - 1 : edge, y0, onRight ? edge : edge + 1, y1, 0x50000000);
    }

    // ------------------------------------------------------------------ pieces

    private int accent(Page p) {
        return ACCENT[Mth.clamp(p.chapter(), 0, ACCENT.length - 1)];
    }

    /** Title, tagline and the gold rule under them; returns the y below. */
    private int header(GuiGraphicsExtractor g, int x, int y, Component title, @Nullable Component tagline, int accent) {
        List<FormattedCharSequence> tl = this.font.split(title, PW);
        for (int i = 0; i < Math.min(2, tl.size()); i++) {
            g.text(this.font, tl.get(i), x + (PW - this.font.width(tl.get(i))) / 2, y, INK, false);
            y += 10;
        }
        if (tagline != null) {
            List<FormattedCharSequence> tag = this.font.split(tagline, PW - 4);
            for (int i = 0; i < Math.min(2, tag.size()); i++) {
                g.text(this.font, tag.get(i), x + (PW - this.font.width(tag.get(i))) / 2, y, accent, false);
                y += 9;
            }
        }
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + (PW - 120) / 2, y + 1, 0, 282, 120, 6, TW, TH);
        return y + 9;
    }

    private void small(GuiGraphicsExtractor g, int i, int x, int y) {
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 44 + 10 * i, 256, 9, 9, TW, TH);
    }

    private void bigItem(GuiGraphicsExtractor g, ItemStack stack, int cx, int cy, boolean bob, float scale) {
        float b = bob ? Mth.sin((this.pageAge) * 0.08F) * 2.0F : 0.0F;
        g.pose().pushMatrix();
        g.pose().translate(cx - 8 * scale, cy - 8 * scale + b);
        g.pose().scale(scale, scale);
        g.item(stack, 0, 0);
        g.pose().popMatrix();
    }

    /** Wrapped lines from {@code first}; returns the number of lines in all. */
    private int paragraph(GuiGraphicsExtractor g, List<FormattedCharSequence> lines, int x, int y, int maxLines, int first, int colour) {
        for (int i = 0; i < maxLines && first + i < lines.size(); i++) {
            g.text(this.font, lines.get(first + i), x, y + i * 9, colour, false);
        }
        return lines.size();
    }

    private void scrollMarks(GuiGraphicsExtractor g, int x, int y0, int y1, int first, int shown, int total) {
        if (first > 0) {
            this.small(g, 4, x + PW - 9, y0 - 2);
        }
        if (first + shown < total) {
            this.small(g, 5, x + PW - 9, y1 - 7);
        }
    }

    // ------------------------------------------------------------------ left page

    private void drawLeft(GuiGraphicsExtractor g, int x, int y, Page p, int mouseX, int mouseY, boolean live, float a) {
        int accent = this.accent(p);
        if (p.kind() == Kind.CONTENTS) {
            this.contentsLeft(g, x, y, p, accent);
            return;
        }
        if (!this.known(p)) {
            this.lockedLeft(g, x, y, p);
            return;
        }
        switch (p.kind()) {
            case ENTRY -> {
                CodexEntry e = p.entry();
                int y1 = this.header(g, x, y, Component.translatable(e.titleKey()), Component.translatable(e.taglineKey()), accent);
                if (e.entity() != null) {
                    this.showcase(g, x, y1 + 2, y + 140, mouseY, live && this.flipTicks == 0);
                    BandVoice voice = BandRegistry.voiceOf(e.entity().get());
                    if (voice != null) {
                        Component v = Component.translatable("knowledge.thesift.voice", Component.translatable(voice.instrumentKey()));
                        g.text(this.font, v, x + (PW - this.font.width(v)) / 2, y + 146, INK_SOFT, false);
                    }
                } else if (e.item() != null) {
                    ItemStack stack = new ItemStack(e.item().get());
                    var recipe = KnowledgePages.recipeFor(e.item().get());
                    if (recipe == null) {
                        this.bigItem(g, stack, x + PW / 2, y1 + 50, live, 3.0F);
                    } else {
                        this.bigItem(g, stack, x + PW / 2, y1 + 22, live, 2.0F);
                        Component r = Component.translatable("knowledge.thesift.recipe");
                        g.text(this.font, r, x + (PW - this.font.width(r)) / 2, y1 + 44, accent, false);
                        this.recipe(g, recipe, x + (PW - 102) / 2, y1 + 56, mouseX, mouseY);
                    }
                }
            }
            case SONG -> {
                Song s = p.song();
                int y1 = this.header(g, x, y, Component.translatable("song.thesift." + s.id()),
                        Component.translatable("knowledge.thesift.song.instrument", Component.translatable(s.instrumentKey() + ".short")), accent);
                g.pose().pushMatrix();
                g.pose().translate(x, y1 + 2);
                g.pose().scale(PW / 176.0F, PW / 176.0F);
                SheetTooltip.draw(g, this.font, s, 0, 0);
                g.pose().popMatrix();
                StringBuilder notes = new StringBuilder();
                for (int i = 0; i < s.length(); i++) {
                    notes.append(i == 0 ? "" : " ").append(Notes.name(s.note(i)));
                }
                List<FormattedCharSequence> nl = this.font.split(Component.literal(notes.toString()), PW);
                this.paragraph(g, nl, x, y1 + 88, 3, 0, INK_SOFT);
            }
            case ENCHANT -> {
                Holder<net.minecraft.world.item.enchantment.Enchantment> h = p.enchant();
                int y1 = this.header(g, x, y, h.value().description(), Component.translatable("knowledge.thesift.ench.max", h.value().getMaxLevel()), accent);
                Component fits = Component.translatable("knowledge.thesift.ench.fits");
                g.text(this.font, fits, x, y1 + 4, accent, false);
                List<ItemStack> items = new ArrayList<>();
                h.value().getSupportedItems().stream().limit(18).forEach(i -> items.add(new ItemStack(i.value())));
                for (int i = 0; i < items.size(); i++) {
                    int ix = x + (i % 7) * 18;
                    int iy = y1 + 16 + (i / 7) * 18;
                    g.item(items.get(i), ix, iy);
                    if (mouseX >= ix && mouseX < ix + 16 && mouseY >= iy && mouseY < iy + 16) {
                        g.setTooltipForNextFrame(this.font, items.get(i), mouseX, mouseY);
                    }
                }
            }
            case LORE -> {
                int y1 = y;
                if (this.loreTitle != null) {
                    int n = this.loreTitle.layout(this.font, PW);
                    this.loreTitle.draw(g, this.font, x, y1, 0, Math.min(2, n), INK, p.lore().origin.accent, a);
                    y1 += Math.min(2, n) * 10;
                }
                Component from = Component.translatable("knowledge.thesift.lore.from", Component.translatable("lore.thesift.origin." + p.lore().origin.id()));
                g.text(this.font, from, x + (PW - this.font.width(from)) / 2, y1, accent, false);
                g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + (PW - 120) / 2, y1 + 10, 0, 282, 120, 6, TW, TH);
                this.bigItem(g, p.lore().stack(), x + PW / 2, y1 + 50, live, 3.0F);
                List<FormattedCharSequence> found = this.font.split(Component.translatable("knowledge.thesift.lore.found",
                        Component.translatable(p.lore().foundKey())), PW);
                this.paragraph(g, found, x, y1 + 82, 3, 0, INK_SOFT);
                if (this.loreBody != null && p.lore().origin.script > 0.0F) {
                    int pct = Math.round(this.loreBody.readable() * 100.0F);
                    Component d = Component.translatable("lore.thesift.deciphered", pct);
                    g.text(this.font, d, x + (PW - this.font.width(d)) / 2, y + 140, this.loreBody.morphing() ? 0xFF2FA8A0 : accent, false);
                }
                Component open = Component.translatable("knowledge.thesift.lore.open");
                List<FormattedCharSequence> ol = this.font.split(open, PW);
                this.paragraph(g, ol, x, y + 150, 1, 0, INK_SOFT);
            }
            case QUEST -> {
                Quest q = p.quest();
                int y1 = this.header(g, x, y, Component.translatable("quest.thesift." + q.id() + ".title"), null, accent);
                this.showcase(g, x, y1 + 2, y + 128, mouseY, live && this.flipTicks == 0);
                boolean done = Knowledge.has(this.player(), q.doneKey());
                Component status = Component.translatable(done ? "knowledge.thesift.quest.done" : "knowledge.thesift.quest.current");
                int sw = this.font.width(status) + 12;
                this.small(g, done ? 1 : 2, x + (PW - sw) / 2, y + 135);
                g.text(this.font, status, x + (PW - sw) / 2 + 12, y + 136, done ? 0xFF3F7A3A : accent, false);
            }
            case RECIPES -> {
                List<RecipeDisplayEntry> r = p.recipes();
                if (r.isEmpty()) {
                    int y1 = this.header(g, x, y, Component.translatable("knowledge.thesift.recipes.title"), null, accent);
                    this.paragraph(g, this.font.split(Component.translatable("knowledge.thesift.recipes.none"), PW), x, y1 + 4, 12, 0, INK);
                    return;
                }
                for (int i = 0; i < Math.min(2, r.size()); i++) {
                    this.titledRecipe(g, r.get(i), x, y + i * 72, mouseX, mouseY, accent);
                }
            }
            default -> {
            }
        }
    }

    private void showcase(GuiGraphicsExtractor g, int x, int y0, int y1, int mouseY, boolean live) {
        if (this.showcase == null) {
            return;
        }
        if (!live) {
            return;
        }
        float h = Math.max(this.showcase.getBbHeight(), this.showcase.getBbWidth() * 0.9F);
        int size = Mth.clamp((int) ((y1 - y0) * 0.62F / h), 14, 60);
        float turn = Mth.sin(this.pageAge * 0.02F) * 1.1F;
        float lookY = (float) Math.atan(((y0 + y1) / 2.0F - mouseY) / 60.0F);
        InventoryScreen.renderEntityInInventoryFollowsAngle(g, x + 2, y0, x + PW - 2, y1, size, 0.0625F, turn, lookY, this.showcase);
    }

    private void lockedLeft(GuiGraphicsExtractor g, int x, int y, Page p) {
        Component title = KnowledgePages.title(p).copy().withStyle(LoreReading.GLYPHS);
        List<FormattedCharSequence> tl = this.font.split(title, PW);
        for (int i = 0; i < Math.min(2, tl.size()); i++) {
            g.text(this.font, tl.get(i), x + (PW - this.font.width(tl.get(i))) / 2, y + i * 10, INK_SOFT, false);
        }
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + (PW - 120) / 2, y + 22, 0, 282, 120, 6, TW, TH);
        // a wax seal over the page
        g.pose().pushMatrix();
        g.pose().translate(x + PW / 2.0F - 13.5F, y + 56);
        g.pose().scale(3.0F, 3.0F);
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, 44, 256, 9, 9, TW, TH);
        g.pose().popMatrix();
        Component u = Component.translatable("knowledge.thesift.undiscovered");
        g.text(this.font, u, x + (PW - this.font.width(u)) / 2, y + 96, INK_SOFT, false);
    }

    private void contentsLeft(GuiGraphicsExtractor g, int x, int y, Page p, int accent) {
        Component name = Component.translatable("knowledge.thesift.chapter." + CodexEntries.CATEGORY_KEYS[p.chapter()]);
        g.pose().pushMatrix();
        g.pose().translate(x + PW / 2.0F, y + 4);
        g.pose().scale(1.5F, 1.5F);
        g.text(this.font, name, -this.font.width(name) / 2, 0, accent, false);
        g.pose().popMatrix();
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + (PW - 120) / 2, y + 20, 0, 282, 120, 6, TW, TH);
        // the ribbon's emblem, large
        g.pose().pushMatrix();
        g.pose().translate(x + PW / 2.0F - 22.0F, y + 30);
        g.pose().scale(2.0F, 2.0F);
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, 24 * p.chapter(), 228, 22, 22, TW, TH);
        g.pose().popMatrix();
        List<FormattedCharSequence> intro = this.font.split(Component.translatable("knowledge.thesift.chapter." + CodexEntries.CATEGORY_KEYS[p.chapter()]
                + ".intro"), PW);
        this.paragraph(g, intro, x, y + 80, 6, 0, INK);
        int[] count = this.count(p.chapter());
        Component found = Component.translatable("knowledge.thesift.discovered", count[0], count[1]);
        g.text(this.font, found, x + (PW - this.font.width(found)) / 2, y + 138, INK_SOFT, false);
        int bx = x + (PW - 102) / 2;
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, bx, y + 149, 0, 292, 102, 7, TW, TH);
        int fill = count[1] == 0 ? 0 : 100 * count[0] / count[1];
        if (fill > 0) {
            g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, bx + 1, y + 150, 0, 300, fill, 5, TW, TH);
        }
    }

    private int[] count(int chapter) {
        int known = 0;
        int all = 0;
        for (Page q : this.pages) {
            if (q.chapter() == chapter && q.kind() != Kind.CONTENTS && q.kind() != Kind.RECIPES) {
                all++;
                if (this.known(q)) {
                    known++;
                }
            }
        }
        return new int[]{known, all};
    }

    // ------------------------------------------------------------------ right page

    private void drawRight(GuiGraphicsExtractor g, int x, int y, Page p, int mouseX, int mouseY, float a) {
        int bottom = y + 152;
        int accent = this.accent(p);
        if (p.kind() == Kind.CONTENTS) {
            this.contentsRight(g, x, y, p, mouseX, mouseY);
            return;
        }
        if (!this.known(p)) {
            List<FormattedCharSequence> hint = this.font.split(Component.translatable("knowledge.thesift.hint." + KnowledgePages.hintKind(p)), PW);
            this.paragraph(g, hint, x, y + 40, 6, 0, INK_SOFT);
            return;
        }
        switch (p.kind()) {
            case ENTRY -> {
                CodexEntry e = p.entry();
                int y1 = y;
                String notesKey = "codex.thesift." + e.key() + ".notes";
                if (net.minecraft.locale.Language.getInstance().has(notesKey)) {
                    y1 = this.fieldNotes(g, x, y1, I18n.get(notesKey), accent) + 4;
                }
                this.body(g, this.font.split(Component.translatable(e.bodyKey()), PW), x, y1, bottom);
            }
            case SONG -> {
                Song s = p.song();
                List<FormattedCharSequence> lines = new ArrayList<>(this.font.split(Component.translatable("song.thesift." + s.id() + ".desc"), PW));
                lines.add(FormattedCharSequence.EMPTY);
                lines.addAll(this.font.split(Component.translatable("knowledge.thesift.song." + s.id()), PW));
                lines.add(FormattedCharSequence.EMPTY);
                lines.addAll(this.font.split(this.labelled("knowledge.thesift.song.answers",
                        Component.translatable("knowledge.thesift.song." + s.id() + ".creature"), accent), PW));
                List<String> band = new ArrayList<>();
                for (BandVoice v : BandRegistry.voices()) {
                    if (v.songs().contains(s)) {
                        band.add(v.type().getDescription().getString());
                    }
                }
                if (!band.isEmpty()) {
                    lines.addAll(this.font.split(this.labelled("knowledge.thesift.song.band", Component.literal(String.join(", ", band)), accent), PW));
                }
                if (Knowledge.has(this.player(), "song:" + s.id())) {
                    lines.add(FormattedCharSequence.EMPTY);
                    lines.addAll(this.font.split(Component.translatable("knowledge.thesift.song.played").withColor(0x3F7A3A), PW));
                }
                this.body(g, lines, x, y, bottom);
            }
            case ENCHANT -> {
                String path = p.enchant().unwrapKey().map(k -> k.identifier().getPath()).orElse("");
                Component text = Component.translatableWithFallback("knowledge.thesift.enchantment." + path,
                        I18n.get("knowledge.thesift.ench.none"));
                this.body(g, this.font.split(text, PW), x, y, bottom);
            }
            case LORE -> {
                if (this.loreBody != null) {
                    int total = this.loreBody.layout(this.font, PW);
                    int show = (bottom - y) / 10;
                    this.scroll = Mth.clamp(this.scroll, 0, Math.max(0, total - show));
                    this.loreBody.draw(g, this.font, x, y, this.scroll, show, p.lore().origin == com.thesift.knowledge.Lore.Origin.SOUL
                            ? 0xFF22305E : INK, p.lore().origin == com.thesift.knowledge.Lore.Origin.SOUL ? 0xFF2F6FB8 : p.lore().origin.accent, a);
                    this.scrollMarks(g, x, y, bottom, this.scroll, show, total);
                }
            }
            case QUEST -> {
                Quest q = p.quest();
                List<FormattedCharSequence> lines = new ArrayList<>(this.font.split(this.labelled("knowledge.thesift.quest.goal",
                        Component.translatable("quest.thesift." + q.id() + ".goal"), accent), PW));
                lines.add(FormattedCharSequence.EMPTY);
                lines.addAll(this.font.split(Component.literal("“").append(Component.translatable("quest.thesift." + q.id() + ".line"))
                        .append("”"), PW));
                if (Knowledge.has(this.player(), q.doneKey()) && q.hasDoneLine()) {
                    lines.add(FormattedCharSequence.EMPTY);
                    lines.addAll(this.font.split(Component.literal("“").append(Component.translatable("quest.thesift." + q.id() + ".done"))
                            .append("”").withColor(0x3F7A3A), PW));
                }
                this.body(g, lines, x, y, bottom);
            }
            case RECIPES -> {
                List<RecipeDisplayEntry> r = p.recipes();
                for (int i = 2; i < r.size(); i++) {
                    this.titledRecipe(g, r.get(i), x, y + (i - 2) * 72, mouseX, mouseY, accent);
                }
            }
            default -> {
            }
        }
    }

    private MutableComponent labelled(String key, Component value, int accent) {
        String label = I18n.get(key, "\u0000");
        int cut = label.indexOf('\u0000');
        if (cut < 0) {
            return Component.translatable(key, value);
        }
        return Component.literal(label.substring(0, cut)).withColor(accent & 0xFFFFFF).append(value.copy().withColor(INK & 0xFFFFFF))
                .append(Component.literal(label.substring(cut + 1)));
    }

    /** Scrollable body text from y to bottom. */
    private void body(GuiGraphicsExtractor g, List<FormattedCharSequence> lines, int x, int y, int bottom) {
        int show = Math.max(1, (bottom - y) / 9);
        this.scroll = Mth.clamp(this.scroll, 0, Math.max(0, lines.size() - show));
        this.paragraph(g, lines, x, y, show, this.scroll, INK);
        this.scrollMarks(g, x, y, bottom, this.scroll, show, lines.size());
    }

    /** The creature's field notes: "Habitat: ...|Temper: ..." in a ruled box. Returns the y below it. */
    private int fieldNotes(GuiGraphicsExtractor g, int x, int y, String notes, int accent) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (String part : notes.split("\\|")) {
            int c = part.indexOf(':');
            MutableComponent line = c < 0 ? Component.literal(part)
                    : Component.literal(part.substring(0, c + 1)).withColor(accent & 0xFFFFFF).append(Component.literal(part.substring(c + 1)).withColor(INK & 0xFFFFFF));
            lines.addAll(this.font.split(line, PW - 6));
        }
        int h = 13 + lines.size() * 9;
        g.fill(x - 1, y, x + PW + 1, y + h, 0x18402A10);
        g.fill(x - 1, y, x + PW + 1, y + 1, accent & 0x00FFFFFF | 0x80000000);
        g.fill(x - 1, y + h - 1, x + PW + 1, y + h, accent & 0x00FFFFFF | 0x80000000);
        Component title = Component.translatable("knowledge.thesift.notes");
        g.text(this.font, title, x + (PW - this.font.width(title)) / 2, y + 2, accent, false);
        for (int i = 0; i < lines.size(); i++) {
            g.text(this.font, lines.get(i), x + 3, y + 12 + i * 9, INK, false);
        }
        return y + h;
    }

    private int contentsHit(Page p, int l, int t, double mx, double my) {
        List<Integer> rows = this.chapterPages(p.chapter());
        int x = l + BW / 2 + 10;
        int y = t + 14 + 12;
        int perCol = 14;
        boolean two = rows.size() > perCol;
        int colW = two ? PW / 2 : PW;
        for (int i = this.scroll * (two ? 2 : 1); i < rows.size(); i++) {
            int k = i - this.scroll * (two ? 2 : 1);
            int col = two ? k / perCol : 0;
            int row = two ? k % perCol : k;
            if (col > 1 || row >= perCol) {
                break;
            }
            int rx = x + col * colW;
            int ry = y + row * 10;
            if (mx >= rx && mx < rx + colW && my >= ry && my < ry + 10) {
                return rows.get(i);
            }
        }
        return -1;
    }

    private List<Integer> chapterPages(int chapter) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < this.pages.size(); i++) {
            Page q = this.pages.get(i);
            if (q.chapter() == chapter && q.kind() != Kind.CONTENTS) {
                out.add(i);
            }
        }
        return out;
    }

    private void contentsRight(GuiGraphicsExtractor g, int x, int y, Page p, int mouseX, int mouseY) {
        Component head = Component.translatable("knowledge.thesift.contents");
        g.text(this.font, head, x + (PW - this.font.width(head)) / 2, y, this.accent(p), false);
        List<Integer> rows = this.chapterPages(p.chapter());
        int perCol = 14;
        boolean two = rows.size() > perCol;
        int colW = two ? PW / 2 : PW;
        int first = this.scroll * (two ? 2 : 1);
        if (first >= rows.size()) {
            this.scroll = 0;
            first = 0;
        }
        for (int i = first; i < rows.size(); i++) {
            int k = i - first;
            int col = two ? k / perCol : 0;
            int row = two ? k % perCol : k;
            if (col > 1 || row >= perCol) {
                break;
            }
            Page q = this.pages.get(rows.get(i));
            int rx = x + col * colW;
            int ry = y + 12 + row * 10;
            boolean known = this.known(q);
            boolean hover = mouseX >= rx && mouseX < rx + colW && mouseY >= ry && mouseY < ry + 10;
            if (hover) {
                g.fill(rx - 1, ry - 1, rx + colW - 1, ry + 9, 0x30C9A24A);
            }
            this.small(g, known ? (q.kind() == Kind.QUEST && Knowledge.has(this.player(), q.quest().doneKey()) ? 1 : 2) : 0, rx, ry);
            Component title = KnowledgePages.title(q);
            String s = title.getString();
            int room = colW - 12;
            if (known) {
                String cut = this.font.plainSubstrByWidth(s, room);
                g.text(this.font, cut.length() < s.length() ? this.font.plainSubstrByWidth(s, room - 6) + ".." : cut, rx + 11, ry, hover ? 0xFF8A5A1A : INK, false);
            } else {
                Component glyphs = Component.literal(this.font.plainSubstrByWidth(s, room - 8)).withStyle(LoreReading.GLYPHS);
                g.text(this.font, glyphs, rx + 11, ry, INK_SOFT, false);
            }
        }
        int shown = (two ? 2 : 1) * perCol;
        if (rows.size() - first > shown) {
            this.small(g, 5, x + PW - 9, y + 12 + perCol * 10);
        }
        if (first > 0) {
            this.small(g, 4, x + PW - 9, y + 2);
        }
    }

    // ------------------------------------------------------------------ recipes

    private void titledRecipe(GuiGraphicsExtractor g, RecipeDisplayEntry e, int x, int y, int mouseX, int mouseY, int accent) {
        ContextMap ctx = SlotDisplayContext.fromLevel(this.minecraft.level);
        ItemStack result = e.display().result().resolveForFirstStack(ctx);
        Component name = result.getHoverName();
        String s = this.font.plainSubstrByWidth(name.getString(), PW);
        g.text(this.font, s, x + (PW - this.font.width(s)) / 2, y, accent, false);
        this.recipe(g, e, x + (PW - 102) / 2, y + 11, mouseX, mouseY);
    }

    /** A crafting grid, the arrow and the result (cycling through each slot's choices); other recipes show their station. */
    private void recipe(GuiGraphicsExtractor g, RecipeDisplayEntry e, int x, int y, int mouseX, int mouseY) {
        ContextMap ctx = SlotDisplayContext.fromLevel(this.minecraft.level);
        RecipeDisplay d = e.display();
        SlotDisplay[] grid = new SlotDisplay[9];
        if (d instanceof ShapedCraftingRecipeDisplay shaped) {
            for (int r = 0; r < shaped.height(); r++) {
                for (int c = 0; c < shaped.width(); c++) {
                    grid[r * 3 + c] = shaped.ingredients().get(r * shaped.width() + c);
                }
            }
        } else if (d instanceof ShapelessCraftingRecipeDisplay shapeless) {
            for (int i = 0; i < Math.min(9, shapeless.ingredients().size()); i++) {
                grid[i] = shapeless.ingredients().get(i);
            }
        } else {
            grid[4] = d.craftingStation();
        }
        for (int i = 0; i < 9; i++) {
            int sx = x + (i % 3) * 18;
            int sy = y + (i / 3) * 18;
            g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, sx, sy, 128, 256, 18, 18, TW, TH);
            if (grid[i] != null) {
                ItemStack stack = this.cycle(grid[i].resolveForStacks(ctx));
                if (!stack.isEmpty()) {
                    g.item(stack, sx + 1, sy + 1);
                    if (mouseX >= sx && mouseX < sx + 18 && mouseY >= sy && mouseY < sy + 18) {
                        g.setTooltipForNextFrame(this.font, stack, mouseX, mouseY);
                    }
                }
            }
        }
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 58, y + 19, 148, 256, 22, 15, TW, TH);
        int rx = x + 84;
        int ry = y + 18;
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, rx, ry, 128, 256, 18, 18, TW, TH);
        ItemStack result = this.cycle(d.result().resolveForStacks(ctx));
        if (!result.isEmpty()) {
            g.item(result, rx + 1, ry + 1);
            g.itemDecorations(this.font, result, rx + 1, ry + 1);
            if (mouseX >= rx && mouseX < rx + 18 && mouseY >= ry && mouseY < ry + 18) {
                g.setTooltipForNextFrame(this.font, result, mouseX, mouseY);
            }
        }
    }

    private ItemStack cycle(List<ItemStack> options) {
        if (options.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return options.get((this.age / 20) % options.size());
    }
}
