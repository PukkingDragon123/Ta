package com.thesift.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.HangingMossBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * W-land: the White Forest's White Lullwoods - tall, pale, weeping trees. A slender pearl trunk that bends a little,
 * branches curving up and out, each ending in a rounded cloud of white leaves whose rim weeps down in long fringes and
 * hanging strands. {@code weeping} is the common tree; {@code grand} is twice the size, on flared roots, with two tiers of
 * branches.
 */
public record PaleTreeFeature(BlockState trunk, BlockState leaves, Optional<BlockState> hanging, String style, int minHeight, int maxHeight)
        implements Feature {
    public static final MapCodec<PaleTreeFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockState.CODEC.fieldOf("trunk").forGetter(PaleTreeFeature::trunk),
            BlockState.CODEC.fieldOf("leaves").forGetter(PaleTreeFeature::leaves),
            BlockState.CODEC.optionalFieldOf("hanging").forGetter(PaleTreeFeature::hanging),
            Codec.STRING.fieldOf("style").forGetter(PaleTreeFeature::style),
            Codec.intRange(3, 32).fieldOf("min_height").forGetter(PaleTreeFeature::minHeight),
            Codec.intRange(3, 32).fieldOf("max_height").forGetter(PaleTreeFeature::maxHeight)
    ).apply(i, PaleTreeFeature::new));

    @Override
    public MapCodec<PaleTreeFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int height = this.minHeight + random.nextInt(Math.max(1, this.maxHeight - this.minHeight + 1));
        if (origin.getY() + height + 6 >= level.getMaxY() || origin.getY() <= level.getMinY() + 1) {
            return false;
        }
        for (int y = 0; y < Math.min(height, 5); y++) {
            if (!canReplace(level, origin.above(y))) {
                return false;
            }
        }
        boolean grand = this.style.equals("grand");
        Tree t = new Tree(level, random);
        if (grand) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                int len = 1 + random.nextInt(2);
                for (int y = 0; y < len; y++) {
                    t.log(origin.relative(d).above(y), Direction.Axis.Y);
                }
            }
        }
        // the trunk, drifting a block sideways once on its way up
        BlockPos.MutableBlockPos p = origin.mutable();
        Direction lean = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        boolean leaned = false;
        BlockPos[] column = new BlockPos[height];
        for (int y = 0; y < height; y++) {
            t.log(p.immutable(), Direction.Axis.Y);
            column[y] = p.immutable();
            if (!leaned && y > height / 2 && y < height - 2 && random.nextFloat() < 0.5F) {
                p.move(lean);
                t.log(p.immutable(), lean.getAxis());
                leaned = true;
            }
            p.move(Direction.UP);
        }
        BlockPos top = column[height - 1];
        float crown = grand ? 3.4F + random.nextFloat() : 2.4F + random.nextFloat() * 0.8F;
        t.cloud(top.above(1), crown, crown * 0.62F, crown);
        // branches curving up and out, each with its own cloud
        int branches = grand ? 6 + random.nextInt(3) : 3 + random.nextInt(3);
        double turn = random.nextDouble() * Math.PI * 2;
        for (int i = 0; i < branches; i++) {
            double a = turn + Math.PI * 2 * i / branches + random.nextDouble() * 0.5;
            float frac = grand ? (i % 2 == 0 ? 0.5F : 0.72F) : 0.55F + random.nextFloat() * 0.25F;
            BlockPos start = column[Math.min(height - 2, (int) (height * frac))];
            int out = grand ? 3 + random.nextInt(3) : 2 + random.nextInt(2);
            BlockPos mid = start.offset((int) Math.round(Math.cos(a) * out * 0.5), 1, (int) Math.round(Math.sin(a) * out * 0.5));
            BlockPos end = start.offset((int) Math.round(Math.cos(a) * out), 2 + random.nextInt(2), (int) Math.round(Math.sin(a) * out));
            t.branch(start, mid);
            t.branch(mid, end);
            float r = grand ? 2.4F + random.nextFloat() * 0.9F : 1.8F + random.nextFloat() * 0.7F;
            t.cloud(end.above(), r, r * 0.6F, r);
        }
        if (t.logs.isEmpty()) {
            return false;
        }
        t.weep(grand ? 0.55F : 0.45F, grand ? 4 : 3);
        t.finish(this.leaves);
        if (this.hanging.isPresent()) {
            t.strands(this.hanging.get(), grand ? 0.22F : 0.16F, grand ? 5 : 3);
        }
        return true;
    }

    private static boolean canReplace(WorldGenLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return s.isAir() || s.canBeReplaced() || s.is(BlockTags.LEAVES) || s.is(BlockTags.REPLACEABLE_BY_TREES);
    }

    private final class Tree {
        final WorldGenLevel level;
        final RandomSource random;
        final Set<BlockPos> logs = new HashSet<>();
        final Set<BlockPos> leafSet = new HashSet<>();

        Tree(WorldGenLevel level, RandomSource random) {
            this.level = level;
            this.random = random;
        }

        void log(BlockPos pos, Direction.Axis axis) {
            if (canReplace(this.level, pos) || this.leafSet.contains(pos)) {
                BlockState s = PaleTreeFeature.this.trunk;
                if (s.hasProperty(RotatedPillarBlock.AXIS)) {
                    s = s.setValue(RotatedPillarBlock.AXIS, axis);
                }
                this.level.setBlock(pos, s, 19);
                this.logs.add(pos);
                this.leafSet.remove(pos);
            }
        }

        void leaf(BlockPos pos) {
            if (!this.logs.contains(pos) && canReplace(this.level, pos)) {
                this.leafSet.add(pos);
            }
        }

        void branch(BlockPos from, BlockPos to) {
            int dx = to.getX() - from.getX();
            int dy = to.getY() - from.getY();
            int dz = to.getZ() - from.getZ();
            int steps = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
            Direction.Axis axis = Math.abs(dy) >= Math.max(Math.abs(dx), Math.abs(dz)) ? Direction.Axis.Y
                    : (Math.abs(dx) >= Math.abs(dz) ? Direction.Axis.X : Direction.Axis.Z);
            for (int i = 1; i <= steps; i++) {
                float f = (float) i / steps;
                this.log(from.offset(Math.round(dx * f), Math.round(dy * f), Math.round(dz * f)), axis);
            }
        }

        /** A rounded, slightly ragged cloud of leaves. */
        void cloud(BlockPos c, float rx, float ry, float rz) {
            int ix = (int) Math.ceil(rx);
            int iy = (int) Math.ceil(ry);
            int iz = (int) Math.ceil(rz);
            for (int dx = -ix; dx <= ix; dx++) {
                for (int dy = -iy; dy <= iy; dy++) {
                    for (int dz = -iz; dz <= iz; dz++) {
                        double d = (dx * dx) / (rx * rx) + (dy * dy) / (ry * ry) + (dz * dz) / (rz * rz);
                        if (d <= 1.0 && (d < 0.72 || this.random.nextFloat() < 0.7F)) {
                            this.leaf(c.offset(dx, dy, dz));
                        }
                    }
                }
            }
        }

        /** The weeping fringe: leaves trailing down from the clouds' lower rims. */
        void weep(float chance, int maxLen) {
            for (BlockPos p : new HashSet<>(this.leafSet)) {
                if (this.leafSet.contains(p.below()) || this.logs.contains(p.below()) || this.random.nextFloat() >= chance) {
                    continue;
                }
                int len = 1 + this.random.nextInt(maxLen);
                for (int i = 1; i <= len; i++) {
                    BlockPos q = p.below(i);
                    if (!canReplace(this.level, q) || this.logs.contains(q)) {
                        break;
                    }
                    this.leaf(q);
                }
            }
        }

        /** Places the leaves with their decay distances (BFS from the logs; out-of-reach ones are made persistent). */
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
                this.level.setBlock(p, s, 19);
            }
        }

        /** Hanging strands under the fringe. */
        void strands(BlockState hangState, float chance, int maxLen) {
            for (BlockPos p : this.leafSet) {
                BlockPos below = p.below();
                if (this.leafSet.contains(below) || this.logs.contains(below) || this.random.nextFloat() >= chance) {
                    continue;
                }
                int len = 1 + this.random.nextInt(maxLen);
                for (int i = 0; i < len; i++) {
                    BlockPos h = p.below(i + 1);
                    if (!this.level.getBlockState(h).isAir()) {
                        break;
                    }
                    boolean tip = i == len - 1 || !this.level.getBlockState(h.below()).isAir();
                    BlockState s = hangState;
                    if (s.hasProperty(HangingMossBlock.TIP)) {
                        s = s.setValue(HangingMossBlock.TIP, tip);
                    }
                    this.level.setBlock(h, s, 2);
                    if (tip) {
                        break;
                    }
                }
            }
        }
    }
}
