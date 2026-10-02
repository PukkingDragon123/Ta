package com.thesift.entity.boss;

import com.thesift.effect.SculkCorruptionEffect;
import com.thesift.entity.siege.Cannonball;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import com.thesift.world.Rumble;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Thumper: a giant Warden-kin snapping turtle that lives under its arena, the Drum Pit. It
 * crawls up out of the ground when its sigil is woken. Nothing you can swing or shoot gets through
 * its shell - every blow clanks off. But glowing sculk vents run along its shell, and they open
 * whenever it strains: after a stomp, while it breathes its beam, when it bursts out of the ground,
 * and for a long while after it rams a wall. The arena's towers carry Ancient Cannons; a
 * cannonball that lands on an open vent is the only thing that truly hurts it.
 *
 * <ol>
 *   <li>Full health to 66%: it stomps (shockwaves - jump them), charges in a straight line and
 *   sweeps its tail round in a full circle.</li>
 *   <li>66% to 33%: it also breathes a sweeping beam of sculk song and hurls boulders.</li>
 *   <li>Below 33%: enraged and faster, it burrows, tunnels after you and erupts beneath you.</li>
 * </ol>
 * Its slams and charges shake the arena apart - but only its crumbling stone (walls of
 * crumbling dreamstone), never anything players built.
 */
public class Thumper extends MiniBoss {
    public static final int STOMP = 1;
    public static final int CHARGE_WINDUP = 2;
    public static final int CHARGE = 3;
    public static final int TAIL_SWEEP = 4;
    /** Spent, panting, its vents gaping open. */
    public static final int EXPOSED = 5;
    /** The entrance: crawling up out of the ground. */
    public static final int EMERGE = 6;
    public static final int BEAM = 7;
    public static final int BOULDERS = 8;
    public static final int BURROW = 9;
    /** Tunnelling under the arena, unseen. */
    public static final int UNDER = 10;
    public static final int ERUPT = 11;
    /** Entering a new phase. */
    public static final int ROAR = 12;

    public static final float SCALE = 2.0F;
    public static final int EMERGE_TICKS = 84;
    public static final int STOMP_IMPACT = 22;
    public static final int SWEEP_START = 14;
    public static final int SWEEP_END = 30;
    public static final int EXPOSED_TICKS = 70;
    public static final int BEAM_CHARGE = 40;
    public static final int BEAM_END = 96;
    public static final int BURROW_TICKS = 24;
    public static final int ERUPT_IMPACT = 8;
    public static final int ROAR_TICKS = 40;
    /** What a cannonball on an open vent takes off (it has 240 health: a dozen good shots). */
    public static final float VENT_DAMAGE = 20.0F;
    private static final double VENT_RADIUS = 1.5;
    private static final byte EVENT_DUST = 102;
    private static final byte EVENT_STOMP = 105;
    private static final byte EVENT_ROAR = 106;
    private static final byte EVENT_QUAKE = 107;
    private static final byte EVENT_STEP = 108;
    private static final EntityDataAccessor<Integer> BEAM_TARGET = SynchedEntityData.defineId(Thumper.class, EntityDataSerializers.INT);

    private Vec3 chargeDir = Vec3.ZERO;
    private final Set<Integer> hits = new HashSet<>();
    private final Set<Integer> beamCorrupted = new HashSet<>();
    private @Nullable Vec3 beamAim;
    private @Nullable Vec3 clientAim;
    private double burrowY;
    private int phase = 1;
    private int hintCooldown;
    private boolean ventHit;
    private boolean ventsHinted;
    private double walked;

    public Thumper(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.GREEN);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 240.0).add(Attributes.ARMOR, 8.0).add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ATTACK_DAMAGE, 10.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.STEP_HEIGHT, 1.5).add(Attributes.ATTACK_KNOCKBACK, 1.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BEAM_TARGET, -1);
    }

    @Override
    protected void setState(int state) {
        super.setState(state);
        if (state != BEAM) {
            this.entityData.set(BEAM_TARGET, -1);
        }
    }

    @Override
    protected double meleeReach() {
        return 1.4;
    }

    /** Called by its sigil: it starts deep in the ground and crawls up out of it. */
    public void beginEmerge() {
        this.setState(EMERGE);
        this.setDeltaMovement(Vec3.ZERO);
    }

    // ------------------------------------------------------------------ the vents, its weak points

    /** Are its vents open in this state, this far into it? (The model reads the same rule.) */
    public static boolean ventsOpen(int state, float t) {
        return switch (state) {
            case EXPOSED, BEAM -> true;
            case STOMP -> t >= STOMP_IMPACT;
            case ERUPT -> t >= ERUPT_IMPACT;
            default -> false;
        };
    }

    public boolean ventsOpen() {
        return ventsOpen(this.getState(), this.stateTicks);
    }

    private Vec3 forward() {
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
    }

    /** The three vents in world space: one on the crown of its shell, one on each flank. */
    public Vec3[] vents() {
        Vec3 f = this.forward();
        Vec3 side = new Vec3(-f.z, 0.0, f.x);
        Vec3 p = this.position();
        return new Vec3[]{p.add(0.0, 2.75, 0.0).add(f.scale(0.1)), p.add(side.scale(1.6)).add(0.0, 1.45, 0.0), p.add(side.scale(-1.6)).add(0.0, 1.45, 0.0)};
    }

    /** Where its beam comes from: its jaws. */
    public Vec3 mouth() {
        return this.position().add(this.forward().scale(3.3)).add(0.0, 1.7, 0.0);
    }

    /** A cannonball struck it: on an open vent it hurts; anywhere else it rings off the shell. */
    public void cannonHit(ServerLevel level, Cannonball ball) {
        int state = this.getState();
        if (state == EMERGE || state == UNDER) {
            this.clank(level, ball.position());
            return;
        }
        Vec3 at = ball.position();
        Vec3 best = null;
        for (Vec3 v : this.vents()) {
            if (v.distanceTo(at) < VENT_RADIUS && (best == null || v.distanceTo(at) < best.distanceTo(at))) {
                best = v;
            }
        }
        Entity owner = ball.getOwner();
        if (!this.ventsOpen() || best == null) {
            this.clank(level, at);
            if (owner instanceof Player p && this.hintCooldown <= 0) {
                p.sendOverlayMessage(Component.translatable(this.ventsOpen() ? "message.thesift.thumper.miss" : "message.thesift.thumper.closed"));
                this.hintCooldown = 80;
            }
            return;
        }
        this.ventHit = true;
        boolean hurt;
        try {
            hurt = super.hurtServer(level, this.damageSources().thrown(ball, owner), VENT_DAMAGE);
        } finally {
            this.ventHit = false;
        }
        if (hurt) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SCULK.defaultBlockState()), best.x, best.y, best.z, 40, 0.5, 0.4, 0.5, 0.3);
            level.sendParticles(ParticleTypes.SCULK_SOUL, best.x, best.y, best.z, 12, 0.4, 0.4, 0.4, 0.08);
            level.sendParticles(ModParticles.RESONANCE_RING.get(), best.x, best.y + 0.2, best.z, 0, 2.0, 0.0, 0.0, 1.0);
            level.sendParticles(ParticleTypes.EXPLOSION, best.x, best.y, best.z, 1, 0, 0, 0, 0);
            this.playSound(SoundEvents.SCULK_SHRIEKER_SHRIEK, 3.0F, 0.6F);
            this.playSound(SoundEvents.SCULK_BLOCK_BREAK, 3.0F, 0.5F);
            if (state == BEAM) {
                // a shot down its throat stops the breath short
                this.setState(EXPOSED);
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || this.ventHit) {
            return super.hurtServer(level, source, amount);
        }
        if (source.getEntity() instanceof LivingEntity le && isBandmate(le)) {
            return false;
        }
        Entity e = source.getDirectEntity();
        this.clank(level, e != null ? e.position().add(0, e.getBbHeight() * 0.6, 0) : this.position().add(0, 1.5, 0));
        if (source.getEntity() instanceof Player p && this.hintCooldown <= 0 && this.getState() != UNDER) {
            p.sendOverlayMessage(Component.translatable("message.thesift.thumper.shell"));
            this.hintCooldown = 120;
        }
        return false;
    }

    /** A blow on the shell: a clank and a shower of sparks, and nothing else. */
    private void clank(ServerLevel level, Vec3 from) {
        if (this.getState() == UNDER) {
            return;
        }
        Vec3 at = from.lerp(this.position().add(0, this.getBbHeight() * 0.45, 0), 0.4);
        level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 8, 0.2, 0.2, 0.2, 0.3);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 4, 0.2, 0.2, 0.2, 0.2);
        this.playSound(ModSounds.THUMPER_CLANK.get(), 1.5F, 0.85F + this.random.nextFloat() * 0.3F);
    }

    @Override
    public boolean isPickable() {
        return this.getState() != UNDER && super.isPickable();
    }

    @Override
    public boolean isPushable() {
        return this.getState() != UNDER && super.isPushable();
    }

    // ------------------------------------------------------------------ the arena

    /** Shakes loose the arena's crumbling stone inside `box` (never anything else); returns how much fell. */
    private int crumble(ServerLevel level, AABB box, float chance, int max) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            if (n >= max) {
                break;
            }
            if (level.getBlockState(p).is(ModBlocks.CRUMBLING_DREAMSTONE.get()) && this.random.nextFloat() < chance) {
                level.destroyBlock(p, false, this);
                n++;
            }
        }
        if (n > 0) {
            this.playSound(ModSounds.CRUMBLE.get(), 3.0F, 0.6F);
        }
        return n;
    }

    // ------------------------------------------------------------------ attacks

    public int phase() {
        float hp = this.getHealth() / this.getMaxHealth();
        return hp > 0.66F ? 1 : hp > 0.33F ? 2 : 3;
    }

    private boolean enraged() {
        return this.phase >= 3;
    }

    @Override
    protected boolean canChase(int state) {
        return state == IDLE;
    }

    @Override
    protected double chaseSpeed() {
        return this.enraged() ? 1.35 : 1.0;
    }

    @Override
    protected boolean holdsState(int state) {
        return state == EMERGE || state == BURROW || state == UNDER || state == ERUPT || state == ROAR;
    }

    @Override
    protected void tickAlways(ServerLevel level, int state) {
        if (this.hintCooldown > 0) {
            this.hintCooldown--;
        }
        int t = this.stateTicks;
        switch (state) {
            case EMERGE -> {
                this.tickEmerge(level, t);
                return;
            }
            case BURROW -> {
                this.tickBurrow(level, t);
                return;
            }
            case UNDER -> {
                this.tickUnder(level, t);
                return;
            }
            case ERUPT -> {
                this.tickErupt(level, t);
                return;
            }
            case ROAR -> {
                this.getNavigation().stop();
                if (t == 1) {
                    this.playSound(ModSounds.THUMPER_ROAR.get(), 5.0F, this.phase == 3 ? 0.55F : 0.7F);
                    this.playSound(SoundEvents.WARDEN_ROAR, 4.0F, 0.7F);
                    level.broadcastEntityEvent(this, EVENT_ROAR);
                    this.crumble(level, this.getBoundingBox().inflate(14.0, 6.0, 14.0), 0.12F, 30);
                    for (Player p : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(48.0))) {
                        p.sendOverlayMessage(Component.translatable("message.thesift.thumper.phase" + this.phase));
                    }
                }
                if (t >= ROAR_TICKS) {
                    this.endAttack(10);
                }
                return;
            }
            default -> {
            }
        }
        int now = this.phase();
        if (now > this.phase) {
            this.phase = now;
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(now == 3 ? 0.26 : 0.2);
            this.setState(ROAR);
            return;
        }
        if (state == EXPOSED && !this.ventsHinted) {
            this.ventsHinted = true;
            for (Player p : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(40.0))) {
                p.sendOverlayMessage(Component.translatable("message.thesift.thumper.vents"));
            }
        }
        this.stepRumble(level);
    }

    @Override
    protected void tickAttacks(ServerLevel level, LivingEntity target, int state) {
        int t = this.stateTicks;
        double dist = this.distanceTo(target);
        switch (state) {
            case IDLE -> {
                if (this.cooldown > 0) {
                    if (this.enraged()) {
                        // enraged, it gets its breath back twice as fast
                        this.cooldown--;
                    }
                    return;
                }
                this.choose(level, target, dist);
            }
            case STOMP -> {
                this.getNavigation().stop();
                this.getLookControl().setLookAt(target, 10.0F, 10.0F);
                if (t == STOMP_IMPACT) {
                    this.stompImpact(level);
                }
                if (t >= STOMP_IMPACT + 18) {
                    this.setState(EXPOSED);
                }
            }
            case CHARGE_WINDUP -> {
                this.getNavigation().stop();
                this.getLookControl().setLookAt(target, 30.0F, 30.0F);
                this.faceTowards(target, 8.0F);
                if (t % 4 == 0) {
                    this.playSound(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 2.0F, 0.6F + t * 0.02F);
                    level.broadcastEntityEvent(this, EVENT_DUST);
                }
                if (t >= (this.enraged() ? 18 : 26)) {
                    Vec3 d = target.position().subtract(this.position()).multiply(1, 0, 1);
                    this.chargeDir = d.lengthSqr() < 1.0E-4 ? this.forward() : d.normalize();
                    this.hits.clear();
                    this.playSound(ModSounds.THUMPER_ROAR.get(), 3.0F, 1.0F);
                    this.setState(CHARGE);
                }
            }
            case CHARGE -> this.tickCharge(level, t);
            case TAIL_SWEEP -> this.tickSweep(level, target, t);
            case EXPOSED -> {
                this.getNavigation().stop();
                if (t % 10 == 0) {
                    for (Vec3 v : this.vents()) {
                        level.sendParticles(ParticleTypes.SCULK_SOUL, v.x, v.y + 0.2, v.z, 2, 0.2, 0.1, 0.2, 0.03);
                    }
                }
                if (t % 20 == 0) {
                    this.playSound(SoundEvents.WARDEN_HEARTBEAT, 3.0F, 0.7F);
                }
                if (t >= EXPOSED_TICKS) {
                    this.endAttack(this.enraged() ? 15 : 30);
                }
            }
            case BEAM -> this.tickBeam(level, target, t);
            case BOULDERS -> this.tickBoulders(level, target, t);
            case EMERGE, BURROW, UNDER, ERUPT, ROAR -> {
                // played out in tickAlways, target or not
            }
            default -> this.endAttack(20);
        }
    }

    private void choose(ServerLevel level, LivingEntity target, double dist) {
        int roll = this.random.nextInt(10);
        boolean sight = this.hasLineOfSight(target);
        if (this.phase >= 3 && roll < 3) {
            this.setState(BURROW);
        } else if (this.phase >= 2 && dist > 7.0 && sight && roll < 6) {
            if (this.random.nextBoolean()) {
                this.setState(BEAM);
                this.entityData.set(BEAM_TARGET, target.getId());
                this.beamAim = target.getEyePosition();
                this.beamCorrupted.clear();
                this.playSound(SoundEvents.WARDEN_SONIC_CHARGE, 5.0F, 0.6F);
            } else {
                this.setState(BOULDERS);
            }
        } else if (dist < 5.5) {
            this.setState(roll < 5 ? TAIL_SWEEP : STOMP);
        } else if (dist < 9.0) {
            this.setState(STOMP);
        } else if (dist < 26 && sight) {
            this.setState(CHARGE_WINDUP);
        }
        if (this.getState() != IDLE) {
            this.getNavigation().stop();
            level.broadcastEntityEvent(this, EVENT_DUST);
            this.playSound(ModSounds.THUMPER_WINDUP.get(), 2.0F, 0.9F + this.random.nextFloat() * 0.2F);
        }
    }

    private void endAttack(int cooldown) {
        this.setState(IDLE);
        this.cooldown = cooldown + this.random.nextInt(20);
    }

    private void faceTowards(Entity e, float maxTurn) {
        double dx = e.getX() - this.getX();
        double dz = e.getZ() - this.getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        this.setYRot(Mth.approachDegrees(this.getYRot(), yaw, maxTurn));
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();
    }

    private void groundBurst(ServerLevel level, Vec3 c, int count, double spread) {
        BlockState ground = level.getBlockState(BlockPos.containing(c).below());
        if (!ground.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), c.x, c.y + 0.1, c.z, count, spread, 0.1, spread, 0.3);
        }
    }

    private void stompImpact(ServerLevel level) {
        this.playSound(ModSounds.THUMPER_SLAM.get(), 4.0F, 0.7F);
        this.playSound(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 4.0F, 0.5F);
        level.broadcastEntityEvent(this, EVENT_STOMP);
        Vec3 c = this.position();
        for (int i = 0; i < 4; i++) {
            level.sendParticles(ModParticles.RESONANCE_RING.get(), c.x, c.y + 0.1, c.z, 0, 2.5 + i * 2.0, 0.0, 0.0, 1.0);
        }
        this.groundBurst(level, c, 80, 3.0);
        level.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.5, c.z, 3, 1.5, 0.2, 1.5, 0.0);
        this.hitAround(level, 7.5, 9.0F, 1.1, 0.75, true);
        this.crumble(level, this.getBoundingBox().inflate(9.0, 4.0, 9.0), 0.2F, 12);
    }

    private void tickCharge(ServerLevel level, int t) {
        double speed = Math.min(this.enraged() ? 1.0 : 0.85, 0.3 + t * 0.06);
        this.setDeltaMovement(this.chargeDir.x * speed, this.getDeltaMovement().y, this.chargeDir.z * speed);
        float yaw = (float) (Mth.atan2(this.chargeDir.z, this.chargeDir.x) * Mth.RAD_TO_DEG) - 90.0F;
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
        if (t % 2 == 0) {
            this.groundBurst(level, this.position(), 12, 1.2);
            level.sendParticles(ParticleTypes.CLOUD, this.getX() - this.chargeDir.x * 1.5, this.getY() + 0.3, this.getZ() - this.chargeDir.z * 1.5, 3, 0.5, 0.2, 0.5, 0.02);
        }
        if (t % 6 == 0) {
            this.playSound(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 2.0F, 0.6F);
            level.broadcastEntityEvent(this, EVENT_STEP);
        }
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.6))) {
            if (e == this || isBandmate(e) || !this.hits.add(e.getId())) {
                continue;
            }
            if (e.hurtServer(level, this.damageSources().mobAttack(this), 13.0F)) {
                e.push(this.chargeDir.x * 2.2, 0.7, this.chargeDir.z * 2.2);
                this.playSound(ModSounds.THUMPER_SLAM.get(), 2.0F, 1.3F);
            }
        }
        // the arena's crumbling walls burst apart in front of it
        AABB ahead = this.getBoundingBox().move(this.chargeDir.x * 1.2, 0.0, this.chargeDir.z * 1.2).inflate(0.4, 0.0, 0.4);
        boolean broke = this.crumble(level, ahead, 1.0F, 20) > 0;
        if (this.horizontalCollision && t > 4 && !broke) {
            // smack into solid stone: dazed, its vents gaping
            this.setDeltaMovement(Vec3.ZERO);
            this.playSound(ModSounds.THUMPER_SLAM.get(), 4.0F, 0.6F);
            this.playSound(ModSounds.THUMPER_DAZED.get(), 2.0F, 1.0F);
            level.broadcastEntityEvent(this, EVENT_STOMP);
            level.sendParticles(ParticleTypes.EXPLOSION, this.getX() + this.chargeDir.x * 1.6, this.getY() + 1.2, this.getZ() + this.chargeDir.z * 1.6,
                    2, 0.4, 0.4, 0.4, 0.0);
            this.crumble(level, this.getBoundingBox().inflate(6.0, 5.0, 6.0), 0.25F, 10);
            this.setState(EXPOSED);
            return;
        }
        if (t >= 48) {
            this.endAttack(30);
        }
    }

    /** Coils, then whips round in a full circle, its tail clubbing everything near it away. */
    private void tickSweep(ServerLevel level, LivingEntity target, int t) {
        this.getNavigation().stop();
        if (t < SWEEP_START) {
            this.faceTowards(target, 6.0F);
            return;
        }
        if (t == SWEEP_START) {
            this.hits.clear();
            this.playSound(ModSounds.THUMPER_ROAR.get(), 2.5F, 1.2F);
        }
        if (t <= SWEEP_END) {
            float yaw = this.getYRot() + 360.0F / (SWEEP_END - SWEEP_START);
            this.setYRot(yaw);
            this.yBodyRot = yaw;
            this.yHeadRot = yaw;
            Vec3 tailTip = this.position().subtract(this.forward().scale(3.4));
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, tailTip.x, tailTip.y + 0.6, tailTip.z, 1, 0.2, 0.1, 0.2, 0.0);
            if (t % 3 == 0) {
                this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.0F, 0.5F);
            }
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(3.4, 0.5, 3.4))) {
                if (e == this || isBandmate(e) || e.distanceTo(this) > 4.8 || !this.hits.add(e.getId())) {
                    continue;
                }
                if (e.hurtServer(level, this.damageSources().mobAttack(this), 8.0F)) {
                    Vec3 out = e.position().subtract(this.position()).multiply(1, 0, 1).normalize();
                    e.push(out.x * 1.8, 0.55, out.z * 1.8);
                }
            }
        }
        if (t >= SWEEP_END + 12) {
            this.endAttack(25);
        }
    }

    /**
     * Its vents light up and its jaws gape; then it breathes a beam of sculk song that sweeps after
     * its target - slowly enough to outrun, and the towers stop it. Whatever it touches takes the
     * corruption. All the while its vents stand wide open.
     */
    private void tickBeam(ServerLevel level, LivingEntity target, int t) {
        this.getNavigation().stop();
        this.getLookControl().setLookAt(target, 6.0F, 6.0F);
        if (t < BEAM_CHARGE) {
            this.faceTowards(target, 3.0F);
            this.beamAim = target.getEyePosition();
            if (t % 6 == 0) {
                this.playSound(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 3.0F, 0.5F + t * 0.02F);
            }
            return;
        }
        if (t == BEAM_CHARGE) {
            this.playSound(SoundEvents.WARDEN_SONIC_BOOM, 6.0F, 0.6F);
            this.playSound(ModSounds.THUMPER_ROAR.get(), 4.0F, 0.6F);
        }
        if (t < BEAM_END) {
            this.faceTowards(target, 1.5F);
            Vec3 want = target.getEyePosition().subtract(0.0, 0.4, 0.0);
            Vec3 aim = this.beamAim == null ? want : this.beamAim.lerp(want, 0.07);
            this.beamAim = aim;
            Vec3 from = this.mouth();
            Vec3 dir = aim.subtract(from);
            if (dir.lengthSqr() < 1.0E-4) {
                return;
            }
            Vec3 end = beamEnd(level, from, dir);
            this.beamParticles(level, end, t);
            if (t % 5 == 0) {
                this.beamHurt(level, from, end);
            }
            if (t % 10 == 0) {
                this.playSound(SoundEvents.WARDEN_SONIC_BOOM, 2.5F, 0.5F + this.random.nextFloat() * 0.2F);
                level.playSound(null, end.x, end.y, end.z, SoundEvents.SCULK_BLOCK_BREAK, SoundSource.HOSTILE, 2.0F, 0.6F);
            }
            return;
        }
        if (t >= BEAM_END + 14) {
            this.endAttack(40);
        }
    }

    private Vec3 beamEnd(Level level, Vec3 from, Vec3 dir) {
        Vec3 to = from.add(dir.normalize().scale(32.0));
        HitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
    }

    private void beamParticles(ServerLevel level, Vec3 end, int t) {
        level.sendParticles(ParticleTypes.SCULK_SOUL, end.x, end.y, end.z, 2, 0.4, 0.3, 0.4, 0.05);
        level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, end.x, end.y, end.z, 4, 0.5, 0.3, 0.5, 0.05);
        if (t % 6 == 0) {
            level.sendParticles(ModParticles.RESONANCE_RING.get(), end.x, end.y + 0.1, end.z, 0, 2.0, 0.0, 0.0, 1.0);
        }
    }

    private void beamHurt(ServerLevel level, Vec3 from, Vec3 end) {
        Vec3 seg = end.subtract(from);
        double len2 = Math.max(1.0E-6, seg.lengthSqr());
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, end).inflate(1.5))) {
            if (e == this || isBandmate(e)) {
                continue;
            }
            Vec3 c = e.getBoundingBox().getCenter();
            double k = Mth.clamp(c.subtract(from).dot(seg) / len2, 0.0, 1.0);
            if (c.distanceTo(from.add(seg.scale(k))) > 1.0 + e.getBbWidth() * 0.5) {
                continue;
            }
            if (e.hurtServer(level, this.damageSources().sonicBoom(this), 3.0F) && this.beamCorrupted.add(e.getId())) {
                SculkCorruptionEffect.stack(e, 0, 200, this);
            }
        }
    }

    /** Rips three boulders out of the arena floor and lobs them at you, one after another. */
    private void tickBoulders(ServerLevel level, LivingEntity target, int t) {
        this.getNavigation().stop();
        this.faceTowards(target, 6.0F);
        this.getLookControl().setLookAt(target, 20.0F, 20.0F);
        if (t < 16 && t % 4 == 0) {
            this.groundBurst(level, this.position().add(this.forward().scale(2.4)), 14, 0.8);
            this.playSound(SoundEvents.DEEPSLATE_BRICKS_BREAK, 1.6F, 0.6F);
        }
        if (t == 16 || t == 24 || t == 32) {
            Vec3 from = this.position().add(this.forward().scale(2.6)).add(0.0, 2.4, 0.0);
            // lead the target a little, and scatter the later throws
            Vec3 to = target.position().add(target.getDeltaMovement().scale(10.0)).add((this.random.nextDouble() - 0.5) * (t - 16) * 0.25, 0.0,
                    (this.random.nextDouble() - 0.5) * (t - 16) * 0.25);
            Vec3 d = to.subtract(from);
            double horiz = Math.max(1.0, d.horizontalDistance());
            double flight = Math.max(10.0, horiz / 0.8);
            Vec3 v = new Vec3(d.x / flight, d.y / flight + 0.5 * 0.06 * flight, d.z / flight);
            Cannonball b = Cannonball.boulder(level, from, this);
            b.setDeltaMovement(v);
            level.addFreshEntity(b);
            this.playSound(ModSounds.THUMPER_SLAM.get(), 2.5F, 1.2F);
            this.groundBurst(level, from.subtract(0.0, 2.4, 0.0), 30, 0.8);
        }
        if (t >= 46) {
            this.endAttack(35);
        }
    }

    // ------------------------------------------------------------------ underground

    /** The entrance: the floor cracks, the ground shakes, and it hauls itself up and roars. */
    private void tickEmerge(ServerLevel level, int t) {
        this.getNavigation().stop();
        this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
        if (t == 1) {
            this.playSound(SoundEvents.WARDEN_EMERGE, 5.0F, 0.6F);
        }
        if (t % 8 == 0 && t < EMERGE_TICKS - 16) {
            level.broadcastEntityEvent(this, EVENT_QUAKE);
            this.playSound(SoundEvents.WARDEN_DIG, 3.0F, 0.5F + t * 0.004F);
            double r = 0.8 + t / (double) EMERGE_TICKS * 2.2;
            for (int i = 0; i < 6; i++) {
                double a = this.random.nextDouble() * Math.PI * 2.0;
                this.groundBurst(level, this.position().add(Math.cos(a) * r, 0.0, Math.sin(a) * r), 8, 0.3);
            }
            level.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY() + 0.3, this.getZ(), 4, r * 0.5, 0.1, r * 0.5, 0.03);
        }
        if (t == EMERGE_TICKS - 22) {
            this.playSound(ModSounds.THUMPER_ROAR.get(), 5.0F, 0.7F);
            this.playSound(SoundEvents.WARDEN_ROAR, 4.0F, 0.8F);
            level.broadcastEntityEvent(this, EVENT_ROAR);
            this.groundBurst(level, this.position(), 80, 2.0);
            for (Player p : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(48.0))) {
                p.sendOverlayMessage(Component.translatable("message.thesift.thumper.emerge"));
            }
        }
        if (t >= EMERGE_TICKS) {
            this.endAttack(20);
        }
    }

    private void tickBurrow(ServerLevel level, int t) {
        this.getNavigation().stop();
        if (t % 4 == 0) {
            this.groundBurst(level, this.position(), 30, 1.5);
            this.playSound(SoundEvents.WARDEN_DIG, 3.0F, 0.6F);
        }
        if (t >= BURROW_TICKS) {
            this.burrowY = this.getY();
            this.setInvisible(true);
            this.noPhysics = true;
            this.setNoGravity(true);
            this.setState(UNDER);
        }
    }

    /** Tunnels after its target: a running crack in the floor, a rumble you can feel coming. */
    private void tickUnder(ServerLevel level, int t) {
        LivingEntity target = this.getTarget();
        Vec3 to = target != null ? target.position().subtract(this.position()).multiply(1, 0, 1) : Vec3.ZERO;
        double d = to.length();
        if (d > 0.3) {
            Vec3 step = to.scale(Math.min(d, 0.34) / d);
            this.setPos(this.getX() + step.x, this.burrowY, this.getZ() + step.z);
            this.setYRot((float) (Mth.atan2(step.z, step.x) * Mth.RAD_TO_DEG) - 90.0F);
            this.yBodyRot = this.getYRot();
        }
        this.setDeltaMovement(Vec3.ZERO);
        if (t % 2 == 0) {
            this.groundBurst(level, this.position(), 10, 0.6);
        }
        if (t % 10 == 0) {
            level.broadcastEntityEvent(this, EVENT_QUAKE);
            level.playSound(null, this.getX(), this.burrowY, this.getZ(), SoundEvents.WARDEN_DIG, SoundSource.HOSTILE, 2.5F, 0.5F);
        }
        if ((t > 20 && d < 1.5) || t >= 80 || target == null) {
            this.surface(level);
            this.setState(ERUPT);
        }
    }

    /** Finds room to burst out at: up through whatever the target stands on, or back where it went down. */
    private void surface(ServerLevel level) {
        this.setInvisible(false);
        this.noPhysics = false;
        this.setNoGravity(false);
        for (int dy = 0; dy <= 6; dy++) {
            for (int sign : new int[]{1, -1}) {
                AABB box = this.getBoundingBox().move(0.0, this.burrowY - this.getY() + dy * sign, 0.0);
                if (level.noCollision(this, box) && !level.noCollision(this, box.move(0.0, -0.5, 0.0))) {
                    this.setPos(this.getX(), this.burrowY + dy * sign, this.getZ());
                    return;
                }
            }
        }
        this.setPos(this.getX(), this.burrowY, this.getZ());
    }

    private void tickErupt(ServerLevel level, int t) {
        this.getNavigation().stop();
        if (t == 1) {
            this.playSound(ModSounds.THUMPER_SLAM.get(), 5.0F, 0.5F);
            this.playSound(SoundEvents.WARDEN_EMERGE, 4.0F, 1.0F);
            level.broadcastEntityEvent(this, EVENT_STOMP);
            this.groundBurst(level, this.position(), 120, 2.5);
            level.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 0.5, this.getZ(), 4, 1.5, 0.4, 1.5, 0.0);
            this.hitAround(level, 4.5, 12.0F, 1.0, 1.1, false);
            this.crumble(level, this.getBoundingBox().inflate(4.0, 3.0, 4.0), 0.6F, 16);
        }
        if (t >= ERUPT_IMPACT + 12) {
            this.setState(EXPOSED);
        }
    }

    private void stepRumble(ServerLevel level) {
        this.walked += this.getDeltaMovement().horizontalDistance();
        if (this.walked > 3.4) {
            this.walked = 0.0;
            this.playSound(ModSounds.THUMPER_SLAM.get(), 1.2F, 0.6F + this.random.nextFloat() * 0.1F);
            level.broadcastEntityEvent(this, EVENT_STEP);
        }
    }

    // ------------------------------------------------------------------ client

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EVENT_DUST -> {
                double r = this.getBbWidth() * 0.35;
                for (int i = 0; i < 8; i++) {
                    this.level().addParticle(ParticleTypes.CLOUD, this.getRandomX(r), this.getY() + 0.2, this.getRandomZ(r), 0, 0.02, 0);
                }
            }
            case EVENT_STOMP -> Rumble.at(this.position(), 3.5F, 40.0F, 20);
            case EVENT_ROAR -> Rumble.at(this.position(), 2.5F, 56.0F, 40);
            case EVENT_QUAKE -> Rumble.at(this.position(), 1.4F, 32.0F, 12);
            case EVENT_STEP -> Rumble.at(this.position(), 0.6F, 20.0F, 6);
            default -> super.handleEntityEvent(id);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.clientTick();
        }
    }

    private void clientTick() {
        Level level = this.level();
        int s = this.getState();
        float t = this.stateTime(0.0F);
        if (ventsOpen(s, t) && this.random.nextInt(2) == 0) {
            for (Vec3 v : this.vents()) {
                level.addParticle(ParticleTypes.SCULK_SOUL, v.x + (this.random.nextDouble() - 0.5) * 0.5, v.y + 0.1, v.z + (this.random.nextDouble() - 0.5) * 0.5,
                        0, 0.05, 0);
            }
        } else if (s == IDLE && this.random.nextInt(14) == 0) {
            Vec3 v = this.vents()[0];
            level.addParticle(ModParticles.GLOW_DUST.get(), v.x + (this.random.nextDouble() - 0.5), v.y + 0.2, v.z + (this.random.nextDouble() - 0.5), 0, 0.02, 0);
        }
        if (s == BEAM && t < BEAM_CHARGE) {
            Vec3 m = this.mouth();
            for (int i = 0; i < 2; i++) {
                Vec3 o = new Vec3(this.random.nextDouble() - 0.5, this.random.nextDouble() - 0.5, this.random.nextDouble() - 0.5).scale(3.0);
                level.addParticle(ParticleTypes.SCULK_CHARGE_POP, m.x + o.x, m.y + o.y, m.z + o.z, -o.x * 0.12, -o.y * 0.12, -o.z * 0.12);
            }
        }
        this.clientBeam(level, s, t);
    }

    /** The beam, drawn here: a thick cyan beam with a white-hot core, sonic rings running down it. */
    private void clientBeam(Level level, int s, float t) {
        Entity target = s == BEAM ? level.getEntity(this.entityData.get(BEAM_TARGET)) : null;
        if (target == null) {
            this.clientAim = null;
            return;
        }
        Vec3 want = target.getEyePosition().subtract(0.0, 0.4, 0.0);
        this.clientAim = t < BEAM_CHARGE || this.clientAim == null ? target.getEyePosition() : this.clientAim.lerp(want, 0.07);
        if (t < BEAM_CHARGE || t >= BEAM_END) {
            return;
        }
        Vec3 from = this.mouth();
        Vec3 dir = this.clientAim.subtract(from);
        if (dir.lengthSqr() < 1.0E-4) {
            return;
        }
        Vec3 d = beamEnd(level, from, dir).subtract(from);
        double len = d.length();
        if (len < 0.5) {
            return;
        }
        float k = Math.min(1.0F, (t - BEAM_CHARGE) / 6.0F);
        DustParticleOptions outer = new DustParticleOptions(0x2FD8F0, 1.0F + 1.2F * k);
        DustParticleOptions core = new DustParticleOptions(0xE8FFFF, 0.5F + 0.5F * k);
        int n = (int) (len * 2.5);
        for (int i = 0; i < n; i++) {
            Vec3 p = from.add(d.scale(this.random.nextDouble()));
            double j = 0.3 * k;
            level.addParticle(outer, p.x + (this.random.nextDouble() - 0.5) * j, p.y + (this.random.nextDouble() - 0.5) * j,
                    p.z + (this.random.nextDouble() - 0.5) * j, 0, 0, 0);
            if (i % 2 == 0) {
                Vec3 q = from.add(d.scale(this.random.nextDouble()));
                level.addParticle(core, q.x, q.y, q.z, 0, 0, 0);
            }
        }
        if (this.tickCount % 3 == 0) {
            Vec3 p = from.add(d.scale((this.tickCount % 9) / 9.0));
            level.addParticle(ParticleTypes.SONIC_BOOM, p.x, p.y, p.z, 0, 0, 0);
        }
        if (this.random.nextInt(2) == 0) {
            Vec3 q = from.add(d.scale(this.random.nextDouble()));
            level.addParticle(ModParticles.SIFT_NOTE.get(), q.x, q.y, q.z, this.random.nextDouble(), 0, 0);
        }
    }

    // ------------------------------------------------------------------ saving

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("VentsHinted", this.ventsHinted);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.ventsHinted = input.getBooleanOr("VentsHinted", false);
        this.phase = this.phase();
        if (this.phase == 3) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.26);
        }
        // never saved half underground
        this.setInvisible(false);
        this.noPhysics = false;
        this.setNoGravity(false);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.THUMPER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.THUMPER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.THUMPER_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 2.0F;
    }

    @Override
    protected int burstColorA() {
        return 0x29DFEB;
    }

    @Override
    protected int burstColorB() {
        return 0xBBC39B;
    }
}
