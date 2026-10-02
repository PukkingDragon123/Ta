package com.thesift.portal;

import com.thesift.block.SiftPortalBlock;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModTags;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Finds portal frames of any shape: a vertical, enclosed pocket of air in one plane whose border is
 * solid and mostly made of {@code #thesift:portal_frame} blocks. That recognises the great reinforced
 * deepslate gates at the heart of Ancient Cities (which stand on a deepslate floor) as well as
 * hand-built Sift Gate Frame portals.
 */
public final class PortalFrames {
    public static final int MAX_CELLS = 700;
    public static final int MAX_SPAN = 34;
    /** At least this many frame blocks must border the pocket... */
    public static final int MIN_FRAME_BLOCKS = 8;
    /** ...and they must make up at least this share of its border. */
    public static final double MIN_FRAME_SHARE = 0.6;

    private static final Direction.Axis[] AXES = {Direction.Axis.X, Direction.Axis.Z};

    public record Frame(Direction.Axis axis, Set<BlockPos> interior, BlockPos center) {}

    /** Result of flooding one pocket: the cells reached, whether it stayed enclosed, and how much of its border is frame. */
    public record Pocket(Set<BlockPos> cells, boolean enclosed, int frameBorder, int otherBorder) {
        public boolean valid() {
            int border = this.frameBorder + this.otherBorder;
            return this.enclosed && this.frameBorder >= MIN_FRAME_BLOCKS && this.frameBorder >= border * MIN_FRAME_SHARE;
        }
    }

    private PortalFrames() {}

    public static boolean isFrame(BlockState state) {
        return state.is(ModTags.Blocks.PORTAL_FRAME);
    }

    public static boolean isFillable(BlockState state) {
        return state.isAir() || state.is(ModBlocks.SIFT_PORTAL.get()) || state.canBeReplaced() && state.getFluidState().isEmpty();
    }

    public static Direction[] planeDirections(Direction.Axis axis) {
        return axis == Direction.Axis.X
                ? new Direction[] {Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN}
                : new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.UP, Direction.DOWN};
    }

    /**
     * Search around {@code origin} for the best enclosed frame pocket: the one with the most frame
     * blocks around it, then the nearest.
     */
    public static @Nullable Frame find(Level level, BlockPos origin, int radius) {
        Frame best = null;
        int bestFrames = 0;
        double bestDist = Double.MAX_VALUE;
        Set<BlockPos> triedX = new HashSet<>();
        Set<BlockPos> triedZ = new HashSet<>();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    p.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (!isFrame(level.getBlockState(p))) continue;
                    for (Direction.Axis axis : AXES) {
                        Set<BlockPos> tried = axis == Direction.Axis.X ? triedX : triedZ;
                        for (Direction d : planeDirections(axis)) {
                            BlockPos start = p.relative(d);
                            if (tried.contains(start) || !isFillable(level.getBlockState(start))) continue;
                            Pocket pocket = flood(level, start, axis);
                            tried.addAll(pocket.cells());
                            tried.add(start);
                            if (!pocket.valid()) continue;
                            BlockPos c = centerOf(pocket.cells());
                            double dist = c.distSqr(origin);
                            if (pocket.frameBorder() > bestFrames || pocket.frameBorder() == bestFrames && dist < bestDist) {
                                bestFrames = pocket.frameBorder();
                                bestDist = dist;
                                best = new Frame(axis, pocket.cells(), c);
                            }
                        }
                    }
                }
            }
        }
        return best;
    }

    /** Flood fill in the plane of {@code axis}, counting the frame and non-frame blocks around the pocket. */
    public static Pocket flood(Level level, BlockPos start, Direction.Axis axis) {
        Direction[] dirs = planeDirections(axis);
        Set<BlockPos> seen = new HashSet<>();
        Set<BlockPos> border = new HashSet<>();
        int frames = 0;
        int others = 0;
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start.immutable());
        seen.add(start.immutable());
        while (!queue.isEmpty()) {
            BlockPos cur = queue.poll();
            if (seen.size() > MAX_CELLS || Math.abs(cur.getX() - start.getX()) > MAX_SPAN || Math.abs(cur.getZ() - start.getZ()) > MAX_SPAN
                    || Math.abs(cur.getY() - start.getY()) > MAX_SPAN) {
                return new Pocket(seen, false, frames, others);
            }
            for (Direction d : dirs) {
                BlockPos n = cur.relative(d);
                if (seen.contains(n)) continue;
                BlockState s = level.getBlockState(n);
                if (isFillable(s)) {
                    seen.add(n);
                    queue.add(n);
                } else if (border.add(n)) {
                    if (isFrame(s)) {
                        frames++;
                    } else if (!s.getFluidState().isEmpty()) {
                        // water or Chrome leaking into the gap: not a portal
                        return new Pocket(seen, false, frames, others);
                    } else {
                        others++;
                    }
                }
            }
        }
        // A frame has to be at least 3 tall somewhere and hold a few cells.
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (BlockPos b : seen) {
            minY = Math.min(minY, b.getY());
            maxY = Math.max(maxY, b.getY());
        }
        boolean bigEnough = seen.size() >= 6 && maxY - minY >= 2;
        return new Pocket(seen, bigEnough, frames, others);
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
        fillCells(level, frame, frame.interior());
    }

    /**
     * Fills only some of the frame's cells - the gate's awakening closes it a ring at a time. The
     * shape updates are skipped on purpose: a half-filled gate would otherwise collapse into the
     * air that is still inside it.
     */
    public static void fillCells(Level level, Frame frame, Collection<BlockPos> cells) {
        BlockState portal = ModBlocks.SIFT_PORTAL.get().defaultBlockState().setValue(SiftPortalBlock.AXIS, frame.axis());
        for (BlockPos b : cells) {
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

    /**
     * Where an arriving traveller should stand: the middle of the portal's bottom row, so they come
     * out inside the membrane and never half inside the frame (an Ancient City gate's bottom row
     * can be a single cell wide at its corner, where the anchor sits).
     */
    public static Vec3 standingSpot(Level level, BlockPos pos) {
        List<BlockPos> cells = connectedPortal(level, pos);
        int minY = Integer.MAX_VALUE;
        for (BlockPos b : cells) {
            minY = Math.min(minY, b.getY());
        }
        List<BlockPos> bottom = new ArrayList<>();
        for (BlockPos b : cells) {
            if (b.getY() == minY) bottom.add(b);
        }
        if (bottom.isEmpty()) {
            return Vec3.atBottomCenterOf(pos);
        }
        bottom.sort((a, b) -> a.getX() != b.getX() ? Integer.compare(a.getX(), b.getX()) : Integer.compare(a.getZ(), b.getZ()));
        return Vec3.atBottomCenterOf(bottom.get(bottom.size() / 2));
    }

    /** True when every cell of the frame already holds portal: the gate is open. */
    public static boolean isOpen(Level level, Frame frame) {
        for (BlockPos b : frame.interior()) {
            if (!level.getBlockState(b).is(ModBlocks.SIFT_PORTAL.get())) return false;
        }
        return true;
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
