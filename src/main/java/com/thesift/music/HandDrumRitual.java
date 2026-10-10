package com.thesift.music;

import com.thesift.block.SiftDrumBlock;
import com.thesift.block.entity.SiftDrumBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * INS free play: the Sift gate's rhythm ritual answered on a hand drum. A drum note played within
 * reach of a Sift Drum that holds a Warden Core counts as a strike of that drum: it wakes the call
 * and answers it beat for beat, so the ritual works with the drum in your hands as well as with the
 * drum on the floor. The floor drum echoes every answered beat (its head bounces, its ring spreads),
 * so you can see the beat arrive.
 */
public final class HandDrumRitual {
    /** How far (blocks, horizontally) a hand drum carries to a ritual drum. */
    public static final int REACH = 6;

    private HandDrumRitual() {
    }

    /** Server: {@code player} just played a drum note. */
    public static void hear(ServerLevel level, Player player) {
        SiftDrumBlockEntity drum = nearest(level, player.blockPosition());
        if (drum == null) {
            return;
        }
        String phase = drum.ritualState().phase();
        if (!phase.equals("ANSWER") && !phase.equals("IDLE")) {
            // the drum is calling (or the gate is opening): listen, do not interrupt
            return;
        }
        SiftDrumBlock.beat(level, drum.getBlockPos(), 0.6F);
        drum.onPlayerBeat(player);
    }

    /** The nearest Sift Drum holding a Warden Core within reach of {@code at}, or null. */
    public static @Nullable SiftDrumBlockEntity nearest(ServerLevel level, BlockPos at) {
        SiftDrumBlockEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-REACH, -3, -REACH), at.offset(REACH, 3, REACH))) {
            BlockState state = level.getBlockState(p);
            if (state.getBlock() instanceof SiftDrumBlock && state.getValue(SiftDrumBlock.CORE)
                    && level.getBlockEntity(p) instanceof SiftDrumBlockEntity drum) {
                double d = p.distSqr(at);
                if (d < bestDist) {
                    bestDist = d;
                    best = drum;
                }
            }
        }
        return best;
    }
}
