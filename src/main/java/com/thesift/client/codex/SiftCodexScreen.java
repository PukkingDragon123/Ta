package com.thesift.client.codex;

import com.thesift.TheSift;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The Sift Codex: a leather-bound field guide to The Sift. Ribbon tabs along the top pick a
 * chapter; the arrows (or a click on a page) turn the page with a little flip. Each left page shows
 * its subject - creatures stand on the page alive, idling and playing their attacks and special
 * moves - and the right page tells you about it.
 */
public class SiftCodexScreen extends Screen {
    private static final Identifier TEXTURE = TheSift.id("textures/gui/codex.png");
    private static final int TW = 512;
    private static final int TH = 256;
    private static final int BW = 292;
    private static final int BH = 180;
    private static final int PAGE_W = 128;
    private static final int FLIP_TICKS = 8;
    private static final int SHOWCASE_ID = Integer.MAX_VALUE - 1000;
    private static final int INK = 0xFF2B1D10;
    private static final int INK_SOFT = 0xFF5A4630;
    private static final int[] TAB_INK = {0xFF1F7F90, 0xFFB0507A, 0xFF9C7A20, 0xFF3F8F68, 0xFF1D2B47};

    private int index;
    private int shownIndex = -1;
    private int flipFrom = -1;
    private int flipTicks;
    private int flipDir = 1;
    private int pageAge;
    private @Nullable LivingEntity showcase;

    public SiftCodexScreen() {
        this(0);
    }

    public SiftCodexScreen(int startIndex) {
        super(Component.translatable("item.thesift.sift_codex"));
        this.index = Mth.clamp(startIndex, 0, CodexEntries.all().size() - 1);
    }

    private int left() {
        return (this.width - BW) / 2;
    }

    private int top() {
        return (this.height - BH) / 2 + 6;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------ paging

    private void turnTo(int target) {
        int n = CodexEntries.all().size();
        target = Math.floorMod(target, n);
        if (target == this.index) {
            return;
        }
        this.flipDir = target > this.index ? 1 : -1;
        this.flipFrom = this.index;
        this.flipTicks = FLIP_TICKS;
        this.index = target;
        if (this.minecraft.player != null) {
            this.minecraft.player.playSound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F);
        }
    }

    private void prepareShowcase() {
        if (this.shownIndex == this.index) {
            return;
        }
        this.shownIndex = this.index;
        this.pageAge = 0;
        CodexEntry e = CodexEntries.all().get(this.index);
        this.showcase = null;
        if (e.entity() != null && this.minecraft.level != null) {
            Entity made = e.entity().get().create(this.minecraft.level, EntitySpawnReason.LOAD);
            if (made instanceof LivingEntity living) {
                // never added to a level, so it needs an id of its own before a renderer asks for one
                living.setId(SHOWCASE_ID - this.index);
                this.showcase = living;
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.prepareShowcase();
        if (this.flipTicks > 0) {
            this.flipTicks--;
        }
        this.pageAge++;
        if (this.showcase != null) {
            this.showcase.tickCount++;
            CodexEntry e = CodexEntries.all().get(this.index);
            if (e.animator() != null) {
                e.animator().accept(this.showcase, this.pageAge);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        int l = this.left();
        int t = this.top();
        for (int c = 0; c < CodexEntries.CATEGORY_KEYS.length; c++) {
            int tx = l + 22 + c * 28;
            if (mx >= tx && mx < tx + 24 && my >= t - 13 && my < t + 1) {
                this.turnTo(CodexEntries.firstOf(c));
                return true;
            }
        }
        if (my >= t + BH - 24 && my < t + BH - 10) {
            if (mx >= l + 14 && mx < l + 34) {
                this.turnTo(this.index - 1);
                return true;
            }
            if (mx >= l + BW - 34 && mx < l + BW - 14) {
                this.turnTo(this.index + 1);
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
        this.prepareShowcase();
        int l = this.left();
        int t = this.top();
        List<CodexEntry> all = CodexEntries.all();
        CodexEntry entry = all.get(this.index);

        // ribbons: the open chapter's ribbon stands taller
        for (int c = 0; c < CodexEntries.CATEGORY_KEYS.length; c++) {
            boolean sel = c == entry.category();
            int tx = l + 22 + c * 28;
            g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, tx, t - (sel ? 13 : 10), c * 26, sel ? 196 : 184, 24, 12, TW, TH);
            if (mouseX >= tx && mouseX < tx + 24 && mouseY >= t - 13 && mouseY < t + 1) {
                g.setTooltipForNextFrame(this.font, Component.translatable("codex.thesift.chapter." + CodexEntries.CATEGORY_KEYS[c]), mouseX, mouseY);
            }
        }
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, l, t, 0, 0, BW, BH, TW, TH);

        float flip = this.flipTicks > 0 ? 1.0F - (this.flipTicks - a) / FLIP_TICKS : 1.0F;
        boolean flipping = this.flipTicks > 0 && this.flipFrom >= 0;
        if (!flipping) {
            this.drawLeft(g, l + 16, t + 14, entry, mouseX, mouseY, true);
            this.drawRight(g, l + BW / 2 + 8, t + 14, entry);
        } else {
            // the turning leaf: the old page folds in towards the spine, then the new one opens out
            CodexEntry from = all.get(this.flipFrom);
            boolean forward = this.flipDir > 0;
            CodexEntry stillLeft = forward ? from : entry;
            CodexEntry stillRight = forward ? entry : from;
            this.drawLeft(g, l + 16, t + 14, stillLeft, mouseX, mouseY, false);
            this.drawRight(g, l + BW / 2 + 8, t + 14, stillRight);
            float k = flip < 0.5F ? 1.0F - flip * 2.0F : flip * 2.0F - 1.0F;
            boolean firstHalf = flip < 0.5F;
            int spine = l + BW / 2;
            int px0 = l + 12;
            int pw = BW / 2 - 14;
            int w = Math.max(1, Math.round(pw * k));
            int srcX = forward == firstHalf ? spine - l : 12;
            int drawX = forward == firstHalf ? spine : spine - w;
            // paper of the turning leaf, squeezed towards the spine, with a soft shadow at its edge
            g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, drawX, t + 11, srcX, 11, w, BH - 22, pw, BH - 22, TW, TH);
            int shadow = (int) (90 * (1.0F - k));
            g.fill(drawX, t + 11, drawX + w, t + BH - 11, shadow << 24);
            g.pose().pushMatrix();
            g.pose().translate(drawX, 0);
            g.pose().scale(k, 1.0F);
            if (forward == firstHalf) {
                this.drawRight(g, 8, t + 14, firstHalf ? from : entry);
            } else {
                this.drawLeft(g, 4, t + 14, firstHalf ? from : entry, mouseX, mouseY, false);
            }
            g.pose().popMatrix();
            int edge = forward == firstHalf ? drawX + w - 1 : drawX;
            g.verticalLine(edge, t + 11, t + BH - 12, 0x60000000);
        }

        // page-turn arrows
        boolean hoverPrev = mouseX >= l + 14 && mouseX < l + 34 && mouseY >= t + BH - 24 && mouseY < t + BH - 10;
        boolean hoverNext = mouseX >= l + BW - 34 && mouseX < l + BW - 14 && mouseY >= t + BH - 24 && mouseY < t + BH - 10;
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, l + 15, t + BH - 22, 148, hoverPrev ? 194 : 184, 18, 10, TW, TH);
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, l + BW - 33, t + BH - 22, 128, hoverNext ? 194 : 184, 18, 10, TW, TH);
        String pageNo = (this.index + 1) + " / " + all.size();
        g.text(this.font, pageNo, l + BW - 40 - this.font.width(pageNo), t + BH - 21, INK_SOFT, false);
    }

    /** Left page: name, tagline and the living showcase. */
    private void drawLeft(GuiGraphicsExtractor g, int x, int y, CodexEntry e, int mouseX, int mouseY, boolean live) {
        Component title = Component.translatable(e.titleKey());
        g.text(this.font, title, x + (PAGE_W - this.font.width(title)) / 2, y, INK, false);
        int ink = TAB_INK[e.category()];
        List<FormattedCharSequence> tag = this.font.split(Component.translatable(e.taglineKey()), PAGE_W - 6);
        for (int i = 0; i < Math.min(2, tag.size()); i++) {
            g.text(this.font, tag.get(i), x + (PAGE_W - this.font.width(tag.get(i))) / 2, y + 11 + i * 9, ink, false);
        }
        int boxY0 = y + 32;
        int boxY1 = y + 132;
        if (live && this.showcase != null && e == CodexEntries.all().get(this.index)) {
            float h = Math.max(this.showcase.getBbHeight(), this.showcase.getBbWidth() * 0.9F);
            int size = Mth.clamp((int) (64.0F / h), 14, 52);
            float turn = Mth.sin(this.pageAge * 0.02F) * 1.1F;
            float lookY = (float) Math.atan(((boxY0 + boxY1) / 2.0F - mouseY) / 60.0F);
            InventoryScreen.renderEntityInInventoryFollowsAngle(g, x + 4, boxY0, x + PAGE_W - 4, boxY1, size, 0.0625F, turn, lookY, this.showcase);
        } else if (e.item() != null) {
            ItemStack stack = new ItemStack(e.item().get());
            float bob = live ? Mth.sin(this.pageAge * 0.08F) * 2.0F : 0.0F;
            g.pose().pushMatrix();
            g.pose().translate(x + PAGE_W / 2.0F - 24, (boxY0 + boxY1) / 2.0F - 24 + bob);
            g.pose().scale(3.0F, 3.0F);
            g.item(stack, 0, 0);
            g.pose().popMatrix();
        }
    }

    /** Right page: what it is, what it does. */
    private void drawRight(GuiGraphicsExtractor g, int x, int y, CodexEntry e) {
        List<FormattedCharSequence> lines = this.font.split(Component.translatable(e.bodyKey()), PAGE_W - 4);
        int max = 15;
        for (int i = 0; i < Math.min(max, lines.size()); i++) {
            g.text(this.font, lines.get(i), x, y + i * 9, INK, false);
        }
    }
}
