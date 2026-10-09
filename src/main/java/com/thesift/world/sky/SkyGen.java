package com.thesift.world.sky;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * W-sky: drawing helpers shared by the Sky Islands' features - everything stays inside the chunks a feature may touch
 * (the chunk being decorated and its eight neighbours), vines are drawn as face-connected paths so their arms join up,
 * and every vine laid gets its connections worked out once the drawing is done.
 */
final class SkyGen {
    /** Block flags for worldgen writes: tell clients, keep the shapes we set (no neighbour updates). */
    static final int FLAGS = 2 | 16;
    final WorldGenLevel level;
    final RandomSource random;
    private final int minX;
    private final int maxX;
    private final int minZ;
    private final int maxZ;
    final List<BlockPos> vines = new ArrayList<>();

    SkyGen(WorldGenLevel level, RandomSource random, BlockPos origin) {
        this.level = level;
        this.random = random;
        int cx = origin.getX() >> 4;
        int cz = origin.getZ() >> 4;
        if (level instanceof WorldGenRegion region) {
            ChunkPos c = region.getCenter();
            cx = c.x();
            cz = c.z();
        }
        this.minX = (cx - 1) << 4;
        this.maxX = ((cx + 1) << 4) + 15;
        this.minZ = (cz - 1) << 4;
        this.maxZ = ((cz + 1) << 4) + 15;
    }

    boolean inside(BlockPos p) {
        return p.getX() >= this.minX && p.getX() <= this.maxX && p.getZ() >= this.minZ && p.getZ() <= this.maxZ && p.getY() > this.level.getMinY()
                && p.getY() < this.level.getMaxY() - 1;
    }

    /** The block at p, or null outside the chunks we may touch. */
    @Nullable BlockState get(BlockPos p) {
        return this.inside(p) ? this.level.getBlockState(p) : null;
    }

    boolean air(BlockPos p) {
        BlockState s = this.get(p);
        return s != null && s.isAir();
    }

    /** Air, or something soft a vine or branch may push aside. */
    boolean free(BlockPos p) {
        BlockState s = this.get(p);
        return s != null && (s.isAir() || s.canBeReplaced() || s.is(BlockTags.REPLACEABLE_BY_TREES));
    }

    /** Something a span can anchor into: solid island rock and soil, roots, logs. */
    boolean solid(BlockPos p) {
        BlockState s = this.get(p);
        return s != null && !s.isAir() && !s.canBeReplaced() && (s.isSolid() || s.is(BlockTags.LOGS));
    }

    void set(BlockPos p, BlockState s) {
        if (this.inside(p)) {
            this.level.setBlock(p, s, FLAGS);
        }
    }

    /** A vine block (its arms are set by {@link #connectVines}). */
    void vine(BlockPos p, BlockState vine) {
        if (this.free(p)) {
            this.set(p, vine);
            this.vines.add(p.immutable());
        }
    }

    /** A log or root along an axis. */
    void log(BlockPos p, BlockState log, Direction.Axis axis) {
        this.set(p, log.hasProperty(RotatedPillarBlock.AXIS) ? log.setValue(RotatedPillarBlock.AXIS, axis) : log);
    }

    /** Every vine laid gets an arm towards each neighbour it holds on to. */
    void connectVines() {
        for (BlockPos p : this.vines) {
            BlockState s = this.level.getBlockState(p);
            if (!(s.getBlock() instanceof SkyVineBlock)) {
                continue;
            }
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                BlockState ns = this.get(n);
                boolean on = ns != null && SkyVineBlock.holdsOn(this.level, n, ns, d);
                s = s.setValue(SkyVineBlock.PROPERTY_BY_DIRECTION.get(d), on);
            }
            this.level.setBlock(p, s, FLAGS);
        }
    }

    /**
     * The blocks of a sagging line from a to b (a rope hangs in a curve: lowest in the middle, by {@code sag} blocks),
     * face-connected so every block touches the next.
     */
    static List<BlockPos> sagPath(Vec3 a, Vec3 b, double sag) {
        List<BlockPos> path = new ArrayList<>();
        double len = a.distanceTo(b);
        int steps = Math.max(2, (int) Math.ceil(len * 3.0));
        BlockPos last = null;
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            Vec3 p = a.lerp(b, t).add(0.0, -sag * 4.0 * t * (1.0 - t), 0.0);
            BlockPos bp = BlockPos.containing(p);
            if (last == null) {
                path.add(bp);
                last = bp;
                continue;
            }
            // walk one axis at a time from the last block to this one: no diagonal gaps
            while (!last.equals(bp)) {
                if (last.getX() != bp.getX()) {
                    last = last.offset(Integer.signum(bp.getX() - last.getX()), 0, 0);
                } else if (last.getZ() != bp.getZ()) {
                    last = last.offset(0, 0, Integer.signum(bp.getZ() - last.getZ()));
                } else {
                    last = last.offset(0, Integer.signum(bp.getY() - last.getY()), 0);
                }
                path.add(last);
            }
        }
        return path;
    }

    /** True when every block of the path is free (the ends may touch what they hang from). */
    boolean clear(List<BlockPos> path) {
        for (BlockPos p : path) {
            if (!this.free(p)) {
                return false;
            }
        }
        return true;
    }

    /**
     * A strand of vine hanging down from just under {@code top} (which must hold it), up to {@code length} long, ending
     * in {@code fruit} with the given chance. Returns how long it came out.
     */
    int strand(BlockPos top, int length, BlockState vine, @Nullable BlockState fruit, float fruitChance) {
        int n = 0;
        BlockPos p = top;
        while (n < length && this.air(p)) {
            this.vine(p, vine);
            p = p.below();
            n++;
        }
        if (n >= 3 && fruit != null && this.random.nextFloat() < fruitChance && this.air(p) && this.air(p.below())) {
            this.set(p, fruit);
        }
        return n;
    }
}
