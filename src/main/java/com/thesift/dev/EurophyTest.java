package com.thesift.dev;

import com.thesift.TheSift;
import com.thesift.block.entity.EurophyRecipes;
import com.thesift.block.entity.EurophyTableBlockEntity;
import com.thesift.music.SongEvents;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModItems;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.jspecify.annotations.Nullable;

/**
 * F1 Materials & Europhy Table checks (run from MechanicsTest): a tune played beside a Europhy Table loaded with
 * Siftite Dust and Copper forms a Siftite Ingot; Bauxite mined in the open bursts and is lost, mined touching
 * Chrome it drops whole.
 */
final class EurophyTest {
    private final ServerLevel sift;
    private final BiConsumer<Boolean, String> check;
    private @Nullable EurophyTableBlockEntity table;

    private EurophyTest(ServerLevel sift, BiConsumer<Boolean, String> check) {
        this.sift = sift;
        this.check = check;
    }

    static EurophyTest start(ServerLevel sift, BiConsumer<Boolean, String> check) {
        EurophyTest t = new EurophyTest(sift, check);
        t.setUpTable();
        t.bauxite();
        return t;
    }

    void tick(int ticks) {
        if (ticks == 40) {
            this.play();
        }
        if (ticks == 320) {
            this.finish();
        }
    }

    // ------------------------------------------------------------------ the Europhy Table

    private void setUpTable() {
        int x = 22, z = 12;
        int y = this.sift.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 3;
        this.sift.setChunkForced(x >> 4, z >> 4, true);
        for (BlockPos p : BlockPos.betweenClosed(x - 3, y - 1, z - 3, x + 3, y + 3, z + 3)) {
            this.sift.setBlock(p, p.getY() == y - 1 ? ModBlocks.POLISHED_DREAMSTONE.get().defaultBlockState() : Blocks.AIR.defaultBlockState(),
                    Block.UPDATE_CLIENTS);
        }
        BlockPos at = new BlockPos(x, y, z);
        this.sift.setBlock(at, ModBlocks.EUROPHY_TABLE.get().defaultBlockState(), Block.UPDATE_ALL);
        if (!(this.sift.getBlockEntity(at) instanceof EurophyTableBlockEntity t)) {
            this.check.accept(false, "europhy: block entity");
            return;
        }
        t.getItems().setItem(0, new ItemStack(ModItems.SIFTITE_DUST.get(), 4));
        t.getItems().setItem(2, new ItemStack(Items.COPPER_INGOT));
        this.check.accept(t.need() == EurophyRecipes.SIFTITE_INGOT.notes(), "europhy: 4 Siftite Dust + a Copper Ingot on the arms are a recipe (need "
                + t.need() + " notes)");
        this.table = t;
    }

    private void play() {
        EurophyTableBlockEntity t = this.table;
        if (t == null) {
            return;
        }
        Vec3 at = Vec3.atCenterOf(t.getBlockPos()).add(2.0, 0.0, 0.0);
        // eight different notes in a row (the repeated 13 does not count)
        for (int pitch : new int[]{6, 10, 13, 13, 18, 15, 13, 10, 8, 6}) {
            SongEvents.note(this.sift, null, at, pitch);
        }
        TheSift.LOGGER.info("SMOKE: europhy charge {}/{} forming {}", t.charge(), t.need(), t.isForming());
        this.check.accept(t.isForming(), "europhy: a tune played beside the table starts the forming");
    }

    private void finish() {
        EurophyTableBlockEntity t = this.table;
        if (t == null) {
            return;
        }
        ItemStack out = t.getItems().getItem(EurophyTableBlockEntity.OUTPUT);
        TheSift.LOGGER.info("SMOKE: europhy output {} inputs {}", out, t.inputs());
        this.check.accept(!t.isForming() && out.is(ModItems.SIFTITE_INGOT.get()) && t.getItems().getItem(0).isEmpty()
                && t.getItems().getItem(2).isEmpty(), "europhy: the table forms a Siftite Ingot from the dust and copper");
    }

    // ------------------------------------------------------------------ Bauxite

    private void bauxite() {
        int x = 70, z = -70;
        int y = this.sift.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 6;
        FakePlayer miner = FakePlayerFactory.getMinecraft(this.sift);
        ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        // 1) in the open: it bursts, nothing drops and the dirt packed round it is blown away
        BlockPos a = new BlockPos(x, y, z);
        this.clear(a);
        for (Direction d : Direction.values()) {
            this.sift.setBlock(a.relative(d), dirt, Block.UPDATE_CLIENTS);
        }
        this.sift.setBlock(a, ModBlocks.BAUXITE_ORE.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        this.mine(a, miner, pick);
        int dirtLeft = 0;
        for (Direction d : Direction.values()) {
            dirtLeft += this.sift.getBlockState(a.relative(d)).is(Blocks.DIRT) ? 1 : 0;
        }
        int dropped = this.bauxiteNear(a);
        TheSift.LOGGER.info("SMOKE: bauxite in the open: dirt left {}, bauxite dropped {}", dirtLeft, dropped);
        this.check.accept(dropped == 0 && dirtLeft < 6, "bauxite: mined in the open it explodes and is lost");
        // 2) Chrome against it (a source in a dirt cup underneath): it comes out whole, nothing bursts
        BlockPos b = a.offset(14, 0, 0);
        this.clear(b);
        BlockPos pool = b.below();
        for (Direction d : Direction.values()) {
            this.sift.setBlock(b.relative(d), dirt, Block.UPDATE_CLIENTS);
            if (d != Direction.UP) {
                this.sift.setBlock(pool.relative(d), dirt, Block.UPDATE_CLIENTS);
            }
        }
        this.sift.setBlock(pool, ModBlocks.CHROME.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        this.sift.setBlock(b, ModBlocks.BAUXITE_ORE.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        this.mine(b, miner, pick);
        int intact = 0;
        for (Direction d : Direction.values()) {
            intact += d != Direction.DOWN && this.sift.getBlockState(b.relative(d)).is(Blocks.DIRT) ? 1 : 0;
        }
        int whole = this.bauxiteNear(b);
        TheSift.LOGGER.info("SMOKE: bauxite touching chrome: dirt intact {}, bauxite dropped {}", intact, whole);
        this.check.accept(whole > 0 && intact == 5, "bauxite: mined touching Chrome it drops whole without bursting");
    }

    private void clear(BlockPos c) {
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-3, -3, -3), c.offset(3, 3, 3))) {
            this.sift.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** What a survival player's pickaxe does: the block is removed, then the block decides what happens. */
    private void mine(BlockPos p, FakePlayer miner, ItemStack tool) {
        BlockState state = this.sift.getBlockState(p);
        this.sift.removeBlock(p, false);
        state.getBlock().playerDestroy(this.sift, miner, p, state, null, tool);
    }

    private int bauxiteNear(BlockPos p) {
        List<ItemEntity> items = this.sift.getEntitiesOfClass(ItemEntity.class, new AABB(p).inflate(3.0), e -> e.getItem().is(ModItems.BAUXITE.get()));
        int n = items.stream().mapToInt(e -> e.getItem().getCount()).sum();
        items.forEach(ItemEntity::discard);
        return n;
    }
}
