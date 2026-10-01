package com.thesift.entity.boss;

import com.thesift.block.entity.ConductorsPodiumBlockEntity;
import com.thesift.entity.KillBurst;
import com.thesift.registry.ModEffects;
import com.thesift.registry.ModEntities;
import com.thesift.registry.ModItems;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import com.thesift.world.TemporaryBlocks;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.SpawnUtil;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Conductor, risen from his Mask on the Grand Stage. He has taken something from each of the
 * three great players he kept, and every movement of his performance is played with it - each
 * steeped in sculk magic: his blows leave Sculk Corruption, a slow wither that darkens your sight.
 *
 * <ol>
 *   <li>Shell (full health to 66%): he swells huge, the Thumper's shell on his back - blows from
 *   behind clank off it. He slams the stage (jump the rings) and charges. Thumplings join him.</li>
 *   <li>Wings (66% to 33%): the Whistler's wings. He takes to the air and plays its song: a beam
 *   that locks on and keeps hurting until you break it (out of sight, out of range, or hit him
 *   hard). He dives. Whistlings join him.</li>
 *   <li>Strings (below 33%): the Strummer's strings hang from his hands. He spits webs, plucks a
 *   string to drag you in, and strums his brood stronger. Strumlings pour in, darkness pulses.</li>
 * </ol>
 * Between movements he transforms: he rises, roars, and the stage shakes - he cannot be hurt
 * until he lands.
 */
public class Dictator extends Monster {
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(Dictator.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(Dictator.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TRANSFORM = SynchedEntityData.defineId(Dictator.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BEAM_TARGET = SynchedEntityData.defineId(Dictator.class, EntityDataSerializers.INT);
    private static final byte EVENT_BLINK = 70;
    private static final byte EVENT_SUMMON = 71;
    private static final byte EVENT_CRESCENDO = 72;
    private static final byte EVENT_SLASH = 73;
    private static final byte EVENT_ROAR = 74;
    public static final int TRANSFORM_TICKS = 70;

    // actions
    public static final int NONE = 0;
    public static final int SLAM = 1;
    public static final int CHARGE_WINDUP = 2;
    public static final int CHARGE = 3;
    public static final int BEAM_CHARGE = 4;
    public static final int BEAM_LOCK = 5;
    public static final int DIVE = 6;
    public static final int WEB = 7;
    public static final int SNAP = 8;
    public static final int STRUM = 9;

    public final AnimationState blinkAnimation = new AnimationState();
    public final AnimationState summonAnimation = new AnimationState();
    public final AnimationState crescendoAnimation = new AnimationState();
    public final AnimationState slashAnimation = new AnimationState();
    public final AnimationState roarAnimation = new AnimationState();

    private final ServerBossEvent bossEvent = new ServerBossEvent(UUID.randomUUID(), Component.translatable("entity.thesift.dictator"),
            BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);
    private @Nullable BlockPos podium;
    private int blinkCooldown = 80;
    private int summonCooldown = 60;
    private int actionCooldown = 60;
    private int darknessCooldown = 100;
    private int ringTicks = -1;
    private int actionTicks;
    public int actionStart;
    private float lockDamage;
    private Vec3 chargeDir = Vec3.ZERO;
    private final Set<Integer> hits = new HashSet<>();

    public Dictator(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 400;
        this.bossEvent.setDarkenScreen(true);
        this.bossEvent.setCreateWorldFog(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 450.0).add(Attributes.ARMOR, 10.0).add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.FOLLOW_RANGE, 48.0).add(Attributes.ATTACK_DAMAGE, 11.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.STEP_HEIGHT, 1.5).add(Attributes.FLYING_SPEED, 0.6);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, true) {
            @Override
            public boolean canUse() {
                return Dictator.this.groundFighting() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return Dictator.this.groundFighting() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, OrchestraMinion.class, MiniBoss.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASE, 1);
        builder.define(ACTION, NONE);
        builder.define(TRANSFORM, 0);
        builder.define(BEAM_TARGET, -1);
    }

    public int getPhase() {
        return this.entityData.get(PHASE);
    }

    public int getAction() {
        return this.entityData.get(ACTION);
    }

    /** Ticks left of the transformation between movements (0 when not transforming). */
    public int transformTicks() {
        return this.entityData.get(TRANSFORM);
    }

    public float actionTime(float partial) {
        return this.tickCount - this.actionStart + partial;
    }

    public @Nullable LivingEntity beamTarget() {
        int id = this.entityData.get(BEAM_TARGET);
        return id >= 0 && this.level().getEntity(id) instanceof LivingEntity le ? le : null;
    }

    private void setAction(int a) {
        this.entityData.set(ACTION, a);
        this.actionTicks = 0;
        this.actionStart = this.tickCount;
        if (a != BEAM_CHARGE && a != BEAM_LOCK) {
            this.entityData.set(BEAM_TARGET, -1);
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (ACTION.equals(accessor)) {
            this.actionStart = this.tickCount;
        }
    }

    private boolean groundFighting() {
        return this.getPhase() != 2 && this.getAction() == NONE && this.transformTicks() == 0;
    }

    public void setPodium(BlockPos pos) {
        this.podium = pos.immutable();
    }

    /** His entrance, straight out of the Mask. */
    public void arrive(ServerLevel level) {
        this.level().broadcastEntityEvent(this, EVENT_ROAR);
        this.playSound(ModSounds.DICTATOR_ROAR.get(), 6.0F, 0.8F);
        this.entityData.set(TRANSFORM, 40);
    }

    // ------------------------------------------------------------------ the performance

    private void trackHealth(ServerLevel level) {
        float hp = this.getHealth() / this.getMaxHealth();
        this.bossEvent.setProgress(hp);
        int phase = hp > 0.66F ? 1 : hp > 0.33F ? 2 : 3;
        if (phase != this.getPhase() && this.isAlive()) {
            this.enterPhase(level, phase);
        }
    }

    private void enterPhase(ServerLevel level, int phase) {
        this.entityData.set(PHASE, phase);
        this.entityData.set(TRANSFORM, TRANSFORM_TICKS);
        this.setAction(NONE);
        this.ringTicks = -1;
        this.level().broadcastEntityEvent(this, EVENT_ROAR);
        this.playSound(ModSounds.DICTATOR_ROAR.get(), 6.0F, phase == 3 ? 0.7F : 0.9F);
        this.playSound(ModSounds.CONDUCTOR_MASK_TRANSFORM.get(), 4.0F, 0.8F + phase * 0.1F);
        level.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY() + 2.0, this.getZ(), 60, 1.5, 1.5, 1.5, 0.08);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(phase == 3 ? 0.4 : 0.34);
    }

    private void tickTransform(ServerLevel level) {
        int left = this.transformTicks();
        int t = TRANSFORM_TICKS - left;
        this.entityData.set(TRANSFORM, left - 1);
        this.getNavigation().stop();
        // lifts off the stage, hangs there in a storm of sculk, and comes down changed
        double lift = t < 30 ? 0.08 : t < 50 ? 0.0 : this.getPhase() == 2 ? 0.0 : -0.06;
        this.setDeltaMovement(0.0, lift, 0.0);
        if (t % 4 == 0) {
            level.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY() + 1.5, this.getZ(), 10, 1.2, 1.5, 1.2, 0.05);
            level.sendParticles(ModParticles.PORTAL_SOUL.get(), this.getX(), this.getY() + 1.5, this.getZ(), 6, 1.0, 1.2, 1.0, 0.05);
        }
        if (t == 40) {
            // the change: a blinding burst, rings across the stage
            level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 2.0, this.getZ(), 60, 0.3, 0.3, 0.3, 0.4);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1.5, this.getZ(), 1, 0, 0, 0, 0);
            for (int i = 0; i < 4; i++) {
                level.sendParticles(ModParticles.RESONANCE_RING.get(), this.getX(), this.getY() + 0.2, this.getZ(), 0, 2.0 + i * 2.5, 0.0, 0.0, 1.0);
            }
            this.playSound(SoundEvents.WARDEN_SONIC_BOOM, 4.0F, 0.6F);
            for (Player p : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(6.0))) {
                Vec3 away = p.position().subtract(this.position()).multiply(1, 0, 1).normalize();
                p.push(away.x * 1.2, 0.5, away.z * 1.2);
                p.hurtMarked = true;
            }
            this.level().broadcastEntityEvent(this, EVENT_CRESCENDO);
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.transformTicks() > 0) {
            this.tickTransform(level);
            return;
        }
        int phase = this.getPhase();
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            if (phase == 2) {
                this.setDeltaMovement(this.getDeltaMovement().scale(0.8));
            }
            return;
        }
        this.actionTicks++;
        if (phase == 2) {
            this.flyAround(target);
        }
        if (this.getAction() != NONE) {
            this.tickAction(level, target);
        } else {
            if (this.actionCooldown > 0) {
                this.actionCooldown--;
            } else {
                this.chooseAction(target, phase);
            }
            if (phase != 2 && --this.blinkCooldown <= 0 && (this.distanceTo(target) > 7.0 || this.random.nextInt(3) == 0)) {
                this.blinkBehind(level, target);
                this.blinkCooldown = phase == 3 ? 70 : 120;
            }
        }
        if (--this.summonCooldown <= 0) {
            this.summonSection(level, phase);
            this.summonCooldown = phase == 3 ? 200 : 300;
        }
        if (this.ringTicks >= 0) {
            this.rollRings(level);
        }
        if (phase == 3 && --this.darknessCooldown <= 0) {
            this.darknessCooldown = 260;
            for (Player p : level.getEntitiesOfClass(Player.class, new AABB(this.blockPosition()).inflate(32.0))) {
                p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0), this);
            }
        }
    }

    private void chooseAction(LivingEntity target, int phase) {
        double d = this.distanceTo(target);
        int roll = this.random.nextInt(10);
        switch (phase) {
            case 1 -> {
                if (d < 7.0 && roll < 6) {
                    this.setAction(SLAM);
                } else if (d < 20.0 && this.hasLineOfSight(target)) {
                    this.setAction(CHARGE_WINDUP);
                }
            }
            case 2 -> {
                if (d < 30.0 && this.hasLineOfSight(target) && roll < 6) {
                    this.setAction(BEAM_CHARGE);
                    this.entityData.set(BEAM_TARGET, target.getId());
                    this.playSound(ModSounds.WHISTLER_CHARGE.get(), 3.0F, 0.8F);
                } else {
                    this.setAction(DIVE);
                    this.playSound(ModSounds.WHISTLER_SCREECH.get(), 3.0F, 0.8F);
                }
            }
            default -> {
                if (roll < 3) {
                    this.setAction(STRUM);
                } else if (d < 16.0 && roll < 6 && this.hasLineOfSight(target)) {
                    this.setAction(SNAP);
                } else if (d < 24.0) {
                    this.setAction(WEB);
                }
                if (this.random.nextInt(3) == 0 && this.ringTicks < 0) {
                    this.startRings();
                }
            }
        }
        if (this.getAction() != NONE) {
            this.getNavigation().stop();
        }
    }

    private void endAction(int cooldown) {
        this.setAction(NONE);
        this.actionCooldown = cooldown + this.random.nextInt(20);
    }

    private void tickAction(ServerLevel level, LivingEntity target) {
        int t = this.actionTicks;
        switch (this.getAction()) {
            case SLAM -> {
                // the Thumper's slam, with sculk rings rolling out after it
                this.getNavigation().stop();
                if (t == 1) {
                    this.level().broadcastEntityEvent(this, EVENT_CRESCENDO);
                }
                if (t == 18) {
                    this.playSound(ModSounds.THUMPER_SLAM.get(), 4.0F, 0.6F);
                    BlockState ground = level.getBlockState(this.blockPosition().below());
                    if (!ground.isAir()) {
                        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), this.getX(), this.getY() + 0.1, this.getZ(), 60, 2.5, 0.1, 2.5, 0.3);
                    }
                    this.hitAround(level, 5.0, 10.0F, 1.2, 0.7);
                    this.startRings();
                }
                if (t >= 30) {
                    this.endAction(40);
                }
            }
            case CHARGE_WINDUP -> {
                this.getNavigation().stop();
                this.getLookControl().setLookAt(target, 30.0F, 30.0F);
                if (t % 4 == 0) {
                    this.playSound(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 2.5F, 0.6F + t * 0.02F);
                }
                if (t >= 20) {
                    Vec3 d = target.position().subtract(this.position()).multiply(1, 0, 1);
                    this.chargeDir = d.lengthSqr() < 1.0E-4 ? this.getLookAngle().multiply(1, 0, 1).normalize() : d.normalize();
                    this.hits.clear();
                    this.playSound(ModSounds.THUMPER_ROAR.get(), 3.0F, 0.8F);
                    this.setAction(CHARGE);
                }
            }
            case CHARGE -> {
                double speed = Math.min(0.95, 0.35 + t * 0.07);
                this.setDeltaMovement(this.chargeDir.x * speed, this.getDeltaMovement().y, this.chargeDir.z * speed);
                float yaw = (float) (Mth.atan2(this.chargeDir.z, this.chargeDir.x) * Mth.RAD_TO_DEG) - 90.0F;
                this.setYRot(yaw);
                this.yBodyRot = yaw;
                if (t % 2 == 0) {
                    level.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY() + 0.5, this.getZ(), 3, 0.5, 0.3, 0.5, 0.02);
                }
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.8))) {
                    if (e != this && !MiniBoss.isBandmate(e) && this.hits.add(e.getId()) && e.hurtServer(level, this.damageSources().mobAttack(this), 14.0F)) {
                        e.push(this.chargeDir.x * 2.0, 0.6, this.chargeDir.z * 2.0);
                        e.hurtMarked = true;
                        this.corrupt(e);
                    }
                }
                if ((this.horizontalCollision && t > 4) || t >= 36) {
                    this.endAction(40);
                }
            }
            case BEAM_CHARGE -> {
                if (t % 5 == 0) {
                    this.playSound(SoundEvents.NOTE_BLOCK_FLUTE.value(), 2.5F, 0.5F + t * 0.03F);
                }
                if (!this.hasLineOfSight(target)) {
                    this.endAction(30);
                } else if (t >= 28) {
                    this.lockDamage = 0.0F;
                    this.playSound(ModSounds.WHISTLER_LOCK.get(), 3.0F, 0.8F);
                    this.setAction(BEAM_LOCK);
                    this.entityData.set(BEAM_TARGET, target.getId());
                }
            }
            case BEAM_LOCK -> {
                if (!this.hasLineOfSight(target) || this.distanceTo(target) > 34.0) {
                    this.playSound(ModSounds.WHISTLER_BREAK.get(), 2.5F, 1.0F);
                    this.endAction(50);
                    return;
                }
                if (t % 10 == 0) {
                    target.hurtServer(level, this.damageSources().sonicBoom(this), 2.5F + t / 40.0F);
                    target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 1), this);
                    this.corrupt(target);
                    this.playSound(SoundEvents.NOTE_BLOCK_FLUTE.value(), 2.5F, 0.6F + (t % 40) * 0.02F);
                }
                if (t >= 150) {
                    this.endAction(60);
                }
            }
            case DIVE -> {
                if (t < 18) {
                    this.setDeltaMovement(this.getDeltaMovement().scale(0.8).add(0, 0.12, 0));
                } else if (t == 18) {
                    this.chargeDir = target.position().add(0, 0.5, 0).subtract(this.position()).normalize();
                    this.hits.clear();
                    this.playSound(ModSounds.WHISTLER_DIVE.get(), 3.0F, 0.8F);
                } else {
                    this.setDeltaMovement(this.chargeDir.scale(Math.min(1.3, 0.5 + (t - 18) * 0.1)));
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.8))) {
                        if (e != this && !MiniBoss.isBandmate(e) && this.hits.add(e.getId()) && e.hurtServer(level, this.damageSources().mobAttack(this), 12.0F)) {
                            e.push(this.chargeDir.x, 0.6, this.chargeDir.z);
                            e.hurtMarked = true;
                            this.corrupt(e);
                        }
                    }
                    if (t >= 46 || ((this.horizontalCollision || this.verticalCollision) && t > 22)) {
                        this.endAction(40);
                    }
                }
            }
            case WEB -> {
                this.getNavigation().stop();
                this.getLookControl().setLookAt(target, 30.0F, 30.0F);
                if (t == 12) {
                    this.playSound(ModSounds.STRUMMER_SPIT.get(), 2.5F, 0.8F);
                    for (int i = 0; i < 5; i++) {
                        WebShot w = new WebShot(level, this);
                        w.setPos(this.getX(), this.getY() + 2.5, this.getZ());
                        Vec3 d = target.position().add(0, target.getBbHeight() * 0.4, 0).subtract(w.position());
                        Vec3 side = new Vec3(-d.z, 0, d.x).normalize().scale(d.length() * (i - 2) * 0.16);
                        Vec3 aim = d.add(side).add(0, d.horizontalDistance() * 0.08, 0);
                        w.shoot(aim.x, aim.y, aim.z, 1.2F, 2.0F);
                        level.addFreshEntity(w);
                    }
                }
                if (t >= 22) {
                    this.endAction(20);
                }
            }
            case SNAP -> {
                this.getNavigation().stop();
                this.getLookControl().setLookAt(target, 30.0F, 30.0F);
                if (t == 1) {
                    this.playSound(ModSounds.STRUMMER_DRAW.get(), 2.5F, 0.8F);
                }
                if (t == 14 && this.hasLineOfSight(target)) {
                    this.playSound(ModSounds.STRUMMER_PLUCK.get(), 3.0F, 0.8F);
                    Vec3 from = this.position().add(0, 2.5, 0);
                    Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0);
                    Vec3 d = to.subtract(from);
                    DustParticleOptions dust = new DustParticleOptions(0x2EF2E2, 1.0F);
                    for (int i = 0; i < d.length() * 4; i++) {
                        Vec3 p = from.add(d.scale(i / (d.length() * 4)));
                        level.sendParticles(dust, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                    }
                    if (target.hurtServer(level, this.damageSources().mobAttack(this), 6.0F)) {
                        Vec3 pull = d.normalize().scale(-1.4);
                        target.push(pull.x, 0.45, pull.z);
                        target.hurtMarked = true;
                        this.corrupt(target);
                    }
                }
                if (t >= 24) {
                    this.endAction(20);
                }
            }
            case STRUM -> {
                if (t % 5 == 0 && t <= 30) {
                    int[] chord = {0, 3, 7, 12, 7, 3, 0};
                    float pitch = (float) Math.pow(2.0, (chord[(t / 5) % chord.length] - 9) / 12.0);
                    this.playSound(SoundEvents.NOTE_BLOCK_GUITAR.value(), 3.0F, pitch);
                    this.playSound(SoundEvents.NOTE_BLOCK_BELL.value(), 1.5F, pitch * 2.0F);
                }
                if (t == 30) {
                    level.sendParticles(ModParticles.RESONANCE_RING.get(), this.getX(), this.getY() + 0.2, this.getZ(), 0, 9.0, 0.0, 0.0, 1.0);
                    level.sendParticles(ModParticles.SIFT_NOTE.get(), this.getX(), this.getY() + 3.0, this.getZ(), 30, 2.5, 1.0, 2.5, 1.0);
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(this.blockPosition()).inflate(18.0))) {
                        if (MiniBoss.isBandmate(e)) {
                            e.addEffect(new MobEffectInstance(MobEffects.SPEED, 160, 1), this);
                            e.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 160, 1), this);
                            e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0), this);
                        }
                    }
                    TemporaryBlocks.webs(level, this.blockPosition(), 4, 0.06F, 160);
                }
                if (t >= 40) {
                    this.endAction(20);
                }
            }
            default -> this.endAction(20);
        }
    }

    /** In his second movement he flies, wheeling above the target like the Whistler. */
    private void flyAround(LivingEntity target) {
        int a = this.getAction();
        if (a == DIVE && this.actionTicks >= 18) {
            return;
        }
        float orbit = this.tickCount * (a == NONE ? 0.02F : 0.006F);
        Vec3 goal = target.position().add(Mth.cos(orbit) * 8.0, 5.5 + Mth.sin(this.tickCount * 0.05F), Mth.sin(orbit) * 8.0);
        Vec3 d = goal.subtract(this.position());
        Vec3 v = this.getDeltaMovement().scale(0.9).add(d.normalize().scale(Math.min(1.0, d.length() * 0.25) * 0.06));
        if (v.length() > 0.55) {
            v = v.normalize().scale(0.55);
        }
        this.setDeltaMovement(v);
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        this.setYRot(Mth.approachDegrees(this.getYRot(), yaw, 10.0F));
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();
    }

    private void hitAround(ServerLevel level, double radius, float damage, double push, double lift) {
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius, 2.0, radius))) {
            if (e == this || MiniBoss.isBandmate(e) || !e.onGround() || e.distanceTo(this) > radius + 0.5) {
                continue;
            }
            if (e.hurtServer(level, this.damageSources().mobAttack(this), damage)) {
                Vec3 away = e.position().subtract(this.position()).multiply(1, 0, 1).normalize();
                e.push(away.x * push, lift, away.z * push);
                e.hurtMarked = true;
                this.corrupt(e);
            }
        }
    }

    /** Sculk magic: every blow leaves the corruption behind, and it builds up. */
    private void corrupt(LivingEntity e) {
        int phase = this.getPhase();
        MobEffectInstance had = e.getEffect(ModEffects.SCULK_CORRUPTION);
        int duration = 120 + phase * 60 + (had != null ? Math.min(400, had.getDuration()) : 0);
        e.addEffect(new MobEffectInstance(ModEffects.SCULK_CORRUPTION, duration, phase - 1), this);
    }

    /** Vanishes in a burst of sculk souls and reappears just behind the target. */
    private void blinkBehind(ServerLevel level, LivingEntity target) {
        Vec3 look = target.getLookAngle().multiply(1, 0, 1).normalize();
        for (double back = 2.5; back >= 1.0; back -= 0.75) {
            Vec3 to = target.position().subtract(look.scale(back));
            BlockPos at = BlockPos.containing(to);
            if (level.getBlockState(at).getCollisionShape(level, at).isEmpty() && level.getBlockState(at.above()).getCollisionShape(level, at.above()).isEmpty()
                    && level.getBlockState(at.above(2)).getCollisionShape(level, at.above(2)).isEmpty()
                    && !level.getBlockState(at.below()).getCollisionShape(level, at.below()).isEmpty()) {
                level.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY() + 1.5, this.getZ(), 20, 0.4, 1.2, 0.4, 0.02);
                this.level().broadcastEntityEvent(this, EVENT_BLINK);
                this.teleportTo(to.x, at.getY(), to.z);
                this.getLookControl().setLookAt(target);
                level.sendParticles(ModParticles.PORTAL_SOUL.get(), this.getX(), this.getY() + 1.5, this.getZ(), 16, 0.4, 1.2, 0.4, 0.05);
                this.playSound(ModSounds.DICTATOR_BLINK.get(), 2.0F, 1.0F);
                return;
            }
        }
    }

    /** Calls up the young of the player whose power he wears. */
    private void summonSection(ServerLevel level, int phase) {
        EntityType<? extends OrchestraMinion> type = switch (phase) {
            case 1 -> ModEntities.THUMPLING.get();
            case 2 -> ModEntities.WHISTLING.get();
            default -> ModEntities.STRUMLING.get();
        };
        int alive = level.getEntities(type, new AABB(this.blockPosition()).inflate(40.0), Entity::isAlive).size();
        int max = phase == 3 ? 8 : 5;
        int count = Math.min(max - alive, phase == 3 ? 4 : 2);
        if (count <= 0) {
            return;
        }
        this.level().broadcastEntityEvent(this, EVENT_SUMMON);
        this.playSound(ModSounds.DICTATOR_SUMMON.get(), 3.0F, 1.0F);
        double floor = this.podium != null ? this.podium.getY() + 1 : this.getY();
        for (int i = 0; i < count; i++) {
            double a = this.random.nextDouble() * Math.PI * 2;
            BlockPos at = BlockPos.containing(this.getX() + Math.cos(a) * 6, floor, this.getZ() + Math.sin(a) * 6);
            SpawnUtil.trySpawnMob(type, EntitySpawnReason.MOB_SUMMONED, level, at, 10, 3, 3, SpawnUtil.Strategy.ON_TOP_OF_COLLIDER, false).ifPresent(m -> {
                m.setTarget(this.getTarget());
                level.sendParticles(ParticleTypes.SCULK_SOUL, m.getX(), m.getY() + 0.5, m.getZ(), 12, 0.4, 0.4, 0.4, 0.05);
            });
        }
    }

    private void startRings() {
        this.ringTicks = 0;
        this.level().broadcastEntityEvent(this, EVENT_CRESCENDO);
        this.playSound(ModSounds.DICTATOR_CRESCENDO.get(), 4.0F, 1.0F);
    }

    /** Rings of sculk sound rolling out across the stage; anyone standing on the ring is struck. */
    private void rollRings(ServerLevel level) {
        int t = this.ringTicks++;
        if (t < 10) {
            return;
        }
        int k = t - 10;
        if (k > 54) {
            this.ringTicks = -1;
            return;
        }
        double baseY = this.podium != null ? this.podium.getY() + 1 : this.getY();
        for (int wave = 0; wave < 3; wave++) {
            int wk = k - wave * 14;
            if (wk < 0 || wk > 26) {
                continue;
            }
            double r = 1.5 + wk * 0.55;
            if (wk % 2 == 0) {
                level.sendParticles(ModParticles.RESONANCE_RING.get(), this.getX(), baseY + 0.1, this.getZ(), 0, r, 0.0, 0.0, 1.0);
            }
            if (wk == 0) {
                this.playSound(ModSounds.THUMPER_SLAM.get(), 3.0F, 1.2F + wave * 0.15F);
            }
            AABB box = new AABB(this.getX() - r - 1, baseY - 1, this.getZ() - r - 1, this.getX() + r + 1, baseY + 2, this.getZ() + r + 1);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box)) {
                if (e == this || MiniBoss.isBandmate(e) || !e.onGround()) {
                    continue;
                }
                double d = Math.sqrt(e.distanceToSqr(this.getX(), e.getY(), this.getZ()));
                if (Math.abs(d - r) < 0.7 && e.hurtServer(level, this.damageSources().sonicBoom(this), 7.0F)) {
                    e.push(0.0, 0.6, 0.0);
                    this.corrupt(e);
                }
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean bypass = source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        if (!bypass && this.transformTicks() > 0) {
            return false;
        }
        if (source.getEntity() instanceof LivingEntity le && MiniBoss.isBandmate(le)) {
            return false;
        }
        if (!bypass && this.getPhase() == 1 && this.hitFromBehind(source)) {
            // the Thumper's shell on his back
            this.playSound(ModSounds.THUMPER_CLANK.get(), 1.5F, 0.8F);
            level.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 2.0, this.getZ(), 8, 0.4, 0.4, 0.4, 0.3);
            return false;
        }
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && this.getAction() == BEAM_LOCK) {
            this.lockDamage += amount;
            if (this.lockDamage >= 16.0F) {
                this.playSound(ModSounds.WHISTLER_BREAK.get(), 3.0F, 0.7F);
                this.endAction(60);
            }
        }
        // every blow moves the performance on, even before his next tick
        this.trackHealth(level);
        return hurt;
    }

    private boolean hitFromBehind(DamageSource source) {
        Entity e = source.getDirectEntity() instanceof Projectile p ? p : source.getEntity();
        if (e == null) {
            return false;
        }
        Vec3 to = e.position().subtract(this.position()).multiply(1, 0, 1);
        if (to.lengthSqr() < 1.0E-4) {
            return false;
        }
        Vec3 facing = Vec3.directionFromRotation(0.0F, this.yBodyRot).multiply(1, 0, 1).normalize();
        return to.normalize().dot(facing) < -0.35;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        this.level().broadcastEntityEvent(this, EVENT_SLASH);
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity le) {
            this.corrupt(le);
        }
        return hit;
    }

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EVENT_BLINK -> this.blinkAnimation.start(this.tickCount);
            case EVENT_SUMMON -> this.summonAnimation.start(this.tickCount);
            case EVENT_CRESCENDO -> this.crescendoAnimation.start(this.tickCount);
            case EVENT_SLASH -> this.slashAnimation.start(this.tickCount);
            case EVENT_ROAR -> this.roarAnimation.start(this.tickCount);
            default -> super.handleEntityEvent(id);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel server) {
            this.trackHealth(server);
            this.setNoGravity(this.getPhase() == 2 || this.transformTicks() > 20);
        }
        if (this.level().isClientSide()) {
            this.clientEffects();
        }
    }

    private void clientEffects() {
        Level level = this.level();
        if (this.random.nextInt(4) == 0) {
            level.addParticle(ModParticles.GLOW_DUST.get(), this.getRandomX(0.6), this.getY() + this.random.nextDouble() * 3.0, this.getRandomZ(0.6), 0, -0.01, 0);
        }
        if (this.deathTime > 2) {
            for (int i = 0; i < 3; i++) {
                level.addParticle(ParticleTypes.SCULK_SOUL, this.getRandomX(0.8), this.getY() + this.random.nextDouble() * 3.2, this.getRandomZ(0.8), 0, 0.05, 0);
            }
        }
        if (this.transformTicks() > 0) {
            for (int i = 0; i < 4; i++) {
                double a = this.random.nextDouble() * Math.PI * 2;
                double r = 1.5 + this.random.nextDouble() * 2.0;
                level.addParticle(ParticleTypes.SCULK_SOUL, this.getX() + Math.cos(a) * r, this.getY() + this.random.nextDouble() * 4.0, this.getZ() + Math.sin(a) * r,
                        -Math.cos(a) * 0.1, 0.02, -Math.sin(a) * 0.1);
            }
        }
        int phase = this.getPhase();
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        if (phase == 2 && this.deathTime == 0) {
            // soul trails from the stolen wings
            float flap = Mth.sin(this.tickCount * 0.3F) * 0.6F;
            for (int side = -1; side <= 1; side += 2) {
                double x = this.getX() + Mth.cos(yaw) * 2.4 * side * Math.cos(flap) + Mth.sin(yaw) * 0.6;
                double z = this.getZ() + Mth.sin(yaw) * 2.4 * side * Math.cos(flap) - Mth.cos(yaw) * 0.6;
                level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, this.getY() + 3.0 + Math.sin(flap) * 2.4, z, 0, 0.01, 0);
            }
        }
        if (phase == 3 && this.random.nextInt(2) == 0) {
            for (int side = -1; side <= 1; side += 2) {
                double x = this.getX() + Mth.cos(yaw) * 0.9 * side;
                double z = this.getZ() + Mth.sin(yaw) * 0.9 * side;
                level.addParticle(new DustParticleOptions(0x2EF2E2, 0.7F), x, this.getY() + 0.8 + this.random.nextDouble() * 1.4, z, 0, 0, 0);
            }
        }
        LivingEntity target = this.beamTarget();
        int a = this.getAction();
        if (target != null && (a == BEAM_CHARGE || a == BEAM_LOCK)) {
            Vec3 from = this.position().add(0, 3.3, 0).add(Vec3.directionFromRotation(0.0F, this.yHeadRot).scale(0.6));
            Vec3 to = target.position().add(0, target.getBbHeight() * 0.6, 0);
            Vec3 d = to.subtract(from);
            boolean locked = a == BEAM_LOCK;
            int n = locked ? (int) (d.length() * 3) : (int) (d.length() * 0.8);
            DustParticleOptions dust = new DustParticleOptions(locked ? 0x2EF2E2 : 0xBFFBFF, locked ? 1.4F : 0.6F);
            for (int i = 0; i < n; i++) {
                Vec3 p = from.add(d.scale(this.random.nextDouble()));
                level.addParticle(dust, p.x, p.y, p.z, 0, 0, 0);
            }
            if (locked && this.tickCount % 3 == 0) {
                Vec3 p = from.add(d.scale((this.tickCount % 12) / 12.0));
                level.addParticle(ParticleTypes.SONIC_BOOM, p.x, p.y, p.z, 0, 0, 0);
            }
        }
    }

    // ------------------------------------------------------------------ the end of the performance

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.level() instanceof ServerLevel level) {
            this.spawnAtLocation(level, new ItemStack(ModItems.CONDUCTORS_STAFF.get()));
            for (OrchestraMinion m : level.getEntitiesOfClass(OrchestraMinion.class, new AABB(this.blockPosition()).inflate(48.0))) {
                m.kill(level);
            }
            if (this.podium != null && level.getBlockEntity(this.podium) instanceof ConductorsPodiumBlockEntity p) {
                p.setDefeated();
            }
            level.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY() + 1.5, this.getZ(), 80, 1.2, 1.5, 1.2, 0.08);
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.DICTATOR_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.DICTATOR_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.DICTATOR_DEATH.get();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Phase", this.getPhase());
        if (this.podium != null) {
            output.putInt("PodiumX", this.podium.getX());
            output.putInt("PodiumY", this.podium.getY());
            output.putInt("PodiumZ", this.podium.getZ());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(PHASE, input.getIntOr("Phase", 1));
        if (input.getIntOr("PodiumY", Integer.MIN_VALUE) != Integer.MIN_VALUE) {
            this.podium = new BlockPos(input.getIntOr("PodiumX", 0), input.getIntOr("PodiumY", 0), input.getIntOr("PodiumZ", 0));
        }
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }

    /** The last chord: a storm of notes and souls. */
    @Override
    public void makePoofParticles() {
        KillBurst.pop(this, 0x2EF2E2, 0xFFFFFF, KillBurst.NOTE, ParticleTypes.SCULK_SOUL);
        KillBurst.pop(this, 0x7A5CFF, 0x2EF2E2, KillBurst.STAR, ModParticles.PORTAL_SOUL.get());
    }
}
