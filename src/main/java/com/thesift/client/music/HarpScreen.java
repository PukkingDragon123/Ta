package com.thesift.client.music;

import com.thesift.music.Instrument;
import com.thesift.music.Notes;
import com.thesift.music.PrismLight;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * M1 instrument play - STRINGS, the Prism Harp: a string for every note of the two octaves, played
 * with the mouse - click a string to pluck it, sweep across them for a glissando - and every note
 * in a colour of light (keys 1-4 or the mouse wheel). Like a concert harp its C strings are red and
 * its F strings blue; a plucked string glows in the light it was played in and throws a ray of it
 * up into the frame. The guide makes the next string shine in the colour the song wants.
 */
public final class HarpScreen extends InstrumentScreen {
    private static final int SPACING = 11;
    private final int count;
    private final float[] vib;
    private final double[] pluckedAt;
    private final int[] pluckRgb;
    private boolean dragging;
    private double lastDragX;

    HarpScreen(Instrument instrument, InteractionHand hand, ItemStack stack) {
        super(instrument, hand, stack);
        this.count = instrument.layoutSize();
        this.vib = new float[this.count];
        this.pluckedAt = new double[this.count];
        this.pluckRgb = new int[this.count];
    }

    private int stringX(int i) {
        return this.width / 2 - (this.count - 1) * SPACING / 2 + i * SPACING;
    }

    /** Where string i meets the soundboard (the low strings are the long ones). */
    private int bottomY(int i) {
        return this.height - 44 - (int) (i * 2.6F);
    }

    /** How tall the harp stands (it shrinks on a small window). */
    private int span() {
        return Mth.clamp(this.height - 118, 80, 160);
    }

    /** Where string i hangs from the neck, which curls like a wave across the top. */
    private int topY(int i) {
        float u = i / (float) (this.count - 1);
        int span = this.span();
        return this.height - 44 - span + (int) (u * span * 0.42F) + (int) (Mth.sin(u * Mth.PI * 2.0F) * 8.0F);
    }

    /** The string under (x, y), or -1. */
    private int stringAt(double x, double y) {
        int i = (int) Math.round((x - this.stringX(0)) / SPACING);
        if (i < 0 || i >= this.count || Math.abs(x - this.stringX(i)) > SPACING / 2.0 + 0.5) {
            return -1;
        }
        return y >= this.topY(i) - 4 && y <= this.bottomY(i) + 4 ? i : -1;
    }

    private void pluck(int i) {
        if (this.play(this.instrument.layout(i), 0)) {
            this.vib[i] = 1.0F;
            this.pluckedAt[i] = clock();
            this.pluckRgb[i] = this.playRgb();
        }
    }

    @Override
    protected void pressed(int key, boolean shift) {
    }

    @Override
    protected void mouseDown(double x, double y, boolean right) {
        int i = this.stringAt(x, y);
        if (i >= 0) {
            this.pluck(i);
        }
        this.dragging = true;
        this.lastDragX = x;
    }

    @Override
    protected void mouseUp(double x, double y, boolean right) {
        this.dragging = false;
    }

    @Override
    protected void mouseDrag(double x, double y) {
        if (!this.dragging) {
            return;
        }
        // a glissando: every string swept past sounds, in order
        double from = this.lastDragX;
        this.lastDragX = x;
        int step = x > from ? 1 : -1;
        for (int i = step > 0 ? 0 : this.count - 1; i >= 0 && i < this.count; i += step) {
            int sx = this.stringX(i);
            boolean crossed = step > 0 ? from < sx && x >= sx : from > sx && x <= sx;
            if (crossed && y >= this.topY(i) - 4 && y <= this.bottomY(i) + 4) {
                this.pluck(i);
            }
        }
    }

    @Override
    protected String glyph(int pitch) {
        return "♪";
    }

    @Override
    protected String controlsKey() {
        return "instrument.thesift.controls.harp";
    }

    @Override
    protected void playTick(LocalPlayer player) {
        for (int i = 0; i < this.count; i++) {
            this.vib[i] *= 0.91F;
        }
    }

    /** Concert-harp string colours: C red, F blue, the sharps dusky, the rest gut-cream. */
    private int stringColour(int pitch) {
        String name = Notes.name(pitch);
        if (name.startsWith("C") && !name.startsWith("C#")) {
            return 0xE8504A;
        }
        if (name.startsWith("F") && !name.startsWith("F#")) {
            return 0x4A70E0;
        }
        return name.contains("#") ? 0x9A8AA8 : 0xF4ECD8;
    }

    @Override
    protected void drawInstrument(GuiGraphicsExtractor g, int mouseX, int mouseY, double now) {
        int first = this.stringX(0);
        int last = this.stringX(this.count - 1);
        // the pillar (left), the neck (top) and the soundboard (the slope at the bottom)
        int pillarX = first - 16;
        g.fill(pillarX, this.topY(0) - 14, pillarX + 9, this.bottomY(0) + 12, 0xFF8C80B0);
        g.fill(pillarX + 2, this.topY(0) - 12, pillarX + 5, this.bottomY(0) + 10, 0xFFE8E2F6);
        for (int i = 0; i < this.count; i++) {
            int x = this.stringX(i);
            int ty = this.topY(i);
            g.fill(x - 6, ty - 8, x + 6, ty - 1, 0xFFE3A73C);
            g.fill(x - 6, ty - 8, x + 6, ty - 6, 0xFFFFE48A);
            int by = this.bottomY(i);
            g.fill(x - 6, by, x + 6, by + 10, 0xFFB18A52);
            g.fill(x - 6, by, x + 6, by + 2, 0xFFD8B47A);
        }
        g.fill(last + 6, this.topY(this.count - 1) - 14, last + 12, this.bottomY(this.count - 1) + 10, 0xFFE3A73C);
        // rainbow gems along the neck
        for (int c = 0; c < PrismLight.COUNT; c++) {
            int gx = first + (last - first) * (c + 1) / (PrismLight.COUNT + 1);
            int i = Math.round((gx - first) / (float) SPACING);
            disc(g, gx, this.topY(Mth.clamp(i, 0, this.count - 1)) - 12, 3, 0xFF000000 | PrismLight.rgb(c));
        }
        int want = this.wanted();
        float pulse = 0.5F + 0.5F * Mth.sin((float) now * 0.5F);
        int hover = this.stringAt(mouseX, mouseY);
        for (int i = 0; i < this.count; i++) {
            int x = this.stringX(i);
            int ty = this.topY(i);
            int by = this.bottomY(i);
            int p = this.instrument.layout(i);
            int base = this.stringColour(p);
            float amp = this.vib[i];
            if (p == want) {
                int rgb = this.guideRgb();
                g.fill(x - 3, ty, x + 4, by, (int) (0x40 + 0x60 * pulse) << 24 | rgb);
                ring(g, x, by + 14, 4, 3, 0xFF000000 | rgb);
            }
            if (amp > 0.04F) {
                // the plucked string shivers and throws a ray of its light up into the frame
                int rgb = this.pluckRgb[i];
                int col = 0xFF000000 | mix(base, rgb, Math.min(1.0F, amp * 1.4F));
                double t = (now - this.pluckedAt[i]) * 3.0;
                for (int y = ty; y < by; y += 3) {
                    double u = (y - ty) / (double) Math.max(1, by - ty);
                    int off = (int) Math.round(Math.sin(Math.PI * u) * Math.sin(t * Math.PI + u * 2.0) * amp * 2.5);
                    g.fill(x + off, y, x + off + 1, Math.min(by, y + 3), col);
                }
                g.fillGradient(x - 2, ty - 40, x + 3, ty, 0, (int) (amp * 0xB0) << 24 | rgb);
                glow(g, x, (ty + by) / 2, 7, rgb, amp * 0.7F);
            } else {
                int w = Notes.name(p).contains("#") ? 1 : 2;
                g.fill(x, ty, x + w, by, 0xFF000000 | base);
            }
            if (i == hover) {
                g.fill(x - 1, ty, x + 2, by, 0x50FFFFFF);
            }
        }
        if (hover >= 0) {
            int p = this.instrument.layout(hover);
            g.text(this.font, Notes.name(p), mouseX + 8, mouseY - 12, 0xFF000000 | Notes.colour(p), true);
        }
        // the note names under the C strings, to find your way
        for (int i = 0; i < this.count; i++) {
            int p = this.instrument.layout(i);
            String name = Notes.name(p);
            if (name.startsWith("C") && !name.startsWith("C#") || i == 0 || i == this.count - 1) {
                g.centeredText(this.font, name, this.stringX(i), this.bottomY(i) + 12, SOFT);
            }
        }
    }

    @Override
    protected int judgementY() {
        return this.topY(0) - 26;
    }
}
