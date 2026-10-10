package com.thesift.dev;

import com.mojang.datafixers.util.Pair;
import com.thesift.TheSift;
import com.thesift.block.LooseBookshelfBlock;
import com.thesift.block.SecretBookshelfBlock;
import com.thesift.entity.mansion.Bard;
import com.thesift.entity.mansion.Hornblower;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModItems;
import com.thesift.registry.ModMansion;
import com.thesift.worldgen.MansionVaultPiece;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * MANSION: CI checks for the Woodland Mansion's secret room and its people.
 * <ul>
 *   <li>the vault structure is registered and is located in the very chunk of the nearest Woodland Mansion (it shares the
 *   mansions' placement and generation point);</li>
 *   <li>a vault built in the world (rotated): its chest keeps the Sift Symphony, the bookcase door stays shut when the
 *   loose books are pulled out of order and opens when they are pulled from the lowest up, its guards stand in it;</li>
 *   <li>the Hornblower's blast throws a pig back, the giant horn does it for a player, and the Bard's last chord heals
 *   a hurt Vindicator.</li>
 * </ul>
 */
final class MansionTest {
    private MansionTest() {
    }

    static void run(MinecraftServer server, BiConsumer<Boolean, String> check) {
        try {
            ServerLevel level = server.overworld();
            located(level, check);
            vault(server, level, check);
            people(level, check);
        } catch (RuntimeException e) {
            TheSift.LOGGER.warn("SMOKE: mansion test threw", e);
            check.accept(false, "mansion: the test ran without throwing (" + e + ")");
        }
    }

    private static void located(ServerLevel level, BiConsumer<Boolean, String> check) {
        Registry<Structure> registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        Optional<Holder.Reference<Structure>> vault = registry.get(ResourceKey.create(Registries.STRUCTURE, TheSift.id("mansion_vault")));
        Optional<Holder.Reference<Structure>> mansion = registry.get(ResourceKey.create(Registries.STRUCTURE, Identifier.withDefaultNamespace("mansion")));
        check.accept(vault.isPresent(), "mansion: the vault structure is registered");
        if (vault.isEmpty() || mansion.isEmpty()) {
            return;
        }
        Pair<BlockPos, Holder<Structure>> m = level.getChunkSource().getGenerator()
                .findNearestMapStructure(level, HolderSet.direct(mansion.get()), BlockPos.ZERO, 12, false);
        Pair<BlockPos, Holder<Structure>> v = level.getChunkSource().getGenerator()
                .findNearestMapStructure(level, HolderSet.direct(vault.get()), BlockPos.ZERO, 12, false);
        TheSift.LOGGER.info("SMOKE: nearest mansion {} nearest vault {}", m == null ? "none" : m.getFirst(), v == null ? "none" : v.getFirst());
        if (m == null) {
            TheSift.LOGGER.warn("SMOKE: no Woodland Mansion within reach of this seed; the vault's placement is not compared");
            return;
        }
        check.accept(v != null && (v.getFirst().getX() >> 4) == (m.getFirst().getX() >> 4) && (v.getFirst().getZ() >> 4) == (m.getFirst().getZ() >> 4),
                "mansion: the nearest mansion has its vault in the same chunk");
    }

    private static void vault(MinecraftServer server, ServerLevel level, BiConsumer<Boolean, String> check) {
        BlockPos cell = new BlockPos(900, 200, 900);
        for (int cx = (cell.getX() - 24) >> 4; cx <= (cell.getX() + 24) >> 4; cx++) {
            for (int cz = (cell.getZ() - 24) >> 4; cz <= (cell.getZ() + 24) >> 4; cz++) {
                level.setChunkForced(cx, cz, true);
            }
        }
        MansionVaultPiece piece = MansionVaultPiece.vault(cell, Rotation.CLOCKWISE_90);
        piece.placeWhole(level);
        // the chest keeps the Sift Symphony
        BlockPos chest = piece.chestPos();
        ResourceKey<LootTable> key = level.getBlockEntity(chest) instanceof RandomizableContainerBlockEntity c ? c.getLootTable() : null;
        check.accept(ModMansion.VAULT_CHEST.equals(key), "mansion: the vault chest has its loot table (" + key + ")");
        LootTable table = server.reloadableRegistries().getLootTable(ModMansion.VAULT_CHEST);
        List<ItemStack> loot = table.getRandomItems(new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(chest))
                .create(LootContextParamSets.CHEST));
        check.accept(loot.stream().anyMatch(s -> s.is(ModItems.MUSIC_SHEET_SYMPHONY.get())), "mansion: the vault chest keeps the Sift Symphony (" + loot + ")");
        // the bookcase door and its puzzle
        BlockPos door = piece.doorPos();
        check.accept(level.getBlockState(door).is(ModBlocks.SECRET_BOOKSHELF.get()) && !level.getBlockState(door).getValue(SecretBookshelfBlock.OPEN),
                "mansion: the bookcase door is shut");
        FakePlayer player = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(UUID.fromString("5f2b6c1d-7a3e-4b9f-8c2d-1e3f4a5b6c7e"), "[SiftMansion]"));
        player.snapTo(door.getX() + 0.5, door.getY(), door.getZ() + 0.5, 0.0F, 0.0F);
        BlockPos[] books = piece.loosePositions();
        pull(level, player, books[1]); // the middle book first: out of turn
        boolean anyPulled = false;
        for (BlockPos b : books) {
            anyPulled |= level.getBlockState(b).getValue(LooseBookshelfBlock.PULLED);
        }
        check.accept(!anyPulled && !level.getBlockState(door).getValue(SecretBookshelfBlock.OPEN), "mansion: a book pulled out of turn clicks back");
        for (BlockPos b : books) {
            pull(level, player, b);
        }
        check.accept(level.getBlockState(door).getValue(SecretBookshelfBlock.OPEN), "mansion: the books pulled from the lowest up open the door");
        // its guards
        AABB box = AABB.of(piece.getBoundingBox()).inflate(2.0);
        int horns = level.getEntitiesOfClass(Hornblower.class, box).size();
        int bards = level.getEntitiesOfClass(Bard.class, box).size();
        TheSift.LOGGER.info("SMOKE: vault built at {} door {} chest {} guards {} + {}", cell, door, chest, horns, bards);
        check.accept(horns >= 1 && bards >= 1, "mansion: the vault's Hornblower and Bard stand guard");
        level.getEntitiesOfClass(Mob.class, box).forEach(Mob::discard);
    }

    private static void pull(ServerLevel level, FakePlayer player, BlockPos book) {
        level.getBlockState(book).useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(book), Direction.UP, book, false));
    }

    private static void people(ServerLevel level, BiConsumer<Boolean, String> check) {
        BlockPos at = new BlockPos(900, 230, 940);
        level.setChunkForced(at.getX() >> 4, at.getZ() >> 4, true);
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-6, -1, -6), at.offset(6, 4, 6))) {
            level.setBlock(p, p.getY() < at.getY() ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        // the Hornblower's blast: a pig three blocks in front of him is thrown back
        Hornblower horn = ModMansion.HORNBLOWER.get().create(level, EntitySpawnReason.COMMAND);
        Mob pig = EntityTypes.PIG.create(level, EntitySpawnReason.COMMAND);
        if (horn == null || pig == null) {
            check.accept(false, "mansion: a Hornblower and a pig are created");
            return;
        }
        horn.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 180.0F, 0.0F); // facing north (-z)
        horn.setYHeadRot(180.0F);
        horn.setNoAi(true);
        pig.snapTo(at.getX() + 0.5, at.getY(), at.getZ() - 2.5, 0.0F, 0.0F);
        pig.setNoAi(true);
        level.addFreshEntity(horn);
        level.addFreshEntity(pig);
        horn.blast(level);
        Vec3 push = pig.getDeltaMovement();
        TheSift.LOGGER.info("SMOKE: hornblower blast pushed the pig {}", push);
        check.accept(push.z < -0.3 && push.y > 0.1, "mansion: the Hornblower's blast throws what is before him back (" + push + ")");
        horn.discard();
        // the giant horn in a player's hands does the same
        pig.setDeltaMovement(Vec3.ZERO);
        FakePlayer player = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(UUID.fromString("5f2b6c1d-7a3e-4b9f-8c2d-1e3f4a5b6c7f"), "[SiftHorn]"));
        player.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 180.0F, 0.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GIANT_GOAT_HORN.get()));
        player.getMainHandItem().use(level, player, InteractionHand.MAIN_HAND);
        Vec3 push2 = pig.getDeltaMovement();
        check.accept(push2.z < -0.3, "mansion: the Giant Goat Horn throws what is before you back (" + push2 + ")");
        check.accept(player.getCooldowns().isOnCooldown(player.getMainHandItem()), "mansion: the Giant Goat Horn needs a breath after a blast");
        pig.discard();
        // the Bard's last chord heals a hurt illager
        Bard bard = ModMansion.BARD.get().create(level, EntitySpawnReason.COMMAND);
        Mob vindicator = EntityTypes.VINDICATOR.create(level, EntitySpawnReason.COMMAND);
        if (bard == null || vindicator == null) {
            check.accept(false, "mansion: a Bard and a Vindicator are created");
            return;
        }
        bard.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 3.5, 0.0F, 0.0F);
        bard.setNoAi(true);
        vindicator.snapTo(at.getX() + 2.5, at.getY(), at.getZ() + 3.5, 0.0F, 0.0F);
        vindicator.setNoAi(true);
        level.addFreshEntity(bard);
        level.addFreshEntity(vindicator);
        vindicator.setHealth(6.0F);
        bard.playHealingChord(level);
        TheSift.LOGGER.info("SMOKE: bard healed the vindicator to {}", vindicator.getHealth());
        check.accept(vindicator.getHealth() > 6.0F, "mansion: the Bard's song heals a hurt illager");
        bard.discard();
        vindicator.discard();
    }
}
