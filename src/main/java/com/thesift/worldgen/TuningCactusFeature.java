package com.thesift.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thesift.block.RattlethornBlock;
import com.thesift.block.TuningCactusBlock;
import com.thesift.block.TuningCactusBudBlock;
import com.thesift.registry.ModBlocks;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import org.jspecify.annotations.Nullable;

/**
 * W-land: a grown Tuning Cactus. It follows the chorus rules (a stem that splits does not go on upwards; a side shoot is
 * one block out, then climbs), in one of four shapes:
 * <ul>
 *   <li>fork - a short trunk splitting into two equal prongs: a giant tuning fork;</li>
 *   <li>candelabra - a taller trunk splitting into two to four arms;</li>
 *   <li>zigzag - one stalk stepping sideways as it climbs;</li>
 *   <li>chorus - free branching, like a chorus plant.</li>
 * </ul>
 * Every tip opens into a crown, about half of them bearing Tuning Fruit. The shape is planned first and only placed
 * when every block of it is free, then each part is joined to its neighbours.
 */
public record TuningCactusFeature(int maxSpread) implements Feature {
    public static final MapCodec<TuningCactusFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(1, 7).fieldOf("max_spread").forGetter(TuningCactusFeature::maxSpread)
    ).apply(i, TuningCactusFeature::new));

    @Override
    public MapCodec<TuningCactusFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (!RattlethornBlock.desertGround(level.getBlockState(origin.below())) || !level.isEmptyBlock(origin)) {
            return false;
        }
        Plan plan = new Plan(origin);
        switch (random.nextInt(4)) {
            case 0 -> this.fork(plan, random);
            case 1 -> this.candelabra(plan, random);
            case 2 -> this.zigzag(plan, random);
            default -> this.chorus(plan, random, origin, 0);
        }
        for (BlockPos p : plan.stems) {
            if (!level.isEmptyBlock(p) && !p.equals(origin)) {
                return false;
            }
        }
        for (BlockPos p : plan.tips.keySet()) {
            if (!level.isEmptyBlock(p) || p.getY() >= level.getMaxY() - 1) {
                return false;
            }
        }
        // a side shoot hangs over open air (the chorus rule), or it would not stay up
        for (BlockPos p : plan.elbows) {
            if (!level.isEmptyBlock(p.below()) || plan.stems.contains(p.below())) {
                return false;
            }
        }
        BlockState stem = ModBlocks.TUNING_CACTUS.get().defaultBlockState();
        BlockState bud = ModBlocks.TUNING_CACTUS_BUD.get().defaultBlockState().setValue(TuningCactusBudBlock.AGE, 5);
        for (BlockPos p : plan.stems) {
            level.setBlock(p, stem, 2);
        }
        for (Map.Entry<BlockPos, Boolean> e : plan.tips.entrySet()) {
            level.setBlock(e.getKey(), bud.setValue(TuningCactusBudBlock.FRUIT, e.getValue()), 2);
        }
        // join every part up to its neighbours now that the whole cactus stands
        for (BlockPos p : plan.stems) {
            level.setBlock(p, TuningCactusBlock.connected(level, p, level.getBlockState(p)), 2);
        }
        for (BlockPos p : plan.tips.keySet()) {
            level.setBlock(p, TuningCactusBlock.connected(level, p, level.getBlockState(p)), 2);
        }
        return true;
    }

    /** Climbs `n` blocks above `from` (which is already a stem); returns the top. */
    private BlockPos climb(Plan plan, BlockPos from, int n) {
        BlockPos p = from;
        for (int i = 0; i < n; i++) {
            p = p.above();
            plan.stems.add(p);
        }
        return p;
    }

    private void tip(Plan plan, BlockPos top, RandomSource random) {
        plan.stems.remove(top.above());
        plan.tips.put(top.above(), random.nextBoolean());
    }

    private void fork(Plan plan, RandomSource random) {
        BlockPos node = this.climb(plan, plan.origin, 1 + random.nextInt(3));
        Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        int prong = 2 + random.nextInt(3);
        for (Direction side : new Direction[]{d, d.getOpposite()}) {
            this.tip(plan, this.climb(plan, plan.elbow(node.relative(side)), prong), random);
        }
    }

    private void candelabra(Plan plan, RandomSource random) {
        BlockPos node = this.climb(plan, plan.origin, 2 + random.nextInt(3));
        Direction[] sides = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        for (int i = 3; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Direction t = sides[i];
            sides[i] = sides[j];
            sides[j] = t;
        }
        int arms = 2 + random.nextInt(3);
        for (int i = 0; i < arms; i++) {
            this.tip(plan, this.climb(plan, plan.elbow(node.relative(sides[i])), 1 + random.nextInt(4)), random);
        }
    }

    private void zigzag(Plan plan, RandomSource random) {
        BlockPos top = this.climb(plan, plan.origin, 1 + random.nextInt(2));
        Direction last = null;
        int steps = 2 + random.nextInt(3);
        for (int i = 0; i < steps; i++) {
            Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            if (d == last || Math.abs(top.relative(d).getX() - plan.origin.getX()) > this.maxSpread
                    || Math.abs(top.relative(d).getZ() - plan.origin.getZ()) > this.maxSpread) {
                d = d.getOpposite();
            }
            top = this.climb(plan, plan.elbow(top.relative(d)), 1 + random.nextInt(3));
            last = d.getOpposite();
        }
        this.tip(plan, top, random);
    }

    /** Free chorus-like branching (vanilla's growTreeRecursive, planned instead of placed). */
    private void chorus(Plan plan, RandomSource random, BlockPos from, int depth) {
        BlockPos top = this.climb(plan, from, random.nextInt(3) + (depth == 0 ? 2 : 1));
        boolean branched = false;
        if (depth < 3) {
            int shoots = random.nextInt(3) + (depth == 0 ? 1 : 0);
            for (int i = 0; i < shoots; i++) {
                Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                BlockPos elbow = top.relative(d);
                if (Math.abs(elbow.getX() - plan.origin.getX()) < this.maxSpread && Math.abs(elbow.getZ() - plan.origin.getZ()) < this.maxSpread
                        && plan.free(elbow, d.getOpposite()) && !plan.stems.contains(elbow.below())) {
                    this.chorus(plan, random, plan.elbow(elbow), depth + 1);
                    branched = true;
                }
            }
        }
        if (!branched) {
            this.tip(plan, top, random);
        }
    }

    private static final class Plan {
        final BlockPos origin;
        final Set<BlockPos> stems = new HashSet<>();
        final Map<BlockPos, Boolean> tips = new HashMap<>();
        final Set<BlockPos> elbows = new HashSet<>();

        Plan(BlockPos origin) {
            this.origin = origin;
            this.stems.add(origin);
        }

        BlockPos elbow(BlockPos p) {
            this.stems.add(p);
            this.elbows.add(p);
            return p;
        }

        /** Nothing planned at `p` or beside it (except the way it came from). */
        boolean free(BlockPos p, @Nullable Direction from) {
            if (this.stems.contains(p) || this.tips.containsKey(p)) {
                return false;
            }
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = p.relative(d);
                if (d != from && (this.stems.contains(n) || this.tips.containsKey(n))) {
                    return false;
                }
            }
            return true;
        }
    }
}
