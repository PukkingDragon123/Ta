package com.thesift.dev;

import com.thesift.TheSift;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModEntities;
import com.thesift.registry.ModItems;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/** A3 Chrome checks for the CI smoke test: Chrome meeting water makes Chime Sand, and a Chrome Bucket scoops up a fish. */
final class ChromeTest {
    private ChromeTest() {
    }

    static void run(ServerLevel sift, BiConsumer<Boolean, String> check) {
        int x = 20, z = -20;
        int y = sift.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 3;
        for (BlockPos p : BlockPos.betweenClosed(x - 2, y - 1, z - 2, x + 2, y + 2, z + 2)) {
            sift.setBlock(p, p.getY() == y - 1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        // still Chrome beside still water is left alone; flowing water turns it to Chime Sand
        BlockPos chrome = new BlockPos(x, y, z);
        sift.setBlock(chrome, ModBlocks.CHROME.get().defaultBlockState(), Block.UPDATE_ALL);
        sift.setBlock(chrome.west(), Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
        check.accept(!sift.getBlockState(chrome).is(ModBlocks.CHIME_SAND.get()), "chrome: still water leaves still chrome alone");
        sift.setBlock(chrome.east(), Fluids.FLOWING_WATER.getFlowing(7, false).createLegacyBlock(), Block.UPDATE_ALL);
        check.accept(sift.getBlockState(chrome).is(ModBlocks.CHIME_SAND.get()), "chrome: flowing water makes chime sand");

        Mob fish = ModEntities.KAZOO_FISH.get().create(sift, EntitySpawnReason.COMMAND);
        if (fish == null) {
            check.accept(false, "chrome: kazoo fish created");
            return;
        }
        fish.snapTo(x + 0.5, y + 1.0, z + 0.5, 0.0F, 0.0F);
        sift.addFreshEntity(fish);
        FakePlayer player = FakePlayerFactory.getMinecraft(sift);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CHROME_BUCKET.get()));
        try {
            fish.interact(player, InteractionHand.MAIN_HAND, fish.position());
        } catch (RuntimeException e) {
            TheSift.LOGGER.warn("SMOKE: chrome bucket pickup threw", e);
        }
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        TheSift.LOGGER.info("SMOKE: chrome bucket on a kazoo fish gave {}", held);
        check.accept(held.is(ModItems.CHROME_KAZOO_FISH_BUCKET.get()) && fish.isRemoved(), "chrome: a chrome bucket scoops up a kazoo fish");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    }
}
