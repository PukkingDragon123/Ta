package com.thesift.client.knowledge;

import com.thesift.knowledge.Glyphs;
import com.thesift.knowledge.LoreReading;
import com.thesift.registry.ModKnowledge;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * F3: a paragraph partly written in the ancient script. Words the player cannot read yet are drawn in the
 * enchanting-table glyphs; when a new clue makes some of them readable they morph back into English, letter by
 * letter, with a faint Sculk whisper. What each text looked like when last seen is remembered for the session, so
 * the words a new clue unlocks morph the next time you look.
 */
public final class GlyphText {
    /** The level of understanding each text was last shown at. */
    private static final Map<String, Float> SEEN = new HashMap<>();
    private static final int MORPH_TICKS = 26;
    private static final int STAGGER = 3;

    private record Word(String text, int index, boolean ancient, float threshold) {
    }

    private final String id;
    private final List<Word> words = new ArrayList<>();
    private final float share;
    private float from = -1.0F;
    private float to = -1.0F;
    private int morphStart = -1;
    private int age;
    private List<List<Word>> lines = List.of();
    private int laidOutWidth = -1;

    public GlyphText(String id, String text, float share) {
        this.id = id;
        this.share = share;
        String[] split = text.split(" ");
        for (int i = 0; i < split.length; i++) {
            if (!split[i].isEmpty()) {
                this.words.add(new Word(split[i], i, Glyphs.ancient(id, i, share), Glyphs.threshold(id, i)));
            }
        }
    }

    public String id() {
        return this.id;
    }

    /** Called every tick with the player's current level of understanding. */
    public void tick(float level) {
        this.age++;
        if (this.to < 0.0F) {
            Float seen = SEEN.get(this.id);
            this.from = seen == null ? level : Math.min(seen, level);
            this.to = level;
            if (this.from < this.to && this.anyBetween(this.from, this.to)) {
                this.startMorph();
            } else {
                this.from = level;
                SEEN.put(this.id, level);
            }
            return;
        }
        if (level > this.to + 1.0E-4F) {
            // a new clue while reading: the newly readable words morph now
            this.from = this.morphStart >= 0 ? this.from : this.to;
            this.to = level;
            if (this.anyBetween(this.from, this.to)) {
                this.startMorph();
            }
        }
        if (this.morphStart >= 0 && this.age - this.morphStart > MORPH_TICKS + STAGGER * 12) {
            this.morphStart = -1;
            this.from = this.to;
            SEEN.put(this.id, this.to);
        }
    }

    private boolean anyBetween(float lo, float hi) {
        for (Word w : this.words) {
            if (w.ancient && w.threshold >= lo && w.threshold < hi) {
                return true;
            }
        }
        return false;
    }

    private void startMorph() {
        this.morphStart = this.age;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.playSound(ModKnowledge.WHISPER.get(), 0.45F, 0.9F + mc.player.getRandom().nextFloat() * 0.2F);
        }
    }

    public boolean morphing() {
        return this.morphStart >= 0;
    }

    /** Share of this text's ancient words the player can read now. */
    public float readable() {
        int n = 0;
        int r = 0;
        for (Word w : this.words) {
            if (w.ancient) {
                n++;
                if (w.threshold < this.to) {
                    r++;
                }
            }
        }
        return n == 0 ? 1.0F : r / (float) n;
    }

    private static int wordWidth(Font font, String s) {
        return Math.max(font.width(s), font.width(Component.literal(s).withStyle(LoreReading.GLYPHS)));
    }

    /** Lines of words within {@code width} pixels (laid out once per width). */
    public int layout(Font font, int width) {
        if (width != this.laidOutWidth) {
            this.laidOutWidth = width;
            List<List<Word>> out = new ArrayList<>();
            List<Word> line = new ArrayList<>();
            int x = 0;
            int space = font.width(" ");
            for (Word w : this.words) {
                int ww = wordWidth(font, w.text);
                if (!line.isEmpty() && x + space + ww > width) {
                    out.add(line);
                    line = new ArrayList<>();
                    x = 0;
                }
                x += (line.isEmpty() ? 0 : space) + ww;
                line.add(w);
            }
            if (!line.isEmpty()) {
                out.add(line);
            }
            this.lines = out;
        }
        return this.lines.size();
    }

    /**
     * Draws lines {@code first} .. {@code first + count - 1} at (x, y), 10 pixels apart. {@code ink} for English,
     * {@code glyph} for the ancient script.
     */
    public void draw(GuiGraphicsExtractor g, Font font, int x, int y, int first, int count, int ink, int glyph, float partial) {
        int space = font.width(" ");
        float now = this.age + partial;
        for (int li = first; li < Math.min(this.lines.size(), first + count); li++) {
            int cx = x;
            int cy = y + (li - first) * 10;
            for (Word w : this.lines.get(li)) {
                int ww = wordWidth(font, w.text);
                boolean hiddenNow = w.ancient && w.threshold >= this.to;
                boolean wasHidden = w.ancient && w.threshold >= this.from;
                if (!hiddenNow && wasHidden && this.morphStart >= 0) {
                    // morphing: the letters turn to English one by one, left to right, with a shimmer on the turning letter
                    int order = Math.floorMod(w.index * 7, 12);
                    float p = (now - this.morphStart - order * STAGGER) / MORPH_TICKS;
                    int n = w.text.length();
                    int shown = Math.max(0, Math.min(n, (int) (p * n)));
                    String eng = w.text.substring(0, shown);
                    String rest = w.text.substring(shown);
                    g.text(font, eng, cx, cy, ink, false);
                    int ex = cx + font.width(eng);
                    if (!rest.isEmpty()) {
                        int shimmer = p > 0.0F ? 0xFF6FF4EC : glyph;
                        g.text(font, Component.literal(rest.substring(0, 1)).withStyle(LoreReading.GLYPHS), ex, cy, shimmer, false);
                        g.text(font, Component.literal(rest.substring(1)).withStyle(LoreReading.GLYPHS), ex + font.width(
                                Component.literal(rest.substring(0, 1)).withStyle(LoreReading.GLYPHS)), cy, glyph, false);
                    }
                } else if (hiddenNow) {
                    g.text(font, Component.literal(w.text).withStyle(LoreReading.GLYPHS), cx, cy, glyph, false);
                } else {
                    g.text(font, w.text, cx, cy, ink, false);
                }
                cx += ww + space;
            }
        }
    }
}
