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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * F3 Knowledge &amp; lore / I1: the Knowledge Book, laid out like vanilla's book screen - one page at a time, text 114
 * wide from (36, 32), "Page X of Y" at the top right, the page arrows at the foot. Its small extras: a ribbon tab per
 * chapter down the cover's edge (Creatures, Songs, Enchantments, Lore, Places, Items &amp; Recipes, Music &amp;
 * Machines, Heralds &amp; Bosses, the Guide's Notes), each chapter's emblem on its first page, a creature alive on its
 * page, and a quiet page effect behind the text ({@link CodexFx}).
 *
 * <p>Every book page ({@link KnowledgePages.Page}) becomes one or more leaves: its picture side (title, creature,
 * item and recipe, song sheet, lore piece) and then as many text leaves as its words need. Pages you have not
 * discovered are sealed, their titles in glyphs, with a hint. Turn with the arrows, the arrow keys or the wheel; click
 * a lore page to read the whole piece ({@link LoreScreen}).
 */
public class KnowledgeBookScreen extends Screen {
    private static final Identifier TEXTURE = TheSift.id("textures/gui/knowledge_book.png");
    private static final int TW = 256;
    private static final int TH = 256;
    private static final int BOOK = 192;
    private static final int TX = 36;
    private static final int TY = 32;
    private static final int PW = 114;
    private static final int LINES = 14;
    private static final int LORE_LINES = 12;
    private static final int ROWS = 11;
    private static final int BOTTOM = TY + LINES * 9;
    private static final int SHOWCASE_ID = Integer.MAX_VALUE - 2000;
    private static final int INK = 0xFF3A2614;
    private static final int INK_SOFT = 0xFF7A6448;
    private static final int[] ACCENT = {0xFF3F7A3A, 0xFF8A5A1A, 0xFF4E566A, 0xFF1F6F6F, 0xFF7A1F2C, 0xFF9A3A6E, 0xFF543A96, 0xFF8A6A1E, 0xFF6A5A3A};

    /** A leaf: part 0 is a page's picture side (or a chapter's title page), parts 1.. its text (or contents rows). */
    private record Leaf(int page, int part) {
    }

    private List<Page> pages = List.of();
    private List<Leaf> leaves = List.of();
    private final Map<Integer, List<FormattedCharSequence>> text = new HashMap<>();
    private int index;
    private int shownPage = -1;
    private int pageAge;
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
        return (this.width - BOOK) / 2;
    }

    private int top() {
        return 2;
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
            this.leaves = this.buildLeaves();
            for (int i = 0; this.startAt != null && i < this.leaves.size(); i++) {
                if (this.pages.get(this.leaves.get(i).page()).id().equals(this.startAt)) {
                    this.index = i;
                    break;
                }
            }
            this.index = Mth.clamp(this.index, 0, Math.max(0, this.leaves.size() - 1));
            this.minecraft.player.playSound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F);
        }
    }

    private Player player() {
        return this.minecraft.player;
    }

    private boolean known(Page p) {
        return KnowledgePages.known(this.player(), p);
    }

    private @Nullable Leaf leaf() {
        return this.leaves.isEmpty() ? null : this.leaves.get(Mth.clamp(this.index, 0, this.leaves.size() - 1));
    }

    // ------------------------------------------------------------------ leaves

    private List<Leaf> buildLeaves() {
        List<Leaf> out = new ArrayList<>();
        this.text.clear();
        for (int i = 0; i < this.pages.size(); i++) {
            Page p = this.pages.get(i);
            int parts = 1;
            if (p.kind() == Kind.CONTENTS) {
                parts += (this.chapterPages(p.chapter()).size() + ROWS - 1) / ROWS;
            } else if (p.kind() == Kind.LORE && this.known(p)) {
                GlyphText body = new GlyphText(p.lore().id(), Component.translatable(p.lore().bodyKey()).getString(), p.lore().origin.script);
                parts += (body.layout(this.font, PW) + LORE_LINES - 1) / LORE_LINES;
            } else if (p.kind() != Kind.RECIPES && this.known(p)) {
                List<FormattedCharSequence> lines = this.textOf(p);
                this.text.put(i, lines);
                parts += (lines.size() + LINES - 1) / LINES;
            }
            for (int k = 0; k < parts; k++) {
                out.add(new Leaf(i, k));
            }
        }
        return out;
    }

    /** The words of a page's text leaves. */
    private List<FormattedCharSequence> textOf(Page p) {
        int accent = this.accent(p);
        List<FormattedCharSequence> lines = new ArrayList<>();
        switch (p.kind()) {
            case ENTRY -> {
                CodexEntry e = p.entry();
                String notesKey = "codex.thesift." + e.key() + ".notes";
                if (net.minecraft.locale.Language.getInstance().has(notesKey)) {
                    lines.addAll(this.font.split(Component.translatable("knowledge.thesift.notes").withColor(accent & 0xFFFFFF), PW));
                    for (String part : I18n.get(notesKey).split("\\|")) {
                        int c = part.indexOf(':');
                        MutableComponent line = c < 0 ? Component.literal(part)
                                : Component.literal(part.substring(0, c + 1)).withColor(accent & 0xFFFFFF)
                                        .append(Component.literal(part.substring(c + 1)).withColor(INK & 0xFFFFFF));
                        lines.addAll(this.font.split(line, PW));
                    }
                    lines.add(FormattedCharSequence.EMPTY);
                }
                lines.addAll(this.font.split(Component.translatable(e.bodyKey()), PW));
            }
            case SONG -> {
                Song s = p.song();
                lines.addAll(this.font.split(Component.translatable("song.thesift." + s.id() + ".desc"), PW));
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
            }
            case ENCHANT -> {
                String path = p.enchant().unwrapKey().map(k -> k.identifier().getPath()).orElse("");
                lines.addAll(this.font.split(Component.translatableWithFallback("knowledge.thesift.enchantment." + path,
                        I18n.get("knowledge.thesift.ench.none")), PW));
            }
            case QUEST -> {
                Quest q = p.quest();
                lines.addAll(this.font.split(this.labelled("knowledge.thesift.quest.goal",
                        Component.translatable("quest.thesift." + q.id() + ".goal"), accent), PW));
                lines.add(FormattedCharSequence.EMPTY);
                lines.addAll(this.font.split(Component.literal("“").append(Component.translatable("quest.thesift." + q.id() + ".line"))
                        .append("”"), PW));
                if (Knowledge.has(this.player(), q.doneKey()) && q.hasDoneLine()) {
                    lines.add(FormattedCharSequence.EMPTY);
                    lines.addAll(this.font.split(Component.literal("“").append(Component.translatable("quest.thesift." + q.id() + ".done"))
                            .append("”").withColor(0x3F7A3A), PW));
                }
            }
            default -> {
            }
        }
        return lines;
    }

    // ------------------------------------------------------------------ paging

    private void turnTo(int target) {
        if (this.leaves.isEmpty()) {
            return;
        }
        target = Mth.clamp(target, 0, this.leaves.size() - 1);
        if (target == this.index) {
            return;
        }
        this.index = target;
        this.player().playSound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 0.9F + this.player().getRandom().nextFloat() * 0.25F);
    }

    private int firstLeafOf(int page) {
        for (int i = 0; i < this.leaves.size(); i++) {
            if (this.leaves.get(i).page() == page) {
                return i;
            }
        }
        return 0;
    }

    private int contentsOf(int chapter) {
        for (int i = 0; i < this.pages.size(); i++) {
            if (this.pages.get(i).kind() == Kind.CONTENTS && this.pages.get(i).chapter() == chapter) {
                return this.firstLeafOf(i);
            }
        }
        return 0;
    }

    private void prepare() {
        Leaf lf = this.leaf();
        if (lf == null || this.shownPage == lf.page()) {
            return;
        }
        this.shownPage = lf.page();
        this.pageAge = 0;
        Page p = this.pages.get(lf.page());
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
                living.setId(SHOWCASE_ID - lf.page());
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
        this.pageAge++;
        this.fx.tick(this.minecraft.player, 172);
        Leaf lf = this.leaf();
        if (lf == null) {
            return;
        }
        Page p = this.pages.get(lf.page());
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
        if (event.key() == 262 || event.key() == 267) { // right arrow, page down
            this.turnTo(this.index + 1);
            return true;
        }
        if (event.key() == 263 || event.key() == 266) { // left arrow, page up
            this.turnTo(this.index - 1);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        this.turnTo(this.index - (int) Math.signum(scrollY));
        return true;
    }

    private boolean over(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private int tabX(int i, boolean raised) {
        return this.left() + (raised ? 169 : 165);
    }

    private int tabY(int i) {
        return this.top() + 10 + i * 18;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        int l = this.left();
        int t = this.top();
        for (int i = 0; i < CodexEntries.ORDER.length; i++) {
            if (this.over(mx, my, this.tabX(i, false), this.tabY(i), 26, 14)) {
                this.turnTo(this.contentsOf(CodexEntries.ORDER[i]));
                return true;
            }
        }
        if (this.index > 0 && this.over(mx, my, l + 43, t + 157, 23, 13)) {
            this.turnTo(this.index - 1);
            return true;
        }
        if (this.index < this.leaves.size() - 1 && this.over(mx, my, l + 116, t + 157, 23, 13)) {
            this.turnTo(this.index + 1);
            return true;
        }
        Leaf lf = this.leaf();
        if (lf != null) {
            Page p = this.pages.get(lf.page());
            if (p.kind() == Kind.CONTENTS && lf.part() > 0) {
                List<Integer> rows = this.chapterPages(p.chapter());
                for (int r = 0; r < ROWS; r++) {
                    int k = (lf.part() - 1) * ROWS + r;
                    if (k < rows.size() && this.over(mx, my, l + TX, t + TY + 10 + r * 10, PW, 10)) {
                        this.turnTo(this.firstLeafOf(rows.get(k)));
                        return true;
                    }
                }
            }
            if (p.kind() == Kind.LORE && this.known(p) && this.over(mx, my, l + 24, t + 5, 138, 150)) {
                this.minecraft.gui.setScreen(new LoreScreen(p.lore(), this));
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        Leaf lf = this.leaf();
        if (lf == null) {
            return;
        }
        this.prepare();
        int l = this.left();
        int t = this.top();
        Page page = this.pages.get(lf.page());

        // the chapter tabs, tucked under the cover's edge: the open chapter's (and a hovered one) stands out
        for (int i = 0; i < CodexEntries.ORDER.length; i++) {
            int ch = CodexEntries.ORDER[i];
            boolean hover = this.over(mouseX, mouseY, this.tabX(i, false), this.tabY(i), 26, 14);
            boolean raised = ch == page.chapter() || hover;
            g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.tabX(i, raised), this.tabY(i), raised ? 214 : 192, 14 * ch, 22, 14, TW, TH);
            if (hover) {
                g.setTooltipForNextFrame(this.font, Component.translatable("knowledge.thesift.chapter." + CodexEntries.CATEGORY_KEYS[ch]), mouseX, mouseY);
            }
        }
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, l, t, 0, 0, BOOK, BOOK, TW, TH);
        this.fx.render(g, l + 24, t + 5, 138, 172, a);

        Component indicator = Component.translatable("book.pageIndicator", this.index + 1, Math.max(1, this.leaves.size()));
        g.text(this.font, indicator, l + 148 - this.font.width(indicator), t + 16, INK_SOFT, false);

        int x = l + TX;
        int y = t + TY;
        if (!this.known(page) && page.kind() != Kind.CONTENTS) {
            this.locked(g, x, y, page);
        } else if (lf.part() == 0) {
            this.front(g, x, y, page, mouseX, mouseY, a);
        } else {
            this.words(g, x, y, page, lf, mouseX, mouseY, a);
        }

        if (this.index > 0) {
            boolean hover = this.over(mouseX, mouseY, l + 43, t + 157, 23, 13);
            g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, l + 43, t + 157, hover ? 69 : 46, 192, 23, 13, TW, TH);
        }
        if (this.index < this.leaves.size() - 1) {
            boolean hover = this.over(mouseX, mouseY, l + 116, t + 157, 23, 13);
            g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, l + 116, t + 157, hover ? 23 : 0, 192, 23, 13, TW, TH);
        }
    }

    // ------------------------------------------------------------------ pieces

    private int accent(Page p) {
        return ACCENT[Mth.clamp(p.chapter(), 0, ACCENT.length - 1)];
    }

    private void rule(GuiGraphicsExtractor g, int x, int y) {
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + (PW - 100) / 2, y, 0, 206, 100, 5, TW, TH);
    }

    /** Title, tagline and the rule under them, centred; returns the y below. */
    private int header(GuiGraphicsExtractor g, int x, int y, Component title, @Nullable Component tagline, int accent) {
        List<FormattedCharSequence> tl = this.font.split(title, PW);
        for (int i = 0; i < Math.min(2, tl.size()); i++) {
            g.text(this.font, tl.get(i), x + (PW - this.font.width(tl.get(i))) / 2, y, INK, false);
            y += 10;
        }
        if (tagline != null) {
            List<FormattedCharSequence> tag = this.font.split(tagline, PW);
            for (int i = 0; i < Math.min(2, tag.size()); i++) {
                g.text(this.font, tag.get(i), x + (PW - this.font.width(tag.get(i))) / 2, y, accent, false);
                y += 9;
            }
        }
        this.rule(g, x, y + 1);
        return y + 8;
    }

    private void small(GuiGraphicsExtractor g, int i, int x, int y) {
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 92 + 10 * i, 192, 9, 9, TW, TH);
    }

    private void bigItem(GuiGraphicsExtractor g, ItemStack stack, int cx, int cy, float scale) {
        float b = Mth.sin(this.pageAge * 0.08F) * 1.5F;
        g.pose().pushMatrix();
        g.pose().translate(cx - 8 * scale, cy - 8 * scale + b);
        g.pose().scale(scale, scale);
        g.item(stack, 0, 0);
        g.pose().popMatrix();
    }

    private void centred(GuiGraphicsExtractor g, Component c, int x, int y, int colour) {
        g.text(this.font, c, x + (PW - this.font.width(c)) / 2, y, colour, false);
    }

    private void paragraph(GuiGraphicsExtractor g, List<FormattedCharSequence> lines, int x, int y, int maxLines, int colour) {
        for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
            g.text(this.font, lines.get(i), x, y + i * 9, colour, false);
        }
    }

    private void showcase(GuiGraphicsExtractor g, int x, int y0, int y1, int mouseY) {
        if (this.showcase == null) {
            return;
        }
        float h = Math.max(this.showcase.getBbHeight(), this.showcase.getBbWidth() * 0.9F);
        int size = Mth.clamp((int) ((y1 - y0) * 0.62F / h), 12, 50);
        float turn = Mth.sin(this.pageAge * 0.02F) * 1.1F;
        float lookY = (float) Math.atan(((y0 + y1) / 2.0F - mouseY) / 60.0F);
        InventoryScreen.renderEntityInInventoryFollowsAngle(g, x + 2, y0, x + PW - 2, y1, size, 0.0625F, turn, lookY, this.showcase);
    }

    /** A sealed page: its title in glyphs, a wax seal, and a hint of where to look. */
    private void locked(GuiGraphicsExtractor g, int x, int y, Page p) {
        Component title = KnowledgePages.title(p).copy().withStyle(LoreReading.GLYPHS);
        List<FormattedCharSequence> tl = this.font.split(title, PW);
        for (int i = 0; i < Math.min(2, tl.size()); i++) {
            g.text(this.font, tl.get(i), x + (PW - this.font.width(tl.get(i))) / 2, y - 4 + i * 10, INK_SOFT, false);
        }
        this.rule(g, x, y + 18);
        g.pose().pushMatrix();
        g.pose().translate(x + PW / 2.0F - 13.5F, y + 28);
        g.pose().scale(3.0F, 3.0F);
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, 92, 192, 9, 9, TW, TH);
        g.pose().popMatrix();
        this.centred(g, Component.translatable("knowledge.thesift.undiscovered"), x, y + 62, INK_SOFT);
        this.paragraph(g, this.font.split(Component.translatable("knowledge.thesift.hint." + KnowledgePages.hintKind(p)), PW), x, y + 76, 5, INK_SOFT);
    }

    /** A page's picture side. */
    private void front(GuiGraphicsExtractor g, int x, int y, Page p, int mouseX, int mouseY, float a) {
        int accent = this.accent(p);
        int top = y - 4;
        switch (p.kind()) {
            case CONTENTS -> this.chapterTitle(g, x, top, p, accent);
            case ENTRY -> {
                CodexEntry e = p.entry();
                int y1 = this.header(g, x, top, Component.translatable(e.titleKey()), Component.translatable(e.taglineKey()), accent);
                if (e.entity() != null) {
                    this.showcase(g, x, y1 + 2, BOTTOM + this.top() - 12, mouseY);
                    BandVoice voice = BandRegistry.voiceOf(e.entity().get());
                    if (voice != null) {
                        this.centred(g, Component.translatable("knowledge.thesift.voice", Component.translatable(voice.instrumentKey())), x,
                                BOTTOM + this.top() - 9, INK_SOFT);
                    }
                } else if (e.item() != null) {
                    ItemStack stack = new ItemStack(e.item().get());
                    var recipe = KnowledgePages.recipeFor(e.item().get());
                    if (recipe == null) {
                        this.bigItem(g, stack, x + PW / 2, y1 + 34, 3.0F);
                    } else {
                        this.centred(g, Component.translatable("knowledge.thesift.recipe"), x, y1 + 2, accent);
                        this.recipe(g, recipe, x + (PW - 102) / 2, y1 + 13, mouseX, mouseY);
                    }
                }
            }
            case SONG -> {
                Song s = p.song();
                int y1 = this.header(g, x, top, Component.translatable("song.thesift." + s.id()),
                        Component.translatable("knowledge.thesift.song.instrument", Component.translatable(s.instrumentKey() + ".short")), accent);
                g.pose().pushMatrix();
                g.pose().translate(x, y1 + 2);
                g.pose().scale(PW / (float) SheetTooltip.W, PW / (float) SheetTooltip.W);
                SheetTooltip.draw(g, this.font, s, 0, 0);
                g.pose().popMatrix();
                StringBuilder notes = new StringBuilder();
                for (int i = 0; i < s.length(); i++) {
                    notes.append(i == 0 ? "" : " ").append(Notes.name(s.note(i)));
                }
                int sheetH = Math.round(SheetTooltip.H * PW / (float) SheetTooltip.W);
                this.paragraph(g, this.font.split(Component.literal(notes.toString()), PW), x, y1 + sheetH + 5, 1, INK_SOFT);
            }
            case ENCHANT -> {
                Holder<net.minecraft.world.item.enchantment.Enchantment> h = p.enchant();
                int y1 = this.header(g, x, top, h.value().description(), Component.translatable("knowledge.thesift.ench.max", h.value().getMaxLevel()), accent);
                g.text(this.font, Component.translatable("knowledge.thesift.ench.fits"), x, y1 + 3, accent, false);
                List<ItemStack> items = new ArrayList<>();
                h.value().getSupportedItems().stream().limit(18).forEach(i -> items.add(new ItemStack(i.value())));
                for (int i = 0; i < items.size(); i++) {
                    int ix = x + 3 + (i % 6) * 18;
                    int iy = y1 + 14 + (i / 6) * 18;
                    g.item(items.get(i), ix, iy);
                    if (mouseX >= ix && mouseX < ix + 16 && mouseY >= iy && mouseY < iy + 16) {
                        g.setTooltipForNextFrame(this.font, items.get(i), mouseX, mouseY);
                    }
                }
            }
            case LORE -> {
                int y1 = top;
                if (this.loreTitle != null) {
                    int n = this.loreTitle.layout(this.font, PW);
                    this.loreTitle.draw(g, this.font, x, y1, 0, Math.min(2, n), INK, p.lore().origin.accent, a);
                    y1 += Math.min(2, n) * 10;
                }
                this.centred(g, Component.translatable("knowledge.thesift.lore.from", Component.translatable("lore.thesift.origin." + p.lore().origin.id())),
                        x, y1, accent);
                this.rule(g, x, y1 + 10);
                this.bigItem(g, p.lore().stack(), x + PW / 2, y1 + 40, 3.0F);
                this.paragraph(g, this.font.split(Component.translatable("knowledge.thesift.lore.found", Component.translatable(p.lore().foundKey())), PW),
                        x, y1 + 66, 2, INK_SOFT);
                if (this.loreBody != null && p.lore().origin.script > 0.0F) {
                    Component d = Component.translatable("lore.thesift.deciphered", Math.round(this.loreBody.readable() * 100.0F));
                    this.centred(g, d, x, BOTTOM + this.top() - 20, this.loreBody.morphing() ? 0xFF2FA8A0 : accent);
                }
                this.paragraph(g, this.font.split(Component.translatable("knowledge.thesift.lore.open"), PW), x, BOTTOM + this.top() - 10, 1, INK_SOFT);
            }
            case QUEST -> {
                Quest q = p.quest();
                int y1 = this.header(g, x, top, Component.translatable("quest.thesift." + q.id() + ".title"), null, accent);
                this.showcase(g, x, y1 + 2, BOTTOM + this.top() - 14, mouseY);
                boolean done = Knowledge.has(this.player(), q.doneKey());
                Component status = Component.translatable(done ? "knowledge.thesift.quest.done" : "knowledge.thesift.quest.current");
                int sw = this.font.width(status) + 12;
                this.small(g, done ? 1 : 3, x + (PW - sw) / 2, BOTTOM + this.top() - 11);
                g.text(this.font, status, x + (PW - sw) / 2 + 12, BOTTOM + this.top() - 10, done ? 0xFF3F7A3A : accent, false);
            }
            case RECIPES -> {
                List<RecipeDisplayEntry> r = p.recipes();
                if (r.isEmpty()) {
                    int y1 = this.header(g, x, top, Component.translatable("knowledge.thesift.recipes.title"), null, accent);
                    this.paragraph(g, this.font.split(Component.translatable("knowledge.thesift.recipes.none"), PW), x, y1 + 4, 10, INK);
                    return;
                }
                for (int i = 0; i < Math.min(2, r.size()); i++) {
                    this.titledRecipe(g, r.get(i), x, top - 2 + i * 64, mouseX, mouseY, accent);
                }
            }
        }
    }

    /** A chapter's first page: its name, its emblem, its introduction and how much of it you have found. */
    private void chapterTitle(GuiGraphicsExtractor g, int x, int y, Page p, int accent) {
        Component name = Component.translatable("knowledge.thesift.chapter." + CodexEntries.CATEGORY_KEYS[p.chapter()]);
        g.pose().pushMatrix();
        g.pose().translate(x + PW / 2.0F, y);
        g.pose().scale(1.5F, 1.5F);
        g.text(this.font, name, -this.font.width(name) / 2, 0, accent, false);
        g.pose().popMatrix();
        this.rule(g, x, y + 15);
        g.pose().pushMatrix();
        g.pose().translate(x + PW / 2.0F - 18.0F, y + 23);
        g.pose().scale(3.0F, 3.0F);
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, 104 + 13 * p.chapter(), 206, 12, 12, TW, TH);
        g.pose().popMatrix();
        this.paragraph(g, this.font.split(Component.translatable("knowledge.thesift.chapter." + CodexEntries.CATEGORY_KEYS[p.chapter()] + ".intro"), PW),
                x, y + 63, 5, INK);
        int[] count = this.count(p.chapter());
        this.centred(g, Component.translatable("knowledge.thesift.discovered", count[0], count[1]), x, y + 111, INK_SOFT);
        int bx = x + (PW - 102) / 2;
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, bx, y + 122, 0, 212, 102, 7, TW, TH);
        int fill = count[1] == 0 ? 0 : 100 * count[0] / count[1];
        if (fill > 0) {
            g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, bx + 1, y + 123, 0, 220, fill, 5, TW, TH);
        }
    }

    /** A text leaf: the page's words from where the last leaf stopped (or a chapter's contents rows). */
    private void words(GuiGraphicsExtractor g, int x, int y, Page p, Leaf lf, int mouseX, int mouseY, float a) {
        if (p.kind() == Kind.CONTENTS) {
            this.contents(g, x, y, p, lf.part() - 1, mouseX, mouseY);
            return;
        }
        if (p.kind() == Kind.LORE) {
            if (this.loreBody != null) {
                this.loreBody.layout(this.font, PW);
                boolean soul = p.lore().origin == com.thesift.knowledge.Lore.Origin.SOUL;
                this.loreBody.draw(g, this.font, x, y, (lf.part() - 1) * LORE_LINES, LORE_LINES, soul ? 0xFF22305E : INK,
                        soul ? 0xFF2F6FB8 : p.lore().origin.accent, a);
            }
            return;
        }
        List<FormattedCharSequence> lines = this.text.getOrDefault(lf.page(), List.of());
        int first = (lf.part() - 1) * LINES;
        for (int i = 0; i < LINES && first + i < lines.size(); i++) {
            g.text(this.font, lines.get(first + i), x, y + i * 9, INK, false);
        }
    }

    private void contents(GuiGraphicsExtractor g, int x, int y, Page p, int part, int mouseX, int mouseY) {
        this.centred(g, Component.translatable("knowledge.thesift.contents"), x, y - 4, this.accent(p));
        List<Integer> rows = this.chapterPages(p.chapter());
        for (int r = 0; r < ROWS; r++) {
            int k = part * ROWS + r;
            if (k >= rows.size()) {
                break;
            }
            Page q = this.pages.get(rows.get(k));
            int ry = y + 10 + r * 10;
            boolean known = this.known(q);
            boolean hover = this.over(mouseX, mouseY, x, ry, PW, 10);
            if (hover) {
                g.fill(x - 1, ry - 1, x + PW, ry + 9, 0x30C9A24A);
            }
            this.small(g, known ? (q.kind() == Kind.QUEST && Knowledge.has(this.player(), q.quest().doneKey()) ? 1 : 2) : 0, x, ry);
            String s = KnowledgePages.title(q).getString();
            int room = PW - 12;
            if (known) {
                String cut = this.font.plainSubstrByWidth(s, room);
                g.text(this.font, cut.length() < s.length() ? this.font.plainSubstrByWidth(s, room - 6) + ".." : cut, x + 11, ry, hover ? 0xFF8A5A1A : INK, false);
            } else {
                g.text(this.font, Component.literal(this.font.plainSubstrByWidth(s, room - 8)).withStyle(LoreReading.GLYPHS), x + 11, ry, INK_SOFT, false);
            }
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

    private MutableComponent labelled(String key, Component value, int accent) {
        String label = I18n.get(key, "\u0000");
        int cut = label.indexOf('\u0000');
        if (cut < 0) {
            return Component.translatable(key, value);
        }
        return Component.literal(label.substring(0, cut)).withColor(accent & 0xFFFFFF).append(value.copy().withColor(INK & 0xFFFFFF))
                .append(Component.literal(label.substring(cut + 1)));
    }

    // ------------------------------------------------------------------ recipes

    private void titledRecipe(GuiGraphicsExtractor g, RecipeDisplayEntry e, int x, int y, int mouseX, int mouseY, int accent) {
        ContextMap ctx = SlotDisplayContext.fromLevel(this.minecraft.level);
        ItemStack result = e.display().result().resolveForFirstStack(ctx);
        String s = this.font.plainSubstrByWidth(result.getHoverName().getString(), PW);
        g.text(this.font, s, x + (PW - this.font.width(s)) / 2, y, accent, false);
        this.recipe(g, e, x + (PW - 102) / 2, y + 10, mouseX, mouseY);
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
            g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, sx, sy, 152, 192, 18, 18, TW, TH);
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
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 58, y + 19, 172, 192, 22, 15, TW, TH);
        int rx = x + 84;
        int ry = y + 18;
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, rx, ry, 152, 192, 18, 18, TW, TH);
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
