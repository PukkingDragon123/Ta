package com.thesift.entity.cave;

import com.thesift.entity.KillBurst;
import com.thesift.registry.ModCaveCreatures;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Jailer: a tall, skinny, blind kin of the Warden that haunts the deepest, darkest caves of the
 * Sift carrying a giant cell of sculk-iron bars. It cannot see. It hears: footsteps that are not
 * sneaking, fighting and music (every note draws it to the player). It creeps towards a sound,
 * listening, and charges once it is sure; when it reaches you it heaves the cell over its head and
 * slams it down over you. Inside, you are hauled around and squeezed now and then. Hit the bars
 * until they break (or have a friend hurt the Jailer badly enough to drop you) to get out. A broken
 * cell slowly grows its bars back.
 */
public class Jailer extends Monster {
    public static final int IDLE = 0;
    public static final int EMERGING = 1;
    public static final int HUNTING = 2;
    public static final int SLAMMING = 3;
    public static final int CARRYING = 4;
    public static final int STUNNED = 5;
    /** Bars in a whole cell (the loose bars of the model; the corner posts never break). */
    public static final int CAGE_WHOLE = 12;
    /** Where the carried cell hangs: centred this far in front of the Jailer, its floor this far off the ground (tools/cave_creatures.py). */
    public static final double CAGE_FORWARD = 1.15;
    public static final double CAGE_LIFT = 0.3;
    /** Footsteps carry this far; notes and fighting further. */
    public static final double HEAR_RANGE = 16.0;
    public static final double NOTE_RANGE = 24.0;
    public static final int EMERGE_TICKS = 100;
    /** The slam: wind-up until LOCK (turning to face the sound), the cell comes down at IMPACT, recovered by END. */
    public static final int SLAM_LOCK = 14;
    public static final int SLAM_IMPACT = 18;
    public static final int SLAM_END = 30;

    private static final EntityDataAccessor<Integer> MODE = SynchedEntityData.defineId(Jailer.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CAGE = SynchedEntityData.defineId(Jailer.class, EntityDataSerializers.INT);
    private static final byte EVENT_SLAM = 94;
    private static final byte EVENT_TRAP = 95;
    private static final byte EVENT_SQUEEZE = 96;
    private static final byte EVENT_RATTLE = 97;
    private static final byte EVENT_BREAK = 98;
    private static final byte EVENT_LISTEN = 99;
    private static final float BAR_HEALTH = 8.0F;
    private static final float DROP_DAMAGE = 10.0F;

    public final AnimationState emergeAnimation = new AnimationState();
    public final AnimationState slamAnimation = new AnimationState();
    public final AnimationState trapAnimation = new AnimationState();
    public final AnimationState squeezeAnimation = new AnimationState();
    public final AnimationState rattleAnimation = new AnimationState();
    public final AnimationState breakAnimation = new AnimationState();
    public final AnimationState listenAnimation = new AnimationState();
    /** Client side: eases from 0 (upright) to 1 (hunched, creeping) while it hunts. */
    public float stalk;
    public float stalkO;

    private @Nullable Vec3 heardAt;
    private @Nullable LivingEntity suspect;
    private float certainty;
    private int heardTicks;
    private int listenTimer;
    private int modeTicks;
    private int slamCooldown;
    private @Nullable Vec3 slamAt;
    private int squeezeTimer;
    private float barHealth = BAR_HEALTH;
    private float haulDamage;
    private int regrowTimer;
    private int hintTimer;

    public Jailer(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 80.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, NOTE_RANGE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MODE, IDLE);
        builder.define(CAGE, CAGE_WHOLE);
    }

    /**
     * S1 never freeze: vanilla stops a mob's random strolls once it has been 100 ticks out of
     * a player's 32-block reach, so Sift creatures seen across a valley stood frozen. The field
     * itself (which drives despawning) is left alone.
     */
    @Override
    public int getNoActionTime() {
        return 0;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new HuntGoal());
        this.goalSelector.addGoal(2, new HaulGoal());
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.5) {
            @Override
            public boolean canUse() {
                return Jailer.this.getMode() == IDLE && super.canUse();
            }
        });
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this) {
            @Override
            public boolean canUse() {
                return Jailer.this.getMode() == IDLE && super.canUse();
            }
        });
    }

    public int getMode() {
        return this.entityData.get(MODE);
    }

    private void setMode(int mode) {
        this.entityData.set(MODE, mode);
        this.modeTicks = 0;
        if (mode == EMERGING || mode == SLAMMING || mode == STUNNED) {
            this.getNavigation().stop();
        }
    }

    /** How whole the cell is, 0 (shattered) .. {@link #CAGE_WHOLE}. */
    public int getCage() {
        return this.entityData.get(CAGE);
    }

    private void setCage(int bars) {
        this.entityData.set(CAGE, Mth.clamp(bars, 0, CAGE_WHOLE));
    }

    public boolean isCarrying() {
        return this.getMode() == CARRYING;
    }

    public @Nullable JailCell cell() {
        return this.getFirstPassenger() instanceof JailCell cell ? cell : null;
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
            @Nullable SpawnGroupData data) {
        if (reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.SPAWN_ITEM_USE || EntitySpawnReason.isSpawner(reason)) {
            this.setMode(EMERGING); // it claws its way up out of the rock
        }
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    // ------------------------------------------------------------------ hearing

    /** A sound at {@code at}; {@code who} made it (null for things it cannot hunt). weight 1 is a clear sound, 2 a hit. */
    public void hear(Vec3 at, @Nullable LivingEntity who, float weight) {
        int mode = this.getMode();
        if (!(this.level() instanceof ServerLevel server) || mode == EMERGING || mode == SLAMMING || mode == CARRYING || mode == STUNNED) {
            return;
        }
        if (who instanceof Player p && (p.isCreative() || p.isSpectator())) {
            who = null;
        }
        if (who == null && this.suspect != null && this.heardTicks > 0) {
            return; // already on someone's trail; stray noises do not distract it
        }
        boolean fresh = this.heardAt == null || this.heardTicks <= 20;
        this.heardAt = at;
        this.heardTicks = 140;
        if (who != null) {
            this.certainty = who == this.suspect ? Math.min(3.0F, this.certainty + weight) : weight;
            this.suspect = who;
        }
        if (fresh || this.random.nextInt(5) == 0) {
            server.broadcastEntityEvent(this, EVENT_LISTEN);
            this.playSound(ModCaveCreatures.JAILER_LISTEN.get(), 1.4F, 0.85F + this.random.nextFloat() * 0.2F);
        }
        if (mode != HUNTING) {
            this.setMode(HUNTING);
        }
    }

    /** How loud a creature is to the Jailer: sneaking or riding makes no sound; fighting, landing and swinging do. */
    private static float loudness(LivingEntity e) {
        float loud = e.hurtTime > 6 ? 1.0F : 0.0F; // something just got hit
        if (e.isSteppingCarefully() || e.isPassenger()) {
            return loud;
        }
        Vec3 v = e.getKnownMovement();
        loud = Math.max(loud, (float) Math.min(1.0, v.horizontalDistance() * (e.isSprinting() ? 6.0 : 4.5)));
        if (!e.onGround() && v.y < -0.35) {
            loud = Math.max(loud, 0.8F);
        }
        if (e.isSwinging()) {
            loud = Math.max(loud, 0.6F);
        }
        return loud;
    }

    private boolean canHear(LivingEntity e) {
        if (e == this || !e.isAlive() || e instanceof Jailer || e instanceof Sculkling || e instanceof Warden) {
            return false;
        }
        return !(e instanceof Player p) || !p.isCreative() && !p.isSpectator();
    }

    private void listen(ServerLevel level) {
        if (--this.listenTimer > 0) {
            return;
        }
        this.listenTimer = 5;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(NOTE_RANGE), this::canHear)) {
            float loud = loudness(e);
            double range = (e.hurtTime > 6 ? NOTE_RANGE : HEAR_RANGE) * loud;
            if (loud > 0.05F && this.distanceToSqr(e) < range * range) {
                // a player is a quarry; anything else is just a noise to look into (unless it hit someone)
                LivingEntity who = e instanceof Player ? e : e.getLastHurtByMob() instanceof Player attacker ? attacker : null;
                this.hear(e.position(), who, e.hurtTime > 6 ? 1.5F : 0.6F);
            }
        }
    }

    // ------------------------------------------------------------------ the server tick

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        this.modeTicks++;
        if (this.slamCooldown > 0) {
            this.slamCooldown--;
        }
        if (this.hintTimer > 0) {
            this.hintTimer--;
        }
        switch (this.getMode()) {
            case EMERGING -> this.tickEmerging(level);
            case SLAMMING -> this.tickSlam(level);
            case CARRYING -> this.tickCarrying(level);
            case STUNNED -> {
                if (this.modeTicks > 50) {
                    this.setMode(this.heardAt != null ? HUNTING : IDLE);
                }
            }
            default -> {
                this.listen(level);
                if (this.heardTicks > 0 && --this.heardTicks == 0) {
                    this.heardAt = null;
                    this.suspect = null;
                    this.certainty = 0.0F;
                    if (this.getMode() == HUNTING) {
                        this.setMode(IDLE);
                    }
                }
                if (this.modeTicks % 40 == 0 && this.certainty > 0.0F) {
                    this.certainty = Math.max(0.0F, this.certainty - 0.5F);
                }
            }
        }
        // a broken cell grows its bars back, one by one
        if (this.getCage() < CAGE_WHOLE && this.getMode() != CARRYING && --this.regrowTimer <= 0) {
            this.regrowTimer = 30;
            this.setCage(this.getCage() + 1);
            if (this.getCage() == CAGE_WHOLE) {
                this.barHealth = BAR_HEALTH;
                this.playSound(ModCaveCreatures.JAILER_REGROW.get(), 1.0F, 1.0F);
                level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, this.cagePos().x, this.cagePos().y + 1.0, this.cagePos().z, 12, 0.5, 0.8, 0.5, 0.02);
            }
        }
    }

    private void tickEmerging(ServerLevel level) {
        if (this.modeTicks == 1) {
            this.playSound(ModCaveCreatures.JAILER_EMERGE.get(), 2.0F, 0.9F);
        }
        BlockState below = level.getBlockState(this.blockPosition().below());
        if (!below.isAir() && this.modeTicks % 2 == 0) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, below), this.getX(), this.getY() + 0.1, this.getZ(), 6, 0.6, 0.1, 0.6, 0.1);
        }
        if (this.modeTicks >= EMERGE_TICKS) {
            this.setMode(IDLE);
        }
    }

    // ------------------------------------------------------------------ the slam

    private void startSlam(ServerLevel level) {
        this.setMode(SLAMMING);
        this.slamAt = null;
        level.broadcastEntityEvent(this, EVENT_SLAM);
        this.playSound(ModCaveCreatures.JAILER_WINDUP.get(), 1.6F, 0.9F + this.random.nextFloat() * 0.15F);
    }

    private void tickSlam(ServerLevel level) {
        LivingEntity target = this.suspect;
        if (this.modeTicks < SLAM_LOCK && target != null) {
            // turn to face the sound while the cell goes up
            double dx = target.getX() - this.getX();
            double dz = target.getZ() - this.getZ();
            float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
            float turned = Mth.approachDegrees(this.getYRot(), yaw, 18.0F);
            this.setYRot(turned);
            this.setYBodyRot(turned);
            this.setYHeadRot(turned);
        } else if (this.modeTicks == SLAM_LOCK) {
            double reach = target == null ? 1.8 : Mth.clamp(Math.sqrt(this.distanceToSqr(target.getX(), this.getY(), target.getZ())), 1.3, 2.5);
            Vec3 dir = Vec3.directionFromRotation(0.0F, this.getYRot());
            this.slamAt = this.position().add(dir.scale(reach));
        } else if (this.modeTicks == SLAM_IMPACT) {
            this.impact(level);
        } else if (this.modeTicks >= SLAM_END && this.getMode() == SLAMMING) {
            this.slamCooldown = 70;
            this.setMode(this.heardAt != null ? HUNTING : IDLE);
        }
    }

    private void impact(ServerLevel level) {
        Vec3 at = this.slamAt != null ? this.slamAt : this.position().add(Vec3.directionFromRotation(0.0F, this.getYRot()).scale(1.8));
        this.playSound(ModCaveCreatures.JAILER_SLAM.get(), 2.0F, 0.85F + this.random.nextFloat() * 0.15F);
        BlockState ground = level.getBlockState(BlockPos.containing(at.x, this.getY() - 0.5, at.z));
        if (!ground.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x, this.getY() + 0.1, at.z, 40, 0.7, 0.1, 0.7, 0.15);
        }
        level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, at.x, this.getY() + 0.3, at.z, 16, 0.7, 0.2, 0.7, 0.03);
        Player caught = null;
        for (Player victim : level.getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(at, at).inflate(0.95, 1.6, 0.95),
                pl -> pl.isAlive() && !pl.isSpectator() && !pl.isCreative() && !pl.isPassenger())) {
            caught = victim;
            break;
        }
        if (caught != null && this.getCage() == CAGE_WHOLE && this.trap(level, caught, at)) {
            return;
        }
        // a miss: the cell crashes down on whatever is under it
        for (LivingEntity crushed : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(1.4, 1.5, 1.4),
                le -> le != this && le.isAlive() && !(le instanceof Jailer) && !(le instanceof Sculkling))) {
            if (crushed.hurtServer(level, this.damageSources().mobAttack(this), 7.0F)) {
                Vec3 push = crushed.position().subtract(at).multiply(1.0, 0.0, 1.0);
                push = push.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0.0F, this.getYRot()) : push.normalize();
                crushed.push(push.x * 0.6, 0.35, push.z * 0.6);
            }
        }
    }

    /** Down comes the cell over the player: they stand inside it, and it is hauled up into the Jailer's arms. */
    private boolean trap(ServerLevel level, Player player, Vec3 at) {
        JailCell cell = ModCaveCreatures.JAIL_CELL.get().create(level, EntitySpawnReason.TRIGGERED);
        if (cell == null) {
            return false;
        }
        cell.snapTo(at.x, this.getY() + CAGE_LIFT, at.z, this.getYRot(), 0.0F);
        if (!level.addFreshEntity(cell)) {
            return false;
        }
        if (!cell.startRiding(this, true, true) || !player.startRiding(cell, true, true)) {
            cell.release();
            return false;
        }
        this.setMode(CARRYING);
        this.barHealth = BAR_HEALTH;
        this.haulDamage = 0.0F;
        this.squeezeTimer = 50 + this.random.nextInt(30);
        level.broadcastEntityEvent(this, EVENT_TRAP);
        this.playSound(ModCaveCreatures.JAILER_TRAP.get(), 2.0F, 0.9F);
        player.sendOverlayMessage(Component.translatable("message.thesift.jailer.trapped"));
        return true;
    }

    // ------------------------------------------------------------------ carrying a prisoner

    private void tickCarrying(ServerLevel level) {
        JailCell cell = this.cell();
        LivingEntity prisoner = cell == null ? null : cell.prisoner();
        if (cell == null || prisoner == null || !prisoner.isAlive()) {
            // the prisoner is gone (dead, or out by some trick): back to listening, cell intact
            if (cell != null) {
                cell.release();
            }
            this.slamCooldown = 100;
            this.setMode(IDLE);
            return;
        }
        if (--this.squeezeTimer <= 0) {
            this.squeezeTimer = 60 + this.random.nextInt(50);
            level.broadcastEntityEvent(this, EVENT_SQUEEZE);
            this.playSound(ModCaveCreatures.JAILER_SQUEEZE.get(), 1.5F, 0.8F + this.random.nextFloat() * 0.2F);
            prisoner.hurtServer(level, this.damageSources().mobAttack(this), 3.0F);
            level.sendParticles(ParticleTypes.SCULK_SOUL, cell.getX(), cell.getY() + 1.6, cell.getZ(), 4, 0.4, 0.4, 0.4, 0.02);
        }
    }

    /** The cell is hit (from inside, or by a friend outside). Enough hits and the bars give way. */
    public boolean hitBars(ServerLevel level, JailCell cell, DamageSource source, float amount) {
        if (!this.isCarrying() || source.getEntity() == this) {
            return false;
        }
        this.barHealth -= Math.max(0.5F, amount / 3.0F);
        // each hit snaps a bar or two
        this.setCage(Math.max(1, Mth.ceil(CAGE_WHOLE * this.barHealth / BAR_HEALTH)));
        level.broadcastEntityEvent(this, EVENT_RATTLE);
        this.playSound(ModCaveCreatures.JAILER_RATTLE.get(), 1.3F, 0.85F + this.random.nextFloat() * 0.3F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.IRON_BARS.defaultBlockState()), cell.getX(), cell.getY() + 1.0,
                cell.getZ(), 10, 0.5, 0.7, 0.5, 0.1);
        if (this.barHealth <= 0.0F) {
            this.breakOut(level);
        }
        return true;
    }

    /** A trapped player sneaking to get out only rattles the bars. */
    public void struggle(Player player) {
        if (this.hintTimer <= 0) {
            this.hintTimer = 60;
            player.sendOverlayMessage(Component.translatable("message.thesift.jailer.struggle"));
            this.playSound(ModCaveCreatures.JAILER_RATTLE.get(), 0.6F, 1.3F);
        }
    }

    /** The cell bursts open (bars broken, or dropped when the Jailer is hurt): the prisoner tumbles out, the Jailer staggers. */
    private void breakOut(ServerLevel level) {
        JailCell cell = this.cell();
        LivingEntity prisoner = cell == null ? null : cell.prisoner();
        Vec3 at = cell != null ? cell.position() : this.cagePos();
        this.setCage(0);
        this.regrowTimer = 200;
        this.barHealth = BAR_HEALTH;
        level.broadcastEntityEvent(this, EVENT_BREAK);
        this.playSound(ModCaveCreatures.JAILER_BREAK.get(), 2.0F, 0.9F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.IRON_BARS.defaultBlockState()), at.x, at.y + 1.0, at.z, 40, 0.6, 0.9,
                0.6, 0.25);
        level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, at.x, at.y + 1.0, at.z, 20, 0.6, 0.9, 0.6, 0.05);
        if (cell != null) {
            cell.release();
        }
        if (prisoner != null) {
            Vec3 away = prisoner.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
            away = away.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0.0F, this.getYRot()) : away.normalize();
            prisoner.push(away.x * 0.5, 0.3, away.z * 0.5);
            // it heard exactly where you went
            this.suspect = prisoner;
            this.heardAt = prisoner.position();
            this.heardTicks = 100;
            this.certainty = 3.0F;
        }
        this.slamCooldown = 120;
        this.setMode(STUNNED);
    }

    private Vec3 cagePos() {
        return this.position().add(Vec3.directionFromRotation(0.0F, this.yBodyRot).scale(CAGE_FORWARD)).add(0.0, CAGE_LIFT, 0.0);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof JailCell && this.getPassengers().isEmpty();
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (passenger instanceof JailCell) {
            Vec3 at = this.cagePos();
            moveFunction.accept(passenger, at.x, at.y, at.z);
        } else {
            super.positionRider(passenger, moveFunction);
        }
    }

    // ------------------------------------------------------------------ damage, death

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (this.getMode() == EMERGING && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && this.isAlive()) {
            if (this.isCarrying()) {
                // a friend outside: hurt it enough and it drops the cell
                this.haulDamage += damage;
                if (this.haulDamage >= DROP_DAMAGE) {
                    this.breakOut(level);
                }
            } else if (source.getEntity() instanceof LivingEntity attacker && this.canHear(attacker)) {
                this.hear(attacker.position(), attacker, 2.0F);
            }
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        JailCell cell = this.cell();
        if (cell != null && this.level() instanceof ServerLevel) {
            cell.release();
        }
        super.die(source);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        level.broadcastEntityEvent(this, EVENT_SLAM);
        return super.doHurtTarget(level, target);
    }

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EVENT_SLAM -> this.slamAnimation.start(this.tickCount);
            case EVENT_TRAP -> this.trapAnimation.start(this.tickCount);
            case EVENT_SQUEEZE -> this.squeezeAnimation.start(this.tickCount);
            case EVENT_RATTLE -> this.rattleAnimation.start(this.tickCount);
            case EVENT_BREAK -> this.breakAnimation.start(this.tickCount);
            case EVENT_LISTEN -> this.listenAnimation.start(this.tickCount);
            default -> super.handleEntityEvent(id);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.emergeAnimation.animateWhen(this.getMode() == EMERGING, this.tickCount);
            this.stalkO = this.stalk;
            this.stalk += ((this.getMode() == HUNTING ? 1.0F : 0.0F) - this.stalk) * 0.08F;
            // souls drift up out of the ribcage, faster while it hunts
            if (this.random.nextInt(this.getMode() == HUNTING ? 5 : 14) == 0) {
                Vec3 chest = this.position().add(Vec3.directionFromRotation(0.0F, this.yBodyRot).scale(0.35)).add(0.0, 2.3, 0.0);
                this.level().addParticle(ParticleTypes.SCULK_SOUL, chest.x, chest.y, chest.z, 0.0, 0.03, 0.0);
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Cage", this.getCage());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setCage(input.getIntOr("Cage", CAGE_WHOLE));
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ModCaveCreatures.JAILER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModCaveCreatures.JAILER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModCaveCreatures.JAILER_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(ModCaveCreatures.JAILER_STEP.get(), 0.7F, 0.9F + this.random.nextFloat() * 0.2F);
    }

    @Override
    protected float getSoundVolume() {
        return 1.5F;
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 1;
    }

    @Override
    public void makePoofParticles() {
        KillBurst.pop(this, 0x0C2C35, 0x5FF8FF, KillBurst.STAR, ParticleTypes.SCULK_SOUL);
    }

    // ------------------------------------------------------------------ goals

    /** Follow the last sound: creep while unsure, stride once certain; reach the quarry and slam the cell down. */
    private class HuntGoal extends Goal {
        private int repath;

        HuntGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return Jailer.this.getMode() == HUNTING && Jailer.this.heardAt != null;
        }

        @Override
        public void start() {
            this.repath = 0;
        }

        @Override
        public void stop() {
            Jailer.this.getNavigation().stop();
        }

        @Override
        public void tick() {
            Jailer j = Jailer.this;
            LivingEntity quarry = j.suspect;
            Vec3 at = j.heardAt;
            if (at == null) {
                return;
            }
            boolean close = quarry != null && quarry.isAlive() && j.distanceToSqr(quarry) < 3.2 * 3.2 && Math.abs(quarry.getY() - j.getY()) < 2.0;
            Vec3 goal = close ? quarry.position() : at;
            j.getLookControl().setLookAt(goal.x, goal.y + 1.0, goal.z);
            if (close && j.level() instanceof ServerLevel server) {
                j.getNavigation().stop();
                if (j.slamCooldown <= 0) {
                    if (j.getCage() == CAGE_WHOLE && quarry instanceof Player) {
                        j.startSlam(server);
                    } else {
                        // no cell to trap with (or no player to trap): it swings the cell at them
                        j.slamCooldown = 30;
                        j.doHurtTarget(server, quarry);
                    }
                }
                return;
            }
            if (--this.repath <= 0 || j.getNavigation().isDone()) {
                this.repath = 10;
                double speed = 0.55 + 0.25 * j.certainty;
                if (j.distanceToSqr(goal) > 1.5 * 1.5) {
                    j.getNavigation().moveTo(goal.x, goal.y, goal.z, speed);
                }
            }
        }
    }

    /** With a prisoner in the cell it wanders off, away from anyone else it can hear coming. */
    private class HaulGoal extends Goal {
        private int timer;

        HaulGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return Jailer.this.isCarrying();
        }

        @Override
        public void tick() {
            Jailer j = Jailer.this;
            JailCell cell = j.cell();
            if (cell != null) {
                j.getLookControl().setLookAt(cell.getX(), cell.getY() + 1.0, cell.getZ());
            }
            if (--this.timer > 0 && !j.getNavigation().isDone()) {
                return;
            }
            this.timer = 60 + j.random.nextInt(60);
            LivingEntity prisoner = cell == null ? null : cell.prisoner();
            Player rescuer = j.level().getNearestPlayer(j, 16.0);
            Vec3 to = rescuer != null && rescuer != prisoner ? DefaultRandomPos.getPosAway(j, 12, 5, rescuer.position())
                    : DefaultRandomPos.getPos(j, 10, 5);
            if (to != null) {
                j.getNavigation().moveTo(to.x, to.y, to.z, 0.7);
            }
        }
    }
}
