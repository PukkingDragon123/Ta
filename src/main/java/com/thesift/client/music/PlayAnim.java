package com.thesift.client.music;

import com.thesift.music.Instrument;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/**
 * INS free play: what every player's instrument is doing right now, by entity id - fed by the
 * local player's own notes (at once) and by everyone else's ({@code InstrumentPlay.Shown}). The
 * arm poses ({@link InstrumentPoses}), the first-person hold ({@link FreePlayHand}) and the moving
 * parts ({@link InstrumentPlayProperty}) all read it, on a real-time clock so animations run
 * smoothly between ticks.
 */
public final class PlayAnim {
    /** Seconds after which a quiet player is forgotten. */
    private static final double FORGET = 30.0;
    private static final Map<Integer, State> STATES = new HashMap<>();

    private PlayAnim() {
    }

    /** One player's instrument. */
    public static final class State {
        public @Nullable Instrument instrument;
        public int pitch = -1;
        /** 0..1: where the note sits in the instrument's range (low to high). */
        public float place = 0.5F;
        public boolean strong;
        public boolean chord;
        /** Which hand struck last (drums): true for the left (free) hand. */
        public boolean leftHand;
        public double noteAt = -100.0;
        public double leftAt = -100.0;
        public double rightAt = -100.0;
        /** A flute note held on until this time. */
        public double sustainUntil = -100.0;
        public int notes;
        double seen;
    }

    /** Seconds on a real-time clock. */
    public static double now() {
        return System.nanoTime() / 1.0E9;
    }

    public static @Nullable State get(int id) {
        return STATES.get(id);
    }

    /** A note played by entity {@code id} on {@code ins} (local or from the server). */
    public static State note(int id, Instrument ins, int pitch, boolean strong, boolean chord) {
        double t = now();
        State s = STATES.computeIfAbsent(id, k -> new State());
        if (STATES.size() > 128) {
            STATES.values().removeIf(o -> t - o.seen > FORGET);
        }
        s.instrument = ins;
        s.pitch = pitch;
        int lo = ins.lowest();
        int hi = ins.highest();
        s.place = hi > lo ? Mth.clamp((pitch - lo) / (float) (hi - lo), 0.0F, 1.0F) : 0.5F;
        s.strong = strong;
        s.chord = chord;
        s.noteAt = t;
        s.seen = t;
        s.notes++;
        if (ins.family() == Instrument.Family.DRUM) {
            // the high drum(s) are played by the free hand, the low one(s) by the hand that holds them
            int pad = ins.padIndex(pitch);
            s.leftHand = pad >= ins.layoutSize() / 2;
            if (s.leftHand) {
                s.leftAt = t;
            } else {
                s.rightAt = t;
            }
        } else {
            s.leftHand = (s.notes & 1) == 0;
        }
        return s;
    }

    /** A held flute note breathing on (local or from the server). */
    public static void sustain(int id, double seconds) {
        State s = STATES.get(id);
        if (s != null) {
            s.sustainUntil = Math.max(s.sustainUntil, now() + seconds);
            s.seen = now();
        }
    }

    /** Seconds since the last note (large if none). */
    public static float since(@Nullable State s) {
        return s == null ? 100.0F : (float) Math.min(100.0, now() - s.noteAt);
    }

    /** How strongly the instrument still rings: 1 on the note, fading over {@code decay} seconds. */
    public static float energy(@Nullable State s, float decay) {
        if (s == null) {
            return 0.0F;
        }
        float e = (float) Math.exp(-since(s) / decay);
        if (s.sustainUntil > now()) {
            e = Math.max(e, 0.6F);
        }
        return e;
    }

    /** A hit's dip (0..1): quickly down on the hit, springing back up with a little overshoot. */
    public static float hit(double at) {
        float t = (float) (now() - at);
        if (t < 0.0F || t > 0.45F) {
            return 0.0F;
        }
        if (t < 0.05F) {
            return t / 0.05F;
        }
        float k = (t - 0.05F) / 0.4F;
        return (float) (Math.exp(-k * 4.0F) * Math.cos(k * 7.0F));
    }

    /** The swing of chimes struck {@code t} seconds ago: a slow, fading pendulum (-1..1). */
    public static float swing(@Nullable State s) {
        if (s == null) {
            return 0.0F;
        }
        float t = since(s);
        return (float) (Math.exp(-t / 1.6) * Math.sin(t * Mth.TWO_PI * 1.1));
    }

    /** Forgets everyone (a new world). */
    public static void clear() {
        STATES.clear();
    }
}
