package com.thesift.entity.boss;

import com.thesift.block.entity.ConductorsPodiumBlockEntity;
import com.thesift.entity.KillBurst;
import com.thesift.registry.ModEffects;
import com.thesift.registry.ModItems;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Conductor. When the performance on the Grand Stage ends only his Mask is left, lying on the
 * floor - then souls, notes and sculk pour in from all round the stage and build his body again
 * around it, piece by piece. He needs no orchestra: every movement he plays alone, each more
 * godlike than the last, and every blow of his leaves Sculk Corruption.
 *
 * <ol>
 *   <li>Full health to 66%: a duelist on the stage - he blinks behind you, lunges and slashes.</li>
 *   <li>66% to 33%: he rises off his feet and floats, firing barrages of homing notes and striking
 *   chords that roll rings of sound across the floor (jump them).</li>
 *   <li>Below 33%: he soars high over the stage on wings of song, rains notes down on marked
 *   spots and, at his finale, floods the whole stage with sound - only a few lit circles are
 *   safe. Spent after each finale, he sinks low for a while.</li>
 * </ol>
 * Between movements he rises in a storm of song and cannot be hurt.
 */
public class Dictator extends Monster {
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(Dictator.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(Dictator.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TRANSFORM = SynchedEntityData.defineId(Dictator.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ASSEMBLE = SynchedEntityData.defineId(Dictator.class, EntityDataSerializers.INT);
    private static final byte EVENT_BLINK = 110;
    private static final byte EVENT_CRESCENDO = 112;
    private static final byte EVENT_SLASH = 113;
    private static final byte EVENT_ROAR = 114;
    public static final int TRANSFORM_TICKS = 60;
    /** How long his body takes to rebuild around the Mask (about six seconds). */
    public static final int ASSEMBLE_TICKS = 120;

    // actions
    public static final int NONE = 0;
    public static final int LUNGE_WINDUP = 1;
    public static final int LUNGE = 2;
    public static final int BARRAGE = 3;
    public static final int CHORD = 4;
    public static final int RAIN = 5;
    public static final int FINALE = 6;
    /** Spent after his finale: low over the stage, in reach. */
    public static final int REST = 7;
    public static final int FINALE_TICKS = 70;
    private static final double SAFE_RADIUS = 2.2;

    public final AnimationState blinkAnimation = new AnimationState();
    public final AnimationState summonAnimation = new AnimationState();
    public final AnimationState crescendoAnimation = new AnimationState();
    public final AnimationState slashAnimation = new AnimationState();
    public final AnimationState roarAnimation = new AnimationState();

    private final ServerBossEvent bossEvent = new ServerBossEvent(UUID.randomUUID(), Component.translatable("entity.thesift.dictator"),
            BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);
    private @Nullable BlockPos podium;
    private double floorY = Double.NaN;
    private int blinkCooldown = 80;
    private int actionCooldown = 50;
    private int darknessCooldown = 160;
    private int ringTicks = -1;
    private int actionTicks;
    public int actionStart;
    private Vec3 chargeDir = Vec3.ZERO;
    private final Set<Integer> hits = new HashSet<>();
    /** His notes in flight: no entities, just points of song the server moves and draws. */
    private final List<Note> notes = new ArrayList<>();
    /** Where falling notes are about to land. */
    private final List<Strike> strikes = new ArrayList<>();
    private final List<Vec3> safeSpots = new ArrayList<>();

    public Dictator(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 400;
        this.bossEvent.setDarkenScreen(true);
        this.bossEvent.setCreateWorldFog(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 450.0).add(Attributes.ARMOR, 10.0).add(Attributes.MOVEMENT_SPEED, 0.36)
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
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, MiniBoss.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASE, 1);
        builder.define(ACTION, NONE);
        builder.define(TRANSFORM, 0);
        builder.define(ASSEMBLE, 0);
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

    /** Ticks left of his body's rebuilding around the Mask (0 once he stands whole). */
    public int assembleTicks() {
        return this.entityData.get(ASSEMBLE);
    }

    public float actionTime(float partial) {
        return this.tickCount - this.actionStart + partial;
    }

    private void setAction(int a) {
        this.entityData.set(ACTION, a);
        this.actionTicks = 0;
        this.actionStart = this.tickCount;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (ACTION.equals(accessor)) {
            this.actionStart = this.tickCount;
        }
    }

    private boolean groundFighting() {
        return this.getPhase() == 1 && this.getAction() == NONE && this.transformTicks() == 0 && this.assembleTicks() == 0;
    }

    private boolean busy() {
        return this.transformTicks() > 0 || this.assembleTicks() > 0;
    }

    public void setPodium(BlockPos pos) {
        this.podium = pos.immutable();
        this.floorY = pos.getY() + 1.0;
    }

    /** The stage floor he fights over. */
    private double floor() {
        if (Double.isNaN(this.floorY)) {
            this.floorY = this.getY();
        }
        return this.floorY;
    }

    private Vec3 stageCentre() {
        return this.podium != null ? Vec3.atBottomCenterOf(this.podium).add(0.0, 1.0, 0.0) : new Vec3(this.getX(), this.floor(), this.getZ());
    }

    /** Called by the fallen Mask: his body begins to rebuild around it. */
    public void rebuild(ServerLevel level) {
        this.entityData.set(ASSEMBLE, ASSEMBLE_TICKS);
        this.playSound(ModSounds.CONDUCTOR_MASK_TRANSFORM.get(), 4.0F, 0.6F);
        for (Player p : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(48.0))) {
            p.sendOverlayMessage(Component.translatable("message.thesift.dictator.rebuild"));
        }
    }

    /** His entrance when raised straight away (commands, tests): a roar, no rebuilding. */
    public void arrive(ServerLevel level) {
        this.level().broadcastEntityEvent(this, EVENT_ROAR);
        this.playSound(ModSounds.DICTATOR_ROAR.get(), 6.0F, 0.8F);
    }

    // ------------------------------------------------------------------ the rebuilding

    /**
     * Souls, notes and sculk stream in from all round the stage. His body comes back in the order
     * the model shows it (DictatorModel): torso, arms, legs, coat, crown - each arrival marked by a
     * chord and a burst - and at the end he rises to his full height and roars.
     */
    private void tickAssemble(ServerLevel level) {
        int left = this.assembleTicks();
        int t = ASSEMBLE_TICKS - left;
        this.entityData.set(ASSEMBLE, left - 1);
        this.getNavigation().stop();
        this.setDeltaMovement(0.0, Math.min(0.0, this.getDeltaMovement().y), 0.0);
        Vec3 c = this.position().add(0.0, Math.min(2.4, 0.3 + t / 40.0), 0.0);
        // the streams: from a wide ring, aimed at where he is forming
        for (int i = 0; i < 3; i++) {
            double a = this.random.nextDouble() * Math.PI * 2.0;
            double r = 6.0 + this.random.nextDouble() * 5.0;
            Vec3 from = new Vec3(this.getX() + Math.cos(a) * r, this.floor() + 0.3 + this.random.nextDouble() * 4.0, this.getZ() + Math.sin(a) * r);
            Vec3 v = c.subtract(from).scale(0.07);
            level.sendParticles(i == 0 ? ModParticles.SIFT_NOTE.get() : ParticleTypes.SCULK_SOUL, from.x, from.y, from.z, 0, v.x, v.y, v.z, 1.0);
        }
        if (t % 3 == 0) {
            level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, c.x, c.y, c.z, 4, 0.4, 0.6, 0.4, 0.02);
        }
        if (t % 10 == 0) {
            // a chord climbing as he forms
            float pitch = (float) Math.pow(2.0, (t / 10 * 2 - 12) / 12.0);
            this.playSound(SoundEvents.NOTE_BLOCK_BELL.value(), 2.0F, pitch);
            this.playSound(SoundEvents.SCULK_CLICKING, 1.5F, 0.6F + t * 0.005F);
        }
        if (t == 30 || t == 50 || t == 66 || t == 82 || t == 100) {
            level.sendParticles(ModParticles.RESONANCE_RING.get(), c.x, c.y, c.z, 0, 1.5 + t * 0.02, 0.0, 0.0, 1.0);
            level.sendParticles(ParticleTypes.SCULK_SOUL, c.x, c.y, c.z, 16, 0.5, 0.8, 0.5, 0.05);
            this.playSound(SoundEvents.NOTE_BLOCK_CHIME.value(), 3.0F, 0.5F + t * 0.006F);
            this.playSound(SoundEvents.SCULK_BLOCK_BREAK, 2.0F, 0.6F);
        }
        if (left <= 1) {
            this.level().broadcastEntityEvent(this, EVENT_ROAR);
            this.playSound(ModSounds.DICTATOR_ROAR.get(), 6.0F, 0.8F);
            this.playSound(SoundEvents.WARDEN_SONIC_BOOM, 4.0F, 0.6F);
            level.sendParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 1.8, this.getZ(), 1, 0, 0, 0, 0);
            for (int i = 0; i < 3; i++) {
                level.sendParticles(ModParticles.RESONANCE_RING.get(), this.getX(), this.floor() + 0.1, this.getZ(), 0, 3.0 + i * 3.0, 0.0, 0.0, 1.0);
            }
            for (Player p : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(6.0))) {
                Vec3 away = p.position().subtract(this.position()).multiply(1, 0, 1).normalize();
                p.push(away.x * 1.2, 0.5, away.z * 1.2);
            }
            Player p = level.getNearestPlayer(this, 48.0);
            if (p != null && !p.isCreative() && !p.isSpectator()) {
                this.setTarget(p);
            }
        }
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
        if (phase > this.getPhase()) {
            BossStages.cleared(level, this, BossStages.CONDUCTOR, phase); // B2 Thumper & cutscenes: the stage cutscene
        }
        this.entityData.set(PHASE, phase);
        this.entityData.set(TRANSFORM, TRANSFORM_TICKS);
        this.setAction(NONE);
        this.ringTicks = -1;
        this.strikes.clear();
        this.safeSpots.clear();
        this.level().broadcastEntityEvent(this, EVENT_ROAR);
        this.playSound(ModSounds.DICTATOR_ROAR.get(), 6.0F, phase == 3 ? 0.7F : 0.9F);
        this.playSound(ModSounds.CONDUCTOR_MASK_TRANSFORM.get(), 4.0F, 0.8F + phase * 0.1F);
        level.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY() + 2.0, this.getZ(), 60, 1.5, 1.5, 1.5, 0.08);
        for (Player p : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(48.0))) {
            p.sendOverlayMessage(Component.translatable("message.thesift.dictator.phase" + phase));
        }
    }

    /** How high over the stage floor he wants to be. */
    private double hoverHeight() {
        int phase = this.getPhase();
        if (phase == 3) {
            return this.getAction() == REST ? 1.6 : 9.0;
        }
        return phase == 2 ? 2.8 : 0.0;
    }

    /** Rises (or sinks) to his new height in a storm of song; at the turn, a burst of power. */
    private void tickTransform(ServerLevel level) {
        int left = this.transformTicks();
        int t = TRANSFORM_TICKS - left;
        this.entityData.set(TRANSFORM, left - 1);
        this.getNavigation().stop();
        double want = this.floor() + Math.max(1.5, this.hoverHeight());
        this.setDeltaMovement(0.0, Mth.clamp((want - this.getY()) * 0.08, -0.15, 0.25), 0.0);
        if (t % 4 == 0) {
            level.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY() + 1.5, this.getZ(), 10, 1.2, 1.5, 1.2, 0.05);
            level.sendParticles(ModParticles.SIFT_NOTE.get(), this.getX(), this.getY() + 2.0, this.getZ(), 4, 1.5, 1.5, 1.5, 1.0);
        }
        if (t == 34) {
            level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 2.0, this.getZ(), 60, 0.3, 0.3, 0.3, 0.4);
            for (int i = 0; i < 4; i++) {
                level.sendParticles(ModParticles.RESONANCE_RING.get(), this.getX(), this.getY() + 0.2, this.getZ(), 0, 2.0 + i * 2.5, 0.0, 0.0, 1.0);
            }
            this.playSound(SoundEvents.WARDEN_SONIC_BOOM, 4.0F, 0.6F);
            for (Player p : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(6.0))) {
                Vec3 away = p.position().subtract(this.position()).multiply(1, 0, 1).normalize();
                p.push(away.x * 1.2, 0.5, away.z * 1.2);
            }
            this.level().broadcastEntityEvent(this, EVENT_CRESCENDO);
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.assembleTicks() > 0) {
            this.tickAssemble(level);
            return;
        }
        this.tickNotes(level);
        this.tickStrikes(level);
        if (this.ringTicks >= 0) {
            this.rollRings(level);
        }
        if (this.transformTicks() > 0) {
            this.tickTransform(level);
            return;
        }
        int phase = this.getPhase();
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            if (phase > 1) {
                this.hover(null);
            }
            return;
        }
        this.actionTicks++;
        if (phase > 1) {
            this.hover(target);
        }
        if (this.getAction() != NONE) {
            this.tickAction(level, target);
        } else {
            if (this.actionCooldown > 0) {
                this.actionCooldown--;
            } else {
                this.chooseAction(target, phase);
            }
            if (phase == 1 && --this.blinkCooldown <= 0 && (this.distanceTo(target) > 7.0 || this.random.nextInt(3) == 0)) {
                this.blinkBehind(level, target);
                this.blinkCooldown = 100;
            }
        }
        if (phase == 3 && --this.darknessCooldown <= 0) {
            this.darknessCooldown = 300;
            for (Player p : level.getEntitiesOfClass(Player.class, new AABB(this.blockPosition()).inflate(32.0))) {
                p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0), this);
            }
        }
    }

    private void chooseAction(LivingEntity target, int phase) {
        double d = this.distanceTo(target);
        int roll = this.random.nextInt(10);
        switch (phase) {
            case 1 -> {
                if (d > 4.0 && d < 16.0 && this.hasLineOfSight(target) && roll < 5) {
                    this.setAction(LUNGE_WINDUP);
                    this.playSound(ModSounds.DICTATOR_CRESCENDO.get(), 2.0F, 1.4F);
                }
            }
            case 2 -> this.setAction(roll < 5 ? BARRAGE : CHORD);
            default -> {
                if (roll < 3) {
                    this.beginFinale();
                } else {
                    this.setAction(roll < 7 ? RAIN : BARRAGE);
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
            case LUNGE_WINDUP -> {
                // the baton drawn back, weight on the back foot
                this.getNavigation().stop();
                this.getLookControl().setLookAt(target, 30.0F, 30.0F);
                if (t >= 14) {
                    Vec3 d = target.position().subtract(this.position()).multiply(1, 0, 1);
                    this.chargeDir = d.lengthSqr() < 1.0E-4 ? this.getLookAngle().multiply(1, 0, 1).normalize() : d.normalize();
                    this.hits.clear();
                    this.level().broadcastEntityEvent(this, EVENT_SLASH);
                    this.playSound(ModSounds.DICTATOR_BLINK.get(), 2.0F, 1.5F);
                    this.setAction(LUNGE);
                }
            }
            case LUNGE -> {
                this.setDeltaMovement(this.chargeDir.x * 1.1, this.getDeltaMovement().y, this.chargeDir.z * 1.1);
                float yaw = (float) (Mth.atan2(this.chargeDir.z, this.chargeDir.x) * Mth.RAD_TO_DEG) - 90.0F;
                this.setYRot(yaw);
                this.yBodyRot = yaw;
                level.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY() + 1.0, this.getZ(), 2, 0.3, 0.4, 0.3, 0.01);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.9))) {
                    if (e != this && !MiniBoss.isBandmate(e) && this.hits.add(e.getId()) && e.hurtServer(level, this.damageSources().mobAttack(this), 12.0F)) {
                        e.push(this.chargeDir.x * 1.4, 0.4, this.chargeDir.z * 1.4);
                        this.corrupt(e);
                    }
                }
                if ((this.horizontalCollision && t > 2) || t >= 10) {
                    this.setDeltaMovement(this.getDeltaMovement().multiply(0.2, 1.0, 0.2));
                    this.endAction(30);
                }
            }
            case BARRAGE -> {
                // three volleys of notes, fanned out, curling in after their target
                this.getLookControl().setLookAt(target, 30.0F, 30.0F);
                if (t == 10 || t == 22 || t == 34) {
                    this.volley(level, target, this.getPhase() == 3 ? 7 : 5);
                }
                if (t >= 46) {
                    this.endAction(this.getPhase() == 3 ? 20 : 35);
                }
            }
            case CHORD -> {
                // gathers, then strikes a chord with both hands: rings roll across the floor
                if (t == 1) {
                    this.level().broadcastEntityEvent(this, EVENT_CRESCENDO);
                    this.playSound(ModSounds.DICTATOR_CRESCENDO.get(), 3.0F, 1.0F);
                }
                if (t == 22) {
                    int[] chord = {0, 4, 7, 12};
                    for (int n : chord) {
                        this.playSound(SoundEvents.NOTE_BLOCK_HARP.value(), 3.0F, (float) Math.pow(2.0, (n - 12) / 12.0));
                    }
                    this.playSound(ModSounds.THUMPER_SLAM.get(), 3.0F, 1.3F);
                    level.sendParticles(ModParticles.SIFT_NOTE.get(), this.getX(), this.getY() + 1.5, this.getZ(), 24, 2.0, 1.0, 2.0, 1.0);
                    this.startRings();
                }
                if (t >= 40) {
                    this.endAction(40);
                }
            }
            case RAIN -> {
                // notes rain down on marked spots round the target, faster and faster
                if (t % Math.max(3, 8 - t / 12) == 0 && t < 70) {
                    double a = this.random.nextDouble() * Math.PI * 2.0;
                    double r = t % 3 == 0 ? 0.0 : 1.5 + this.random.nextDouble() * 5.0;
                    Vec3 at = new Vec3(target.getX() + Math.cos(a) * r, this.floor(), target.getZ() + Math.sin(a) * r);
                    this.strikes.add(new Strike(at, 24));
                }
                if (t % 8 == 0) {
                    this.playSound(SoundEvents.NOTE_BLOCK_CHIME.value(), 2.5F, 0.5F + this.random.nextFloat() * 0.6F);
                }
                if (t >= 90) {
                    this.endAction(30);
                }
            }
            case FINALE -> this.tickFinale(level, t);
            case REST -> {
                if (t % 20 == 0) {
                    this.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 2.0F, 0.5F);
                }
                if (t >= 90) {
                    this.endAction(20);
                }
            }
            default -> this.endAction(20);
        }
    }

    /** Floats over the stage, keeping his height and circling his target. */
    private void hover(@Nullable LivingEntity target) {
        int a = this.getAction();
        Vec3 centre = target != null ? target.position() : this.stageCentre();
        double radius = this.getPhase() == 3 && a != REST ? 9.0 : 6.0;
        float speed = a == NONE ? 0.018F : 0.006F;
        float orbit = this.tickCount * speed;
        Vec3 goal = new Vec3(centre.x + Mth.cos(orbit) * radius, this.floor() + this.hoverHeight() + Mth.sin(this.tickCount * 0.05F) * 0.4,
                centre.z + Mth.sin(orbit) * radius);
        if (a == FINALE || a == REST) {
            // the finale is played from above the middle of the stage
            Vec3 s = this.stageCentre();
            goal = new Vec3(s.x, this.floor() + this.hoverHeight(), s.z);
        }
        Vec3 d = goal.subtract(this.position());
        Vec3 v = this.getDeltaMovement().scale(0.9).add(d.normalize().scale(Math.min(1.0, d.length() * 0.25) * 0.06));
        if (v.length() > 0.5) {
            v = v.normalize().scale(0.5);
        }
        this.setDeltaMovement(v);
        if (target != null) {
            double dx = target.getX() - this.getX();
            double dz = target.getZ() - this.getZ();
            float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
            this.setYRot(Mth.approachDegrees(this.getYRot(), yaw, 10.0F));
            this.yBodyRot = this.getYRot();
            this.yHeadRot = this.getYRot();
        }
    }

    // ------------------------------------------------------------------ his music, made solid

    private void volley(ServerLevel level, LivingEntity target, int count) {
        Vec3 from = this.position().add(0.0, 2.6, 0.0);
        Vec3 aim = target.getEyePosition().subtract(from).normalize();
        Vec3 side = new Vec3(-aim.z, 0.0, aim.x).normalize();
        for (int i = 0; i < count; i++) {
            double k = (i - (count - 1) / 2.0) * 0.22;
            Vec3 dir = aim.add(side.scale(k)).add(0.0, Math.abs(k) * 0.3, 0.0).normalize();
            this.notes.add(new Note(from, dir.scale(0.55), target.getId()));
        }
        this.level().broadcastEntityEvent(this, EVENT_SLASH);
        int[] arp = {0, 4, 7};
        this.playSound(SoundEvents.NOTE_BLOCK_FLUTE.value(), 2.5F, (float) Math.pow(2.0, (arp[this.random.nextInt(3)] - 6) / 12.0));
    }

    private void tickNotes(ServerLevel level) {
        DustParticleOptions dust = new DustParticleOptions(0x2EF2E2, 1.1F);
        for (Iterator<Note> it = this.notes.iterator(); it.hasNext(); ) {
            Note n = it.next();
            n.life++;
            if (level.getEntity(n.target) instanceof LivingEntity t && n.life < 40) {
                Vec3 want = t.getBoundingBox().getCenter().subtract(n.pos).normalize().scale(0.55);
                n.vel = n.vel.lerp(want, 0.05);
            }
            n.pos = n.pos.add(n.vel);
            level.sendParticles(ModParticles.SIFT_NOTE.get(), n.pos.x, n.pos.y, n.pos.z, 1, 0.0, 0.0, 0.0, 1.0);
            level.sendParticles(dust, n.pos.x, n.pos.y, n.pos.z, 1, 0.05, 0.05, 0.05, 0.0);
            boolean done = n.life > 90 || !level.getBlockState(BlockPos.containing(n.pos)).getCollisionShape(level, BlockPos.containing(n.pos)).isEmpty();
            if (!done) {
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(n.pos, n.pos).inflate(0.6))) {
                    if (e != this && !MiniBoss.isBandmate(e) && e.hurtServer(level, this.damageSources().indirectMagic(this, this), 4.0F)) {
                        this.corrupt(e);
                        done = true;
                        break;
                    }
                }
            }
            if (done) {
                level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, n.pos.x, n.pos.y, n.pos.z, 4, 0.2, 0.2, 0.2, 0.02);
                it.remove();
            }
        }
    }

    /** A falling note: its spot glows on the floor, the note drops, and whatever stands there is struck. */
    private void tickStrikes(ServerLevel level) {
        for (Iterator<Strike> it = this.strikes.iterator(); it.hasNext(); ) {
            Strike s = it.next();
            s.fuse--;
            if (s.fuse % 6 == 0 && s.fuse > 8) {
                level.sendParticles(ModParticles.RESONANCE_RING.get(), s.at.x, s.at.y + 0.1, s.at.z, 0, 1.4, 0.0, 0.0, 1.0);
            }
            if (s.fuse <= 10) {
                level.sendParticles(ModParticles.SIFT_NOTE.get(), s.at.x, s.at.y + s.fuse * 1.2, s.at.z, 1, 0.0, 0.0, 0.0, 1.0);
                level.sendParticles(ParticleTypes.END_ROD, s.at.x, s.at.y + s.fuse * 1.2, s.at.z, 1, 0.05, 0.1, 0.05, 0.0);
            }
            if (s.fuse <= 0) {
                level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, s.at.x, s.at.y + 0.2, s.at.z, 10, 0.6, 0.1, 0.6, 0.05);
                level.sendParticles(ModParticles.SIFT_NOTE.get(), s.at.x, s.at.y + 0.5, s.at.z, 4, 0.6, 0.3, 0.6, 1.0);
                level.playSound(null, s.at.x, s.at.y, s.at.z, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.HOSTILE, 2.0F, 0.5F + this.random.nextFloat() * 0.5F);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(s.at, s.at).inflate(1.5, 2.0, 1.5))) {
                    if (e != this && !MiniBoss.isBandmate(e) && e.position().distanceTo(s.at) < 1.6
                            && e.hurtServer(level, this.damageSources().indirectMagic(this, this), 6.0F)) {
                        this.corrupt(e);
                    }
                }
                it.remove();
            }
        }
    }

    /** His finale: a few circles of light open on the stage; then the whole stage rings, except them. */
    private void beginFinale() {
        this.setAction(FINALE);
        this.safeSpots.clear();
        Vec3 c = this.stageCentre();
        double a0 = this.random.nextDouble() * Math.PI * 2.0;
        for (int i = 0; i < 3; i++) {
            double a = a0 + i * Math.PI * 2.0 / 3.0 + (this.random.nextDouble() - 0.5) * 0.6;
            double r = 3.5 + this.random.nextDouble() * 4.5;
            this.safeSpots.add(new Vec3(c.x + Math.cos(a) * r, this.floor(), c.z + Math.sin(a) * r));
        }
        this.playSound(ModSounds.DICTATOR_CRESCENDO.get(), 5.0F, 0.6F);
        this.level().broadcastEntityEvent(this, EVENT_CRESCENDO);
    }

    private void tickFinale(ServerLevel level, int t) {
        Vec3 c = this.stageCentre();
        if (t % 5 == 0) {
            for (Vec3 s : this.safeSpots) {
                level.sendParticles(ModParticles.RESONANCE_RING.get(), s.x, s.y + 0.1, s.z, 0, SAFE_RADIUS, 0.0, 0.0, 1.0);
                level.sendParticles(ParticleTypes.END_ROD, s.x, s.y + 0.5, s.z, 6, 0.15, 1.5, 0.15, 0.01);
            }
            // the rest of the stage simmers with sculk
            for (int i = 0; i < 12; i++) {
                double a = this.random.nextDouble() * Math.PI * 2.0;
                double r = this.random.nextDouble() * 12.0;
                level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, c.x + Math.cos(a) * r, this.floor() + 0.2, c.z + Math.sin(a) * r, 1, 0.1, 0.05, 0.1, 0.0);
            }
        }
        if (t % 10 == 0 && t < FINALE_TICKS) {
            this.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 4.0F, 0.5F + t * 0.01F);
            this.playSound(SoundEvents.WARDEN_HEARTBEAT, 3.0F, 1.0F + t * 0.008F);
        }
        if (t == FINALE_TICKS) {
            this.playSound(SoundEvents.WARDEN_SONIC_BOOM, 6.0F, 0.5F);
            this.playSound(ModSounds.DICTATOR_ROAR.get(), 5.0F, 0.7F);
            for (int i = 0; i < 5; i++) {
                level.sendParticles(ModParticles.RESONANCE_RING.get(), c.x, this.floor() + 0.15, c.z, 0, 2.0 + i * 3.0, 0.0, 0.0, 1.0);
            }
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(16.0, 6.0, 16.0))) {
                if (e == this || MiniBoss.isBandmate(e)) {
                    continue;
                }
                boolean safe = false;
                for (Vec3 s : this.safeSpots) {
                    safe |= e.position().multiply(1, 0, 1).distanceTo(s.multiply(1, 0, 1)) < SAFE_RADIUS;
                }
                if (!safe && e.hurtServer(level, this.damageSources().sonicBoom(this), 14.0F)) {
                    e.push(0.0, 0.7, 0.0);
                    this.corrupt(e);
                }
            }
            this.safeSpots.clear();
        }
        if (t >= FINALE_TICKS + 10) {
            // spent: he sinks low over the stage for a while
            this.setAction(REST);
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
        if (t > 46) {
            this.ringTicks = -1;
            return;
        }
        Vec3 c = this.getPhase() == 1 ? this.position() : new Vec3(this.getX(), this.floor(), this.getZ());
        for (int wave = 0; wave < 3; wave++) {
            int wk = t - wave * 10;
            if (wk < 0 || wk > 26) {
                continue;
            }
            double r = 1.5 + wk * 0.6;
            if (wk % 2 == 0) {
                level.sendParticles(ModParticles.RESONANCE_RING.get(), c.x, this.floor() + 0.1, c.z, 0, r, 0.0, 0.0, 1.0);
            }
            if (wk == 0) {
                this.playSound(ModSounds.THUMPER_SLAM.get(), 3.0F, 1.2F + wave * 0.15F);
            }
            AABB box = new AABB(c.x - r - 1, this.floor() - 1, c.z - r - 1, c.x + r + 1, this.floor() + 2, c.z + r + 1);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box)) {
                if (e == this || MiniBoss.isBandmate(e) || !e.onGround()) {
                    continue;
                }
                double d = Math.sqrt(e.distanceToSqr(c.x, e.getY(), c.z));
                if (Math.abs(d - r) < 0.7 && e.hurtServer(level, this.damageSources().sonicBoom(this), 7.0F)) {
                    e.push(0.0, 0.6, 0.0);
                    this.corrupt(e);
                }
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

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean bypass = source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        if (!bypass && this.busy()) {
            return false;
        }
        if (source.getEntity() instanceof LivingEntity le && MiniBoss.isBandmate(le)) {
            return false;
        }
        boolean hurt = super.hurtServer(level, source, amount);
        // every blow moves the performance on, even before his next tick
        this.trackHealth(level);
        return hurt;
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
            if (this.assembleTicks() == 0) {
                this.trackHealth(server);
            }
            this.setNoGravity(this.getPhase() > 1 || this.transformTicks() > 0);
        }
        if (this.level().isClientSide()) {
            this.clientEffects();
        }
    }

    private void clientEffects() {
        Level level = this.level();
        if (this.assembleTicks() > 0) {
            return;
        }
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
        if (phase >= 2 && this.deathTime == 0 && this.random.nextInt(2) == 0) {
            // notes swirl round him once he leaves the ground
            double a = this.tickCount * 0.15 + this.random.nextDouble() * 0.5;
            level.addParticle(ModParticles.SIFT_NOTE.get(), this.getX() + Math.cos(a) * 1.4, this.getY() + 1.0 + this.random.nextDouble() * 2.0,
                    this.getZ() + Math.sin(a) * 1.4, this.random.nextDouble(), 0, 0);
        }
        if (phase == 3 && this.deathTime == 0) {
            // soul trails from his wings of song
            float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
            float flap = Mth.sin(this.tickCount * 0.2F) * 0.5F;
            for (int side = -1; side <= 1; side += 2) {
                double x = this.getX() + Mth.cos(yaw) * 2.6 * side * Math.cos(flap) + Mth.sin(yaw) * 0.6;
                double z = this.getZ() + Mth.sin(yaw) * 2.6 * side * Math.cos(flap) - Mth.cos(yaw) * 0.6;
                level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, this.getY() + 3.0 + Math.sin(flap) * 2.4, z, 0, -0.02, 0);
            }
        }
    }

    // ------------------------------------------------------------------ the end of the performance

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.level() instanceof ServerLevel level) {
            BossStages.cleared(level, this, BossStages.CONDUCTOR, BossStages.DEFEATED); // B2 Thumper & cutscenes: the defeat cutscene
            this.spawnAtLocation(level, new ItemStack(ModItems.CONDUCTORS_STAFF.get()));
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
        return this.assembleTicks() > 0 ? null : ModSounds.DICTATOR_AMBIENT.get();
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
        output.putInt("Assemble", this.assembleTicks());
        if (!Double.isNaN(this.floorY)) {
            output.putDouble("FloorY", this.floorY);
        }
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
        this.entityData.set(ASSEMBLE, input.getIntOr("Assemble", 0));
        this.floorY = input.getDoubleOr("FloorY", Double.NaN);
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

    /** A note of his in flight. */
    private static final class Note {
        Vec3 pos;
        Vec3 vel;
        final int target;
        int life;

        Note(Vec3 pos, Vec3 vel, int target) {
            this.pos = pos;
            this.vel = vel;
            this.target = target;
        }
    }

    /** A falling note's mark on the floor and how long until it lands. */
    private static final class Strike {
        final Vec3 at;
        int fuse;

        Strike(Vec3 at, int fuse) {
            this.at = at;
            this.fuse = fuse;
        }
    }
}
