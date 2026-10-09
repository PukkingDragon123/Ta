package com.thesift.world.sky;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * W-sky: the vines and roots that hold the Sky Islands together.
 *
 * <ul>
 *   <li>{@code bridge}: from a point on an island it walks to the island's edge, looks out across the gap for another
 *       island (or another arm of the same one) and spans it: a thick, sagging Sky Root bridge you can walk on, or a
 *       huge Sky Vine slung across like a rope - swing from it, or go hand over hand along it. Lianas hang from both,
 *       some ending in a giant Driftfruit.</li>
 *   <li>{@code dangle}: finds an island's underside above a random point and hangs a long liana from it (sometimes
 *       a root stalactite first), often with a Driftfruit at the end.</li>
 * </ul>
 */
public record SkyBridgeFeature(String mode, BlockState vine, BlockState root, BlockState fruit, int maxSpan, float fruitChance) implements Feature {
    public static final MapCodec<SkyBridgeFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("mode").forGetter(SkyBridgeFeature::mode),
            BlockState.CODEC.fieldOf("vine").forGetter(SkyBridgeFeature::vine),
            BlockState.CODEC.fieldOf("root").forGetter(SkyBridgeFeature::root),
            BlockState.CODEC.fieldOf("fruit").forGetter(SkyBridgeFeature::fruit),
            Codec.intRange(8, 48).fieldOf("max_span").forGetter(SkyBridgeFeature::maxSpan),
            Codec.floatRange(0.0F, 1.0F).fieldOf("fruit_chance").forGetter(SkyBridgeFeature::fruitChance)
    ).apply(i, SkyBridgeFeature::new));
    /** Islands float well above this; anything lower is the ground. */
    private static final int SKY = 120;

    @Override
    public MapCodec<SkyBridgeFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        SkyGen g = new SkyGen(level, random, origin);
        boolean done = this.mode.equals("dangle") ? this.dangle(g, origin) : this.bridge(g, origin);
        g.connectVines();
        return done;
    }

    // ------------------------------------------------------------------ bridges

    /** The surface block of the column at p (searched a few blocks up and down from p's height), or null. */
    private static @Nullable BlockPos surface(SkyGen g, BlockPos p) {
        for (int dy = 4; dy >= -8; dy--) {
            BlockPos q = p.above(dy);
            if (g.solid(q) && g.air(q.above())) {
                return q;
            }
        }
        return null;
    }

    private boolean bridge(SkyGen g, BlockPos origin) {
        RandomSource r = g.random;
        BlockPos top = origin.below();
        if (origin.getY() < SKY || !g.solid(top)) {
            return false;
        }
        double ang = r.nextDouble() * Math.PI * 2.0;
        Vec3 dir = new Vec3(Math.cos(ang), 0.0, Math.sin(ang));
        // walk to the island's edge
        BlockPos edge = top;
        for (int k = 1; k <= 48; k++) {
            BlockPos col = BlockPos.containing(top.getX() + 0.5 + dir.x * k, edge.getY(), top.getZ() + 0.5 + dir.z * k);
            if (!g.inside(col)) {
                return false;
            }
            BlockPos s = surface(g, col);
            if (s == null) {
                break;
            }
            edge = s;
            if (k == 48) {
                return false;
            }
        }
        // anchor a little way down the island's side and look across the gap for the far side
        int down = 1 + r.nextInt(4);
        Vec3 from = new Vec3(edge.getX() + 0.5, edge.getY() - down + 0.5, edge.getZ() + 0.5);
        BlockPos far = null;
        int gap = 0;
        for (int k = 1; k <= this.maxSpan; k++) {
            Vec3 c = from.add(dir.scale(k));
            BlockPos col = BlockPos.containing(c);
            if (!g.inside(col)) {
                return false;
            }
            BlockPos hit = null;
            for (int j = 0; j <= 10 && hit == null; j++) {
                for (int sgn : new int[]{-1, 1}) {
                    BlockPos q = col.above(sgn * j);
                    if (j <= 6 || sgn < 0) {
                        if (g.solid(q)) {
                            hit = q;
                            break;
                        }
                    }
                }
            }
            if (hit != null) {
                if (k <= 3) {
                    return false; // still the same cliff: no gap here
                }
                far = hit;
                gap = k;
                break;
            }
        }
        if (far == null || gap < 6) {
            return false;
        }
        Vec3 to = Vec3.atCenterOf(far).subtract(dir.scale(0.6));
        boolean root = r.nextFloat() < 0.4F;
        double sag = gap * (root ? 0.05 + r.nextDouble() * 0.05 : 0.12 + r.nextDouble() * 0.1);
        List<BlockPos> path = SkyGen.sagPath(from.add(dir), to, sag);
        if (path.size() < 4) {
            return false;
        }
        // the ends may sit in the rock they anchor to; the rest of the way must be open sky
        if (!g.clear(path.subList(1, path.size() - 1))) {
            return false;
        }
        Direction.Axis axis = Math.abs(dir.x) >= Math.abs(dir.z) ? Direction.Axis.X : Direction.Axis.Z;
        if (root) {
            this.rootSpan(g, path, axis, dir);
        } else {
            for (BlockPos p : path) {
                g.vine(p, this.vine);
            }
        }
        // lianas hanging from the span every few blocks
        int next = 2 + g.random.nextInt(3);
        for (int i = 2; i < path.size() - 2; i++) {
            if (i < next) {
                continue;
            }
            next = i + 3 + g.random.nextInt(4);
            BlockPos under = root ? path.get(i).below(2) : path.get(i).below();
            g.strand(under, 3 + g.random.nextInt(12), this.vine, this.fruit, this.fruitChance);
        }
        return true;
    }

    /** A Sky Root bridge: a thick bundle of roots along the path, pushing into the rock at both ends. */
    private void rootSpan(SkyGen g, List<BlockPos> path, Direction.Axis axis, Vec3 dir) {
        Direction side = axis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
        for (int i = 0; i < path.size(); i++) {
            BlockPos p = path.get(i);
            BlockPos prev = path.get(Math.max(0, i - 1));
            Direction.Axis a = prev.getY() != p.getY() && prev.getX() == p.getX() && prev.getZ() == p.getZ() ? Direction.Axis.Y : axis;
            g.log(p, this.root, a);
            g.log(p.below(), this.root, a);
            if (g.random.nextFloat() < 0.75F && g.free(p.relative(side))) {
                g.log(p.relative(side), this.root, a);
            }
            if (g.random.nextFloat() < 0.35F && g.free(p.relative(side.getOpposite()).below())) {
                g.log(p.relative(side.getOpposite()).below(), this.root, a);
            }
        }
        // the root ends dig two blocks into the islands
        BlockPos a = path.get(0);
        BlockPos b = path.get(path.size() - 1);
        Direction out = Math.abs(dir.x) >= Math.abs(dir.z) ? (dir.x > 0 ? Direction.EAST : Direction.WEST) : (dir.z > 0 ? Direction.SOUTH : Direction.NORTH);
        for (int k = 1; k <= 2; k++) {
            g.log(a.relative(out.getOpposite(), k), this.root, axis);
            g.log(b.relative(out, k), this.root, axis);
        }
    }

    // ------------------------------------------------------------------ hanging under the islands

    private boolean dangle(SkyGen g, BlockPos origin) {
        RandomSource r = g.random;
        BlockPos ceiling = null;
        for (int dy = 0; dy <= 40; dy++) {
            BlockPos p = origin.above(dy);
            if (!g.inside(p)) {
                return false;
            }
            if (!g.air(p)) {
                if (g.solid(p) && g.air(p.below())) {
                    ceiling = p;
                }
                break;
            }
        }
        if (ceiling == null || !g.air(ceiling.below(2))) {
            return false;
        }
        BlockPos start = ceiling.below();
        if (r.nextFloat() < 0.35F) {
            // a root stalactite first, the liana hanging on from its tip
            int len = 2 + r.nextInt(5);
            for (int i = 0; i < len && g.air(start); i++) {
                g.log(start, this.root, Direction.Axis.Y);
                start = start.below();
            }
        }
        return g.strand(start, 4 + r.nextInt(this.maxSpan - 3), this.vine, this.fruit, this.fruitChance) > 0;
    }
}
