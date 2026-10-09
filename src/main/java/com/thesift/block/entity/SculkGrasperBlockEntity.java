package com.thesift.block.entity;

import com.thesift.block.SculkGrasperBlock;
import com.thesift.registry.ModCaves;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * W-deep caves: the Sculk Grasper's hunt. IDLE, it listens: a player (not in creative or spectator) within
 * {@link #RANGE} blocks and in sight makes it LUNGE - a tendril shoots out over {@link #LUNGE_TICKS} ticks. If the prey
 * is still in reach it HOLDS: the tendril reels them in to the maw ({@link #PULL} blocks a tick), their legs go heavy and
 * the maw bites every second once they are close. They escape by striking the pod ({@link #struck}), by struggling -
 * moving or jumping for {@link #STRUGGLE} ticks while held - or by getting out of reach; otherwise it lets go after
 * {@link #HOLD_TICKS} ticks. It then RECOILS and rests a few seconds. The phase and the prey are synced to the client
 * for SculkGrasperRenderer (the tendril) and the block's ACTIVE state shows the open, glowing maw.
 */
public class SculkGrasperBlockEntity extends BlockEntity {
    public static final int IDLE = 0;
    public static final int LUNGE = 1;
    public static final int HOLD = 2;
    public static final int RECOIL = 3;
    public static final double RANGE = 6.0;
    public static final int LUNGE_TICKS = 7;
    public static final int RECOIL_TICKS = 10;
    private static final int HOLD_TICKS = 90;
    private static final int STRUGGLE = 26;
    private static final int REST_TICKS = 80;
    private static final double PULL = 0.16;
    private static final double SNAP_DISTANCE = 9.0;

    private int phase = IDLE;
    private int timer;
    private int targetId = -1;
    private int cooldown = 40;
    private int struggle;
    // client side: when the current phase began (game time), and where the prey was last seen (for the recoil)
    private long phaseStart;
    private @Nullable Vec3 lastTarget;

    public SculkGrasperBlockEntity(BlockPos pos, BlockState state) {
        super(ModCaves.SCULK_GRASPER.get(), pos, state);
    }

    public int phase() {
        return this.phase;
    }

    public long phaseStart() {
        return this.phaseStart;
    }

    public @Nullable Entity target() {
        return this.level != null && this.targetId >= 0 ? this.level.getEntity(this.targetId) : null;
    }

    public @Nullable Vec3 lastTarget() {
        return this.lastTarget;
    }

    /** Where the tendril leaves the pod. */
    public Vec3 mouth() {
        return Vec3.atBottomCenterOf(this.worldPosition).add(0.0, 0.5, 0.0);
    }

    // ------------------------------------------------------------------ server

    public static void serverTick(Level level, BlockPos pos, BlockState state, SculkGrasperBlockEntity grasper) {
        if (level instanceof ServerLevel server) {
            grasper.tick(server, pos, state);
        }
    }

    private void tick(ServerLevel level, BlockPos pos, BlockState state) {
        this.timer++;
        switch (this.phase) {
            case LUNGE -> {
                Player prey = this.prey(level);
                if (prey == null || prey.position().distanceTo(this.mouth()) > RANGE + 2.0) {
                    this.setPhase(level, RECOIL, null);
                } else if (this.timer >= LUNGE_TICKS) {
                    this.struggle = 0;
                    this.setPhase(level, HOLD, prey);
                    level.playSound(null, prey.blockPosition(), SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.HOSTILE, 0.9F, 0.6F);
                    level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, prey.getX(), prey.getY() + prey.getBbHeight() * 0.5, prey.getZ(), 10, 0.3, 0.3, 0.3,
                            0.02);
                }
            }
            case HOLD -> this.hold(level);
            case RECOIL -> {
                if (this.timer >= RECOIL_TICKS) {
                    this.cooldown = REST_TICKS;
                    this.setPhase(level, IDLE, null);
                }
            }
            default -> {
                if (this.cooldown > 0) {
                    this.cooldown--;
                } else if (level.getGameTime() % 5L == (pos.asLong() & 3L)) {
                    Player found = this.findPrey(level);
                    if (found != null) {
                        this.setPhase(level, LUNGE, found);
                        level.playSound(null, pos, SoundEvents.WARDEN_TENDRIL_CLICKS, SoundSource.HOSTILE, 1.4F, 1.3F);
                        level.playSound(null, pos, SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.HOSTILE, 1.0F, 0.6F);
                    }
                }
            }
        }
    }

    private @Nullable Player prey(ServerLevel level) {
        Entity e = this.target();
        return e instanceof Player p && p.isAlive() && !p.isCreative() && !p.isSpectator() ? p : null;
    }

    private @Nullable Player findPrey(ServerLevel level) {
        Vec3 eye = this.mouth().add(0.0, 0.45, 0.0);
        Player p = level.getNearestPlayer(eye.x, eye.y, eye.z, RANGE, true);
        if (p == null || !p.isAlive() || p.isSpectator()) {
            return null;
        }
        Vec3 to = p.getEyePosition();
        HitResult hit = level.clip(new ClipContext(eye, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return hit.getType() == HitResult.Type.MISS ? p : null;
    }

    private void hold(ServerLevel level) {
        Player prey = this.prey(level);
        Vec3 mouth = this.mouth();
        if (prey == null || prey.position().distanceTo(mouth) > SNAP_DISTANCE || this.timer > HOLD_TICKS) {
            this.letGo(level, false);
            return;
        }
        if (prey.isPassenger()) {
            prey.stopRiding();
        }
        Vec3 body = prey.position().add(0.0, prey.getBbHeight() * 0.4, 0.0);
        Vec3 to = mouth.subtract(body);
        double d = to.length();
        // a steady drag along the tendril, then held fast at the maw
        Vec3 want = d > 1.1 ? to.scale(PULL / d) : to.scale(0.2);
        Vec3 delta = want.subtract(prey.getDeltaMovement());
        prey.push(delta.x * 0.7, delta.y * 0.7 + (d > 1.1 ? 0.03 : 0.0), delta.z * 0.7);
        prey.resetFallDistance();
        if (this.timer % 10 == 0) {
            prey.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 1, false, false));
        }
        if (d < 1.8 && this.timer % 20 == 0) {
            prey.hurtServer(level, level.damageSources().source(ModCaves.SCULK_GRASP), 2.0F);
            level.playSound(null, this.worldPosition, SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.HOSTILE, 0.6F, 1.4F);
        }
        if (this.timer % 12 == 0) {
            level.playSound(null, prey.blockPosition(), SoundEvents.SCULK_CLICKING, SoundSource.HOSTILE, 0.8F, 0.8F + level.getRandom().nextFloat() * 0.3F);
        }
        if (this.timer % 3 == 0) {
            Vec3 p = body.add(to.scale(level.getRandom().nextDouble()));
            level.sendParticles(ParticleTypes.SCULK_SOUL, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.01);
        }
        // struggling: every tick the prey pushes against its inputs counts
        if (prey instanceof ServerPlayer sp) {
            Input in = sp.getLastClientInput();
            if (in.forward() || in.backward() || in.left() || in.right() || in.jump()) {
                this.struggle++;
            }
        }
        if (this.struggle >= STRUGGLE) {
            this.letGo(level, true);
        }
    }

    /** The pod was struck: it shrieks, lets go and recoils. */
    public void struck(ServerLevel level) {
        if (this.phase == LUNGE || this.phase == HOLD) {
            level.playSound(null, this.worldPosition, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 0.5F, 1.6F);
            this.letGo(level, true);
        }
    }

    private void letGo(ServerLevel level, boolean torn) {
        Entity prey = this.target();
        if (prey != null) {
            level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, prey.getX(), prey.getY() + prey.getBbHeight() * 0.5, prey.getZ(), 14, 0.35, 0.35, 0.35, 0.04);
        }
        level.playSound(null, this.worldPosition, torn ? SoundEvents.LEAD_BREAK : SoundEvents.SCULK_CLICKING_STOP, SoundSource.HOSTILE, 1.0F, 0.7F);
        this.setPhase(level, RECOIL, prey);
    }

    private void setPhase(ServerLevel level, int phase, @Nullable Entity target) {
        this.phase = phase;
        this.timer = 0;
        this.targetId = target != null ? target.getId() : -1;
        BlockState state = this.getBlockState();
        boolean active = phase == LUNGE || phase == HOLD;
        if (state.hasProperty(SculkGrasperBlock.ACTIVE) && state.getValue(SculkGrasperBlock.ACTIVE) != active) {
            level.setBlock(this.worldPosition, state.setValue(SculkGrasperBlock.ACTIVE, active), Block.UPDATE_CLIENTS);
        }
        this.setChanged();
        level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
    }

    // ------------------------------------------------------------------ client

    public static void clientTick(Level level, BlockPos pos, BlockState state, SculkGrasperBlockEntity grasper) {
        Entity e = grasper.target();
        if (e != null) {
            grasper.lastTarget = e.position().add(0.0, e.getBbHeight() * 0.45, 0.0);
        }
    }

    // ------------------------------------------------------------------ sync & save

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Phase", this.phase);
        output.putInt("Target", this.targetId);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int newPhase = input.getIntOr("Phase", IDLE);
        this.targetId = input.getIntOr("Target", -1);
        if (newPhase != this.phase && this.level != null) {
            this.phaseStart = this.level.getGameTime();
        }
        this.phase = newPhase;
        if (this.level != null && !this.level.isClientSide()) {
            // a hunt never survives a reload: start over, rested
            this.phase = IDLE;
            this.targetId = -1;
        }
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
