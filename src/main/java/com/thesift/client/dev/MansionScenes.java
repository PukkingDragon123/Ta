package com.thesift.client.dev;

import com.thesift.block.SculkSummonerBlock;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModItems;
import com.thesift.registry.ModMansion;
import com.thesift.worldgen.MansionVaultPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * MANSION: the client smoke test's scenes for the mansion's secret room, the Sculk Summoner and the two Pillagers. Each
 * builds its set on the server thread and returns the camera (eye x, y, z, then the point it looks at).
 */
final class MansionScenes {
    private MansionScenes() {
    }

    private static void fill(ServerLevel level, int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
        for (BlockPos p : BlockPos.betweenClosed(x0, y0, z0, x1, y1, z1)) {
            level.setBlock(p, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    /** The secret room built as in a mansion (unrotated: u east, v south), with the corridor round the bookcase. */
    private static MansionVaultPiece vault(ServerLevel level, int stageY) {
        BlockPos cell = new BlockPos(150, stageY + 2, -40);
        fill(level, cell.getX() - 14, cell.getY() - 8, cell.getZ() - 3, cell.getX() + 9, cell.getY() + 9, cell.getZ() + 12, Blocks.AIR.defaultBlockState());
        // the mansion's corridor: its floor, its outer walls north and east, a ceiling
        fill(level, cell.getX() - 1, cell.getY(), cell.getZ() - 1, cell.getX() + 7, cell.getY(), cell.getZ() + 7, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        fill(level, cell.getX() - 1, cell.getY() + 1, cell.getZ() - 1, cell.getX() + 7, cell.getY() + 7, cell.getZ() - 1, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        fill(level, cell.getX() + 7, cell.getY() + 1, cell.getZ() - 1, cell.getX() + 7, cell.getY() + 7, cell.getZ() + 7, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        fill(level, cell.getX() - 1, cell.getY() + 8, cell.getZ() - 1, cell.getX() + 7, cell.getY() + 8, cell.getZ() + 7, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        level.getEntitiesOfClass(Mob.class, new AABB(cell).inflate(16.0)).forEach(Mob::discard);
        MansionVaultPiece piece = MansionVaultPiece.vault(cell, Rotation.NONE);
        piece.placeWhole(level);
        for (Mob m : level.getEntitiesOfClass(Mob.class, new AABB(cell).inflate(16.0))) {
            m.setNoAi(true);
        }
        return piece;
    }

    /** The corridor's tall bookcase - three loose books, the door at its east end - seen from the corridor. */
    static double[] bookcase(ServerLevel level, int stageY) {
        MansionVaultPiece p = vault(level, stageY);
        BlockPos eye = p.at(3, 2, 6);
        BlockPos look = p.at(3, 3, 1);
        return new double[]{eye.getX() + 0.5, eye.getY() + 0.6, eye.getZ() + 0.5, look.getX() + 0.5, look.getY() + 0.5, look.getZ() + 0.5};
    }

    /** The vault under the mansion: the library wall and the Symphony's chest, the practice summoner, the guards. */
    static double[] vaultInside(ServerLevel level, int stageY) {
        MansionVaultPiece p = vault(level, stageY);
        BlockPos eye = p.at(-2, -4, 8);
        BlockPos look = p.at(-7, -5, 1);
        return new double[]{eye.getX() + 0.5, eye.getY() + 0.7, eye.getZ() + 0.2, look.getX() + 0.5, look.getY() + 0.5, look.getZ() + 0.5};
    }

    /** A Sift gate frame with an armed Sculk Summoner before it, ringed by its four Sculk Sensors. */
    static double[] summoner(ServerLevel level, int stageY) {
        int x0 = 200;
        int z = -40;
        int y = stageY;
        fill(level, x0 - 8, y - 1, z - 4, x0 + 10, y - 1, z + 10, Blocks.DEEPSLATE_TILES.defaultBlockState());
        fill(level, x0 - 8, y, z - 4, x0 + 10, y + 7, z + 10, Blocks.AIR.defaultBlockState());
        BlockState frame = ModBlocks.SIFT_GATE_FRAME.get().defaultBlockState();
        for (int dx = -1; dx <= 3; dx++) {
            for (int dy = 0; dy <= 5; dy++) {
                if (dx == -1 || dx == 3 || dy == 0 || dy == 5) {
                    level.setBlock(new BlockPos(x0 + dx, y + dy, z), frame, Block.UPDATE_CLIENTS);
                }
            }
        }
        BlockPos at = new BlockPos(x0 + 1, y, z + 4);
        level.setBlock(at, Blocks.SCULK_CATALYST.defaultBlockState(), Block.UPDATE_ALL);
        for (BlockPos s : new BlockPos[]{at.east(3), at.west(3), at.south(2).east(2), at.south(2).west(2)}) {
            level.setBlock(s, Blocks.SCULK_SENSOR.defaultBlockState(), Block.UPDATE_ALL);
        }
        SculkSummonerBlock.arm(level, at, null);
        return new double[]{at.getX() + 3.5, y + 3.2, at.getZ() + 6.0, at.getX() + 0.5, y + 1.5, z + 0.5};
    }

    /** The Hornblower with the horn at his lips, and the Bard playing beside him. */
    static double[] pillagers(ServerLevel level, int stageY) {
        int x0 = 230;
        int z = -40;
        int y = stageY;
        fill(level, x0 - 5, y - 1, z - 5, x0 + 5, y - 1, z + 5, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        fill(level, x0 - 5, y, z - 5, x0 + 5, y + 4, z + 5, Blocks.AIR.defaultBlockState());
        level.getEntitiesOfClass(Mob.class, new AABB(new BlockPos(x0, y, z)).inflate(8.0)).forEach(Mob::discard);
        pose(level, ModMansion.HORNBLOWER.get().create(level, EntitySpawnReason.COMMAND), x0 - 1.0, y, z, new ItemStack(ModItems.GIANT_GOAT_HORN.get()));
        pose(level, ModMansion.BARD.get().create(level, EntitySpawnReason.COMMAND), x0 + 1.4, y, z, new ItemStack(ModItems.GUITAR.get()));
        return new double[]{x0 + 0.2, y + 2.2, z - 4.2, x0 + 0.2, y + 1.2, z + 0.5};
    }

    private static void pose(ServerLevel level, AbstractIllager mob, double x, int y, int z, ItemStack held) {
        if (mob == null) {
            return;
        }
        mob.snapTo(x, y, z + 0.5, 180.0F, 0.0F);
        mob.setYHeadRot(180.0F);
        mob.setYBodyRot(180.0F);
        mob.setNoAi(true);
        mob.setPersistenceRequired();
        mob.setItemSlot(EquipmentSlot.MAINHAND, held);
        level.addFreshEntity(mob);
        mob.startUsingItem(InteractionHand.MAIN_HAND);
    }
}
