package com.thesift.portal;

import com.thesift.block.SiftPortalBlock;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModTags;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Finds portal frames of any shape: a vertical, fully enclosed pocket of air whose in-plane border
 * is made entirely of {@code #thesift:portal_frame} blocks. This recognises the great reinforced
 * deepslate frames at the heart of Ancient Cities as well as hand-built Echo Frame portals.
 */
public final class PortalFrames {
    public static final int MAX_CELLS = 700;
    public static final int MAX_SPAN = 34;

    public record Frame(Direction.Axis axis, Set<BlockPos> interior, BlockPos center) {}

    private PortalFrames() {}

    public static boolean isFrame(BlockState state) {
        return state.is(ModTags.Blocks.PORTAL_FRAME);
    }

    private static boolean isFillable(BlockState state) {
        return state.isAir() || state.is(ModBlocks.SIFT_PORTAL.get()) || state.canBeReplaced() && state.getFluidState().isEmpty();
    }

    /** Search around {@code origin} for the nearest enclosed frame pocket. */
    public static @Nullable Frame find(Level level, BlockPos origin, int radius) {
        Frame best = null;
        double bestDist = Double.MAX_VALUE;
        Set<BlockPos> tried = new HashSet<>();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dy = -radius / 2; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    p.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (!isFrame(level.getBlockState(p))) continue;
                    BlockPos above = p.above();
                    if (tried.contains(above) || !isFillable(level.getBlockState(above))) continue;
                    for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
                        Set<BlockPos> interior = flood(level, above, axis);
                        if (interior != null) {
                            tried.addAll(interior);
                            BlockPos c = centerOf(interior);
                            double d = c.distSqr(origin);
                            if (d < bestDist) {
                                bestDist = d;
                                best = new Frame(axis, interior, c);
                            }
                        }
                    }
                }
            }
        }
        return best;
    }

    /** Flood fill in the plane of {@code axis}; returns null if the pocket leaks or is too big/small. */
    public static @Nullable Set<BlockPos> flood(Level level, BlockPos start, Direction.Axis axis) {
        Direction[] dirs = axis == Direction.Axis.X
                ? new Direction[] {Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN}
                : new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.UP, Direction.DOWN};
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start.immutable());
        seen.add(start.immutable());
        while (!queue.isEmpty()) {
            BlockPos cur = queue.poll();
            if (seen.size() > MAX_CELLS || Math.abs(cur.getX() - start.getX()) > MAX_SPAN || Math.abs(cur.getZ() - start.getZ()) > MAX_SPAN
                    || Math.abs(cur.getY() - start.getY()) > MAX_SPAN) {
                return null;
            }
            for (Direction d : dirs) {
                BlockPos n = cur.relative(d);
                if (seen.contains(n)) continue;
                BlockState s = level.getBlockState(n);
                if (isFrame(s)) continue;
                if (!isFillable(s)) return null;
                seen.add(n);
                queue.add(n);
            }
        }
        // A frame has to be at least 2 wide and 3 tall somewhere.
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (BlockPos b : seen) {
            minY = Math.min(minY, b.getY());
            maxY = Math.max(maxY, b.getY());
        }
        if (seen.size() < 6 || maxY - minY < 2) {
            return null;
        }
        return seen;
    }

    public static BlockPos centerOf(Set<BlockPos> cells) {
        long x = 0, y = 0, z = 0;
        for (BlockPos b : cells) {
            x += b.getX();
            y += b.getY();
            z += b.getZ();
        }
        int n = Math.max(1, cells.size());
        return new BlockPos((int) (x / n), (int) (y / n), (int) (z / n));
    }

    public static void fill(Level level, Frame frame) {
        BlockState portal = ModBlocks.SIFT_PORTAL.get().defaultBlockState().setValue(SiftPortalBlock.AXIS, frame.axis());
        for (BlockPos b : frame.interior()) {
            level.setBlock(b, portal, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    /** All connected portal blocks starting from {@code pos}. */
    public static List<BlockPos> connectedPortal(Level level, BlockPos pos) {
        List<BlockPos> out = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(pos.immutable());
        seen.add(pos.immutable());
        while (!queue.isEmpty() && out.size() < MAX_CELLS) {
            BlockPos cur = queue.poll();
            out.add(cur);
            for (Direction d : Direction.values()) {
                BlockPos n = cur.relative(d);
                if (!seen.contains(n) && level.getBlockState(n).is(ModBlocks.SIFT_PORTAL.get())) {
                    seen.add(n);
                    queue.add(n);
                }
            }
        }
        return out;
    }

    /** A stable identifier for a portal: its lowest, then smallest-x/z block. */
    public static BlockPos anchorOf(Level level, BlockPos pos) {
        BlockPos best = pos;
        for (BlockPos b : connectedPortal(level, pos)) {
            if (b.getY() < best.getY() || b.getY() == best.getY() && (b.getX() < best.getX() || b.getX() == best.getX() && b.getZ() < best.getZ())) {
                best = b;
            }
        }
        return best.immutable();
    }
}
