package com.thesift.client.music;

import com.mojang.blaze3d.platform.InputConstants;
import com.thesift.music.Instrument;
import com.thesift.music.Notes;
import com.thesift.music.PrismLight;
import com.thesift.music.Song;
import com.thesift.music.SongMatcher;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * M1 instrument play - DRUM: rhythm. The drums stand at the bottom, each head split into its
 * strokes (the Conga Drum: bass in the middle, the open tone in a ring, the slap at the rim; the
 * Thunder Drums and the Prism Drum: four drums, a low and a high stroke each); above them a beat
 * lane scrolls down to a hit line, one lane per stroke.
 *
 * <p>Strike with the keys under the lanes (S D F J K L, or A S D F J K L ;) or click the heads.
 * Following a drum song, its notes fall down the lanes in its rhythm, waiting on the line for the
 * first beat and then keeping time from every beat you land - each hit is judged Perfect, Good or
 * off the beat. In free play your own beats rise up the lanes behind you. The Thunder and Prism
 * Drums roll while a key is held.
 */
public final class DrumScreen extends InstrumentScreen {
    private static final int[] KEYS_6 = {InputConstants.KEY_S, InputConstants.KEY_D, InputConstants.KEY_F, InputConstants.KEY_J, InputConstants.KEY_K,
            InputConstants.KEY_L};
    private static final int[] KEYS_8 = {InputConstants.KEY_A, InputConstants.KEY_S, InputConstants.KEY_D, InputConstants.KEY_F, InputConstants.KEY_J,
            InputConstants.KEY_K, InputConstants.KEY_L, InputConstants.KEY_SEMICOLON};
    /** How far ahead the lane looks (ticks). */
    private static final double AHEAD = 40.0;
    private static final int LANE_W = 18;
    /** A held pad starts to roll after this many ticks, then strikes every {@link #ROLL_EVERY}. */
    private static final double ROLL_DELAY = 5.0;
    private static final double ROLL_EVERY = 2.0;

    private final int pads;
    private final int strokes;
    private final int drums;
    private final int[] keys;
    private final float[] hit;
    private final int[] hitRgb;
    private final double[] heldSince;
    private final double[] lastStrike;
    private final List<Spark> sparks = new ArrayList<>();
    private int mousePad = -1;

    private record Spark(int pad, double at, int rgb) {
    }

    DrumScreen(Instrument instrument, InteractionHand hand, ItemStack stack) {
        super(instrument, hand, stack);
        this.pads = instrument.layoutSize();
        this.strokes = this.pads == 6 ? 3 : 2;
        this.drums = this.pads / this.strokes;
        this.keys = this.pads == 6 ? KEYS_6 : KEYS_8;
        this.hit = new float[this.pads];
        this.hitRgb = new int[this.pads];
        this.heldSince = new double[this.pads];
        this.lastStrike = new double[this.pads];
        java.util.Arrays.fill(this.heldSince, -1.0);
    }

    // ------------------------------------------------------------------ geometry

    private int headY() {
        return this.height - 80;
    }

    private int drumX(int d) {
        int gap = this.drums == 2 ? 110 : 82;
        return this.width / 2 + (int) ((d - (this.drums - 1) / 2.0F) * gap);
    }

    private int drumR(int d) {
        return this.drums == 2 ? 40 - d * 6 : 34 - d * 3;
    }

    private int hitY() {
        return this.headY() - 52;
    }

    private int laneTop() {
        return Math.max(70, this.hitY() - 120);
    }

    private double speed() {
        return (this.hitY() - this.laneTop()) / AHEAD;
    }

    private int laneX(int pad) {
        return this.width / 2 - this.pads * LANE_W / 2 + pad * LANE_W;
    }

    /** The pad a point on the drums strikes, or -1. */
    private int padAt(double x, double y) {
        for (int d = 0; d < this.drums; d++) {
            int r = this.drumR(d);
            double nx = (x - this.drumX(d)) / r;
            double ny = (y - this.headY()) / (r * 0.5);
            double dist = Math.sqrt(nx * nx + ny * ny);
            if (dist <= 1.0) {
                int stroke = this.strokes == 3 ? (dist < 0.45 ? 0 : dist < 0.78 ? 1 : 2) : (dist < 0.55 ? 0 : 1);
                return d * this.strokes + stroke;
            }
        }
        return -1;
    }

    /** The pad that plays a written note (the playable note nearest it), or -1. */
    private int padFor(int pitch) {
        int n = this.instrument.nearest(pitch);
        for (int i = 0; i < this.pads; i++) {
            if (this.instrument.layout(i) == n) {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------ playing

    private void strike(int pad) {
        LocalPlayer p = this.minecraft.player;
        if (p == null) {
            return;
        }
        double now = clock();
        Song s = this.song;
        long time = p.level().getGameTime();
        int before = s != null && this.songFits ? Notes.CLIENT.progress(s, time) : -1;
        double due = before > 0 && s.rhythmic() ? Notes.CLIENT.lastAt(s) + s.gap(before - 1) : Double.NaN;
        boolean strong = this.shiftHeld();
        if (!this.play(this.instrument.layout(pad), strong ? Notes.STRONG : 0)) {
            return;
        }
        this.hit[pad] = 1.0F;
        this.hitRgb[pad] = this.playRgb();
        this.lastStrike[pad] = now;
        this.sparks.add(new Spark(pad, now, this.playRgb()));
        if (s != null && before > 0 && s.rhythmic() && !this.justCompleted()) {
            int after = Notes.CLIENT.progress(s, time);
            if (after == before + 1) {
                double off = Math.abs(now - due);
                if (off <= 1.2) {
                    this.judge(Component.translatable("instrument.thesift.judge.perfect"), GOLD);
                } else if (off <= SongMatcher.window(s.gap(before - 1))) {
                    this.judge(Component.translatable(now < due ? "instrument.thesift.judge.early" : "instrument.thesift.judge.late"), GOOD);
                } else {
                    this.judge(Component.translatable("instrument.thesift.judge.off"), WARN);
                }
            } else if (after <= 1 && before > 1) {
                this.judge(Component.translatable("instrument.thesift.judge.lost"), 0xFFFF5A5A);
            }
        }
    }

    @Override
    protected void pressed(int key, boolean shift) {
        for (int i = 0; i < this.pads; i++) {
            if (key == this.keys[i]) {
                this.heldSince[i] = clock();
                this.strike(i);
                return;
            }
        }
    }

    @Override
    protected void released(int key) {
        for (int i = 0; i < this.pads; i++) {
            if (key == this.keys[i]) {
                this.heldSince[i] = -1.0;
            }
        }
    }

    @Override
    protected void mouseDown(double x, double y, boolean right) {
        int pad = this.padAt(x, y);
        if (pad >= 0) {
            this.mousePad = pad;
            this.heldSince[pad] = clock();
            this.strike(pad);
        }
    }

    @Override
    protected void mouseUp(double x, double y, boolean right) {
        if (this.mousePad >= 0 && !this.held.contains(this.keys[this.mousePad])) {
            this.heldSince[this.mousePad] = -1.0;
        }
        this.mousePad = -1;
    }

    @Override
    protected void playTick(LocalPlayer player) {
        double now = clock();
        for (int i = 0; i < this.pads; i++) {
            this.hit[i] *= 0.78F;
            // the Thunder and Prism Drums roll while a pad is held
            if (this.instrument.extra() > 0 && this.heldSince[i] >= 0.0 && now - this.heldSince[i] >= ROLL_DELAY
                    && now - this.lastStrike[i] >= ROLL_EVERY - 0.1) {
                this.strike(i);
            }
        }
        for (Iterator<Spark> it = this.sparks.iterator(); it.hasNext(); ) {
            if (now - it.next().at() > AHEAD) {
                it.remove();
            }
        }
    }

    @Override
    protected String glyph(int pitch) {
        int pad = this.padFor(pitch);
        return pad < 0 ? "·" : keyName(this.keys[pad]);
    }

    // ------------------------------------------------------------------ drawing

    private int shellLight() {
        return switch (this.instrument) {
            case THUNDER_DRUMS -> 0x8A3A2A;
            case PRISM_DRUM -> 0xDCDFF4;
            default -> 0xC8823A;
        };
    }

    private int shellDark() {
        return switch (this.instrument) {
            case THUNDER_DRUMS -> 0x4A1A16;
            case PRISM_DRUM -> 0x8A88B4;
            default -> 0x7A4A22;
        };
    }

    private int hoop() {
        return switch (this.instrument) {
            case THUNDER_DRUMS -> 0x5A6068;
            case PRISM_DRUM -> 0xE3A73C;
            default -> 0xC8CCD8;
        };
    }

    @Override
    protected void drawInstrument(GuiGraphicsExtractor g, int mouseX, int mouseY, double now) {
        this.drawLane(g, now);
        int hy = this.headY();
        int hover = this.padAt(mouseX, mouseY);
        int want = this.wanted();
        float pulse = 0.5F + 0.5F * Mth.sin((float) now * 0.5F);
        for (int d = 0; d < this.drums; d++) {
            int x = this.drumX(d);
            int r = this.drumR(d);
            int ry = r / 2;
            int dip = 0;
            for (int s = 0; s < this.strokes; s++) {
                dip = Math.max(dip, (int) (this.hit[d * this.strokes + s] * 2.0F));
            }
            int y = hy + dip;
            // the shell, tapering to its foot, banded with hoops
            int depth = (int) (r * 0.95F);
            for (int k = 0; k <= depth; k++) {
                float u = k / (float) depth;
                int hw = (int) (r * (1.0F - 0.18F * u));
                int col = mix(this.shellLight(), this.shellDark(), 0.25F + 0.5F * u);
                g.fill(x - hw, y + k, x + hw, y + k + 1, 0xFF000000 | col);
                g.fill(x - hw, y + k, x - hw + 3, y + k + 1, 0xFF000000 | mix(col, 0, 0.35F));
                g.fill(x + hw - 4, y + k, x + hw, y + k + 1, 0xFF000000 | mix(col, 0, 0.45F));
                g.fill(x - hw / 3, y + k, x - hw / 3 + 2, y + k + 1, 0xFF000000 | mix(col, 0xFFFFFF, 0.18F));
            }
            for (float band : new float[]{0.08F, 0.55F, 0.95F}) {
                int by = y + (int) (depth * band);
                int hw = (int) (r * (1.0F - 0.18F * band)) + 1;
                g.fill(x - hw, by, x + hw, by + 2, 0xFF000000 | this.hoop());
            }
            if (this.instrument == Instrument.PRISM_DRUM) {
                for (int c = 0; c < PrismLight.COUNT; c++) {
                    disc(g, x - r / 2 + c * r / 3, y + depth / 3, 2, 0xFF000000 | PrismLight.rgb(c));
                }
            }
            // the head: hide with its rim, its stroke zones faintly ringed
            ellipse(g, x, y, r + 2, ry + 1, 0xFF000000 | this.hoop());
            ellipse(g, x, y, r, ry, 0xFFEFE2C4);
            ellipse(g, x - r / 5, y - ry / 4, r / 2, ry / 3, 0x30FFFFFF);
            for (int s = this.strokes - 1; s >= 0; s--) {
                int pad = d * this.strokes + s;
                float outer = this.strokes == 3 ? (s == 0 ? 0.45F : s == 1 ? 0.78F : 1.0F) : (s == 0 ? 0.55F : 1.0F);
                float lit = this.hit[pad];
                if (lit > 0.05F) {
                    ellipse(g, x, y, (int) (r * outer), (int) (ry * outer), (int) (lit * 0xC0) << 24 | this.hitRgb[pad]);
                    ring(g, x, y, (int) (r * outer + (1.0F - lit) * 14.0F), (int) (ry * outer + (1.0F - lit) * 7.0F),
                            (int) (lit * 0xFF) << 24 | this.hitRgb[pad]);
                }
                if (pad == want) {
                    ring(g, x, y, (int) (r * outer) - 1, (int) (ry * outer) - 1, (int) (0x70 + 0x8F * pulse) << 24 | this.guideRgb());
                }
                if (pad == hover) {
                    ring(g, x, y, (int) (r * outer) - 2, (int) (ry * outer) - 1, 0xA0FFFFFF);
                }
                if (s < this.strokes - 1) {
                    ring(g, x, y, (int) (r * outer), (int) (ry * outer), 0x40503020);
                }
                // which key strikes this zone
                float mid = this.strokes == 3 ? (s == 0 ? 0.0F : s == 1 ? 0.62F : 0.9F) : (s == 0 ? 0.0F : 0.78F);
                g.centeredText(this.font, keyName(this.keys[pad]), x + (int) (r * mid), y - 4, this.held.contains(this.keys[pad]) ? GOLD : 0xFF6A5030);
            }
        }
        if (hover >= 0) {
            int p = this.instrument.layout(hover);
            g.text(this.font, Notes.name(p), mouseX + 8, mouseY - 12, 0xFF000000 | Notes.colour(p), true);
        }
    }

    /** The beat lane: the song's notes falling to the hit line in its rhythm, or your own beats rising away. */
    private void drawLane(GuiGraphicsExtractor g, double now) {
        int top = this.laneTop();
        int hy = this.hitY();
        int x0 = this.laneX(0);
        int x1 = this.laneX(this.pads);
        double speed = this.speed();
        g.fill(x0 - 3, top - 3, x1 + 3, hy + 16, 0xA0080A12);
        for (int i = 0; i < this.pads; i++) {
            int lx = this.laneX(i);
            int drumRgb = i / this.strokes % 2 == 0 ? 0x2A2A3A : 0x22303A;
            g.fill(lx + 1, top, lx + LANE_W - 1, hy + 14, 0xC0000000 | drumRgb);
            if (this.hit[i] > 0.05F) {
                g.fillGradient(lx + 1, top, lx + LANE_W - 1, hy, 0, (int) (this.hit[i] * 0x90) << 24 | this.hitRgb[i]);
            }
            this.keyCap(g, lx + LANE_W / 2, hy + 18, keyName(this.keys[i]), this.held.contains(this.keys[i]), this.playRgb());
        }
        Song s = this.song;
        LocalPlayer p = this.minecraft.player;
        boolean following = s != null && this.songFits && p != null;
        long time = p == null ? 0L : p.level().getGameTime();
        int done = following ? Notes.CLIENT.progress(s, time) : 0;
        // the beat grid: a line every beat, in step with the song once it has begun
        double anchor = following && done > 0 && s.rhythmic() ? Notes.CLIENT.lastAt(s) : Math.floor(now / 10.0) * 10.0;
        for (int k = -4; k < 8; k++) {
            double t = anchor + k * 10.0;
            double y = hy - (t - now) * speed;
            if (y >= top && y <= hy) {
                g.fill(x0, (int) y, x1, (int) y + 1, 0x30FFFFFF);
            }
        }
        // the hit line
        g.fill(x0 - 2, hy - 1, x1 + 2, hy + 2, 0xD0F4ECD8);
        // your own beats, rising away
        for (Spark sp : this.sparks) {
            double y = hy - (now - sp.at()) * speed;
            if (y >= top) {
                float k = (float) ((y - top) / Math.max(1.0, hy - top));
                int lx = this.laneX(sp.pad());
                g.fill(lx + 4, (int) y - 1, lx + LANE_W - 4, (int) y + 2, (int) (k * 0xC0) << 24 | sp.rgb());
            }
        }
        if (!following) {
            return;
        }
        // the song's notes, falling in its rhythm (waiting on the line for its first beat)
        int idx = Notes.CLIENT.nextIndex(s, time);
        boolean waiting = done == 0 || !s.rhythmic();
        double due = waiting ? now : Notes.CLIENT.lastAt(s) + s.gap(done - 1);
        float pulse = 0.5F + 0.5F * Mth.sin((float) now * 0.6F);
        for (int k = idx; k < s.length(); k++) {
            if (k > idx) {
                due += s.rhythmic() ? s.gap(k - 1) : 10.0;
            }
            double y = hy - (due - now) * speed;
            if (y < top) {
                break;
            }
            int pad = this.padFor(s.note(k));
            if (pad < 0) {
                continue;
            }
            int lx = this.laneX(pad);
            int rgb = s.prism() ? PrismLight.rgb(s.colour(k)) : 0xFFE7A0;
            float fadeOut = y > hy + 4 ? Math.max(0.0F, 1.0F - (float) (y - hy - 4) / 30.0F) : 1.0F;
            int gy = (int) Math.min(y, hy + 30);
            if (fadeOut <= 0.0F) {
                continue;
            }
            if (k == idx && waiting) {
                glow(g, lx + LANE_W / 2, hy, 10, rgb, 0.6F + 0.4F * pulse);
            }
            g.fill(lx + 2, gy - 3, lx + LANE_W - 2, gy + 3, fade(0xFF000000 | rgb, fadeOut));
            g.fill(lx + 3, gy - 2, lx + LANE_W - 3, gy - 1, fade(0xC0FFFFFF, fadeOut));
            if (s.rhythmic() && s.beat(k) > 1) {
                // a long note trails its length
                int tail = (int) (s.gap(k) * speed * 0.5);
                g.fill(lx + LANE_W / 2 - 1, gy - 3 - tail, lx + LANE_W / 2 + 1, gy - 3, fade(0x80000000 | rgb, fadeOut));
            }
        }
    }

    @Override
    protected int judgementY() {
        return this.laneTop() - 12;
    }
}
