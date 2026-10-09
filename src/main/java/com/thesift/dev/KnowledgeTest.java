package com.thesift.dev;

import com.mojang.authlib.GameProfile;
import com.thesift.TheSift;
import com.thesift.entity.MiniCreator;
import com.thesift.knowledge.Glyphs;
import com.thesift.knowledge.Knowledge;
import com.thesift.knowledge.KnowledgeTracker;
import com.thesift.knowledge.Lore;
import com.thesift.knowledge.Quest;
import com.thesift.registry.ModItems;
import com.thesift.registry.ModKnowledge;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * F3 Knowledge &amp; lore checks for the CI smoke test: discoveries are recorded once, survive a save and load of the
 * player and a death (the attachment is copied to the respawned player), the inventory scan records items, reading a
 * Lore Scroll records it and deciphers more of the ancient script, a placed Lore Book keeps its text, and meeting the
 * Mini Creator starts the quests and hands over a Knowledge Book.
 */
final class KnowledgeTest {
    private KnowledgeTest() {
    }

    static void run(ServerLevel sift, BiConsumer<Boolean, String> check) {
        FakePlayer player = FakePlayerFactory.get(sift, new GameProfile(UUID.fromString("5f3a0c1e-6b0d-4e8e-9a3c-0d1f2e3a4b5c"), "knowledge_test"));
        try {
            unlocks(sift, player, check);
            lore(sift, player, check);
            guide(sift, player, check);
        } catch (RuntimeException e) {
            TheSift.LOGGER.error("SMOKE: knowledge test threw", e);
            check.accept(false, "knowledge: no exception (" + e + ")");
        }
    }

    private static void unlocks(ServerLevel sift, FakePlayer player, BiConsumer<Boolean, String> check) {
        check.accept(Knowledge.unlock(player, "entity:thesift:bulb", true), "knowledge: a first discovery is new");
        check.accept(!Knowledge.unlock(player, "entity:thesift:bulb", true), "knowledge: a discovery is recorded once");
        Knowledge.unlock(player, "song:lullaby", true);
        check.accept(Knowledge.has(player, "entity:thesift:bulb") && Knowledge.has(player, "song:lullaby"), "knowledge: has() sees discoveries");

        // saved with the player and read back into a fresh one
        TagValueOutput out = TagValueOutput.createWithContext(new ProblemReporter.Collector(), sift.registryAccess());
        player.saveWithoutId(out);
        CompoundTag tag = out.buildResult();
        TheSift.LOGGER.info("SMOKE: knowledge saved as {}", tag.toString().contains("song:lullaby") ? "present" : "MISSING");
        check.accept(tag.toString().contains("entity:thesift:bulb") && tag.toString().contains("song:lullaby"), "knowledge: saved with the player");
        FakePlayer loaded = FakePlayerFactory.get(sift, new GameProfile(UUID.fromString("5f3a0c1e-6b0d-4e8e-9a3c-0d1f2e3a4b5d"), "knowledge_load"));
        loaded.load(TagValueInput.create(new ProblemReporter.Collector(), sift.registryAccess(), tag));
        check.accept(Knowledge.has(loaded, "entity:thesift:bulb") && Knowledge.has(loaded, "song:lullaby"), "knowledge: loaded back from the save");

        // kept through death (copyOnDeath): what a respawned player gets
        FakePlayer respawned = FakePlayerFactory.get(sift, new GameProfile(UUID.fromString("5f3a0c1e-6b0d-4e8e-9a3c-0d1f2e3a4b5e"), "knowledge_dead"));
        respawned.copyAttachmentsFrom(player, true);
        check.accept(Knowledge.has(respawned, "song:lullaby"), "knowledge: kept through death");

        // the scan records the Sift items you carry
        player.getInventory().setItem(0, new ItemStack(ModItems.CHROME_PEARL.get()));
        KnowledgeTracker.scan(player);
        check.accept(Knowledge.has(player, "item:thesift:chrome_pearl"), "knowledge: carried items are recorded");
        player.getInventory().setItem(0, ItemStack.EMPTY);
    }

    private static void lore(ServerLevel sift, FakePlayer player, BiConsumer<Boolean, String> check) {
        float before = Glyphs.level(Knowledge.clues(player));
        ItemStack scroll = Lore.SOUL_CREATOR.stack();
        check.accept(scroll.is(ModItems.SOUL_LORE_SCROLL.get()) && Lore.of(scroll) == Lore.SOUL_CREATOR, "knowledge: a lore scroll carries its text");
        player.setItemInHand(InteractionHand.MAIN_HAND, scroll);
        scroll.getItem().use(sift, player, InteractionHand.MAIN_HAND);
        check.accept(Knowledge.has(player, Lore.SOUL_CREATOR.key()), "knowledge: reading a scroll records it");
        check.accept(Glyphs.level(Knowledge.clues(player)) > before, "knowledge: each piece read deciphers more of the script");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

        // a placed Lore Book keeps its text
        int x = 26;
        int z = -26;
        int y = sift.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 2;
        BlockPos at = new BlockPos(x, y, z);
        sift.setBlock(at.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        sift.setBlock(at, com.thesift.registry.ModBlocks.CREATOR_LORE_BOOK.get().defaultBlockState(), Block.UPDATE_ALL);
        if (sift.getBlockEntity(at) instanceof com.thesift.block.entity.LoreBookBlockEntity book) {
            book.applyComponentsFromItemStack(Lore.CREATOR_HALL.stack());
            check.accept(Lore.CREATOR_HALL.id().equals(book.lore()), "knowledge: a placed lore book keeps its text");
        } else {
            check.accept(false, "knowledge: a placed lore book has its block entity");
        }
        sift.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    private static void guide(ServerLevel sift, FakePlayer player, BiConsumer<Boolean, String> check) {
        player.snapTo(30.5, sift.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 30, -30), -29.5, 0.0F, 0.0F);
        MiniCreator guide = ModKnowledge.MINI_CREATOR.get().create(sift, EntitySpawnReason.COMMAND);
        if (guide == null) {
            check.accept(false, "knowledge: mini creator created");
            return;
        }
        guide.snapTo(player.getX() + 2.0, player.getY(), player.getZ(), 0.0F, 0.0F);
        sift.addFreshEntity(guide);
        guide.interact(player, InteractionHand.MAIN_HAND, guide.position());
        check.accept(Knowledge.has(player, Quest.ARRIVAL.doneKey()), "knowledge: meeting the mini creator starts the quests");
        check.accept(guide.isGuiding(player), "knowledge: the mini creator guides whoever greets him");
        boolean book = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            book |= player.getInventory().getItem(i).is(ModItems.KNOWLEDGE_BOOK.get());
        }
        check.accept(book, "knowledge: the mini creator hands over a knowledge book");
        check.accept(Quest.current(player) == Quest.INSTRUMENT, "knowledge: the first goal is an instrument");
        check.accept(!guide.hurtServer(sift, sift.damageSources().generic(), 5.0F), "knowledge: the mini creator cannot be hurt");
        guide.discard();
    }
}
