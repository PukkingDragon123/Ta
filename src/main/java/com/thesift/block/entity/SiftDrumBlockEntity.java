package com.thesift.block.entity;

import com.thesift.block.SiftDrumBlock;
import com.thesift.portal.PortalFrames;
import com.thesift.registry.ModBlockEntities;
import com.thesift.registry.ModItems;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Runs the Warden Core rhythm ritual.
 *
 * <p>Once a core is slotted in, the drum "calls": it plays a rhythm while the nearby sculk
 * sensors flash along. The player must answer by hitting the drum with the same rhythm. Three
 * rounds, each longer and stricter. Success tears open the frame with a storm of cyan and pink
 * souls; a mistake makes the sculk shriek and the round starts over.</p>
 */
public class SiftDrumBlockEntity extends BlockEntity {
    public static final int ROUNDS = 3;
    private static final int[] INTERVAL_CHOICES = {6, 10, 14};

    private enum Phase { IDLE, CALL, ANSWER, FAILED, OPENING }

    private Phase phase = Phase.IDLE;
    private int round;
    private int timer;
    private int beatIndex;
    private int[] pattern = new int[0];
    private long lastAnswerTick;
    private final List<BlockPos> sensors = new ArrayList<>();
    private @Nullable PortalFrames.Frame frame;

    public SiftDrumBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SIFT_DRUM.get(), pos, state);
    }

    // ------------------------------------------------------------------ interaction

    public boolean tryInsertCore(ServerLevel level, Player player) {
        List<BlockPos> found = findSensors(level);
        if (found.size() < 3) {
            player.displayClientMessage(Component.translatable("message.thesift.drum.need_sensors", found.size()), true);
            level.playSound(null, this.worldPosition, SoundEvents.SCULK_CLICKING_STOP, SoundSource.BLOCKS, 1.0F, 0.6F);
            return false;
        }
        PortalFrames.Frame f = PortalFrames.find(level, this.worldPosition, 12);
        if (f == null) {
            player.displayClientMessage(Component.translatable("message.thesift.drum.no_frame"), true);
            level.playSound(null, this.worldPosition, SoundEvents.SCULK_CLICKING_STOP, SoundSource.BLOCKS, 1.0F, 0.6F);
            return false;
        }
        this.sensors.clear();
        this.sensors.addAll(found);
        this.frame = f;
        level.setBlock(this.worldPosition, this.getBlockState().setValue(SiftDrumBlock.CORE, true), Block.UPDATE_ALL);
        level.playSound(null, this.worldPosition, ModSounds.WARDEN_CORE_PULSE.get(), SoundSource.BLOCKS, 1.5F, 0.8F);
        level.sendParticles(ParticleTypes.SCULK_SOUL, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 1.0, this.worldPosition.getZ() + 0.5,
                20, 0.3, 0.3, 0.3, 0.03);
        this.round = 0;
        this.startCall(level, 40);
        this.message(level, "message.thesift.drum.listen", 1);
        this.setChanged();
        return true;
    }

    public void ejectCore(ServerLevel level) {
        if (this.getBlockState().getValue(SiftDrumBlock.CORE)) {
            Block.popResource(level, this.worldPosition.above(), new ItemStack(ModItems.WARDEN_CORE.get()));
            level.setBlock(this.worldPosition, this.getBlockState().setValue(SiftDrumBlock.CORE, false), Block.UPDATE_ALL);
        }
        this.phase = Phase.IDLE;
        this.setChanged();
    }

    public void onPlayerBeat(@Nullable Player player) {
        if (!(this.level instanceof ServerLevel level) || player == null) return;
        if (this.phase != Phase.ANSWER) return;
        long now = level.getGameTime();
        if (this.beatIndex == 0) {
            // First hit sets the downbeat.
            this.beatIndex = 1;
            this.lastAnswerTick = now;
            this.flashSensor(level, 0);
            level.playSound(null, this.worldPosition, ModSounds.RHYTHM_GOOD.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
            this.timer = this.pattern.length > 0 ? this.pattern[0] + 12 : 40;
            return;
        }
        int expected = this.pattern[this.beatIndex - 1];
        int actual = (int) (now - this.lastAnswerTick);
        int tolerance = 5 - this.round;
        if (Math.abs(actual - expected) <= tolerance) {
            this.flashSensor(level, this.beatIndex);
            level.playSound(null, this.worldPosition, ModSounds.RHYTHM_GOOD.get(), SoundSource.BLOCKS, 0.8F, 1.0F + this.beatIndex * 0.08F);
            this.lastAnswerTick = now;
            this.beatIndex++;
            if (this.beatIndex > this.pattern.length) {
                this.roundComplete(level);
            } else {
                this.timer = this.pattern[this.beatIndex - 1] + 12;
            }
        } else {
            this.fail(level, actual < expected ? "message.thesift.drum.too_early" : "message.thesift.drum.too_late");
        }
    }

    // ------------------------------------------------------------------ ticking

    public static void serverTick(Level level, BlockPos pos, BlockState state, SiftDrumBlockEntity drum) {
        if (level instanceof ServerLevel server) {
            drum.tick(server);
        }
    }

    private void tick(ServerLevel level) {
        if (this.phase == Phase.IDLE) {
            return;
        }
        if (!this.getBlockState().getValue(SiftDrumBlock.CORE)) {
            this.phase = Phase.IDLE;
            return;
        }
        if (this.timer > 0) {
            this.timer--;
        }
        switch (this.phase) {
            case CALL -> {
                if (this.timer == 0) {
                    // Play the next beat of the call.
                    SiftDrumBlock.beat(level, this.worldPosition, 0.5F);
                    level.playSound(null, this.worldPosition, ModSounds.RHYTHM_CALL.get(), SoundSource.BLOCKS, 1.0F, 0.7F + this.beatIndex * 0.06F);
                    this.flashSensor(level, this.beatIndex);
                    if (this.beatIndex >= this.pattern.length) {
                        this.phase = Phase.ANSWER;
                        this.beatIndex = 0;
                        this.timer = 20 * 8;
                        this.message(level, "message.thesift.drum.your_turn", this.round + 1);
                    } else {
                        this.timer = this.pattern[this.beatIndex];
                        this.beatIndex++;
                    }
                }
            }
            case ANSWER -> {
                if (this.timer == 0) {
                    this.fail(level, "message.thesift.drum.too_late");
                }
            }
            case FAILED -> {
                if (this.timer == 0) {
                    this.startCall(level, 10);
                    this.message(level, "message.thesift.drum.listen", this.round + 1);
                }
            }
            case OPENING -> this.tickOpening(level);
            default -> {}
        }
    }

    private void startCall(ServerLevel level, int delay) {
        RandomSource r = level.getRandom();
        int beats = 3 + this.round;
        this.pattern = new int[beats];
        for (int i = 0; i < beats; i++) {
            this.pattern[i] = INTERVAL_CHOICES[r.nextInt(INTERVAL_CHOICES.length)];
        }
        this.phase = Phase.CALL;
        this.beatIndex = 0;
        this.timer = delay;
        this.setChanged();
    }

    private void roundComplete(ServerLevel level) {
        this.round++;
        level.playSound(null, this.worldPosition, ModSounds.RHYTHM_ROUND.get(), SoundSource.BLOCKS, 1.2F, 0.8F + this.round * 0.15F);
        level.sendParticles(ModParticles.RESONANCE_RING.get(), this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.1,
                this.worldPosition.getZ() + 0.5, 0, 6.0, 0, 0, 1.0);
        if (this.round >= ROUNDS) {
            this.phase = Phase.OPENING;
            this.timer = 80;
            this.message(level, "message.thesift.drum.opening", 0);
        } else {
            this.message(level, "message.thesift.drum.round_done", this.round);
            this.startCall(level, 40);
        }
        this.setChanged();
    }

    private void fail(ServerLevel level, String key) {
        this.phase = Phase.FAILED;
        this.timer = 60;
        this.beatIndex = 0;
        level.playSound(null, this.worldPosition, ModSounds.RHYTHM_FAIL.get(), SoundSource.BLOCKS, 1.5F, 1.0F);
        for (BlockPos s : this.sensors) {
            level.sendParticles(ParticleTypes.SHRIEK, s.getX() + 0.5, s.getY() + 0.6, s.getZ() + 0.5, 1, 0, 0, 0, 0);
        }
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, new AABB(this.worldPosition).inflate(16))) {
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
        }
        this.message(level, key, 0);
    }

    private void tickOpening(ServerLevel level) {
        PortalFrames.Frame f = this.frame;
        if (f == null) {
            this.phase = Phase.IDLE;
            return;
        }
        Vec3 c = Vec3.atCenterOf(f.center());
        RandomSource r = level.getRandom();
        // Souls stream from every sensor and from the drum into the frame, faster and faster.
        int streams = 2 + (80 - this.timer) / 8;
        for (BlockPos s : this.sensors) {
            for (int i = 0; i < streams / 2 + 1; i++) {
                Vec3 from = Vec3.atCenterOf(s).add(0, 0.4, 0);
                Vec3 v = c.subtract(from).scale(0.06);
                level.sendParticles(ModParticles.PORTAL_SOUL.get(), from.x, from.y, from.z, 0, v.x, v.y + 0.02, v.z, 1.0);
            }
        }
        if (this.timer % 10 == 0) {
            SiftDrumBlock.beat(level, this.worldPosition, 1.0F);
        }
        if (this.timer % 4 == 0) {
            // Spiral around the frame rim.
            double a = level.getGameTime() * 0.35;
            for (BlockPos b : f.interior()) {
                if (r.nextInt(6) != 0) continue;
                level.sendParticles(r.nextBoolean() ? ModParticles.DRIFTING_SOUL.get() : ModParticles.STAR_SPARKLE.get(),
                        b.getX() + 0.5 + Math.cos(a) * 0.3, b.getY() + 0.5, b.getZ() + 0.5 + Math.sin(a) * 0.3, 1, 0.2, 0.2, 0.2, 0.02);
            }
        }
        if (this.timer == 0) {
            PortalFrames.fill(level, f);
            level.playSound(null, f.center(), ModSounds.PORTAL_ACTIVATE.get(), SoundSource.BLOCKS, 4.0F, 1.0F);
            level.playSound(null, f.center(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.BLOCKS, 1.5F, 1.4F);
            for (int i = 0; i < 6; i++) {
                level.sendParticles(ModParticles.RESONANCE_RING.get(), c.x, f.center().getY() - 1.0 + i * 1.5, c.z, 0, 4.0 + i, 0, 0, 1.0);
            }
            for (BlockPos b : f.interior()) {
                level.sendParticles(ModParticles.PORTAL_SOUL.get(), b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0.2);
                if (r.nextInt(3) == 0) {
                    level.sendParticles(ModParticles.SIFT_NOTE.get(), b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5, 0, r.nextDouble(), 0, 0, 1);
                }
            }
            for (BlockPos s : this.sensors) {
                level.sendParticles(ParticleTypes.SONIC_BOOM, s.getX() + 0.5, s.getY() + 1.0, s.getZ() + 0.5, 1, 0, 0, 0, 0);
            }
            this.message(level, "message.thesift.drum.opened", 0);
            level.setBlock(this.worldPosition, this.getBlockState().setValue(SiftDrumBlock.CORE, false), Block.UPDATE_ALL);
            this.phase = Phase.IDLE;
            this.frame = null;
            this.setChanged();
        }
    }

    // ------------------------------------------------------------------ helpers

    private List<BlockPos> findSensors(Level level) {
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(this.worldPosition.offset(-8, -4, -8), this.worldPosition.offset(8, 4, 8))) {
            BlockState s = level.getBlockState(p);
            if (s.is(Blocks.SCULK_SENSOR) || s.is(Blocks.CALIBRATED_SCULK_SENSOR)) {
                out.add(p.immutable());
            }
        }
        return out;
    }

    private void flashSensor(ServerLevel level, int index) {
        if (this.sensors.isEmpty()) return;
        BlockPos s = this.sensors.get(Math.floorMod(index, this.sensors.size()));
        level.playSound(null, s, SoundEvents.SCULK_CLICKING, SoundSource.BLOCKS, 1.0F, 0.8F + index * 0.1F);
        level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, s.getX() + 0.5, s.getY() + 0.7, s.getZ() + 0.5, 6, 0.2, 0.1, 0.2, 0.02);
        level.sendParticles(ModParticles.SIFT_NOTE.get(), s.getX() + 0.5, s.getY() + 1.1, s.getZ() + 0.5, 0, 0.55, 0, 0, 1);
    }

    private void message(ServerLevel level, String key, int arg) {
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, new AABB(this.worldPosition).inflate(20))) {
            p.displayClientMessage(Component.translatable(key, arg, ROUNDS), true);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Round", this.round);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.round = input.getIntOr("Round", 0);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (state.getValue(SiftDrumBlock.CORE) && this.level != null) {
            Block.popResource(this.level, pos, new ItemStack(ModItems.WARDEN_CORE.get()));
        }
        super.preRemoveSideEffects(pos, state);
    }
}
