package com.thesift.block.entity;

import com.thesift.entity.boss.ConductorMask;
import com.thesift.entity.boss.Dictator;
import com.thesift.music.Performance;
import com.thesift.registry.ModBlockEntities;
import com.thesift.registry.ModEntities;
import com.thesift.registry.ModItems;
import com.thesift.registry.ModSounds;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The heart of the Grand Stage. It waits for the three Instrument Altars around it to be filled;
 * then the instruments play together - the performance (see {@link Performance}) - the Mask rises
 * from beneath the stage and becomes the Conductor. When he is beaten the instruments fall silent
 * and can be taken back; the stage may be played again.
 */
public class ConductorsPodiumBlockEntity extends BlockEntity {
    private static final int ALTAR_RANGE = 7;

    private boolean defeated;
    private @Nullable UUID boss;
    /** -1 while waiting; otherwise ticks into the performance. */
    private int performance = -1;

    public ConductorsPodiumBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONDUCTORS_PODIUM.get(), pos, state);
    }

    public boolean isDefeated() {
        return this.defeated;
    }

    public int performanceTick() {
        return this.performance;
    }

    public void setDefeated() {
        this.defeated = true;
        this.boss = null;
        this.performance = -1;
        for (InstrumentAltarBlockEntity a : this.altars()) {
            a.setLocked(false);
        }
        this.sync();
    }

    private List<InstrumentAltarBlockEntity> altars() {
        List<InstrumentAltarBlockEntity> out = new ArrayList<>();
        if (this.level == null) {
            return out;
        }
        for (BlockPos p : BlockPos.betweenClosed(this.worldPosition.offset(-ALTAR_RANGE, -2, -ALTAR_RANGE), this.worldPosition.offset(ALTAR_RANGE, 2, ALTAR_RANGE))) {
            if (this.level.getBlockEntity(p) instanceof InstrumentAltarBlockEntity a) {
                out.add(a);
            }
        }
        return out;
    }

    /** True when the drum, the flute and the guitar are all on their altars. */
    private boolean allThree() {
        boolean drum = false;
        boolean flute = false;
        boolean guitar = false;
        for (InstrumentAltarBlockEntity a : this.altars()) {
            ItemStack s = a.getItem();
            drum |= s.is(ModItems.CONGA_DRUM.get());
            flute |= s.is(ModItems.CRANE_FLUTE.get());
            guitar |= s.is(ModItems.WEAVER_GUITAR.get());
        }
        return drum && flute && guitar;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ConductorsPodiumBlockEntity podium) {
        if (level.isClientSide()) {
            if (podium.performance >= 0) {
                podium.performance++;
                Performance.clientStage = pos;
                Performance.clientTick = podium.performance;
                Performance.clientSeen = level.getGameTime();
            }
            return;
        }
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (podium.performance >= 0) {
            podium.perform(server, pos);
            return;
        }
        if (server.getGameTime() % 10 != 0) {
            return;
        }
        if (podium.boss != null) {
            Entity e = server.getEntity(podium.boss);
            if (e != null && e.isAlive()) {
                return;
            }
            // the Conductor is gone without being beaten (despawned or removed): reopen the stage
            podium.boss = null;
            for (InstrumentAltarBlockEntity a : podium.altars()) {
                a.setLocked(false);
            }
            podium.sync();
        }
        if (podium.allThree()) {
            podium.begin(server, pos);
        }
    }

    private void begin(ServerLevel level, BlockPos pos) {
        this.performance = 0;
        this.defeated = false;
        for (InstrumentAltarBlockEntity a : this.altars()) {
            a.setLocked(true);
        }
        for (Player p : level.players()) {
            if (p.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 64 * 64) {
                p.sendOverlayMessage(Component.translatable("message.thesift.stage.begins"));
            }
        }
        this.sync();
    }

    private void perform(ServerLevel level, BlockPos pos) {
        int t = this.performance++;
        if (t % Performance.BAR == 0) {
            level.sendParticles(com.thesift.registry.ModParticles.RESONANCE_RING.get(), pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 0,
                    3.0 + (t / Performance.BAR) * 0.4, 0.0, 0.0, 1.0);
        }
        if (t == Performance.MASK_RISES) {
            ConductorMask mask = ModEntities.CONDUCTOR_MASK.get().create(level, EntitySpawnReason.TRIGGERED);
            if (mask != null) {
                mask.snapTo(pos.getX() + 0.5, pos.getY() - 2.5, pos.getZ() + 0.5, 180.0F, 0.0F);
                mask.setStage(pos, Performance.LENGTH - Performance.MASK_RISES);
                level.addFreshEntity(mask);
            }
        }
        if (t >= Performance.LENGTH + 40) {
            // the Mask never made it (removed?): the Conductor comes anyway
            this.wake(level, pos, null);
        }
    }

    /** Called by the Mask when it becomes the Conductor. */
    public void bossRaised(Dictator d) {
        this.boss = d.getUUID();
        this.performance = -1;
        this.sync();
    }

    /** Raises the Conductor straight away, skipping the performance (tests and commands). */
    public @Nullable Dictator wake(ServerLevel level, BlockPos pos, @Nullable Player player) {
        Dictator d = ModEntities.DICTATOR.get().create(level, EntitySpawnReason.TRIGGERED);
        if (d == null) {
            return null;
        }
        d.snapTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 2.5, 180.0F, 0.0F);
        d.setPodium(pos);
        level.addFreshEntity(d);
        this.bossRaised(d);
        level.sendParticles(ParticleTypes.SCULK_SOUL, d.getX(), d.getY() + 1.5, d.getZ(), 60, 0.8, 1.6, 0.8, 0.06);
        level.playSound(null, pos, ModSounds.DICTATOR_ROAR.get(), SoundSource.HOSTILE, 5.0F, 0.9F);
        return d;
    }

    private void sync() {
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("Defeated", this.defeated);
        output.putInt("Performance", this.performance);
        if (this.boss != null) {
            output.putString("Boss", this.boss.toString());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.defeated = input.getBooleanOr("Defeated", false);
        this.performance = input.getIntOr("Performance", -1);
        String b = input.getStringOr("Boss", "");
        this.boss = b.isEmpty() ? null : UUID.fromString(b);
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }
}
