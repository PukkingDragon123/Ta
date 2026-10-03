package com.thesift.client.music;

import com.mojang.blaze3d.platform.InputConstants;
import com.thesift.music.Instrument;
import com.thesift.music.Notes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * M1 instrument play - STRINGS: pick notes on a fretboard (the Guitar: four strings; the Star Lute
 * and the Weaver's Guitar: six, and chords).
 *
 * <p>The neck lies across the screen, the high string on top. Click a string between two frets to
 * pick it there, or over the body to pick it open; drag across the strings to strum them. On the
 * keyboard the left hand holds a fret (Q W E R = frets 1-4, nothing = open) and the right hand
 * picks the strings (J K L ; - or H J K L ; ' for six). Shift or the right mouse button strums the
 * chord on the note (Star Lute, Weaver's Guitar). The guide rings every place the next note can be
 * picked.
 */
public final class StringsScreen extends InstrumentScreen {
    private static final int[] FRET_KEYS = {InputConstants.KEY_Q, InputConstants.KEY_W, InputConstants.KEY_E, InputConstants.KEY_R};
    private static final int[] PICK_4 = {InputConstants.KEY_J, InputConstants.KEY_K, InputConstants.KEY_L, InputConstants.KEY_SEMICOLON};
    private static final int[] PICK_6 = {InputConstants.KEY_H, InputConstants.KEY_J, InputConstants.KEY_K, InputConstants.KEY_L, InputConstants.KEY_SEMICOLON,
            InputConstants.KEY_APOSTROPHE};
    /** Widths of the fret spaces 1-4 (they narrow up the neck). */
    private static final int[] FRET_W = {52, 48, 44, 40};
    private static final int SPACING = 12;

    private final int strings;
    private final int frets;
    private final int[] pickKeys;
    private final float[] vib;
    private final int[] vibFret;
    private final double[] pluckedAt;
    private int lastDragString = -1;
    private boolean dragging;

    StringsScreen(Instrument instrument, InteractionHand hand, ItemStack stack) {
        super(instrument, hand, stack);
        this.strings = instrument.layoutSize();
        this.frets = Math.min(FRET_KEYS.length, instrument.extra() - 1);
        this.pickKeys = this.strings > 4 ? PICK_6 : PICK_4;
        this.vib = new float[this.strings];
        this.vibFret = new int[this.strings];
        this.pluckedAt = new double[this.strings];
    }

    // ------------------------------------------------------------------ geometry

    private int neckLeft() {
        return this.width / 2 - 140;
    }

    /** x of the fret wire at the high end of fret space {@code f} (0 = the nut). */
    private int fretX(int f) {
        int x = this.neckLeft();
        for (int i = 0; i < f; i++) {
            x += FRET_W[i];
        }
        return x;
    }

    private int bodyLeft() {
        return this.fretX(this.frets);
    }

    private int bridgeX() {
        return this.bodyLeft() + 88;
    }

    private int centreY() {
        return this.height - 64 - (this.strings - 4) * 6;
    }

    /** y of string {@code s} (0 = the lowest, drawn at the bottom). */
    private int stringY(int s) {
        return this.centreY() + ((this.strings - 1) * SPACING) / 2 - s * SPACING;
    }

    private int pitch(int string, int fret) {
        return this.instrument.layout(string) + fret;
    }

    /** The string under y, or -1. */
    private int stringAt(double y) {
        for (int s = 0; s < this.strings; s++) {
            if (Math.abs(y - this.stringY(s)) <= SPACING / 2.0) {
                return s;
            }
        }
        return -1;
    }

    /** The fret under x: 1-4 on the neck, 0 over the body (open), -1 off the instrument. */
    private int fretAt(double x) {
        if (x >= this.bodyLeft() && x <= this.bridgeX() + 6) {
            return 0;
        }
        for (int f = 1; f <= this.frets; f++) {
            if (x >= this.fretX(f - 1) && x < this.fretX(f)) {
                return f;
            }
        }
        return -1;
    }

    /** The highest fret held on the keyboard (0 if none). */
    private int heldFret() {
        int fret = 0;
        for (int f = 0; f < this.frets; f++) {
            if (this.held.contains(FRET_KEYS[f])) {
                fret = f + 1;
            }
        }
        return fret;
    }

    // ------------------------------------------------------------------ playing

    private void pick(int string, int fret, boolean chord) {
        int p = this.pitch(string, fret);
        if (this.play(p, chord ? Notes.CHORD : 0)) {
            this.vib[string] = 1.0F;
            this.vibFret[string] = fret;
            this.pluckedAt[string] = clock();
        }
    }

    @Override
    protected void pressed(int key, boolean shift) {
        for (int s = 0; s < this.strings; s++) {
            if (key == this.pickKeys[s]) {
                this.pick(s, this.heldFret(), shift);
                return;
            }
        }
    }

    @Override
    protected void mouseDown(double x, double y, boolean right) {
        int s = this.stringAt(y);
        int f = this.fretAt(x);
        if (s >= 0 && f >= 0) {
            this.pick(s, f, right || this.shiftHeld());
            this.lastDragString = s;
            this.dragging = true;
        }
    }

    @Override
    protected void mouseUp(double x, double y, boolean right) {
        this.dragging = false;
        this.lastDragString = -1;
    }

    @Override
    protected void mouseDrag(double x, double y) {
        if (!this.dragging) {
            return;
        }
        int s = this.stringAt(y);
        int f = this.fretAt(x);
        if (s >= 0 && f >= 0 && s != this.lastDragString) {
            // a strum: every string the pick crosses sounds
            this.pick(s, f, false);
            this.lastDragString = s;
        }
    }

    @Override
    protected String glyph(int pitch) {
        // the easiest place to pick it: the lowest fret
        for (int f = 0; f <= this.frets; f++) {
            for (int s = this.strings - 1; s >= 0; s--) {
                if (this.pitch(s, f) == pitch) {
                    String pick = keyName(this.pickKeys[s]);
                    return f == 0 ? pick : keyName(FRET_KEYS[f - 1]) + "+" + pick;
                }
            }
        }
        return "·";
    }

    @Override
    protected void playTick(net.minecraft.client.player.LocalPlayer player) {
        for (int s = 0; s < this.strings; s++) {
            this.vib[s] *= 0.9F;
        }
    }

    // ------------------------------------------------------------------ drawing

    private int woodLight() {
        return switch (this.instrument) {
            case LUTE -> 0xC88A4A;
            case WEAVER_GUITAR -> 0x2C4E59;
            default -> 0xD8963E;
        };
    }

    private int woodDark() {
        return switch (this.instrument) {
            case LUTE -> 0x7A4422;
            case WEAVER_GUITAR -> 0x0F2028;
            default -> 0x8A5228;
        };
    }

    @Override
    protected void drawInstrument(GuiGraphicsExtractor g, int mouseX, int mouseY, double now) {
        int left = this.neckLeft();
        int body = this.bodyLeft();
        int bridge = this.bridgeX();
        int top = this.stringY(this.strings - 1) - 9;
        int bottom = this.stringY(0) + 9;
        int cy = (top + bottom) / 2;
        int wl = 0xFF000000 | this.woodLight();
        int wd = 0xFF000000 | this.woodDark();
        // the body: a rounded slab with a soundhole and a rosette
        int bh = bottom - top + 16;
        for (int dy = -bh / 2; dy <= bh / 2; dy++) {
            double k = dy / (bh / 2.0);
            int hw = (int) (52 * Math.sqrt(Math.max(0.0, 1.0 - k * k * k * k)));
            g.fill(body + 46 - hw, cy + dy, body + 46 + hw, cy + dy + 1, dy < -bh / 4 ? 0xFF000000 | mix(this.woodLight(), 0xFFFFFF, 0.12F) : wl);
        }
        ellipse(g, body + 34, cy, 13, 13, 0xFF000000 | mix(this.woodDark(), 0, 0.45F));
        ring(g, body + 34, cy, 15, 15, 0xFF000000 | this.instrument.colour());
        ring(g, body + 34, cy, 17, 17, 0x80000000 | this.instrument.colour());
        g.fill(bridge - 3, top + 4, bridge + 4, bottom - 4, wd);
        g.fill(bridge - 2, top + 5, bridge + 1, bottom - 5, 0xFFEFE6D0);
        // the headstock and its pegs
        g.fill(left - 34, top - 2, left, bottom + 2, wd);
        g.fill(left - 32, top, left - 2, bottom, 0xFF000000 | mix(this.woodDark(), this.woodLight(), 0.35F));
        for (int s = 0; s < this.strings; s++) {
            int y = this.stringY(s);
            g.fill(left - 30 + (s % 2) * 12, y - 2, left - 24 + (s % 2) * 12, y + 3, 0xFFD8DCE6);
        }
        // the neck, the nut and the frets
        g.fill(left, top, body, bottom, wd);
        g.fill(left, top + 1, body, top + 3, 0xFF000000 | mix(this.woodDark(), 0xFFFFFF, 0.15F));
        g.fill(left - 2, top, left + 1, bottom, 0xFFF0E8D8);
        for (int f = 1; f <= this.frets; f++) {
            int x = this.fretX(f);
            g.fill(x - 1, top, x + 1, bottom, 0xFFC8CCD8);
            g.fill(x - 1, top, x, bottom, 0xFFF4F6FA);
            // fret markers between the strings
            if (f == 3) {
                disc(g, x - FRET_W[f - 1] / 2, cy, 2, 0xFFEFE6D0);
            }
            String fk = keyName(FRET_KEYS[f - 1]);
            boolean down = this.held.contains(FRET_KEYS[f - 1]);
            this.keyCap(g, x - FRET_W[f - 1] / 2, top - 16, fk, down, this.instrument.colour());
        }
        g.centeredText(this.font, Component.translatable("instrument.thesift.open").getString(), body + 64, top - 13, DIM);
        // the fret held on the keyboard: a bar of fingertips across the strings
        int hf = this.heldFret();
        if (hf > 0) {
            int fx = this.fretX(hf) - 9;
            for (int s = 0; s < this.strings; s++) {
                ellipse(g, fx, this.stringY(s), 4, 3, 0xD0E8B898);
            }
        }
        // the guide: every place the next note can be picked
        int want = this.wanted();
        float pulse = 0.5F + 0.5F * Mth.sin((float) now * 0.5F);
        if (want >= 0) {
            int rgb = this.guideRgb();
            for (int s = 0; s < this.strings; s++) {
                for (int f = 0; f <= this.frets; f++) {
                    if (this.pitch(s, f) == want) {
                        int x = f == 0 ? body + 64 : this.fretX(f) - FRET_W[f - 1] / 2;
                        int y = this.stringY(s);
                        glow(g, x, y, 9, rgb, 0.5F + 0.5F * pulse);
                        ring(g, x, y, 7, 6, 0xFF000000 | rgb);
                    }
                }
            }
        }
        // the hovered place
        int hs = this.stringAt(mouseY);
        int hfret = this.fretAt(mouseX);
        if (hs >= 0 && hfret >= 0 && this.instrument.canPlay(this.pitch(hs, hfret))) {
            int x = hfret == 0 ? body + 64 : this.fretX(hfret) - FRET_W[hfret - 1] / 2;
            ring(g, x, this.stringY(hs), 6, 5, 0xC0FFFFFF);
            String name = Notes.name(this.pitch(hs, hfret));
            g.text(this.font, name, mouseX + 8, mouseY - 12, 0xFF000000 | Notes.colour(this.pitch(hs, hfret)), true);
        }
        // the strings: low ones thick and bronze, high ones fine and silver; a picked one shivers
        for (int s = 0; s < this.strings; s++) {
            int y = this.stringY(s);
            int thick = s < this.strings / 2 ? 2 : 1;
            int base = this.instrument == Instrument.WEAVER_GUITAR ? 0x9FFBFF : s < this.strings / 2 ? 0xD8A868 : 0xE6E8F0;
            float amp = this.vib[s];
            int col = 0xFF000000 | (amp > 0.05F ? mix(base, this.playRgb(), Math.min(1.0F, amp * 1.5F)) : base);
            int from = this.vibFret[s] == 0 ? body : this.fretX(this.vibFret[s]);
            g.fill(left - 30, y, from, y + thick, col);
            if (amp < 0.05F) {
                g.fill(from, y, bridge, y + thick, col);
            } else {
                int len = bridge - from;
                double t = (now - this.pluckedAt[s]) * 2.6;
                for (int x = from; x < bridge; x += 3) {
                    double u = (x - from) / (double) len;
                    int off = (int) Math.round(Math.sin(Math.PI * u) * Math.sin(t * Math.PI + u * 3.0) * amp * 3.0);
                    g.fill(x, y + off, Math.min(bridge, x + 3), y + off + thick, col);
                }
                if (amp > 0.3F) {
                    glow(g, (from + bridge) / 2, y, 6, this.playRgb(), amp * 0.6F);
                }
            }
            // its pick key and its open note
            boolean down = this.held.contains(this.pickKeys[s]);
            this.keyCap(g, bridge + 24, y - 5, keyName(this.pickKeys[s]), down, this.playRgb());
            g.text(this.font, Notes.name(this.pitch(s, 0)), left - 58, y - 4, SOFT, true);
        }
        if (this.instrument.chords()) {
            g.text(this.font, Component.translatable("instrument.thesift.chord").getString(), bridge + 36, this.centreY() - 4,
                    this.shiftHeld() ? GOLD : DIM, true);
        }
    }

    @Override
    protected int judgementY() {
        return this.stringY(this.strings - 1) - 34;
    }
}
