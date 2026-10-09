package com.thesift.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thesift.block.SeaLogging;
import com.thesift.block.TrumpetCoralBellBlock;
import com.thesift.block.TrumpetCoralPipeBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * W-sea: a grown Trumpet Coral - a branching brass instrument rooted on the sea floor (in water, or in Chrome). One of four
 * forms, each in one metal with now and then a branch of another:
 * <ul>
 *   <li>an organ rank: a row of coral blocks with upright tubes of rising and falling height, a bell on each;</li>
 *   <li>a horn: a trunk that forks into branches that bend out and up, ending in bells that open up or outward;</li>
 *   <li>a trumpet: a trunk, a long level lead pipe with valve stubs, and a big bell opening sideways;</li>
 *   <li>a bush: a short trunk spraying tubes in every direction, like a coral tree made of brass.</li>
 * </ul>
 * {@code blocks}, {@code pipes} and {@code bells} line up by metal.
 */
public record TrumpetCoralFeature(List<BlockState> blocks, List<BlockState> pipes, List<BlockState> bells) implements Feature {
    public static final MapCodec<TrumpetCoralFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockState.CODEC.listOf().fieldOf("blocks").forGetter(TrumpetCoralFeature::blocks),
            BlockState.CODEC.listOf().fieldOf("pipes").forGetter(TrumpetCoralFeature::pipes),
            BlockState.CODEC.listOf().fieldOf("bells").forGetter(TrumpetCoralFeature::bells)
    ).apply(i, TrumpetCoralFeature::new));

    /** How often each metal grows (brass, silver, copper, verdigris). */
    private static final int[] WEIGHTS = {4, 2, 3, 3};

    @Override
    public MapCodec<TrumpetCoralFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int n = Math.min(this.blocks.size(), Math.min(this.pipes.size(), this.bells.size()));
        if (n == 0 || !SeaLogging.openSea(level, origin) || !level.getBlockState(origin.below()).isFaceSturdy(level, origin.below(), Direction.UP)) {
            return false;
        }
        Grower g = new Grower(level, random, this.pickMetal(random, n), n);
        switch (random.nextInt(4)) {
            case 0 -> g.organ(origin);
            case 1 -> g.horn(origin);
            case 2 -> g.trumpet(origin);
            default -> g.bush(origin);
        }
        g.finish();
        return g.placed > 0;
    }

    private int pickMetal(RandomSource random, int n) {
        int total = 0;
        for (int i = 0; i < n; i++) {
            total += i < WEIGHTS.length ? WEIGHTS[i] : 1;
        }
        int k = random.nextInt(total);
        for (int i = 0; i < n; i++) {
            k -= i < WEIGHTS.length ? WEIGHTS[i] : 1;
            if (k < 0) {
                return i;
            }
        }
        return 0;
    }

    /** Lays one coral down, block by block. */
    private final class Grower {
        final WorldGenLevel level;
        final RandomSource random;
        final int metal;
        final int metals;
        final List<BlockPos> tubes = new ArrayList<>();
        int placed;

        Grower(WorldGenLevel level, RandomSource random, int metal, int metals) {
            this.level = level;
            this.random = random;
            this.metal = metal;
            this.metals = metals;
        }

        /** Mostly the coral's own metal, now and then a branch of another. */
        int branchMetal() {
            return this.random.nextInt(5) == 0 ? this.random.nextInt(this.metals) : this.metal;
        }

        boolean open(BlockPos p) {
            return SeaLogging.openSea(this.level, p);
        }

        boolean tube(BlockPos p, int m) {
            if (!this.open(p)) {
                return false;
            }
            this.level.setBlock(p, SeaLogging.inFluidAt(TrumpetCoralFeature.this.pipes.get(m), this.level, p), Block.UPDATE_CLIENTS);
            this.tubes.add(p.immutable());
            this.placed++;
            return true;
        }

        void block(BlockPos p, int m) {
            if (this.open(p)) {
                this.level.setBlock(p, TrumpetCoralFeature.this.blocks.get(m), Block.UPDATE_CLIENTS);
                this.placed++;
            }
        }

        void bell(BlockPos p, Direction facing, int m) {
            if (!this.open(p)) {
                return;
            }
            BlockState s = TrumpetCoralFeature.this.bells.get(m);
            if (s.hasProperty(TrumpetCoralBellBlock.FACING)) {
                s = s.setValue(TrumpetCoralBellBlock.FACING, facing);
            }
            if (s.hasProperty(TrumpetCoralBellBlock.AGE)) {
                // most wild bells are nearly grown; a few young ones keep the reef growing
                s = s.setValue(TrumpetCoralBellBlock.AGE, this.random.nextInt(4) == 0 ? 3 + this.random.nextInt(2) : 5 + this.random.nextInt(3));
            }
            this.level.setBlock(p, SeaLogging.inFluidAt(s, this.level, p), Block.UPDATE_CLIENTS);
            this.placed++;
        }

        /** A run of tube from {@code from} (not included) {@code len} blocks along {@code d}; returns where it ended. */
        BlockPos run(BlockPos from, Direction d, int len, int m) {
            BlockPos p = from;
            for (int i = 0; i < len; i++) {
                BlockPos q = p.relative(d);
                if (!this.tube(q, m)) {
                    break;
                }
                p = q;
            }
            return p;
        }

        /** A bell at the end of a run: it opens the way the run was going (or straight up from a level run, sometimes). */
        void cap(BlockPos end, Direction d, int m) {
            Direction facing = d.getAxis().isHorizontal() && this.random.nextInt(3) == 0 ? Direction.UP : d;
            BlockPos at = end.relative(facing);
            if (this.open(at)) {
                this.bell(at, facing, m);
            } else if (this.open(end.relative(d))) {
                this.bell(end.relative(d), d, m);
            }
        }

        /** A small rock of coral blocks round the root. */
        void base(BlockPos root, int size) {
            this.block(root, this.metal);
            for (int i = 0; i < size; i++) {
                Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(this.random);
                BlockPos p = root.relative(d);
                if (this.level.getBlockState(p.below()).isFaceSturdy(this.level, p.below(), Direction.UP)) {
                    this.block(p, this.random.nextInt(4) == 0 ? this.branchMetal() : this.metal);
                }
            }
        }

        void organ(BlockPos root) {
            Direction along = Direction.Plane.HORIZONTAL.getRandomDirection(this.random);
            int pipes = 3 + this.random.nextInt(3);
            int peak = this.random.nextInt(pipes);
            for (int i = 0; i < pipes; i++) {
                BlockPos foot = root.relative(along, i * 2);
                if (!this.open(foot) || !this.level.getBlockState(foot.below()).isFaceSturdy(this.level, foot.below(), Direction.UP)) {
                    break;
                }
                this.block(foot, this.metal);
                if (i > 0) {
                    this.block(foot.relative(along.getOpposite()), this.metal);
                }
                int m = this.branchMetal();
                int h = 7 - Math.abs(i - peak) * 2 + this.random.nextInt(2);
                BlockPos top = this.run(foot, Direction.UP, Math.max(2, h), m);
                this.cap(top, Direction.UP, m);
            }
        }

        void horn(BlockPos root) {
            this.base(root, 1 + this.random.nextInt(3));
            BlockPos top = this.run(root, Direction.UP, 2 + this.random.nextInt(3), this.metal);
            int branches = 2 + this.random.nextInt(2);
            Direction first = Direction.Plane.HORIZONTAL.getRandomDirection(this.random);
            for (int b = 0; b < branches; b++) {
                Direction out = b == 0 ? first : b == 1 ? first.getOpposite() : first.getClockWise();
                int m = this.branchMetal();
                // two blocks out at least, so the rising branch never touches the trunk
                BlockPos p = this.run(top, out, 2 + this.random.nextInt(2), m);
                p = this.run(p, Direction.UP, 1 + this.random.nextInt(3), m);
                if (this.random.nextBoolean()) {
                    // a second bend: out again, like the curl of a horn
                    p = this.run(p, out, 1, m);
                    this.cap(p, out, m);
                } else {
                    this.cap(p, Direction.UP, m);
                }
            }
            // the trunk itself plays on above the fork
            BlockPos crown = this.run(top, Direction.UP, 1 + this.random.nextInt(2), this.metal);
            if (!crown.equals(top)) {
                this.cap(crown, Direction.UP, this.metal);
            }
        }

        void trumpet(BlockPos root) {
            this.base(root, 2 + this.random.nextInt(2));
            Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(this.random);
            BlockPos top = this.run(root, Direction.UP, 2 + this.random.nextInt(2), this.metal);
            // a mouthpiece stub behind, the lead pipe forward with valve stubs, and the big bell at the front
            this.run(top, d.getOpposite(), 1, this.metal);
            int len = 3 + this.random.nextInt(3);
            BlockPos p = top;
            for (int i = 0; i < len; i++) {
                BlockPos q = p.relative(d);
                if (!this.tube(q, this.metal)) {
                    break;
                }
                p = q;
                if (i % 2 == 1 && i < len - 1 && this.random.nextInt(3) != 0) {
                    int m = this.branchMetal();
                    BlockPos valve = this.run(p, Direction.UP, 1, m);
                    if (!valve.equals(p)) {
                        this.bell(valve.above(), Direction.UP, m);
                    }
                }
            }
            if (this.open(p.relative(d))) {
                this.bell(p.relative(d), d, this.metal);
            }
        }

        void bush(BlockPos root) {
            this.base(root, 2 + this.random.nextInt(3));
            BlockPos top = this.run(root, Direction.UP, 1 + this.random.nextInt(2), this.metal);
            for (Direction out : Direction.Plane.HORIZONTAL) {
                if (this.random.nextInt(4) == 0) {
                    continue;
                }
                int m = this.branchMetal();
                BlockPos p = this.run(top, out, 2, m);
                p = this.run(p, Direction.UP, 1 + this.random.nextInt(3), m);
                this.cap(p, this.random.nextInt(3) == 0 ? out : Direction.UP, m);
            }
            BlockPos crown = this.run(top, Direction.UP, 2 + this.random.nextInt(3), this.metal);
            this.cap(crown, Direction.UP, this.metal);
        }

        /** Every tube joins its neighbours once the whole coral is down. */
        void finish() {
            for (BlockPos p : this.tubes) {
                BlockState s = this.level.getBlockState(p);
                if (s.getBlock() instanceof TrumpetCoralPipeBlock) {
                    this.level.setBlock(p, TrumpetCoralPipeBlock.withConnections(this.level, p, s), Block.UPDATE_CLIENTS);
                }
            }
        }
    }
}
