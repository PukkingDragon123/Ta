package com.thesift.block.entity;

import com.thesift.block.EchoerHutHeartBlock;
import com.thesift.registry.ModEchoer;
import com.thesift.registry.ModEntities;
import com.thesift.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Brings an Echoer's hearth to life once: the Echoer and two wild Soul Golems (see {@link EchoerHutHeartBlock}). */
public class EchoerHutHeartBlockEntity extends BlockEntity {
    private static final double WAKE_RANGE = 40.0;

    public EchoerHutHeartBlockEntity(BlockPos pos, BlockState state) {
        super(ModEchoer.ECHOER_HUT_HEART_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EchoerHutHeartBlockEntity heart) {
        if (!(level instanceof ServerLevel server) || server.getGameTime() % 20 != 0 || state.getValue(EchoerHutHeartBlock.SPENT)) {
            return;
        }
        Player player = server.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, WAKE_RANGE, p -> !p.isSpectator());
        if (player != null) {
            populate(server, pos);
            server.setBlock(pos, state.setValue(EchoerHutHeartBlock.SPENT, true), Block.UPDATE_ALL);
        }
    }

    /** The household: the Echoer beside its hearth, a Soul Golem either side. */
    public static void populate(ServerLevel level, BlockPos pos) {
        spawn(level, ModEntities.ENCHOER.get(), pos.offset(0, 1, 2));
        spawn(level, ModEchoer.SOUL_GOLEM.get(), pos.offset(2, 1, -1));
        spawn(level, ModEchoer.SOUL_GOLEM.get(), pos.offset(-2, 1, -1));
        level.sendParticles(ModParticles.RESONANCE_RING.get(), pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 0, 3.0, 0.0, 0.0, 1.0);
    }

    private static void spawn(ServerLevel level, EntityType<? extends Mob> type, BlockPos at) {
        Mob m = type.create(level, EntitySpawnReason.STRUCTURE);
        if (m == null) {
            return;
        }
        m.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
        m.setPersistenceRequired();
        level.addFreshEntity(m);
    }
}
