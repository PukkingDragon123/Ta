package com.thesift.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thesift.block.SeaLogging;
import com.thesift.block.TrumpetCoralBellBlock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * W-sea: a coral reef growth on the sea floor, in water or in Chrome - the Chrome Coral Ocean's strange Bubble Coral shapes
 * and the Brass Coral Reef's metal mounds. One of these shapes, in one or two of the {@code blocks} (colours):
 * <ul>
 *   <li>{@code tree}: a stalk with branches that step out and up, each ending in a round puff;</li>
 *   <li>{@code dome}: a low, lumpy brain-coral dome, speckled with a second colour;</li>
 *   <li>{@code spire}: a tall, twisting spire banded in two colours;</li>
 *   <li>{@code arch}: an arch springing from the floor;</li>
 *   <li>{@code cluster}: a heap of coral bubbles.</li>
 * </ul>
 * Then it is dressed: on top of each block, with {@code top_chance}, one of {@code tops} (corals and fans, or a bell facing
 * up); on each open side, with {@code side_chance}, one of {@code sides} (wall fans, or a bell) facing out. Decorations
 * mostly match the colour of the block they grow on: entry {@code j} belongs to colour {@code j % blocks.size()}.
 */
public record SiftReefFeature(List<BlockState> blocks, List<BlockState> tops, List<BlockState> sides, float topChance, float sideChance,
                              List<String> shapes) implements Feature {
    public static final MapCodec<SiftReefFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockState.CODEC.listOf().fieldOf("blocks").forGetter(SiftReefFeature::blocks),
            BlockState.CODEC.listOf().fieldOf("tops").forGetter(SiftReefFeature::tops),
            BlockState.CODEC.listOf().fieldOf("sides").forGetter(SiftReefFeature::sides),
            Codec.floatRange(0.0F, 1.0F).fieldOf("top_chance").forGetter(SiftReefFeature::topChance),
            Codec.floatRange(0.0F, 1.0F).fieldOf("side_chance").forGetter(SiftReefFeature::sideChance),
            Codec.STRING.listOf().fieldOf("shapes").forGetter(SiftReefFeature::shapes)
    ).apply(i, SiftReefFeature::new));

    @Override
    public MapCodec<SiftReefFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (this.blocks.isEmpty() || this.shapes.isEmpty() || !SeaLogging.openSea(level, origin)
                || !level.getBlockState(origin.below()).isFaceSturdy(level, origin.below(), Direction.UP)) {
            return false;
        }
        Reef r = new Reef(level, random);
        int a = random.nextInt(this.blocks.size());
        int b = this.blocks.size() > 1 && random.nextInt(3) != 0 ? Math.floorMod(a + 1 + random.nextInt(this.blocks.size() - 1), this.blocks.size()) : a;
        switch (this.shapes.get(random.nextInt(this.shapes.size()))) {
            case "tree" -> r.tree(origin, a, b);
            case "dome" -> r.dome(origin, a, b);
            case "spire" -> r.spire(origin, a, b == a ? Math.floorMod(a + 1, this.blocks.size()) : b);
            case "arch" -> r.arch(origin, a);
            default -> r.cluster(origin, a, b);
        }
        r.dress();
        return !r.placed.isEmpty();
    }

    private final class Reef {
        final WorldGenLevel level;
        final RandomSource random;
        /** Every block laid, with its colour. */
        final Map<BlockPos, Integer> placed = new LinkedHashMap<>();

        Reef(WorldGenLevel level, RandomSource random) {
            this.level = level;
            this.random = random;
        }

        void put(BlockPos p, int colour) {
            if (!this.placed.containsKey(p) && SeaLogging.openSea(this.level, p)) {
                this.level.setBlock(p, SiftReefFeature.this.blocks.get(colour), Block.UPDATE_CLIENTS);
                this.placed.put(p.immutable(), colour);
            }
        }

        void ball(BlockPos c, int radius, int colour) {
            for (BlockPos p : BlockPos.betweenClosed(c.offset(-radius, -radius, -radius), c.offset(radius, radius, radius))) {
                int dx = p.getX() - c.getX();
                int dy = p.getY() - c.getY();
                int dz = p.getZ() - c.getZ();
                if (dx * dx + dy * dy + dz * dz <= radius * radius + (radius > 1 ? 1 : 0) && this.random.nextInt(9) != 0) {
                    this.put(p, colour);
                }
            }
        }

        void tree(BlockPos root, int a, int b) {
            int h = 2 + this.random.nextInt(4);
            for (int y = 0; y < h; y++) {
                this.put(root.above(y), a);
            }
            int branches = 2 + this.random.nextInt(3);
            for (int i = 0; i < branches; i++) {
                Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(this.random);
                BlockPos p = root.above(h / 2 + this.random.nextInt(Math.max(1, h - h / 2)));
                int steps = 2 + this.random.nextInt(3);
                for (int s = 0; s < steps; s++) {
                    p = this.random.nextInt(3) == 0 ? p.above() : p.relative(d).above(this.random.nextInt(2));
                    this.put(p, a);
                }
                this.ball(p.above(), this.random.nextInt(3) == 0 ? 2 : 1, this.random.nextBoolean() ? b : a);
            }
            this.ball(root.above(h), 1, b);
        }

        void dome(BlockPos root, int a, int b) {
            int r = 2 + this.random.nextInt(3);
            float squash = 0.65F + this.random.nextFloat() * 0.3F;
            for (BlockPos p : BlockPos.betweenClosed(root.offset(-r, 0, -r), root.offset(r, r, r))) {
                float dx = p.getX() - root.getX();
                float dy = (p.getY() - root.getY()) / squash;
                float dz = p.getZ() - root.getZ();
                float d = Mth.sqrt(dx * dx + dy * dy + dz * dz);
                // a lumpy surface: brain-coral folds where the shell is thinnest
                if (d <= r + 0.3F && this.random.nextInt(12) != 0) {
                    this.put(p, d > r - 1.0F && this.random.nextInt(5) == 0 ? b : a);
                }
            }
        }

        void spire(BlockPos root, int a, int b) {
            int h = 5 + this.random.nextInt(8);
            float base = 1.4F + this.random.nextFloat() * 0.9F;
            float twist = 0.45F + this.random.nextFloat() * 0.4F;
            float phase = this.random.nextFloat() * Mth.TWO_PI;
            int band = 1 + this.random.nextInt(2);
            for (int y = 0; y < h; y++) {
                float t = (float) y / h;
                float r = Math.max(0.5F, base * (1.0F - t * 0.85F));
                float cx = Mth.cos(phase + y * twist) * 0.9F * t * base;
                float cz = Mth.sin(phase + y * twist) * 0.9F * t * base;
                int c = (y / band) % 2 == 0 ? a : b;
                int ri = Mth.ceil(r + 1.0F);
                for (int dx = -ri; dx <= ri; dx++) {
                    for (int dz = -ri; dz <= ri; dz++) {
                        float ex = dx - cx;
                        float ez = dz - cz;
                        if (ex * ex + ez * ez <= r * r) {
                            this.put(root.offset(dx, y, dz), c);
                        }
                    }
                }
            }
        }

        void arch(BlockPos root, int a) {
            Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(this.random);
            int span = 4 + this.random.nextInt(5);
            int height = 3 + this.random.nextInt(3);
            for (int i = 0; i <= span; i++) {
                int top = Math.round(Mth.sin(Mth.PI * i / span) * height);
                BlockPos col = root.relative(d, i);
                int from = i == 0 || i == span ? 0 : Math.max(0, top - 1);
                for (int y = from; y <= top; y++) {
                    this.put(col.above(y), a);
                }
            }
        }

        void cluster(BlockPos root, int a, int b) {
            int n = 3 + this.random.nextInt(4);
            BlockPos c = root;
            for (int i = 0; i < n; i++) {
                int radius = this.random.nextInt(3) == 0 ? 2 : 1;
                this.ball(c.above(radius - 1), radius, i % 2 == 0 ? a : b);
                c = c.offset(this.random.nextInt(3) - 1, this.random.nextInt(2), this.random.nextInt(3) - 1);
            }
        }

        BlockState pick(List<BlockState> from, int colour) {
            int n = SiftReefFeature.this.blocks.size();
            if (this.random.nextInt(4) != 0) {
                int matches = 0;
                for (int j = colour; j < from.size(); j += n) {
                    matches++;
                }
                if (matches > 0) {
                    return from.get(colour + n * this.random.nextInt(matches));
                }
            }
            return from.get(this.random.nextInt(from.size()));
        }

        /** Corals and fans on top, wall fans (or bells) on the open sides. */
        void dress() {
            List<BlockState> tops = SiftReefFeature.this.tops;
            List<BlockState> sides = SiftReefFeature.this.sides;
            for (Map.Entry<BlockPos, Integer> e : this.placed.entrySet()) {
                BlockPos p = e.getKey();
                BlockPos up = p.above();
                if (!tops.isEmpty() && this.random.nextFloat() < SiftReefFeature.this.topChance && SeaLogging.openSea(this.level, up)) {
                    this.level.setBlock(up, SeaLogging.inFluidAt(this.face(this.pick(tops, e.getValue()), Direction.UP), this.level, up), Block.UPDATE_CLIENTS);
                }
                if (sides.isEmpty()) {
                    continue;
                }
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    BlockPos s = p.relative(d);
                    if (this.random.nextFloat() < SiftReefFeature.this.sideChance && SeaLogging.openSea(this.level, s)) {
                        this.level.setBlock(s, SeaLogging.inFluidAt(this.face(this.pick(sides, e.getValue()), d), this.level, s), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }

        BlockState face(BlockState s, Direction d) {
            if (s.hasProperty(BlockStateProperties.HORIZONTAL_FACING) && d.getAxis().isHorizontal()) {
                s = s.setValue(BlockStateProperties.HORIZONTAL_FACING, d);
            } else if (s.hasProperty(BlockStateProperties.FACING)) {
                s = s.setValue(BlockStateProperties.FACING, d);
            }
            if (s.hasProperty(TrumpetCoralBellBlock.AGE)) {
                s = s.setValue(TrumpetCoralBellBlock.AGE, 4 + this.random.nextInt(4));
            }
            return s;
        }
    }
}
