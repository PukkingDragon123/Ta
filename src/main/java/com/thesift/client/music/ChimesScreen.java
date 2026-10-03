package com.thesift.client.music;

import com.mojang.blaze3d.platform.InputConstants;
import com.thesift.music.Instrument;
import com.thesift.music.Notes;
import com.thesift.music.PrismLight;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * M1 instrument play - CHIMES: timing. The chimes hang from their crossbar and swing in the wind,
 * the long low ones slowly, the short high ones fast; under each hangs its mark. A chime rings
 * true only when it is struck as it swings past its mark - struck right on it, it rings out bright
 * (Perfect); struck while it is swung out wide, it only clinks and plays nothing.
 *
 * <p>Strike with the keys under the chimes (A S D F G H J, and K L ; on the ten-chime sets) or
 * click a chime. The Glass Bells and Prism Chimes answer a gust: hold Space and they swing wider
 * and twice as fast - they pass their marks twice as often, but the moment is shorter. The guide
 * lights the next chime and closes a ring on its mark as it swings in.
 */
public final class ChimesScreen extends InstrumentScreen {
    private static final int[] KEYS = {InputConstants.KEY_A, InputConstants.KEY_S, InputConstants.KEY_D, InputConstants.KEY_F, InputConstants.KEY_G,
            InputConstants.KEY_H, InputConstants.KEY_J, InputConstants.KEY_K, InputConstants.KEY_L, InputConstants.KEY_SEMICOLON};
    /** How far a chime swings (radians). */
    private static final double SWING = 0.24;
    /** Within this share of its swing from the mark a strike rings... */
    private static final double GOOD_WINDOW = 0.5;
    /** ...and within this it rings perfectly. */
    private static final double PERFECT_WINDOW = 0.2;
    private static final int PITCH_GAP = 32;

    private final int count;
    private final double[] phase;
    private final double[] omega;
    private final float[] ringing;
    private final int[] ringRgb;
    private final float[] miss;
    private double lastClock = Double.NaN;
    private float gust;

    ChimesScreen(Instrument instrument, InteractionHand hand, ItemStack stack) {
        super(instrument, hand, stack);
        this.count = instrument.layoutSize();
        this.phase = new double[this.count];
        this.omega = new double[this.count];
        this.ringing = new float[this.count];
        this.ringRgb = new int[this.count];
        this.miss = new float[this.count];
        for (int i = 0; i < this.count; i++) {
            // a pendulum's period grows with its length: the long low chimes swing slowly
            double period = 1.15 + 0.85 * this.tube(i) / 74.0;
            this.omega[i] = Math.PI * 2.0 / (period * 20.0);
            this.phase[i] = (i * 2.399 + 0.7) % (Math.PI * 2.0);
        }
    }

    // ------------------------------------------------------------------ geometry and swing

    private int barY() {
        // from the bottom up: the controls, the names and keys, the marks, the longest chime, its cord
        return Math.max(84, this.height - 154);
    }

    private int pivotX(int i) {
        return this.width / 2 - (this.count - 1) * PITCH_GAP / 2 + i * PITCH_GAP;
    }

    /** Tube length: the low chimes are the long ones. */
    private int tube(int i) {
        return 74 - (int) (i * 40.0F / Math.max(1, this.count - 1));
    }

    private static final int CORD = 16;

    private int markY(int i) {
        return this.barY() + CORD + this.tube(i) + 6;
    }

    private boolean gusting() {
        return this.instrument.extra() > 0 && this.held.contains(InputConstants.KEY_SPACE);
    }

    /** Swings every chime on to {@code now}. */
    private void advance(double now) {
        if (Double.isNaN(this.lastClock)) {
            this.lastClock = now;
        }
        double dt = Mth.clamp(now - this.lastClock, 0.0, 5.0);
        this.lastClock = now;
        double speed = 1.0 + this.gust;
        for (int i = 0; i < this.count; i++) {
            this.phase[i] = (this.phase[i] + this.omega[i] * dt * speed) % (Math.PI * 2.0);
        }
    }

    /** How far chime i is swung out, -1..1 of its swing. */
    private double swing(int i) {
        return Math.sin(this.phase[i]);
    }

    private double angle(int i) {
        return this.swing(i) * SWING * (1.0 + 0.3 * this.gust);
    }

    /** Ticks until chime i next swings through its mark. */
    private double untilMark(int i) {
        double left = Math.PI - this.phase[i] % Math.PI;
        return left / (this.omega[i] * (1.0 + this.gust));
    }

    // ------------------------------------------------------------------ playing

    private void strike(int i) {
        this.advance(clock());
        double off = Math.abs(this.swing(i));
        LocalPlayer p = this.minecraft.player;
        if (off > GOOD_WINDOW) {
            // swung out wide: it only clinks against the mallet
            this.miss[i] = 1.0F;
            if (p != null) {
                p.playSound(SoundEvents.NOTE_BLOCK_HAT.value(), 0.35F, 1.6F);
            }
            this.judge(Component.translatable("instrument.thesift.judge.wait"), WARN);
            return;
        }
        boolean perfect = off <= PERFECT_WINDOW;
        if (this.play(this.instrument.layout(i), perfect ? com.thesift.music.Notes.STRONG : 0)) {
            this.ringing[i] = 1.0F;
            this.ringRgb[i] = this.playRgb();
            if (!this.justCompleted()) {
                this.judge(Component.translatable(perfect ? "instrument.thesift.judge.perfect" : "instrument.thesift.judge.good"), perfect ? GOLD : GOOD);
            }
        }
    }

    @Override
    protected void pressed(int key, boolean shift) {
        for (int i = 0; i < this.count && i < KEYS.length; i++) {
            if (key == KEYS[i]) {
                this.strike(i);
                return;
            }
        }
    }

    @Override
    protected void mouseDown(double x, double y, boolean right) {
        int i = this.chimeAt(x, y);
        if (i >= 0) {
            this.strike(i);
        }
    }

    /** The chime under the mouse (near its swinging tube), or -1. */
    private int chimeAt(double x, double y) {
        int best = -1;
        double bestD = 12.0;
        for (int i = 0; i < this.count; i++) {
            double a = this.angle(i);
            int px = this.pivotX(i);
            int py = this.barY();
            for (int k = CORD; k <= CORD + this.tube(i); k += 4) {
                double cx = px - Math.sin(a) * k;
                double cy = py + Math.cos(a) * k;
                double d = Math.hypot(x - cx, y - cy);
                if (d < bestD) {
                    bestD = d;
                    best = i;
                }
            }
        }
        return best;
    }

    @Override
    protected void playTick(LocalPlayer player) {
        float target = this.gusting() ? 1.0F : 0.0F;
        this.gust += (target - this.gust) * 0.15F;
        for (int i = 0; i < this.count; i++) {
            this.ringing[i] *= 0.93F;
            this.miss[i] *= 0.75F;
        }
    }

    @Override
    protected String glyph(int pitch) {
        int n = this.instrument.nearest(pitch);
        for (int i = 0; i < this.count && i < KEYS.length; i++) {
            if (this.instrument.layout(i) == n) {
                return keyName(KEYS[i]);
            }
        }
        return "·";
    }

    // ------------------------------------------------------------------ drawing

    private int tubeLight() {
        return switch (this.instrument) {
            case GLASS_BELLS -> 0xC8F6FF;
            case PRISM_CHIMES -> 0xF4F2FF;
            default -> 0xF0F8FF;
        };
    }

    private int tubeDark() {
        return switch (this.instrument) {
            case GLASS_BELLS -> 0x5AA8C8;
            case PRISM_CHIMES -> 0x8A88B4;
            default -> 0x7F9CB6;
        };
    }

    @Override
    protected void drawInstrument(GuiGraphicsExtractor g, int mouseX, int mouseY, double now) {
        this.advance(now);
        int by = this.barY();
        int x0 = this.pivotX(0) - 18;
        int x1 = this.pivotX(this.count - 1) + 18;
        int want = this.wanted();
        float pulse = 0.5F + 0.5F * Mth.sin((float) now * 0.5F);
        // the wind: streaks blowing across while it gusts
        if (this.gust > 0.05F) {
            for (int k = 0; k < 14; k++) {
                double t = now * (2.0 + k % 3) + k * 37.0;
                int wx = x0 - 40 + (int) (t * 6.0 % (x1 - x0 + 80));
                int wy = by + 10 + k * 9 % 90;
                g.fill(wx, wy, wx + 14 + k % 4 * 4, wy + 1, (int) (this.gust * 0x50) << 24 | 0xFFFFFF);
            }
        }
        // the crossbar and its hook
        int barRgb = this.instrument == Instrument.WIND_CHIMES ? 0xC49464 : this.instrument == Instrument.GLASS_BELLS ? 0x5A6068 : 0xE3A73C;
        g.fill(x0, by - 4, x1, by + 3, 0xFF000000 | barRgb);
        g.fill(x0, by - 4, x1, by - 2, 0xFF000000 | mix(barRgb, 0xFFFFFF, 0.3F));
        g.fill(x0, by + 2, x1, by + 3, 0xFF000000 | mix(barRgb, 0, 0.45F));
        line(g, this.width / 2.0F, by - 4, x0 + 6, by - 24, 1, 0xFFB0B8C8);
        line(g, this.width / 2.0F, by - 4, x1 - 6, by - 24, 1, 0xFFB0B8C8);
        ring(g, this.width / 2, by - 28, 4, 4, 0xFFB0B8C8);
        for (int i = 0; i < this.count; i++) {
            int px = this.pivotX(i);
            int len = this.tube(i);
            int my = this.markY(i);
            double a = this.angle(i);
            int p = this.instrument.layout(i);
            boolean next = p == want;
            // the mark: where the chime rings true
            double off = Math.abs(this.swing(i));
            boolean inWindow = off <= GOOD_WINDOW;
            int markCol = inWindow ? 0xFF7CFF8A : 0xFFB8AC98;
            g.fill(px - 3, my + 2, px + 4, my + 4, markCol);
            g.fill(px - 1, my, px + 2, my + 2, markCol);
            if (next) {
                // the guide: a ring that closes on the mark as the chime swings in
                double until = this.untilMark(i);
                int rr = 5 + (int) Math.min(30.0, until * 2.2);
                int rgb = this.guideRgb();
                ring(g, px, my - 4, rr, rr, (int) (0x90 + 0x6F * pulse) << 24 | rgb);
                if (inWindow) {
                    glow(g, px, my - 4, 10, rgb, 0.8F);
                }
            }
            // the chime itself, swung about its cord
            g.pose().pushMatrix();
            g.pose().translate(px, by);
            g.pose().rotate((float) a);
            g.fill(0, 0, 1, CORD, 0xFFD8D0BC);
            int top = CORD;
            int light = this.tubeLight();
            int dark = this.tubeDark();
            float lit = this.ringing[i];
            int glowRgb = this.ringRgb[i];
            if (this.instrument == Instrument.PRISM_CHIMES) {
                light = mix(light, PrismLight.rgb(i % PrismLight.COUNT), 0.18F);
            }
            if (lit > 0.05F) {
                light = mix(light, glowRgb, lit * 0.8F);
                dark = mix(dark, glowRgb, lit * 0.5F);
            }
            if (this.instrument == Instrument.GLASS_BELLS) {
                // a bell of chime glass at the end of a longer cord, lit through
                int bellH = Math.max(12, len / 2);
                int bellW = 5 + len / 9;
                int y0 = top + len - bellH;
                g.fill(0, top, 1, y0, 0xFFD8D0BC);
                for (int k = 0; k < bellH; k++) {
                    float u = k / (float) (bellH - 1);
                    int hw = k >= bellH - 2 ? bellW + 1 : 2 + (int) (Math.pow(u, 1.7) * (bellW - 2));
                    g.fill(-hw, y0 + k, hw + 1, y0 + k + 1, 0xE0000000 | (k < bellH / 3 ? light : mix(light, dark, 0.3F + 0.4F * u)));
                    g.fill(-hw, y0 + k, -hw + 1, y0 + k + 1, 0xFF000000 | mix(dark, 0, 0.3F));
                    g.fill(hw, y0 + k, hw + 1, y0 + k + 1, 0xFF000000 | mix(dark, 0, 0.45F));
                }
                g.fill(-bellW / 2, y0 + 3, -bellW / 2 + 1, y0 + bellH - 4, 0xC0FFFFFF);
                g.fill(-1, top + len - 1, 2, top + len + 2, 0xFF3A2A5A);
            } else {
                g.fill(-3, top, 4, top + len, 0xFF000000 | dark);
                g.fill(-3, top, 1, top + len, 0xFF000000 | light);
                g.fill(-2, top + 1, -1, top + len - 2, 0xC0FFFFFF);
                g.fill(-4, top, 5, top + 2, 0xFF000000 | mix(dark, 0, 0.3F));
                g.fill(-4, top + len - 2, 5, top + len, 0xFF000000 | (this.instrument == Instrument.WIND_CHIMES ? 0x4FD8E8 : mix(light, 0xFFFFFF, 0.4F)));
            }
            if (this.miss[i] > 0.1F) {
                g.fill(-5, top + len - 3, 6, top + len + 1, (int) (this.miss[i] * 0xA0) << 24 | 0xFF6A4A);
            }
            g.pose().popMatrix();
            if (lit > 0.05F) {
                int bx = px - (int) (Math.sin(a) * (CORD + len));
                int bottom = by + (int) (Math.cos(a) * (CORD + len));
                glow(g, bx, bottom - len / 2, 9 + (int) (lit * 6.0F), glowRgb, lit);
                ring(g, bx, bottom - len / 2, (int) (8 + (1.0F - lit) * 26.0F), (int) (8 + (1.0F - lit) * 26.0F), (int) (lit * 0xC0) << 24 | glowRgb);
            }
            if (i < KEYS.length) {
                this.keyCap(g, px, my + 8, keyName(KEYS[i]), this.held.contains(KEYS[i]), this.playRgb());
            }
            g.centeredText(this.font, Notes.name(p), px, my + 22, next ? GOLD : DIM);
        }
        if (this.instrument.extra() > 0) {
            String gustText = Component.translatable("instrument.thesift.gust").getString();
            g.text(this.font, gustText, x0, by - 16, this.gusting() ? GOLD : DIM, true);
        }
        int hover = this.chimeAt(mouseX, mouseY);
        if (hover >= 0) {
            int p = this.instrument.layout(hover);
            g.text(this.font, Notes.name(p), mouseX + 8, mouseY - 12, 0xFF000000 | Notes.colour(p), true);
        }
    }

    @Override
    protected int judgementY() {
        return this.barY() - 18;
    }
}
