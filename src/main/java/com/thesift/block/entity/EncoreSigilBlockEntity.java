package com.thesift.block.entity;

import com.thesift.block.EncoreSigilBlock;
import com.thesift.registry.ModBlockEntities;
import com.thesift.registry.ModEntities;
import com.thesift.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Calls its mini-boss when a player (not in creative) comes within range, once. */
public class EncoreSigilBlockEntity extends BlockEntity {
    private static final double WAKE_RANGE = 10.0;

    public EncoreSigilBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENCORE_SIGIL.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EncoreSigilBlockEntity sigil) {
        if (!(level instanceof ServerLevel server) || server.getGameTime() % 10 != 0 || state.getValue(EncoreSigilBlock.SPENT)) {
            return;
        }
        Player player = server.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, WAKE_RANGE,
                p -> p instanceof Player pl && !pl.isCreative() && !pl.isSpectator());
        if (player != null) {
            summon(server, pos, state.getValue(EncoreSigilBlock.BOSS), player);
            server.setBlock(pos, state.setValue(EncoreSigilBlock.SPENT, true), Block.UPDATE_ALL);
        }
    }

    public static Mob summon(ServerLevel level, BlockPos pos, int boss, Player player) {
        EntityType<? extends Mob> type = switch (boss) {
            case 1 -> ModEntities.WHISTLER.get();
            case 2 -> ModEntities.STRUMMER.get();
            default -> ModEntities.THUMPER.get();
        };
        Mob m = type.create(level, EntitySpawnReason.TRIGGERED);
        if (m == null) {
            return null;
        }
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        float yaw = (float) (Math.atan2(player.getZ() - z, player.getX() - x) * (180.0 / Math.PI)) - 90.0F;
        m.snapTo(x, pos.getY() + 1.0, z, yaw, 0.0F);
        m.setTarget(player);
        level.addFreshEntity(m);
        level.sendParticles(ModParticles.RESONANCE_RING.get(), x, pos.getY() + 1.1, z, 0, 4.0, 0.0, 0.0, 1.0);
        level.sendParticles(ParticleTypes.SCULK_SOUL, x, pos.getY() + 1.5, z, 40, 1.0, 1.0, 1.0, 0.05);
        level.sendParticles(ModParticles.SIFT_NOTE.get(), x, pos.getY() + 2.5, z, 20, 1.5, 1.0, 1.5, 1.0);
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.HOSTILE, 3.0F, 0.5F);
        level.playSound(null, pos, SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 2.0F, 1.2F);
        for (Player p : level.players()) {
            if (p.distanceToSqr(x, pos.getY(), z) < 48 * 48) {
                p.sendOverlayMessage(Component.translatable("message.thesift.encore." + boss));
            }
        }
        return m;
    }
}
