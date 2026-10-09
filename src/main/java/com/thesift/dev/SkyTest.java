package com.thesift.dev;

import com.thesift.TheSift;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModItems;
import com.thesift.world.sky.SkyIslands;
import com.thesift.world.sky.SkyRope;
import com.thesift.world.sky.SkySwing;
import com.thesift.world.sky.SkyVineBlock;
import com.thesift.world.sky.SkyVines;
import com.thesift.world.sky.SkyrindBunchBlock;
import com.thesift.world.sky.SwingPhysics;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * W-sky checks for the CI smoke test: the giant islands really float in the Sift's sky and lie in the Sky Island
 * biome; a rope swings like a pendulum and can be pumped higher; a grabbed vine hangs from the right place; the server
 * tracks a swinging player and their rope; Driftfruit breaks into slices; a ripe Skyrind bunch picks and regrows; the
 * Phase 4 food tags hold; the bridge and tree features build what they should.
 */
final class SkyTest {
    private static final ResourceKey<Biome> SKY_ISLAND = ResourceKey.create(Registries.BIOME, TheSift.id("sky_island"));

    private SkyTest() {
    }

    static void run(ServerLevel sift, BiConsumer<Boolean, String> check) {
        physics(check);
        islands(sift, check);
        vines(sift, check);
        fruit(sift, check);
        features(sift, check);
    }

    // ------------------------------------------------------------------ the rope

    private static void physics(BiConsumer<Boolean, String> check) {
        Vec3 pivot = new Vec3(0.0, 100.0, 0.0);
        double length = 8.0;
        double a = Math.toRadians(60.0);
        Vec3 hand = new Vec3(Math.sin(a) * length, 100.0 - Math.cos(a) * length, 0.0);
        Vec3 v = Vec3.ZERO;
        double stretched = 0.0;
        double far = 0.0;
        for (int t = 0; t < 40; t++) {
            SwingPhysics.State s = SwingPhysics.step(hand, v, pivot, length, Vec3.ZERO);
            hand = s.hand();
            v = s.velocity();
            stretched = Math.max(stretched, hand.distanceTo(pivot) - length);
            far = Math.min(far, hand.x);
        }
        double reached = Math.toDegrees(Math.asin(Math.min(1.0, -far / length)));
        TheSift.LOGGER.info("SMOKE: sky swing from 60 degrees reaches {} degrees on the far side, rope stretched {}", String.format("%.1f", reached),
                String.format("%.4f", stretched));
        check.accept(reached > 50.0 && reached < 61.0, "sky: a swing from 60 degrees comes up the other side almost as high");
        check.accept(stretched < 0.001, "sky: the rope never stretches");
        // leaning into the swing pumps it higher
        hand = new Vec3(Math.sin(Math.toRadians(10.0)) * length, 100.0 - Math.cos(Math.toRadians(10.0)) * length, 0.0);
        v = Vec3.ZERO;
        double best = 0.0;
        Vec3 look = new Vec3(1.0, 0.0, 0.0);
        for (int t = 0; t < 400; t++) {
            boolean towards = v.x > 0.0;
            look = towards ? new Vec3(1.0, 0.0, 0.0) : new Vec3(-1.0, 0.0, 0.0);
            Vec3 push = SwingPhysics.push(v, 1, 0, look, hand.subtract(pivot));
            SwingPhysics.State s = SwingPhysics.step(hand, v, pivot, length, push);
            hand = s.hand();
            v = s.velocity();
            best = Math.max(best, Math.abs(Math.toDegrees(Math.atan2(hand.x - pivot.x, pivot.y - hand.y))));
        }
        TheSift.LOGGER.info("SMOKE: sky swing pumped from 10 degrees reaches {} degrees", String.format("%.1f", best));
        check.accept(best > 45.0, "sky: pumping a swing makes it bigger");
    }

    // ------------------------------------------------------------------ the islands

    private static void islands(ServerLevel sift, BiConsumer<Boolean, String> check) {
        ChunkGenerator generator = sift.getChunkSource().getGenerator();
        RandomState state = sift.getChunkSource().randomState();
        int columns = 0;
        int floating = 0;
        int inBiome = 0;
        int thick = 0;
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 20; j++) {
                int x = -800 + i * 80 + 7;
                int z = -800 + j * 80 + 3;
                NoiseColumn col = generator.getBaseColumn(x, z, sift, state);
                columns++;
                int top = Integer.MIN_VALUE;
                int bottom = Integer.MIN_VALUE;
                for (int y = 300; y >= 100; y--) {
                    BlockState b = col.getBlock(y);
                    boolean solid = !b.isAir() && b.getFluidState().isEmpty();
                    if (solid && top == Integer.MIN_VALUE) {
                        top = y;
                    } else if (!solid && top != Integer.MIN_VALUE) {
                        bottom = y;
                        break;
                    }
                }
                if (top == Integer.MIN_VALUE || bottom == Integer.MIN_VALUE || top < 140) {
                    continue; // no island over this column (or only the ground rising this high)
                }
                floating++;
                if (top - bottom >= 12) {
                    thick++;
                }
                Holder<Biome> biome = sift.getUncachedNoiseBiome(x >> 2, (top + 1) >> 2, z >> 2);
                if (biome.is(SKY_ISLAND)) {
                    inBiome++;
                }
            }
        }
        double cover = (double) floating / columns;
        TheSift.LOGGER.info("SMOKE: sky islands over {} of {} columns ({}%), {} at least 12 blocks thick, {} of them in the Sky Island biome", floating,
                columns, String.format("%.0f", cover * 100.0), thick, inBiome);
        check.accept(cover > 0.08 && cover < 0.6, "sky: giant islands float over a good share of the Sift (" + floating + "/" + columns + ")");
        check.accept(thick * 4 >= floating, "sky: the islands are thick, not thin rafts");
        check.accept(floating > 0 && inBiome * 4 >= floating * 3, "sky: island tops lie in the Sky Island biome (" + inBiome + "/" + floating + ")");
    }

    // ------------------------------------------------------------------ vines and swinging

    private static void clear(ServerLevel sift, BlockPos a, BlockPos b) {
        for (BlockPos p : BlockPos.betweenClosed(a, b)) {
            sift.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private static void vines(ServerLevel sift, BiConsumer<Boolean, String> check) {
        BlockPos base = new BlockPos(-40, 262, -72);
        clear(sift, base.offset(-2, -2, -2), base.offset(24, 14, 2));
        BlockState stone = ModBlocks.DREAMSTONE.get().defaultBlockState();
        BlockState vine = ModBlocks.SKY_VINE.get().defaultBlockState();
        // a strand of eight hanging from a block of stone
        sift.setBlock(base.above(10), stone, Block.UPDATE_ALL);
        for (int y = 9; y >= 2; y--) {
            sift.setBlock(base.above(y), vine, Block.UPDATE_ALL);
        }
        for (int y = 9; y >= 2; y--) {
            BlockPos p = base.above(y);
            sift.setBlock(p, SkyVineBlock.connected(sift, p, vine), Block.UPDATE_ALL);
        }
        SkyVines.Anchor strand = SkyVines.anchor(sift, base.above(4));
        check.accept(strand.block().equals(base.above(9)) && !strand.span() && Math.abs(strand.pivot().y - (base.getY() + 10.0)) < 1.0E-6
                && Math.abs(strand.reach() - 8.6) < 1.0E-6, "sky: a strand hangs from the top of its highest vine (" + strand + ")");
        check.accept(sift.getBlockState(base.above(9)).getValue(SkyVineBlock.UP), "sky: a vine reaches up to the stone it hangs from");
        // a span between two stone posts
        BlockPos left = base.offset(10, 12, 0);
        BlockPos right = base.offset(20, 12, 0);
        sift.setBlock(left, stone, Block.UPDATE_ALL);
        sift.setBlock(right, stone, Block.UPDATE_ALL);
        for (int x = 11; x <= 19; x++) {
            BlockPos p = base.offset(x, 12, 0);
            sift.setBlock(p, SkyVineBlock.connected(sift, p, vine), Block.UPDATE_ALL);
        }
        BlockPos mid = base.offset(15, 12, 0);
        SkyVines.Anchor span = SkyVines.anchor(sift, mid);
        BlockPos next = SkyVines.along(sift, mid, new Vec3(1.0, 0.0, 0.0));
        check.accept(span.span() && span.block().equals(mid), "sky: a span is held where it is grabbed");
        check.accept(mid.east().equals(next), "sky: hand over hand along a span goes the way you face");
        check.accept(sift.getBlockState(mid).getValue(SkyVineBlock.EAST) && sift.getBlockState(mid).getValue(SkyVineBlock.WEST),
                "sky: a span's vines join up");
        // the server tracks a swinging player and shows their rope
        FakePlayer player = FakePlayerFactory.getMinecraft(sift);
        player.setPos(base.getX() + 0.5, base.getY() + 2.0, base.getZ() + 0.5);
        SkyRope rope = SkySwing.serverSwing(player, true, strand.block(), false);
        check.accept(SkySwing.isSwinging(player) && rope != null && rope.position().distanceTo(strand.pivot()) < 0.01,
                "sky: grabbing a vine hangs a rope from where it hangs");
        SkySwing.serverSwing(player, false, strand.block(), false);
        check.accept(!SkySwing.isSwinging(player) && rope != null && rope.isRemoved(), "sky: letting go takes the rope away");
        // a vine can be grabbed with an empty hand
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(base.above(5)), Direction.NORTH, base.above(5), false);
        check.accept(sift.getBlockState(base.above(5)).useWithoutItem(sift, player, hit).consumesAction(), "sky: using a vine grabs it");
        // cutting the strand drops everything under the cut
        sift.setBlock(base.above(9), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        check.accept(!sift.getBlockState(base.above(8)).getValue(SkyVineBlock.UP), "sky: a cut vine lets go of what held it");
    }

    // ------------------------------------------------------------------ fruit

    private static void fruit(ServerLevel sift, BiConsumer<Boolean, String> check) {
        BlockPos base = new BlockPos(-72, 262, -72);
        clear(sift, base.offset(-2, -2, -2), base.offset(4, 6, 2));
        BlockState drift = ModBlocks.DRIFTFRUIT.get().defaultBlockState();
        check.accept(drift.is(SkyIslands.SKY_WHALE_FOOD), "sky: Sky Whales eat Driftfruit (block tag thesift:sky_whale_food)");
        check.accept(new ItemStack(ModItems.SKYRIND.get()).is(SkyIslands.SWINGER_FOOD), "sky: Swingers are tamed with Skyrinds (item tag thesift:swinger_food)");
        sift.setBlock(base, ModBlocks.DREAMSTONE.get().defaultBlockState(), Block.UPDATE_ALL);
        sift.setBlock(base.above(), drift, Block.UPDATE_ALL);
        List<ItemStack> drops = Block.getDrops(sift.getBlockState(base.above()), sift, base.above(), null, null, ItemStack.EMPTY);
        int slices = drops.stream().filter(s -> s.is(ModItems.DRIFTFRUIT_SLICE.get())).mapToInt(ItemStack::getCount).sum();
        check.accept(slices >= 3 && slices <= 9, "sky: a Driftfruit breaks into slices (" + slices + ")");
        // a ripe bunch under the fronds: picked, it drops Skyrinds and starts again
        BlockPos leaf = base.offset(2, 4, 0);
        sift.setBlock(leaf, ModBlocks.SKYPALM_LEAVES.get().defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true),
                Block.UPDATE_ALL);
        BlockPos bunch = leaf.below();
        sift.setBlock(bunch, ModBlocks.SKYRIND_BUNCH.get().defaultBlockState().setValue(SkyrindBunchBlock.AGE, SkyrindBunchBlock.RIPE), Block.UPDATE_ALL);
        int ripe = Block.getDrops(sift.getBlockState(bunch), sift, bunch, null, null, ItemStack.EMPTY).stream()
                .filter(st -> st.is(ModItems.SKYRIND.get())).mapToInt(ItemStack::getCount).sum();
        FakePlayer player = FakePlayerFactory.getMinecraft(sift);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(bunch), Direction.NORTH, bunch, false);
        boolean used = sift.getBlockState(bunch).useWithoutItem(sift, player, hit).consumesAction();
        BlockState after = sift.getBlockState(bunch);
        check.accept(ripe >= 3 && used && after.is(ModBlocks.SKYRIND_BUNCH.get()) && after.getValue(SkyrindBunchBlock.AGE) == 1,
                "sky: a ripe Skyrind bunch is picked (" + ripe + " Skyrinds) and grows again");
    }

    // ------------------------------------------------------------------ the features

    private static Optional<Feature> feature(ServerLevel sift, String name) {
        return sift.registryAccess().lookupOrThrow(Registries.FEATURE).get(ResourceKey.create(Registries.FEATURE, TheSift.id(name))).map(Holder::value);
    }

    private static int count(ServerLevel sift, BlockPos a, BlockPos b, Block block) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(a, b)) {
            if (sift.getBlockState(p).is(block)) {
                n++;
            }
        }
        return n;
    }

    private static void features(ServerLevel sift, BiConsumer<Boolean, String> check) {
        ChunkGenerator generator = sift.getChunkSource().getGenerator();
        // an island ringed by another: any way the bridge looks, it finds a gap to span
        BlockPos c = new BlockPos(-40, 270, -40);
        clear(sift, c.offset(-17, -16, -17), c.offset(17, 4, 17));
        BlockState stone = ModBlocks.DREAMSTONE.get().defaultBlockState();
        BlockState grass = ModBlocks.SKY_GRASS_BLOCK.get().defaultBlockState();
        for (int dx = -16; dx <= 16; dx++) {
            for (int dz = -16; dz <= 16; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 <= 9 || (d2 >= 13 * 13 && d2 <= 16 * 16)) {
                    for (int dy = -6; dy <= 0; dy++) {
                        sift.setBlock(c.offset(dx, dy, dz), dy == 0 ? grass : stone, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        Optional<Feature> bridge = feature(sift, "sky_bridge");
        boolean built = false;
        for (int seed = 0; seed < 12 && bridge.isPresent() && !built; seed++) {
            built = bridge.get().place(sift, generator, RandomSource.create(seed), c.above());
        }
        int spanned = count(sift, c.offset(-12, -12, -12), c.offset(12, 0, 12), ModBlocks.SKY_VINE.get())
                + count(sift, c.offset(-12, -12, -12), c.offset(12, 0, 12), ModBlocks.SKY_ROOT.get());
        TheSift.LOGGER.info("SMOKE: sky bridge built={} with {} vine/root blocks across the gap", built, spanned);
        check.accept(bridge.isPresent() && built && spanned >= 8, "sky: a bridge of roots or vines spans the gap between islands");
        // trees: a Skypalm (with its bunches) and a Cloudpuff tree grow on Sky Grass
        for (String[] t : new String[][]{{"skypalm_tree", "skypalm_log"}, {"cloud_tree", "sky_root"}}) {
            BlockPos at = new BlockPos(t[0].startsWith("sky") ? -72 : -104, 270, -40);
            clear(sift, at.offset(-8, -1, -8), at.offset(8, 18, 8));
            sift.setBlock(at.below(), grass, Block.UPDATE_ALL);
            Optional<Feature> tree = feature(sift, t[0]);
            boolean grown = tree.isPresent() && tree.get().place(sift, generator, RandomSource.create(7), at);
            Block log = t[1].equals("sky_root") ? ModBlocks.SKY_ROOT.get() : ModBlocks.SKYPALM_LOG.get();
            int logs = count(sift, at.offset(-8, 0, -8), at.offset(8, 18, 8), log);
            check.accept(grown && logs >= 4, "sky: a " + t[0] + " grows (" + logs + " logs)");
        }
    }
}
