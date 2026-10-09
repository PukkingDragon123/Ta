package com.thesift.dev;

import com.thesift.block.EchoerDeviceBlock;
import com.thesift.block.entity.EchoerDeviceBlockEntity;
import com.thesift.registry.ModBlocks;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * RR: the Echoer Drill reads a rhythm. Two steady beats bore a straight 1x1 tunnel exactly as deep as they asked and
 * hand the cobblestone to the chest behind the drill; two fast beats dig a walkable staircase down. Built in the sky
 * (x 94-108, z 37-49, y 276-288), clear of the other tests.
 */
final class DrillTest {
    private static final int X = 96;
    private static final int Y = 284;
    private static final int Z = 40;

    private final ServerLevel sift;
    private final BiConsumer<Boolean, String> check;
    private @Nullable EchoerDeviceBlockEntity straight;
    private @Nullable EchoerDeviceBlockEntity stairs;

    private DrillTest(ServerLevel sift, BiConsumer<Boolean, String> check) {
        this.sift = sift;
        this.check = check;
    }

    static DrillTest start(ServerLevel sift, BiConsumer<Boolean, String> check) {
        DrillTest t = new DrillTest(sift, check);
        t.build();
        return t;
    }

    private void build() {
        this.sift.setChunkForced(X >> 4, Z >> 4, true);
        for (BlockPos p : BlockPos.betweenClosed(X - 2, Y - 8, Z - 3, X + 12, Y + 4, Z + 9)) {
            this.sift.setBlock(p, p.getX() > X ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        this.straight = this.place(new BlockPos(X, Y, Z));
        this.stairs = this.place(new BlockPos(X, Y, Z + 6));
        this.sift.setBlock(new BlockPos(X - 1, Y, Z), Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
    }

    private @Nullable EchoerDeviceBlockEntity place(BlockPos at) {
        this.sift.setBlock(at, ModBlocks.ECHOER_DEVICE.get().defaultBlockState().setValue(EchoerDeviceBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
        if (this.sift.getBlockEntity(at) instanceof EchoerDeviceBlockEntity d) {
            return d;
        }
        this.check.accept(false, "drill: block entity");
        return null;
    }

    void tick(int ticks) {
        if (this.straight != null && (ticks == 10 || ticks == 20)) {
            this.straight.hear(this.sift, null, 6); // a steady tempo (10 ticks apart): straight ahead, 6 deep
        }
        if (this.stairs != null && (ticks == 10 || ticks == 13)) {
            this.stairs.hear(this.sift, null, 4); // a fast tempo (3 ticks apart): stairs down, 4 deep
        }
        if (ticks == 160) {
            this.finish();
        }
    }

    private boolean air(int x, int y, int z) {
        return this.sift.getBlockState(new BlockPos(x, y, z)).isAir();
    }

    private void finish() {
        boolean bored = true;
        for (int i = 1; i <= 6; i++) {
            bored &= this.air(X + i, Y, Z);
        }
        boolean exact = !this.air(X + 7, Y, Z) && !this.air(X + 1, Y + 1, Z) && !this.air(X + 1, Y - 1, Z);
        this.check.accept(bored && exact, "drill: two steady beats bore a straight 1x1 tunnel exactly 6 deep");
        int got = 0;
        if (this.sift.getBlockEntity(new BlockPos(X - 1, Y, Z)) instanceof ChestBlockEntity chest) {
            for (int i = 0; i < chest.getContainerSize(); i++) {
                if (chest.getItem(i).is(Items.COBBLESTONE)) {
                    got += chest.getItem(i).getCount();
                }
            }
        }
        this.check.accept(got == 6, "drill: the drill hands its drops to the chest behind it (" + got + "/6)");
        boolean down = this.air(X + 3, Y - 3, Z + 6) && this.air(X + 3, Y - 1, Z + 6) && !this.air(X + 3, Y - 4, Z + 6)
                && !this.air(X + 5, Y - 5, Z + 6);
        this.check.accept(down, "drill: two fast beats dig a staircase down, 4 steps");
    }
}
