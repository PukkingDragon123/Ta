package com.thesift.world.sky;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.phys.Vec3;

/**
 * W-sky: the Sky Islands' trees.
 *
 * <ul>
 *   <li>{@code skypalm}: an alien tropical tree - a ringed, gently curving trunk on flared roots, a crown of long
 *       arching fronds, Skyrind bunches hanging under the crown, lianas from the frond tips - and vines slung from its
 *       crown to the Skypalms around it, like leads from tree to tree (you can swing on them).</li>
 *   <li>{@code cloud}: a Cloudpuff tree - a twisting Sky Root trunk and branches under flat-bottomed, cumulus-like puffs
 *       of white leaves, with the odd liana (and Driftfruit) hanging from the cloud.</li>
 * </ul>
 */
public record SkyTreeFeature(String style, BlockState trunk, BlockState leaves, BlockState vine, BlockState fruit, int minHeight, int maxHeight)
        implements Feature {
    public static final MapCodec<SkyTreeFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("style").forGetter(SkyTreeFeature::style),
            BlockState.CODEC.fieldOf("trunk").forGetter(SkyTreeFeature::trunk),
            BlockState.CODEC.fieldOf("leaves").forGetter(SkyTreeFeature::leaves),
            BlockState.CODEC.fieldOf("vine").forGetter(SkyTreeFeature::vine),
            BlockState.CODEC.fieldOf("fruit").forGetter(SkyTreeFeature::fruit),
            Codec.intRange(3, 32).fieldOf("min_height").forGetter(SkyTreeFeature::minHeight),
            Codec.intRange(3, 32).fieldOf("max_height").forGetter(SkyTreeFeature::maxHeight)
    ).apply(i, SkyTreeFeature::new));
    /** How far a Skypalm reaches out for a neighbour to tie a vine to. */
    private static final int LINK_MIN = 6;
    private static final int LINK_MAX = 17;

    @Override
    public MapCodec<SkyTreeFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        SkyGen g = new SkyGen(level, random, origin);
        int height = this.minHeight + random.nextInt(Math.max(1, this.maxHeight - this.minHeight + 1));
        if (origin.getY() + height + 6 >= level.getMaxY() || !level.getBlockState(origin.below()).is(BlockTags.DIRT)) {
            return false;
        }
        for (int y = 0; y < Math.min(height, 5); y++) {
            if (!g.free(origin.above(y))) {
                return false;
            }
        }
        Tree t = new Tree(g);
        if (this.style.equals("cloud")) {
            this.cloud(t, origin, height);
        } else {
            this.skypalm(t, origin, height);
        }
        if (t.logs.isEmpty()) {
            return false;
        }
        t.finish(this.leaves);
        if (this.style.equals("cloud")) {
            this.cloudVines(t);
        }
        g.connectVines();
        return true;
    }

    // ------------------------------------------------------------------ Skypalm

    private void skypalm(Tree t, BlockPos origin, int height) {
        SkyGen g = t.g;
        RandomSource r = g.random;
        Direction lean = Direction.Plane.HORIZONTAL.getRandomDirection(r);
        double bend = 1.0 + r.nextDouble() * 2.2;
        // flared roots
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (r.nextFloat() < 0.55F) {
                t.log(origin.relative(d), d.getAxis());
            }
        }
        // the trunk curves away from the lean side as it climbs
        int prev = 0;
        BlockPos top = origin;
        for (int y = 0; y < height; y++) {
            double f = (double) y / height;
            int off = (int) Math.round(bend * f * f);
            if (off != prev) {
                t.log(origin.relative(lean, prev).above(y), lean.getAxis());
                prev = off;
            }
            top = origin.relative(lean, off).above(y);
            t.log(top, Direction.Axis.Y);
        }
        // the crown: a tuft on top and long fronds arching out and drooping
        t.leaf(top.above());
        t.leaf(top.above(2));
        int fronds = 6 + r.nextInt(3);
        double turn = r.nextDouble() * Math.PI * 2.0;
        List<BlockPos> tips = new ArrayList<>();
        for (int i = 0; i < fronds; i++) {
            double a = turn + Math.PI * 2.0 * i / fronds + (r.nextDouble() - 0.5) * 0.5;
            int len = 4 + r.nextInt(3);
            BlockPos last = top.above();
            for (int s = 1; s <= len; s++) {
                double dy = 1.0 + 0.55 * s - 0.2 * s * s;
                BlockPos p = top.offset((int) Math.round(Math.cos(a) * s), (int) Math.round(dy), (int) Math.round(Math.sin(a) * s));
                for (BlockPos q : SkyGen.sagPath(Vec3.atCenterOf(last), Vec3.atCenterOf(p), 0.0)) {
                    t.leaf(q);
                }
                last = p;
            }
            tips.add(last);
        }
        // Skyrind bunches under the crown, by the trunk
        BlockState bunch = this.fruit;
        int bunches = 1 + r.nextInt(2);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (bunches <= 0) {
                break;
            }
            BlockPos at = top.relative(d);
            if (r.nextFloat() < 0.6F && g.free(at)) {
                t.leaf(at.above());
                if (bunch.hasProperty(SkyrindBunchBlock.AGE)) {
                    bunch = bunch.setValue(SkyrindBunchBlock.AGE, 1 + r.nextInt(3));
                }
                t.extras.put(at.immutable(), bunch);
                bunches--;
            }
        }
        BlockPos crown = top;
        t.after.add(() -> {
            // lianas from some frond tips
            for (BlockPos tip : tips) {
                if (r.nextFloat() < 0.4F) {
                    g.strand(tip.below(), 2 + r.nextInt(5), this.vine, null, 0.0F);
                }
            }
            this.linkNeighbours(g, crown);
        });
    }

    /** Vines slung from this crown to the other Skypalms round it, like leads from tree to tree. */
    private void linkNeighbours(SkyGen g, BlockPos top) {
        RandomSource r = g.random;
        List<BlockPos> found = new ArrayList<>();
        for (int dx = -LINK_MAX; dx <= LINK_MAX; dx++) {
            for (int dz = -LINK_MAX; dz <= LINK_MAX; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 < LINK_MIN * LINK_MIN || d2 > LINK_MAX * LINK_MAX) {
                    continue;
                }
                for (int dy = -5; dy <= 3; dy++) {
                    BlockPos p = top.offset(dx, dy, dz);
                    BlockState s = g.get(p);
                    if (s != null && s.is(this.trunk.getBlock()) && g.get(p.above()) != null && g.get(p.above()).is(BlockTags.LEAVES)) {
                        found.add(p.immutable());
                        break;
                    }
                }
            }
        }
        if (found.isEmpty()) {
            return;
        }
        found.sort((p1, p2) -> Double.compare(p1.distSqr(top), p2.distSqr(top)));
        int links = 0;
        List<Vec3> used = new ArrayList<>();
        for (BlockPos other : found) {
            if (links >= 2) {
                break;
            }
            Vec3 dir = Vec3.atCenterOf(other).subtract(Vec3.atCenterOf(top)).multiply(1.0, 0.0, 1.0).normalize();
            boolean same = false;
            for (Vec3 u : used) {
                if (u.dot(dir) > 0.7) {
                    same = true;
                    break;
                }
            }
            if (same) {
                continue;
            }
            // from just under this crown to just under that one, sagging like a slack lead
            Vec3 a = Vec3.atCenterOf(top.below(2)).add(dir);
            Vec3 b = Vec3.atCenterOf(other.below(2)).subtract(dir);
            double len = a.distanceTo(b);
            List<BlockPos> path = SkyGen.sagPath(a, b, len * (0.14 + r.nextDouble() * 0.1));
            if (path.size() < 3 || !g.clear(path)) {
                continue;
            }
            for (BlockPos p : path) {
                g.vine(p, this.vine);
            }
            used.add(dir);
            links++;
        }
    }

    // ------------------------------------------------------------------ Cloudpuff

    private void cloud(Tree t, BlockPos origin, int height) {
        RandomSource r = t.g.random;
        BlockPos.MutableBlockPos p = origin.mutable();
        Direction lean = Direction.Plane.HORIZONTAL.getRandomDirection(r);
        boolean kinked = false;
        for (int y = 0; y < height; y++) {
            t.log(p.immutable(), Direction.Axis.Y);
            if (!kinked && y > 1 && y < height - 2 && r.nextFloat() < 0.35F) {
                p.move(lean);
                t.log(p.immutable(), lean.getAxis());
                kinked = true;
            }
            p.move(Direction.UP);
        }
        BlockPos top = p.below().immutable();
        List<BlockPos> puffs = new ArrayList<>();
        puffs.add(top.above());
        int branches = 2 + r.nextInt(2);
        for (int i = 0; i < branches; i++) {
            double a = Math.PI * 2.0 * i / branches + r.nextDouble() * 0.9;
            int len = 2 + r.nextInt(2);
            BlockPos from = top.below(1 + r.nextInt(2));
            BlockPos end = from.offset((int) Math.round(Math.cos(a) * len), 1 + r.nextInt(2), (int) Math.round(Math.sin(a) * len));
            for (BlockPos q : SkyGen.sagPath(Vec3.atCenterOf(from), Vec3.atCenterOf(end), 0.0)) {
                t.log(q, Math.abs(end.getX() - from.getX()) >= Math.abs(end.getZ() - from.getZ()) ? Direction.Axis.X : Direction.Axis.Z);
            }
            puffs.add(end.above());
        }
        // cumulus puffs: rounded on top, nearly flat underneath
        for (BlockPos c : puffs) {
            float rx = 2.4F + r.nextFloat() * 1.4F;
            float ry = 1.7F + r.nextFloat() * 0.7F;
            t.puff(c, rx, ry, rx * (0.85F + r.nextFloat() * 0.3F));
        }
    }

    /** A few lianas from the cloud's flat underside, some with a Driftfruit. */
    private void cloudVines(Tree t) {
        SkyGen g = t.g;
        int n = 0;
        for (BlockPos leaf : t.leafSet) {
            if (n >= 3) {
                break;
            }
            if (g.random.nextFloat() < 0.06F && g.air(leaf.below()) && g.air(leaf.below(2)) && !t.logs.contains(leaf.below())) {
                if (g.strand(leaf.below(), 2 + g.random.nextInt(5), this.vine, this.fruit, 0.3F) > 0) {
                    n++;
                }
            }
        }
    }

    // ------------------------------------------------------------------ building

    private final class Tree {
        final SkyGen g;
        final Set<BlockPos> logs = new HashSet<>();
        final Set<BlockPos> leafSet = new HashSet<>();
        final Map<BlockPos, BlockState> extras = new HashMap<>();
        final List<Runnable> after = new ArrayList<>();

        Tree(SkyGen g) {
            this.g = g;
        }

        void log(BlockPos pos, Direction.Axis axis) {
            if (this.g.free(pos) || this.leafSet.contains(pos)) {
                this.g.log(pos, SkyTreeFeature.this.trunk, axis);
                this.logs.add(pos.immutable());
                this.leafSet.remove(pos);
            }
        }

        void leaf(BlockPos pos) {
            if (!this.logs.contains(pos) && this.g.free(pos)) {
                this.leafSet.add(pos.immutable());
            }
        }

        void puff(BlockPos c, float rx, float ry, float rz) {
            int ix = (int) Math.ceil(rx);
            int iy = (int) Math.ceil(ry);
            int iz = (int) Math.ceil(rz);
            for (int dx = -ix; dx <= ix; dx++) {
                for (int dy = -iy; dy <= iy; dy++) {
                    for (int dz = -iz; dz <= iz; dz++) {
                        double ey = dy < 0 ? dy / (ry * 0.45) : dy / ry;
                        double d = (dx * dx) / (rx * rx) + ey * ey + (dz * dz) / (rz * rz);
                        if (d <= 1.0 && (d < 0.75 || this.g.random.nextFloat() < 0.7F)) {
                            this.leaf(c.offset(dx, dy, dz));
                        }
                    }
                }
            }
        }

        /** Places the leaves with their decay distances (leaves out of a log's reach stay put), then the rest. */
        void finish(BlockState leafState) {
            Map<BlockPos, Integer> dist = new HashMap<>();
            ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            for (BlockPos log : this.logs) {
                dist.put(log, 0);
                queue.add(log);
            }
            while (!queue.isEmpty()) {
                BlockPos p = queue.poll();
                int d = dist.get(p);
                if (d >= 6) {
                    continue;
                }
                for (Direction dir : Direction.values()) {
                    BlockPos n = p.relative(dir);
                    if (this.leafSet.contains(n) && !dist.containsKey(n)) {
                        dist.put(n, d + 1);
                        queue.add(n);
                    }
                }
            }
            for (BlockPos p : this.leafSet) {
                BlockState s = leafState;
                Integer d = dist.get(p);
                if (s.hasProperty(LeavesBlock.DISTANCE)) {
                    s = s.setValue(LeavesBlock.DISTANCE, d == null ? 7 : Math.max(1, d));
                }
                if (d == null && s.hasProperty(LeavesBlock.PERSISTENT)) {
                    s = s.setValue(LeavesBlock.PERSISTENT, true);
                }
                this.g.set(p, s);
            }
            for (Map.Entry<BlockPos, BlockState> e : this.extras.entrySet()) {
                if (this.g.free(e.getKey())) {
                    this.g.set(e.getKey(), e.getValue());
                }
            }
            for (Runnable r : this.after) {
                r.run();
            }
        }
    }
}
