package com.thesift.world.sky;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * W-sky: where a grabbed Sky Vine hangs from and how long a rope it makes - shared by the player's swinging
 * ({@link SkySwingClient}), the server's checks ({@link SkySwing}) and, later, the Swingers.
 *
 * <p>A hanging strand swings from the top of its highest block, so the rope is as long as the strand above your hands.
 * A span (a vine stretched between islands or trees) is held where you grab it, and you can go hand over hand along it.
 */
public final class SkyVines {
    /** The longest strand followed up or down. */
    public static final int MAX_STRAND = 48;

    private SkyVines() {
    }

    /**
     * Where the rope hangs from.
     *
     * @param block the vine block the rope hangs from (the top of the strand, or the span block held)
     * @param pivot the point it hangs from
     * @param span  true when the hands hold a span directly (hand over hand travel is possible)
     * @param reach how far the strand goes down below the pivot (the rope can be no longer than this)
     */
    public record Anchor(BlockPos block, Vec3 pivot, boolean span, double reach) {
    }

    public static boolean isVine(BlockState state) {
        return state.getBlock() instanceof SkyVineBlock;
    }

    /** True if a vine leads off sideways from pos: pos is part of a span, not a plain hanging strand. */
    public static boolean holdsSideways(BlockGetter level, BlockPos pos) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (isVine(level.getBlockState(pos.relative(d)))) {
                return true;
            }
        }
        return false;
    }

    /** The anchor of the vine grabbed at {@code grabbed}. */
    public static Anchor anchor(BlockGetter level, BlockPos grabbed) {
        BlockPos top = grabbed;
        int up = 0;
        while (up < MAX_STRAND && isVine(level.getBlockState(top.above()))) {
            top = top.above();
            up++;
        }
        boolean span = up == 0 && holdsSideways(level, top);
        Vec3 pivot = span ? Vec3.atCenterOf(top) : new Vec3(top.getX() + 0.5, top.getY() + 1.0, top.getZ() + 0.5);
        BlockPos bottom = grabbed;
        int down = 0;
        while (down < MAX_STRAND && isVine(level.getBlockState(bottom.below()))) {
            bottom = bottom.below();
            down++;
        }
        double reach = Math.max(1.0, pivot.y - bottom.getY() + 0.6);
        return new Anchor(top.immutable(), pivot, span, reach);
    }

    /**
     * The next block of a span from {@code from} towards {@code dir} (horizontal, unit length), for hand over hand travel:
     * a sideways neighbour, or one step up or down where the span sags or rises; null at the end of the span.
     */
    public static @Nullable BlockPos along(BlockGetter level, BlockPos from, Vec3 dir) {
        BlockPos best = null;
        double bestScore = 0.35;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos n = from.relative(d);
            double score = d.getStepX() * dir.x + d.getStepZ() * dir.z;
            if (score > bestScore && isVine(level.getBlockState(n))) {
                bestScore = score;
                best = n;
            }
        }
        if (best != null) {
            return best.immutable();
        }
        for (Direction v : new Direction[]{Direction.DOWN, Direction.UP}) {
            BlockPos n = from.relative(v);
            if (!isVine(level.getBlockState(n))) {
                continue;
            }
            for (Direction d : Direction.Plane.HORIZONTAL) {
                double score = (d.getStepX() * dir.x + d.getStepZ() * dir.z) * 0.9;
                if (score > bestScore && isVine(level.getBlockState(n.relative(d)))) {
                    bestScore = score;
                    best = n;
                }
            }
        }
        return best == null ? null : best.immutable();
    }
}
