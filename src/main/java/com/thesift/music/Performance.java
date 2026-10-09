package com.thesift.music;

import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

/**
 * The Grand Stage's performance timeline, shared by the server (its hidden Boss Den) and the client (the
 * score, the dark sky, the silence). Ticks from the moment the three instruments reach the stage.
 */
public final class Performance {
    /** Ticks per beat (120 bpm). */
    public static final int BEAT = 10;
    public static final int BAR = BEAT * 4;
    /** When the Mask rises from beneath the stage. */
    public static final int MASK_RISES = BAR * 20;
    /** When the performance ends: the fallen Mask is rebuilt into the Conductor and the fight begins. */
    public static final int LENGTH = BAR * 26;

    /** Client side: the nearest stage that is performing, refreshed every tick by its block entity. */
    public static @Nullable BlockPos clientStage;
    public static int clientTick;
    public static long clientSeen;

    private Performance() {
    }

    /** How dark the sky has grown at this point of the performance (0..1). */
    public static float darkness(int tick) {
        if (tick < BAR * 4) {
            return 0.0F;
        }
        return Math.min(1.0F, (tick - BAR * 4) / (float) (BAR * 10));
    }
}
