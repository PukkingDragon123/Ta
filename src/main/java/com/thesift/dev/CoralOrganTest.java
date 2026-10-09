package com.thesift.dev;

import com.thesift.TheSift;
import com.thesift.entity.CoralOrgan;
import com.thesift.registry.ModEntities;
import com.thesift.registry.ModSculkSea;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * WATER checks for the CI smoke test: the Sculk Coral Organ is a hostile monster, and in a tub of water it notices
 * a fish swimming in front of it, gapes (the telegraph) while the fish is still unharmed, and then bites it.
 */
final class CoralOrganTest {
    private CoralOrganTest() {
    }

    static void run(ServerLevel sift, BiConsumer<Boolean, String> check) {
        CoralOrgan organ = ModSculkSea.CORAL_ORGAN.get().create(sift, EntitySpawnReason.COMMAND);
        Mob fish = ModEntities.KAZOO_FISH.get().create(sift, EntitySpawnReason.COMMAND);
        if (organ == null || fish == null) {
            check.accept(false, "coral organ: created");
            return;
        }
        check.accept(organ instanceof Enemy && organ.getType().getCategory() == MobCategory.MONSTER, "coral organ: a hostile monster");
        if (sift.getDifficulty() == Difficulty.PEACEFUL) {
            TheSift.LOGGER.info("SMOKE: peaceful - the coral organ's bite is not tested");
            return;
        }
        int x = 44, z = -44;
        int y = sift.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 4;
        // a little sea: a stone tub full of water
        for (BlockPos p : BlockPos.betweenClosed(x - 4, y - 1, z - 6, x + 4, y + 5, z + 4)) {
            boolean wall = p.getY() == y - 1 || p.getX() == x - 4 || p.getX() == x + 4 || p.getZ() == z - 6 || p.getZ() == z + 4;
            sift.setBlock(p, wall ? Blocks.STONE.defaultBlockState() : Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        organ.snapTo(x + 0.5, y, z + 0.5, 180.0F, 0.0F); // facing north, its mouth towards the fish
        fish.snapTo(x + 0.5, y + 1.0, z - 2.0, 0.0F, 0.0F);
        fish.setNoAi(true);
        sift.addFreshEntity(organ);
        sift.addFreshEntity(fish);
        float hp = fish.getHealth();
        boolean telegraphed = false;
        boolean bitten = false;
        for (int t = 0; t < 600 && !bitten; t++) {
            organ.tick();
            bitten = !fish.isAlive() || fish.getHealth() < hp;
            if (organ.isGaping() && !bitten) {
                telegraphed = true;
            }
        }
        TheSift.LOGGER.info("SMOKE: coral organ telegraphed {} bitten {}", telegraphed, bitten);
        check.accept(telegraphed, "coral organ: its mouth gapes (the telegraph) before it bites");
        check.accept(bitten, "coral organ: it bites prey swimming in front of it");
        organ.discard();
        fish.discard();
    }
}
