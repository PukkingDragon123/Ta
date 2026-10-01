package com.thesift.client.music;

import com.thesift.music.Performance;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/**
 * The music of the Grand Stage, written out note by note for the note-block instruments.
 *
 * <p>The performance (26 bars at 120 bpm, in A minor over vi-IV-I-V: Am - F - C - G): the Conga
 * Drum begins alone; the Crane Flute enters with the melody; the Guitar joins with rolling
 * arpeggios; bells answer the flute as the sky goes dark; everything swells to a climax - then
 * silence but for a music box while the Mask rises, and a slow descending theme with a drone and a
 * gathering drum roll as it becomes the Conductor.
 *
 * <p>Then a loop for each movement of the fight: heavy drums and bass for the Shell, a racing
 * flute for the Wings, frantic guitar, didgeridoo and bells for the Strings.
 *
 * <p>Pitches are note-block semitones: 0 is the lowest note (F#), 24 two octaves up.
 */
public final class Score {
    public record Note(Holder<SoundEvent> sound, int n, float volume) {
        public float pitch() {
            return (float) Math.pow(2.0, (this.n - 12) / 12.0);
        }
    }

    private static final Map<Integer, List<Note>> PERFORMANCE = new HashMap<>();
    @SuppressWarnings("unchecked")
    private static final Map<Integer, List<Note>>[] FIGHT = new Map[]{new HashMap<>(), new HashMap<>(), new HashMap<>()};
    public static final int FIGHT_LOOP = Performance.BAR * 2;

    private static final int B = Performance.BEAT;
    private static final int BAR = Performance.BAR;
    /** Am, F, C, G. */
    private static final int[] ROOT = {3, 11, 6, 1};
    private static final int[][] ARP = {{3, 10, 15, 10, 18, 15, 10, 15}, {11, 15, 18, 15, 23, 18, 15, 18}, {6, 13, 18, 13, 22, 18, 13, 18},
            {1, 8, 13, 8, 17, 13, 8, 13}};
    /** The flute's melody: per bar, (beat, note) pairs. */
    private static final int[][][] MELODY = {
            {{0, 10}, {2, 8}, {3, 6}}, {{0, 3}, {2, 6}, {3, 8}}, {{0, 10}, {1, 13}, {2, 10}, {3, 8}}, {{0, 6}, {3, 5}},
            {{0, 3}, {2, 6}, {3, 10}}, {{0, 15}, {2, 13}, {3, 11}}, {{0, 10}, {1, 8}, {2, 6}, {3, 8}}, {{0, 8}, {2, 5}},
    };
    private static final int[][] BELLS = {{15, 10}, {15, 11}, {13, 10}, {13, 8}};

    static {
        writePerformance();
        writeFight();
    }

    private Score() {
    }

    public static List<Note> performance(int tick) {
        return PERFORMANCE.getOrDefault(tick, List.of());
    }

    public static List<Note> fight(int phase, int tick) {
        return FIGHT[Math.max(0, Math.min(2, phase - 1))].getOrDefault(Math.floorMod(tick, FIGHT_LOOP), List.of());
    }

    private static void add(Map<Integer, List<Note>> m, int tick, Holder<SoundEvent> s, int n, float vol) {
        m.computeIfAbsent(tick, k -> new ArrayList<>()).add(new Note(s, Math.max(0, Math.min(24, n)), vol));
    }

    private static void p(int tick, Holder<SoundEvent> s, int n, float vol) {
        add(PERFORMANCE, tick, s, n, vol);
    }

    private static void drums(int bar, float vol, boolean full) {
        int t0 = bar * BAR;
        // a conga pattern: low on 1 and the "and" of 3, high slaps on the "and" of 2 and 4
        p(t0, SoundEvents.NOTE_BLOCK_BASEDRUM, 6, vol);
        p(t0 + B * 2 + B / 2, SoundEvents.NOTE_BLOCK_BASEDRUM, 6, vol * 0.8F);
        p(t0 + B + B / 2, SoundEvents.NOTE_BLOCK_BASEDRUM, 14, vol * 0.6F);
        p(t0 + B * 3 + B / 2, SoundEvents.NOTE_BLOCK_BASEDRUM, 14, vol * 0.6F);
        if (full) {
            p(t0 + B, SoundEvents.NOTE_BLOCK_SNARE, 12, vol * 0.45F);
            p(t0 + B * 3, SoundEvents.NOTE_BLOCK_SNARE, 12, vol * 0.45F);
            for (int i = 0; i < 8; i++) {
                p(t0 + i * B / 2, SoundEvents.NOTE_BLOCK_HAT, 18, vol * 0.2F);
            }
        }
    }

    private static void melody(int bar, int line, Holder<SoundEvent> s, int shift, float vol) {
        for (int[] note : MELODY[line % MELODY.length]) {
            p(bar * BAR + note[0] * B, s, note[1] + shift, vol);
        }
    }

    private static void arps(int bar, float vol) {
        int[] a = ARP[bar % 4];
        for (int i = 0; i < 8; i++) {
            p(bar * BAR + i * B / 2, SoundEvents.NOTE_BLOCK_GUITAR, a[i], vol * (i == 0 ? 1.0F : 0.75F));
        }
    }

    private static void bass(int bar, float vol) {
        int r = ROOT[bar % 4];
        p(bar * BAR, SoundEvents.NOTE_BLOCK_BASS, r, vol);
        p(bar * BAR + B * 2, SoundEvents.NOTE_BLOCK_BASS, r, vol * 0.8F);
        p(bar * BAR + B * 3 + B / 2, SoundEvents.NOTE_BLOCK_BASS, r + 7, vol * 0.6F);
    }

    private static void writePerformance() {
        // bars 0-3: the Conga Drum alone, softly
        for (int bar = 0; bar < 4; bar++) {
            drums(bar, 0.5F + bar * 0.08F, false);
        }
        // bars 4-7: the Crane Flute enters with the melody
        for (int bar = 4; bar < 8; bar++) {
            drums(bar, 0.6F, false);
            melody(bar, bar - 4, SoundEvents.NOTE_BLOCK_FLUTE, 0, 0.8F);
        }
        // bars 8-11: the Guitar joins
        for (int bar = 8; bar < 12; bar++) {
            drums(bar, 0.65F, true);
            melody(bar, bar - 4, SoundEvents.NOTE_BLOCK_FLUTE, 0, 0.85F);
            arps(bar, 0.6F);
        }
        // bars 12-15: bells answer, the bass arrives, the sky darkens
        for (int bar = 12; bar < 16; bar++) {
            drums(bar, 0.7F, true);
            melody(bar, bar - 12, SoundEvents.NOTE_BLOCK_FLUTE, 0, 0.9F);
            arps(bar, 0.6F);
            bass(bar, 0.8F);
            int[] bell = BELLS[bar % 4];
            p(bar * BAR + B, SoundEvents.NOTE_BLOCK_BELL, bell[0], 0.5F);
            p(bar * BAR + B * 3, SoundEvents.NOTE_BLOCK_BELL, bell[1], 0.45F);
        }
        // bars 16-19: the climax - the melody doubled on bells, chimes, every drum
        for (int bar = 16; bar < 20; bar++) {
            drums(bar, 0.9F, true);
            p(bar * BAR + B * 2, SoundEvents.NOTE_BLOCK_BASEDRUM, 6, 0.8F);
            melody(bar, bar - 12, SoundEvents.NOTE_BLOCK_FLUTE, 0, 1.0F);
            melody(bar, bar - 12, SoundEvents.NOTE_BLOCK_BELL, 0, 0.55F);
            arps(bar, 0.7F);
            bass(bar, 0.95F);
            int[] a = ARP[bar % 4];
            for (int i = 1; i < 8; i += 2) {
                p(bar * BAR + i * B / 2, SoundEvents.NOTE_BLOCK_CHIME, a[i], 0.35F);
            }
        }
        // the last climactic chord, then silence
        p(20 * BAR - 1, SoundEvents.NOTE_BLOCK_BASEDRUM, 3, 1.0F);
        // bars 20-21: a music box, alone, while the Mask rises from beneath the stage
        int[] box = {3, 6, 10, 15, 10, 6, 3, -1};
        for (int i = 0; i < box.length; i++) {
            if (box[i] >= 0) {
                p(20 * BAR + B / 2 + i * B, SoundEvents.NOTE_BLOCK_BELL, box[i], 0.6F);
                p(20 * BAR + B / 2 + i * B, SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE, box[i], 0.25F);
            }
        }
        // bars 22-25: the Mask's theme - a drone, a slow descent, and the drums gathering
        int[][] fall = {{15, 13, 11, 10}, {11, 10, 8, 6}, {8, 6, 5, 3}, {3, 3, 3, 3}};
        for (int k = 0; k < 4; k++) {
            int bar = 22 + k;
            p(bar * BAR, SoundEvents.NOTE_BLOCK_BASS, 3, 0.9F);
            p(bar * BAR, SoundEvents.NOTE_BLOCK_DIDGERIDOO, 3, 0.6F);
            for (int beat = 0; beat < 4; beat++) {
                p(bar * BAR + beat * B, SoundEvents.NOTE_BLOCK_HARP, fall[k][beat], 0.7F);
                if (k < 3) {
                    p(bar * BAR + beat * B, SoundEvents.NOTE_BLOCK_HARP, fall[k][beat] + 12, 0.35F);
                }
            }
            if (k >= 1) {
                for (int beat = 0; beat < 4; beat++) {
                    p(bar * BAR + beat * B, SoundEvents.NOTE_BLOCK_BASEDRUM, 4, 0.5F + k * 0.12F);
                }
            }
        }
        // a drum roll building to the transformation
        for (int t = 25 * BAR; t < 26 * BAR; t += 2) {
            p(t, SoundEvents.NOTE_BLOCK_SNARE, 12, 0.25F + 0.6F * (t - 25 * BAR) / BAR);
        }
    }

    private static void f(int phase, int tick, Holder<SoundEvent> s, int n, float vol) {
        add(FIGHT[phase], tick, s, n, vol);
    }

    private static void writeFight() {
        // the Shell: heavy drums and a stomping bass riff
        int[][] riff = {{3, 3, 6, 3, 8, 6, 5, 1}, {3, 3, 6, 3, 10, 8, 6, 5}};
        for (int bar = 0; bar < 2; bar++) {
            int t0 = bar * BAR;
            for (int beat : new int[]{0, 3, 4, 6}) {
                f(0, t0 + beat * B / 2, SoundEvents.NOTE_BLOCK_BASEDRUM, 4, 0.7F);
            }
            f(0, t0 + B, SoundEvents.NOTE_BLOCK_SNARE, 12, 0.4F);
            f(0, t0 + B * 3, SoundEvents.NOTE_BLOCK_SNARE, 12, 0.4F);
            for (int i = 0; i < 8; i++) {
                f(0, t0 + i * B / 2, SoundEvents.NOTE_BLOCK_BASS, riff[bar][i], 0.6F);
            }
        }
        // the Wings: a racing flute ostinato over harp and a light beat
        int[][] fl = {{15, 10, 13, 10, 15, 10, 18, 13}, {13, 8, 11, 8, 13, 8, 17, 11}};
        for (int bar = 0; bar < 2; bar++) {
            int t0 = bar * BAR;
            for (int i = 0; i < 8; i++) {
                f(1, t0 + i * B / 2, SoundEvents.NOTE_BLOCK_FLUTE, fl[bar][i], 0.45F);
                f(1, t0 + i * B / 2, SoundEvents.NOTE_BLOCK_HAT, 18, 0.15F);
            }
            f(1, t0, SoundEvents.NOTE_BLOCK_BASEDRUM, 6, 0.5F);
            f(1, t0 + B * 2, SoundEvents.NOTE_BLOCK_BASEDRUM, 6, 0.45F);
            f(1, t0 + B, SoundEvents.NOTE_BLOCK_HARP, fl[bar][0] - 12, 0.4F);
            f(1, t0 + B * 3, SoundEvents.NOTE_BLOCK_HARP, fl[bar][4] - 12, 0.4F);
            f(1, t0, SoundEvents.NOTE_BLOCK_BASS, bar == 0 ? 3 : 1, 0.6F);
        }
        // the Strings: frantic guitar, a didgeridoo drone, bells stabbing, snare on every beat
        int[][] gt = {{3, 6, 10, 6, 3, 6, 10, 13}, {1, 5, 8, 5, 1, 5, 8, 11}};
        for (int bar = 0; bar < 2; bar++) {
            int t0 = bar * BAR;
            for (int i = 0; i < 8; i++) {
                f(2, t0 + i * B / 2, SoundEvents.NOTE_BLOCK_GUITAR, gt[bar][i], 0.5F);
                if (i % 2 == 1) {
                    f(2, t0 + i * B / 2, SoundEvents.NOTE_BLOCK_BIT, gt[bar][i] + 12, 0.2F);
                }
            }
            for (int beat = 0; beat < 4; beat++) {
                f(2, t0 + beat * B, SoundEvents.NOTE_BLOCK_SNARE, 12, 0.35F);
                f(2, t0 + beat * B, SoundEvents.NOTE_BLOCK_BASEDRUM, 4, beat % 2 == 0 ? 0.7F : 0.4F);
            }
            f(2, t0, SoundEvents.NOTE_BLOCK_DIDGERIDOO, bar == 0 ? 3 : 1, 0.6F);
            f(2, t0, SoundEvents.NOTE_BLOCK_BELL, bar == 0 ? 15 : 13, 0.4F);
            f(2, t0 + B * 2 + B / 2, SoundEvents.NOTE_BLOCK_BELL, bar == 0 ? 18 : 17, 0.3F);
        }
    }
}
