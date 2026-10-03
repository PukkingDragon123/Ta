package com.thesift.client.music;

import com.mojang.blaze3d.platform.InputConstants;
import com.thesift.music.Instrument;
import com.thesift.music.Notes;
import com.thesift.music.PrismLight;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * M1 instrument play - FLUTE: breath and fingering. The flute lies across the screen. Hold your
 * breath (Space, or hold the mouse on the flute) to blow; the holes you cover choose the note -
 * the lowest covered hole decides, the holes above it fall under your fingers by themselves.
 * Keys: A S D (F) for the left hand's holes, J K L for the right. Change the fingering while you
 * blow and the note slurs to the next. Breath runs out - let go to breathe in again. The Serbim and
 * Prism Flutes have a seventh hole and overblow (Shift or the right mouse button) an octave up.
 * The guide shows the next fingering as ghost fingertips.
 */
public final class FluteScreen extends InstrumentScreen {
    private static final int[] KEYS_6 = {InputConstants.KEY_A, InputConstants.KEY_S, InputConstants.KEY_D, InputConstants.KEY_J, InputConstants.KEY_K,
            InputConstants.KEY_L};
    private static final int[] KEYS_7 = {InputConstants.KEY_A, InputConstants.KEY_S, InputConstants.KEY_D, InputConstants.KEY_F, InputConstants.KEY_J,
            InputConstants.KEY_K, InputConstants.KEY_L};
    private static final int HOLE_GAP = 34;

    private final int holes;
    private final int[] keys;
    private final float drain;
    private float breath = 1.0F;
    private boolean blowing;
    private boolean winded;
    private int sounding = -1;
    private int sustain;
    private int mouseHole = -1;
    private boolean mouseBlowing;
    private boolean mouseOver;
    private float air;

    FluteScreen(Instrument instrument, InteractionHand hand, ItemStack stack) {
        super(instrument, hand, stack);
        this.holes = instrument.layoutSize() - 1;
        this.keys = this.holes > 6 ? KEYS_7 : KEYS_6;
        // the Crane Flute holds about three and a half seconds of breath, the better flutes five
        this.drain = instrument.tier() > 1 ? 1.0F / 100.0F : 1.0F / 70.0F;
    }

    // ------------------------------------------------------------------ geometry

    private int flY() {
        return this.height - 74;
    }

    private int left() {
        return this.width / 2 - 170;
    }

    private int right() {
        return this.width / 2 + 170;
    }

    private int mouthX() {
        return this.left() + 34;
    }

    private int holeX(int h) {
        int first = this.left() + 104 + (this.holes > 6 ? 0 : 12);
        return first + h * HOLE_GAP + (h >= (this.holes > 6 ? 4 : 3) ? 14 : 0);
    }

    private int holeAt(double x, double y) {
        if (Math.abs(y - this.flY()) > 10) {
            return -1;
        }
        for (int h = 0; h < this.holes; h++) {
            if (Math.abs(x - this.holeX(h)) <= 10) {
                return h;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------ fingering

    /** The lowest covered hole (0-based), -1 if all are open. */
    private int lowestCovered() {
        int lowest = this.mouseHole;
        for (int h = 0; h < this.holes; h++) {
            if (this.held.contains(this.keys[h])) {
                lowest = Math.max(lowest, h);
            }
        }
        return lowest;
    }

    private boolean overblown() {
        return this.instrument.extra() > 0 && (this.shiftHeld() || this.mouseOver);
    }

    /** The note a fingering sounds: the lowest covered hole picks the step of the scale, overblowing lifts it an octave. */
    private int pitch(int lowest, boolean over) {
        int index = this.holes - 1 - lowest;
        return this.instrument.layout(index) + (over ? 12 : 0);
    }

    private int currentPitch() {
        return this.pitch(this.lowestCovered(), this.overblown());
    }

    // ------------------------------------------------------------------ playing

    private boolean breathHeld() {
        return this.held.contains(InputConstants.KEY_SPACE) || this.mouseBlowing;
    }

    /** Starts or slurs a note when the breath or the fingering changes. */
    private void update() {
        boolean want = this.breathHeld() && !this.winded && this.breath > 0.02F;
        if (!want) {
            this.blowing = false;
            this.sounding = -1;
            return;
        }
        int p = this.currentPitch();
        if (!this.blowing || p != this.sounding) {
            this.blowing = true;
            this.sounding = p;
            this.sustain = 0;
            if (!this.play(p, 0)) {
                // too high to sound: only air
                this.sounding = -2;
            }
        }
    }

    @Override
    protected void pressed(int key, boolean shift) {
        this.update();
    }

    @Override
    protected void released(int key) {
        if (key == InputConstants.KEY_SPACE && !this.mouseBlowing) {
            this.winded = false;
        }
        this.update();
    }

    @Override
    protected void mouseDown(double x, double y, boolean right) {
        if (right) {
            this.mouseOver = true;
            this.update();
            return;
        }
        // press on a hole to cover it (and those above) and blow; anywhere else on the flute just blows
        this.mouseHole = this.holeAt(x, y);
        this.mouseBlowing = true;
        this.update();
    }

    @Override
    protected void mouseUp(double x, double y, boolean right) {
        if (right) {
            this.mouseOver = false;
        } else {
            this.mouseHole = -1;
            this.mouseBlowing = false;
            if (!this.held.contains(InputConstants.KEY_SPACE)) {
                this.winded = false;
            }
        }
        this.update();
    }

    @Override
    protected void playTick(LocalPlayer player) {
        if (this.blowing) {
            this.breath -= this.drain;
            this.air = Math.min(1.0F, this.air + 0.25F);
            if (this.breath <= 0.0F) {
                // out of breath: the note dies until you let go and breathe in
                this.breath = 0.0F;
                this.winded = true;
                this.judge(Component.translatable("instrument.thesift.judge.breath"), WARN);
                this.update();
            } else if (this.sounding >= 0 && ++this.sustain % 9 == 0) {
                // the held note goes on sounding (only for the player; the world heard it begin)
                player.playSound(this.instrument.sound(), 0.32F, Notes.soundPitch(this.sounding));
            }
        } else {
            this.breath = Math.min(1.0F, this.breath + 0.045F);
            this.air *= 0.8F;
        }
    }

    @Override
    protected String glyph(int pitch) {
        int n = this.instrument.nearest(pitch);
        for (int over = 0; over <= this.instrument.extra(); over++) {
            for (int lowest = -1; lowest < this.holes; lowest++) {
                if (this.pitch(lowest, over == 1) == n) {
                    String k = lowest < 0 ? "○" : keyName(this.keys[lowest]);
                    return over == 1 ? k + "↑" : k;
                }
            }
        }
        return "·";
    }

    /** The fingering of the note the guide wants: {lowest covered hole, overblown 0/1}, or null. */
    private int[] wantedFingering() {
        int want = this.wanted();
        if (want < 0) {
            return null;
        }
        for (int over = 0; over <= this.instrument.extra(); over++) {
            for (int lowest = -1; lowest < this.holes; lowest++) {
                if (this.pitch(lowest, over == 1) == want) {
                    return new int[]{lowest, over};
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ drawing

    private int bodyLight() {
        return switch (this.instrument) {
            case SERBIM_FLUTE -> 0xC8DCF0;
            case PRISM_FLUTE -> 0xECEBF8;
            default -> 0xF4E2B8;
        };
    }

    private int bodyDark() {
        return switch (this.instrument) {
            case SERBIM_FLUTE -> 0x5A7898;
            case PRISM_FLUTE -> 0x8A88B4;
            default -> 0xA8844A;
        };
    }

    @Override
    protected void drawInstrument(GuiGraphicsExtractor g, int mouseX, int mouseY, double now) {
        int y = this.flY();
        int l = this.left();
        int r = this.right();
        int light = this.bodyLight();
        int dark = this.bodyDark();
        // the body: a long tube, lit from above
        g.fill(l, y - 7, r, y + 8, 0xFF000000 | mix(dark, 0, 0.35F));
        g.fill(l + 1, y - 6, r - 1, y + 7, 0xFF000000 | dark);
        g.fill(l + 1, y - 6, r - 1, y + 1, 0xFF000000 | mix(light, dark, 0.35F));
        g.fill(l + 1, y - 6, r - 1, y - 3, 0xFF000000 | light);
        g.fill(l + 1, y - 5, r - 1, y - 4, 0xC0FFFFFF);
        // its joints and ends
        for (int jx : new int[]{l + 2, l + 70, r - 60, r - 4}) {
            int band = this.instrument == Instrument.FLUTE ? 0x9A62C8 : this.instrument == Instrument.PRISM_FLUTE ? 0xE3A73C : 0xE6E8F0;
            g.fill(jx - 2, y - 8, jx + 3, y + 9, 0xFF000000 | band);
            g.fill(jx - 2, y - 8, jx + 3, y - 6, 0xFF000000 | mix(band, 0xFFFFFF, 0.4F));
        }
        if (this.instrument == Instrument.FLUTE) {
            // the Crane Flute's red ribbon tied at its foot, two white feathers hanging from it
            int fx = r - 24;
            g.fill(fx - 3, y + 7, fx + 4, y + 10, 0xFFC83A4A);
            g.fill(fx - 1, y + 10, fx + 1, y + 20, 0xFFC83A4A);
            g.fill(fx + 1, y + 10, fx + 3, y + 17, 0xFFF06A6A);
            line(g, fx, y + 19, fx - 6, y + 31, 3, 0xFFFFFFFF);
            line(g, fx + 2, y + 17, fx + 7, y + 28, 3, 0xFFC8CCD8);
            line(g, fx, y + 19, fx - 6, y + 31, 1, 0xFF8A8A9A);
        }
        // the mouthpiece
        ellipse(g, this.mouthX(), y - 1, 6, 3, 0xFF2A1A12);
        this.keyCap(g, this.mouthX(), y + 14, keyName(InputConstants.KEY_SPACE), this.breathHeld(), this.playRgb());
        // breath, blown across the mouthpiece and down the flute
        if (this.air > 0.05F) {
            for (int k = 0; k < 10; k++) {
                double t = (now * 0.9 + k * 0.37) % 1.0;
                int ax = this.mouthX() - 20 + (int) (t * (r - l - 20));
                int ay = y - 14 - (int) (Math.sin(t * Math.PI * 3.0 + k) * 3.0);
                g.fill(ax, ay, ax + 8, ay + 1, (int) (this.air * (1.0 - t) * 0xA0) << 24 | (this.instrument.prism() ? PrismLight.rgb(this.light) : 0xFFFFFF));
            }
        }
        // the holes, the fingers on them, and the guide's ghost fingers
        int lowest = this.lowestCovered();
        int[] ghost = this.wantedFingering();
        float pulse = 0.5F + 0.5F * Mth.sin((float) now * 0.5F);
        int hover = this.holeAt(mouseX, mouseY);
        for (int h = 0; h < this.holes; h++) {
            int hx = this.holeX(h);
            boolean covered = h <= lowest;
            boolean pressed = this.held.contains(this.keys[h]) || h == this.mouseHole;
            int holeRgb = this.instrument == Instrument.PRISM_FLUTE ? PrismLight.rgb(h % PrismLight.COUNT) : this.instrument == Instrument.FLUTE
                    ? 0x22C7C4 : 0x1A2A3A;
            ellipse(g, hx, y - 1, 5, 4, 0xFF000000 | mix(holeRgb, 0, 0.5F));
            ellipse(g, hx, y - 1, 3, 2, 0xFF000000 | holeRgb);
            if (ghost != null && h <= ghost[0]) {
                ring(g, hx, y - 1, 8, 7, (int) (0x70 + 0x8F * pulse) << 24 | this.guideRgb());
            }
            if (covered) {
                // a fingertip: pressed ones solid, the ones that fall in place above them softer
                ellipse(g, hx, y - 2, 7, 6, pressed ? 0xF0E8B898 : 0x90E8B898);
                ellipse(g, hx - 2, y - 4, 3, 2, pressed ? 0xC0FFE8D8 : 0x60FFE8D8);
            }
            if (h == hover) {
                ring(g, hx, y - 1, 9, 8, 0xA0FFFFFF);
            }
            this.keyCap(g, hx, y + 14, keyName(this.keys[h]), pressed, this.playRgb());
        }
        // overblowing
        if (this.instrument.extra() > 0) {
            boolean over = this.overblown();
            String ob = Component.translatable("instrument.thesift.overblow").getString();
            g.text(this.font, ob, r - this.font.width(ob), y - 28, over ? GOLD : DIM, true);
            if (ghost != null && ghost[1] == 1) {
                g.text(this.font, "↑", r - this.font.width(ob) - 10, y - 28, (int) (0x80 + 0x7F * pulse) << 24 | this.guideRgb(), true);
            }
        }
        // the breath gauge
        int gx0 = l;
        int gx1 = l + 120;
        int gy = y - 30;
        g.fill(gx0 - 1, gy - 1, gx1 + 1, gy + 5, 0xA0000000);
        int fill = (int) ((gx1 - gx0) * this.breath);
        int bc = this.winded ? 0xFFFF6A4A : this.breath < 0.25F ? 0xFFFFC341 : 0xFF9FE8F0;
        g.fill(gx0, gy, gx0 + fill, gy + 4, bc);
        g.fill(gx0, gy, gx0 + fill, gy + 1, 0x80FFFFFF);
        g.text(this.font, Component.translatable("instrument.thesift.breath").getString(), gx1 + 6, gy - 2, this.winded ? WARN : SOFT, true);
        // the note sounding now
        int now2 = this.blowing && this.sounding >= 0 ? this.sounding : this.currentPitch();
        String name = this.instrument.canPlay(now2) ? Notes.name(now2) : "—";
        int nameRgb = this.blowing && this.sounding >= 0 ? 0xFF000000 | (this.instrument.prism() ? PrismLight.rgb(this.light) : Notes.colour(now2))
                : DIM;
        if (this.blowing && this.sounding >= 0) {
            glow(g, this.width / 2, y - 42, 12, this.playRgb(), 0.4F + 0.2F * pulse);
        }
        g.centeredText(this.font, name, this.width / 2, y - 46, nameRgb);
    }

    @Override
    protected int judgementY() {
        return this.flY() - 64;
    }
}
