package com.thesift.block.entity;

import com.thesift.block.BossDenBlock;
import com.thesift.entity.boss.ConductorMask;
import com.thesift.entity.boss.Dictator;
import com.thesift.music.Performance;
import com.thesift.registry.ModBlockEntities;
import com.thesift.registry.ModEntities;
import com.thesift.registry.ModItems;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
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
 * RR: the hidden heart of a boss's den (see {@link BossDenBlock}). It merges what the Encore Sigil and the
 * Conductor's Podium did, without their altar blocks:
 * <ul>
 *   <li>A den (BOSS 0 the Thumper, 2 the Weaver): the first player (not in creative) within ten blocks wakes its
 *   boss, once; then the den is spent.</li>
 *   <li>The Grand Stage (BOSS 3): when a player carrying the Conga Drum, the Crane Flute and the Weaver's Guitar
 *   steps onto the stage, the three instruments play together - the performance (see {@link Performance}) - the
 *   Mask rises from beneath the stage and becomes the Conductor. When he is beaten the stage waits until the
 *   players have stepped off it, then it may be played again.</li>
 * </ul>
 */
public class BossDenBlockEntity extends BlockEntity {
    private static final double WAKE_RANGE = 10.0;
    private static final double STAGE_RANGE = 7.0;

    private boolean defeated;
    private @Nullable UUID boss;
    /** -1 while waiting; otherwise ticks into the performance. */
    private int performance = -1;
    /** The stage is ready for a new performance once nobody carrying the three instruments stands on it. */
    private boolean armed = true;

    public BossDenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BOSS_DEN.get(), pos, state);
    }

    // ------------------------------------------------------------------ the dens (Thumper, Weaver)

    public static void denTick(Level level, BlockPos pos, BlockState state, BossDenBlockEntity den) {
        if (!(level instanceof ServerLevel server) || server.getGameTime() % 10 != 0 || state.getValue(BossDenBlock.SPENT)) {
            return;
        }
        Player player = server.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, WAKE_RANGE,
                p -> p instanceof Player pl && !pl.isCreative() && !pl.isSpectator());
        if (player != null) {
            summon(server, pos, state.getValue(BossDenBlock.BOSS), player);
            server.setBlock(pos, state.setValue(BossDenBlock.SPENT, true), Block.UPDATE_ALL);
        }
    }

    public static @Nullable Mob summon(ServerLevel level, BlockPos pos, int den, Player player) {
        // 0 the Thumper, 2 the Weaver (1 was the Whistler, who is gone: old dens wake the Thumper)
        int which = den == 2 ? 2 : 0;
        EntityType<? extends Mob> type = which == 2 ? ModEntities.STRUMMER.get() : ModEntities.THUMPER.get();
        Mob m = type.create(level, EntitySpawnReason.TRIGGERED);
        if (m == null) {
            return null;
        }
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        float yaw = (float) (Math.atan2(player.getZ() - z, player.getX() - x) * (180.0 / Math.PI)) - 90.0F;
        // the den sits in the air just above the floor: the boss comes up right there
        m.snapTo(x, pos.getY(), z, yaw, 0.0F);
        m.setTarget(player);
        if (m instanceof com.thesift.entity.boss.Thumper th) {
            // it was asleep under the arena: it crawls up out of the floor
            th.beginEmerge();
        }
        level.addFreshEntity(m);
        level.sendParticles(ModParticles.RESONANCE_RING.get(), x, pos.getY() + 0.1, z, 0, 4.0, 0.0, 0.0, 1.0);
        level.sendParticles(ParticleTypes.SCULK_SOUL, x, pos.getY() + 0.5, z, 40, 1.0, 1.0, 1.0, 0.05);
        level.sendParticles(ModParticles.SIFT_NOTE.get(), x, pos.getY() + 1.5, z, 20, 1.5, 1.0, 1.5, 1.0);
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.HOSTILE, 3.0F, 0.5F);
        level.playSound(null, pos, SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 2.0F, 1.2F);
        for (Player p : level.players()) {
            if (p.distanceToSqr(x, pos.getY(), z) < 48 * 48) {
                p.sendOverlayMessage(Component.translatable("message.thesift.encore." + which));
            }
        }
        return m;
    }

    // ------------------------------------------------------------------ the Grand Stage (the Conductor)

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
        this.armed = false;
        this.sync();
    }

    /** True when the Conga Drum, the Crane Flute and the Weaver's Guitar are all in this player's inventory. */
    private static boolean carriesAllThree(Player p) {
        boolean drum = false;
        boolean flute = false;
        boolean guitar = false;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            drum |= s.is(ModItems.CONGA_DRUM.get());
            flute |= s.is(ModItems.CRANE_FLUTE.get());
            guitar |= s.is(ModItems.WEAVER_GUITAR.get()); // only the Weaver's own guitar sounds on the Grand Stage
        }
        return drum && flute && guitar;
    }

    private @Nullable Player performer(ServerLevel level) {
        BlockPos pos = this.worldPosition;
        for (Player p : level.players()) {
            if (!p.isSpectator() && p.isAlive() && p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < STAGE_RANGE * STAGE_RANGE
                    && carriesAllThree(p)) {
                return p;
            }
        }
        return null;
    }

    public static void stageTick(Level level, BlockPos pos, BlockState state, BossDenBlockEntity stage) {
        if (level.isClientSide()) {
            if (stage.performance >= 0) {
                stage.performance++;
                Performance.clientStage = pos;
                Performance.clientTick = stage.performance;
                Performance.clientSeen = level.getGameTime();
            }
            return;
        }
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (stage.performance >= 0) {
            stage.perform(server, pos);
            return;
        }
        if (server.getGameTime() % 10 != 0) {
            return;
        }
        if (stage.boss != null) {
            Entity e = server.getEntity(stage.boss);
            if (e != null && e.isAlive()) {
                return;
            }
            // the Conductor is gone without being beaten (despawned or removed): reopen the stage
            stage.boss = null;
            stage.sync();
        }
        Player performer = stage.performer(server);
        if (performer == null) {
            if (!stage.armed) {
                stage.armed = true;
                stage.setChanged();
            }
        } else if (stage.armed) {
            stage.begin(server, pos);
        }
    }

    private void begin(ServerLevel level, BlockPos pos) {
        this.performance = 0;
        this.defeated = false;
        this.armed = false;
        for (Player p : level.players()) {
            if (p.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 64 * 64) {
                p.sendOverlayMessage(Component.translatable("message.thesift.stage.begins"));
            }
        }
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 1.5F, 0.8F);
        level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.0F, 1.4F);
        this.sync();
    }

    private void perform(ServerLevel level, BlockPos pos) {
        int t = this.performance++;
        if (t % Performance.BAR == 0) {
            level.sendParticles(ModParticles.RESONANCE_RING.get(), pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5, 0,
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
        output.putBoolean("Armed", this.armed);
        if (this.boss != null) {
            output.putString("Boss", this.boss.toString());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.defeated = input.getBooleanOr("Defeated", false);
        this.performance = input.getIntOr("Performance", -1);
        this.armed = input.getBooleanOr("Armed", true);
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
