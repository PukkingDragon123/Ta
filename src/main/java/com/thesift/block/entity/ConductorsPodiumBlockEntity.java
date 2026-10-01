package com.thesift.block.entity;

import com.thesift.entity.boss.Dictator;
import com.thesift.registry.ModBlockEntities;
import com.thesift.registry.ModEntities;
import com.thesift.registry.ModSounds;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** Wakes the Dictator when a player reaches the top of his castle; remembers when he is gone for good. */
public class ConductorsPodiumBlockEntity extends BlockEntity {
    private static final double WAKE_RANGE = 11.0;

    private boolean defeated;
    private @Nullable UUID boss;

    public ConductorsPodiumBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONDUCTORS_PODIUM.get(), pos, state);
    }

    public boolean isDefeated() {
        return this.defeated;
    }

    public void setDefeated() {
        this.defeated = true;
        this.boss = null;
        this.setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ConductorsPodiumBlockEntity podium) {
        if (podium.defeated || !(level instanceof ServerLevel server) || server.getGameTime() % 10 != 0) {
            return;
        }
        if (podium.boss != null) {
            Entity e = server.getEntity(podium.boss);
            if (e != null && e.isAlive()) {
                return;
            }
        }
        Player player = server.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, WAKE_RANGE,
                p -> p instanceof Player pl && !pl.isCreative() && !pl.isSpectator());
        if (player != null) {
            podium.wake(server, pos, player);
        }
    }

    /** Raises the Dictator behind the podium, facing whoever climbed up. */
    public @Nullable Dictator wake(ServerLevel level, BlockPos pos, @Nullable Player player) {
        Dictator d = ModEntities.DICTATOR.get().create(level, EntitySpawnReason.TRIGGERED);
        if (d == null) {
            return null;
        }
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 2.5;
        float yaw = 180.0F;
        if (player != null) {
            double dx = player.getX() - x;
            double dz = player.getZ() - z;
            yaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        }
        d.snapTo(x, pos.getY(), z, yaw, 0.0F);
        d.setPodium(pos);
        level.addFreshEntity(d);
        this.boss = d.getUUID();
        this.setChanged();
        level.sendParticles(ParticleTypes.SCULK_SOUL, x, pos.getY() + 1.5, z, 60, 0.8, 1.6, 0.8, 0.06);
        level.playSound(null, pos, ModSounds.DICTATOR_ROAR.get(), SoundSource.HOSTILE, 5.0F, 0.9F);
        for (Player p : level.players()) {
            if (p.distanceToSqr(x, pos.getY(), z) < 48 * 48) {
                p.sendOverlayMessage(Component.translatable("message.thesift.dictator.wakes"));
            }
        }
        return d;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("Defeated", this.defeated);
        if (this.boss != null) {
            output.putString("Boss", this.boss.toString());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.defeated = input.getBooleanOr("Defeated", false);
        String b = input.getStringOr("Boss", "");
        this.boss = b.isEmpty() ? null : UUID.fromString(b);
    }
}
