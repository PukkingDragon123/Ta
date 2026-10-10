package com.thesift.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The awakening of a gate once the Sift Symphony is played at a Sculk Summoner: its timeline and its
 * music, shared by the server (the summoner plays it, see
 * {@link com.thesift.block.entity.SculkSummonerBlockEntity}) and the client (the camera follows it).
 * Ticks count from the Symphony's last note.
 *
 * <ol>
 *   <li>the rim lights up, a stretch at a time climbing both sides of the frame, each with a note of
 *   an arpeggio in A minor (Am - F - G - E) over a walking bass;</li>
 *   <li>souls stream in from the sculk sensors and the summoner's core and spiral into a vortex;</li>
 *   <li>cyan light gathers ring by ring from the rim inward, a chime per ring climbing to the
 *   leading tone, while the world bends towards the gate;</li>
 *   <li>the last ring closes on an A major chord - the minor song resolves - in a blinding flash,
 *   and the portal appears all at once with a shockwave.</li>
 * </ol>
 *
 * <p>Pitches are note-block semitones: 0 is F#, 24 two octaves up (for the harp, F#3 to F#5; the
 * bell and the chime sound two octaves higher, the bass two lower).</p>
 */
public final class GateAwakening {
    /** The first note of the rim; then one every {@link #RIM_STEP} ticks. */
    public static final int RIM_START = 8;
    public static final int RIM_STEP = 4;
    public static final int RIM_NOTES = 16;
    /** The portal closes ring by ring between these two ticks. */
    public static final int RINGS_START = 80;
    public static final int RINGS_END = 140;
    /** The final chord: the gate is open. */
    public static final int CLIMAX = 150;
    // B1 Portal & sky FX: the staged opening seen on the client (see com.thesift.client.gate.GateAwakeningFx):
    // cyan light swells from the first note, the world starts to bend at WARP_START, everything is drawn
    // in just before the climax, the flash goes off on the chord and the shockwave rolls out of it.
    /** The world begins to bend round the gate. */
    public static final int WARP_START = 56;
    /** The last breath before the flash: everything is pulled in towards the gate. */
    public static final int IMPLODE_START = CLIMAX - 12;
    /** How long the shockwave rolls outwards after the flash. */
    public static final int SHOCK_TICKS = 16;
    /** A falling sparkle after the chord. */
    public static final int TAIL_START = CLIMAX + 8;
    public static final int TAIL_STEP = 6;
    /** The camera is home again and the summoner is quiet. */
    public static final int LENGTH = 200;
    /** Players this close to the gate see it through the cinematic camera. */
    public static final double CINEMATIC_RANGE = 32.0;

    /** i - VI - VII - V in A minor (Am, F, G, E), one rising arpeggio per bar. */
    public static final int[] ARPEGGIO = {3, 6, 10, 15, 11, 15, 18, 23, 8, 13, 17, 20, 10, 14, 17, 22};
    /** The bass under each bar of the arpeggio: A, F, G, E. */
    public static final int[] BASS = {3, 11, 13, 10};
    /** A major: the minor song resolves as the gate opens. */
    public static final int[] FINAL_CHORD = {3, 7, 10, 15, 19};
    /** Down the A major chord, after the climax. */
    public static final int[] TAIL = {15, 10, 7, 3};
    /** The rings climb A harmonic minor to its leading tone, G#, which the final chord resolves. */
    private static final int[] RING_SCALE = {3, 5, 6, 8, 10, 11, 14};
    // Client side: the gate waking nearby, refreshed every tick by its summoner's block entity.
    public static @Nullable BlockPos clientSource;
    public static @Nullable Vec3 clientCentre;
    public static float clientSpan;
    public static int clientTick;
    public static long clientSeen;

    private GateAwakening() {
    }

    /** Note-block pitch for a semitone (0..24). */
    public static float pitch(int n) {
        return (float) Math.pow(2.0, (Math.max(0, Math.min(24, n)) - 12) / 12.0);
    }

    /** Plays one note of the awakening's music for everyone nearby. */
    public static void note(ServerLevel level, Vec3 at, Holder<SoundEvent> instrument, int n, float volume) {
        level.playSound(null, at.x, at.y, at.z, instrument.value(), SoundSource.BLOCKS, volume, pitch(n));
    }

    /** The chime for step {@code ring} of {@code rings} as the portal closes: a climb that always ends on the leading tone. */
    public static int ringNote(int ring, int rings) {
        int last = RING_SCALE.length - 1;
        int i = rings > RING_SCALE.length ? (ring * last + (rings - 1) / 2) / (rings - 1) : RING_SCALE.length - rings + ring;
        return RING_SCALE[Math.max(0, Math.min(last, i))];
    }

    /** When step {@code ring} of {@code rings} (outermost first) falls into place. */
    public static int ringTick(int ring, int rings) {
        return rings <= 1 ? RINGS_START : RINGS_START + ring * (RINGS_END - RINGS_START) / (rings - 1);
    }
}
