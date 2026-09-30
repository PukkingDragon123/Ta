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
 * Hand-rolled Sift trees. Unlike vanilla foliage placers these build soft, cloud-like canopies out of
 * overlapping squashed spheres, with curving trunks, branches and hanging leaf curtains - the
 * fluffy look of the Sift's forests.
 *
 * <p>Styles: {@code puff} (Lullwood), {@code grand} (2x2 giant Lullwood), {@code wish} (drooping
 * Wishwood) and {@code tall_wish} (spire-like Wishwood).
 */
public record SiftTreeFeature(BlockState trunk, BlockState leaves, Optional<BlockState> hanging, String style, int minHeight, int maxHeight)
        implements Feature {
    public static final MapCodec<SiftTreeFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockState.CODEC.fieldOf("trunk").forGetter(SiftTreeFeature::trunk),
            BlockState.CODEC.fieldOf("leaves").forGetter(SiftTreeFeature::leaves),
            BlockState.CODEC.optionalFieldOf("hanging").forGetter(SiftTreeFeature::hanging),
            Codec.STRING.fieldOf("style").forGetter(SiftTreeFeature::style),
            Codec.intRange(3, 40).fieldOf("min_height").forGetter(SiftTreeFeature::minHeight),
            Codec.intRange(3, 40).fieldOf("max_height").forGetter(SiftTreeFeature::maxHeight)
    ).apply(i, SiftTreeFeature::new));

    @Override
    public MapCodec<SiftTreeFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int height = this.minHeight + random.nextInt(Math.max(1, this.maxHeight - this.minHeight + 1));
        if (origin.getY() + height + 8 >= level.getMaxY() || origin.getY() <= level.getMinY() + 1) {
            return false;
        }
        boolean wide = this.style.equals("grand");
        // Need open space for the trunk.
        for (int y = 0; y < Math.min(height, 6); y++) {
            if (!canReplace(level, origin.above(y))) {
                return false;
            }
        }
        Builder b = new Builder(level, random);
        switch (this.style) {
            case "grand" -> this.grand(b, origin, height);
            case "wish" -> this.wish(b, origin, height, false);
            case "tall_wish" -> this.wish(b, origin, height, true);
            default -> this.puff(b, origin, height);
        }
        if (b.logs.isEmpty()) {
            return false;
        }
        b.finish(this.leaves);
        if (this.hanging.isPresent()) {
            b.hangCurtains(this.hanging.get(), wide ? 0.22F : 0.14F, wide ? 4 : 3);
        }
        return true;
    }

    // ------------------------------------------------------------------ styles

    private void puff(Builder b, BlockPos origin, int height) {
        RandomSource r = b.random;
        BlockPos top = this.trunkColumn(b, origin, height, 0.35F);
        float main = 2.6F + r.nextFloat() * 0.9F;
        b.blob(top.above(1), main, main * 0.72F, main);
        int puffs = 2 + r.nextInt(3);
        for (int i = 0; i < puffs; i++) {
            double a = (Math.PI * 2 * i) / puffs + r.nextDouble() * 0.8;
            int dist = 2 + r.nextInt(2);
            int dy = -1 - r.nextInt(3);
            BlockPos c = top.offset((int) Math.round(Math.cos(a) * dist), dy, (int) Math.round(Math.sin(a) * dist));
            this.branch(b, top.below(1 - dy > 3 ? 2 : 1), c);
            float rad = 1.8F + r.nextFloat() * 0.9F;
            b.blob(c, rad, rad * 0.7F, rad);
        }
    }

    private void grand(Builder b, BlockPos origin, int height) {
        RandomSource r = b.random;
        BlockPos top = origin;
        for (int dx = 0; dx < 2; dx++) {
            for (int dz = 0; dz < 2; dz++) {
                BlockPos t = this.trunkColumn(b, origin.offset(dx, 0, dz), height, 0.0F);
                if (dx == 0 && dz == 0) {
                    top = t;
                }
            }
        }
        // Buttress roots.
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos root = origin.offset(d.getStepX() == 1 ? 2 : d.getStepX(), 0, d.getStepZ() == 1 ? 2 : d.getStepZ());
            int len = 1 + r.nextInt(3);
            for (int y = 0; y < len; y++) {
                b.log(root.above(y), Direction.Axis.Y);
            }
        }
        BlockPos crown = top.offset(0, 1, 0);
        b.blob(crown.offset(1, 1, 1), 4.2F, 3.0F, 4.2F);
        int puffs = 4 + r.nextInt(3);
        for (int i = 0; i < puffs; i++) {
            double a = (Math.PI * 2 * i) / puffs + r.nextDouble() * 0.6;
            int dist = 4 + r.nextInt(2);
            int dy = -2 - r.nextInt(4);
            BlockPos c = crown.offset((int) Math.round(Math.cos(a) * dist), dy, (int) Math.round(Math.sin(a) * dist));
            this.branch(b, crown.below(3 + r.nextInt(3)), c);
            float rad = 2.4F + r.nextFloat() * 1.2F;
            b.blob(c, rad, rad * 0.7F, rad);
        }
    }

    private void wish(Builder b, BlockPos origin, int height, boolean tall) {
        RandomSource r = b.random;
        BlockPos top = this.trunkColumn(b, origin, height, tall ? 0.15F : 0.5F);
        if (tall) {
            // Stacked, shrinking tiers like a soft spire.
            float rad = 2.8F;
            BlockPos c = top.below(height / 2);
            while (c.getY() <= top.getY() + 1) {
                b.blob(c, rad, 1.3F, rad);
                c = c.above(2);
                rad = Math.max(1.0F, rad - 0.45F);
            }
            b.blob(top.above(2), 1.1F, 1.6F, 1.1F);
        } else {
            // A dome that droops into curtains.
            float rad = 3.0F + r.nextFloat();
            b.blob(top, rad, rad * 0.6F, rad);
            for (int i = 0; i < 3; i++) {
                double a = r.nextDouble() * Math.PI * 2;
                BlockPos c = top.offset((int) Math.round(Math.cos(a) * 2.5), -1, (int) Math.round(Math.sin(a) * 2.5));
                this.branch(b, top.below(2), c);
                b.blob(c, 1.8F, 1.2F, 1.8F);
            }
            // Weeping fringe of leaves around the dome rim.
            int ri = (int) rad;
            for (int dx = -ri; dx <= ri; dx++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d > rad - 1.2 && d <= rad + 0.2 && r.nextFloat() < 0.7F) {
                        int len = 1 + r.nextInt(3);
                        for (int y = 0; y < len; y++) {
                            b.leaf(top.offset(dx, -1 - y, dz));
                        }
                    }
                }
            }
        }
    }

    /** A trunk that may drift sideways near the top. Returns the top log position. */
    private BlockPos trunkColumn(Builder b, BlockPos base, int height, float leanChance) {
        BlockPos.MutableBlockPos p = base.mutable();
        boolean leaned = false;
        for (int y = 0; y < height; y++) {
            b.log(p.immutable(), Direction.Axis.Y);
            if (!leaned && y > height * 0.55 && y < height - 1 && b.random.nextFloat() < leanChance) {
                Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(b.random);
                p.move(d);
                b.log(p.immutable(), d.getAxis());
                leaned = true;
            }
            p.move(Direction.UP);
        }
        return p.below().immutable();
    }

    private void branch(Builder b, BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        int dz = to.getZ() - from.getZ();
        int steps = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        Direction.Axis axis = Math.abs(dx) >= Math.abs(dz) ? Direction.Axis.X : Direction.Axis.Z;
        if (Math.abs(dy) > Math.max(Math.abs(dx), Math.abs(dz))) {
            axis = Direction.Axis.Y;
        }
        for (int i = 1; i <= steps; i++) {
            float t = (float) i / steps;
            b.log(from.offset(Math.round(dx * t), Math.round(dy * t), Math.round(dz * t)), axis);
        }
    }

    private static boolean canReplace(WorldGenLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return s.isAir() || s.canBeReplaced() || s.is(BlockTags.LEAVES) || s.is(BlockTags.REPLACEABLE_BY_TREES);
    }

    // ------------------------------------------------------------------ builder

    private final class Builder {
        final WorldGenLevel level;
        final RandomSource random;
        final Set<BlockPos> logs = new HashSet<>();
        final Set<BlockPos> leafSet = new HashSet<>();

        Builder(WorldGenLevel level, RandomSource random) {
            this.level = level;
            this.random = random;
        }

        void log(BlockPos pos, Direction.Axis axis) {
            if (canReplace(this.level, pos) || this.leafSet.contains(pos)) {
                BlockState s = SiftTreeFeature.this.trunk;
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

        /** A squashed, slightly ragged sphere of leaves. */
        void blob(BlockPos c, float rx, float ry, float rz) {
            int ix = (int) Math.ceil(rx);
            int iy = (int) Math.ceil(ry);
            int iz = (int) Math.ceil(rz);
            for (int dx = -ix; dx <= ix; dx++) {
                for (int dy = -iy; dy <= iy; dy++) {
                    for (int dz = -iz; dz <= iz; dz++) {
                        double d = (dx * dx) / (rx * rx) + (dy * dy) / (ry * ry) + (dz * dz) / (rz * rz);
                        if (d <= 1.0 && (d < 0.7 || this.random.nextFloat() < 0.75F)) {
                            this.leaf(c.offset(dx, dy, dz));
                        }
                    }
                }
            }
        }

        /** Places leaves with correct decay distances (BFS from logs). */
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

        /** Hanging leaf strands under the canopy. */
        void hangCurtains(BlockState hangState, float chance, int maxLen) {
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
