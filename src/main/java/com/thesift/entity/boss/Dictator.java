package com.thesift.entity.boss;

import com.thesift.block.entity.BossDenBlockEntity;
import com.thesift.entity.KillBurst;
import com.thesift.registry.ModEffects;
import com.thesift.registry.ModEntities;
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
import net.minecraft.util.SpawnUtil;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
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
 *   <li>Below 33% (C3): his mask splits and he swells into a sculk colossus (the stage cutscene
 *   films it): he stalks the stage, slams the floor with his fists, roars a sweeping beam of
 *   sound out of the maw behind the mask, stamps out shockwave rings (jump them), rains notes
 *   from the organ pipes on his back and, at his finale, floods the stage with sound - only a
 *   few lit circles are safe. Spent after each finale, he kneels for a while.</li>
 * </ol>
 * In every movement he conducts his band up out of the stage - Sculk Parasites, and from the second
 * movement Strumlings too - never more than a few at once; they all fall with him. Between
 * movements he rises (or swells) in a storm of song and cannot be hurt.
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
    // C3 Conductor: entity events -41..-50
    private static final byte EVENT_SUMMON = -41;
    private static final byte EVENT_SLAM = -42;
    public static final int TRANSFORM_TICKS = 60;
    /** His swelling into the colossus: as long as the stage film that shows it. */
    public static final int KAIJU_TICKS = BossStages.LENGTH;
    /** When, in the swelling, the mask splits and the colossus bursts out. */
    public static final int KAIJU_BURST = 40;
    /** How much bigger the colossus is than the man (width, height). */
    public static final float KAIJU_WIDE = 2.6F;
    public static final float KAIJU_TALL = 1.95F;
    /** Marks the band he calls, so they can be counted and fall with him. */
    public static final String SUMMON_TAG = "thesift_conductor_band";
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
    /** Conducting his band up out of the stage. */
    public static final int SUMMON = 8;
    /** Colossus: both fists raised and brought down on the floor in front of him. */
    public static final int SLAM = 9;
    /** Colossus: the mask swings open and he roars a beam of sound that sweeps after you. */
    public static final int BEAM = 10;
    /** Colossus: rears up and stamps - shockwave rings of music roll out across the stage. */
    public static final int QUAKE = 11;
    public static final int SUMMON_AT = 16;
    public static final int SUMMON_TICKS = 34;
    public static final int SLAM_HIT = 20;
    public static final int SLAM_TICKS = 44;
    public static final int BEAM_CHARGE = 26;
    public static final int BEAM_TICKS = 72;
    public static final int QUAKE_HIT = 18;
    public static final int QUAKE_TICKS = 46;
    public static final int FINALE_TICKS = 70;
    private static final double SAFE_RADIUS = 2.2;

    public final AnimationState blinkAnimation = new AnimationState();
    public final AnimationState summonAnimation = new AnimationState();
    public final AnimationState crescendoAnimation = new AnimationState();
    public final AnimationState slashAnimation = new AnimationState();
    public final AnimationState roarAnimation = new AnimationState();
    public final AnimationState slamAnimation = new AnimationState();

    private final ServerBossEvent bossEvent = new ServerBossEvent(UUID.randomUUID(), Component.translatable("entity.thesift.dictator"),
            BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);
    private @Nullable BlockPos podium;
    private double floorY = Double.NaN;
    private int blinkCooldown = 80;
    private int actionCooldown = 50;
    private int darknessCooldown = 160;
    private int summonCooldown = 200;
    private int ringTicks = -1;
    /** Where the rolling rings start (null: under him). */
    private @Nullable Vec3 ringCentre;
    private Vec3 beamDir = Vec3.ZERO;
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

    /** How long the transformation into the current movement takes. */
    public int transformLength() {
        return this.getPhase() == 3 ? KAIJU_TICKS : TRANSFORM_TICKS;
    }

    /** The last movement: the sculk colossus. */
    public boolean isKaiju() {
        return this.getPhase() >= 3;
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
        if (PHASE.equals(accessor)) {
            this.refreshDimensions();
        }
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        EntityDimensions d = super.getDefaultDimensions(pose);
        return this.isKaiju() ? d.scale(KAIJU_WIDE, KAIJU_TALL) : d;
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
        boolean onward = phase > this.getPhase();
        // C3: the phase (and with it his size) first, so the film frames the whole colossus
        this.entityData.set(PHASE, phase);
        if (onward) {
            BossStages.cleared(level, this, BossStages.CONDUCTOR, phase); // B2 Thumper & cutscenes: the stage cutscene
        }
        this.entityData.set(TRANSFORM, this.transformLength());
        this.getNavigation().stop();
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
        return phase == 2 ? 2.8 : 0.0;
    }

    /** Rises (or sinks) to his new height in a storm of song; at the turn, a burst of power. */
    private void tickTransform(ServerLevel level) {
        int left = this.transformTicks();
        int t = this.transformLength() - left;
        this.entityData.set(TRANSFORM, left - 1);
        this.getNavigation().stop();
        if (this.isKaiju()) {
            this.tickSwell(level, t);
            return;
        }
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


    /**
     * The last movement begins: he sinks to the stage while souls and song pour into him from
     * every side, his heart pounding faster; then the mask splits, he bursts out of himself as the
     * colossus, and at last throws his head back and roars.
     */
    private void tickSwell(ServerLevel level, int t) {
        this.setDeltaMovement(0.0, Math.min(0.0, this.getDeltaMovement().y), 0.0);
        Vec3 c = this.position().add(0.0, Math.min(this.getBbHeight() * 0.45, 2.0 + t * 0.12), 0.0);
        if (t < KAIJU_BURST) {
            for (int i = 0; i < 2 + t / 10; i++) {
                double a = this.random.nextDouble() * Math.PI * 2.0;
                double r = 8.0 + this.random.nextDouble() * 8.0;
                Vec3 from = new Vec3(this.getX() + Math.cos(a) * r, this.floor() + 0.3 + this.random.nextDouble() * 6.0, this.getZ() + Math.sin(a) * r);
                Vec3 v = c.subtract(from).scale(0.06);
                level.sendParticles(i % 3 == 0 ? ModParticles.SIFT_NOTE.get() : ParticleTypes.SCULK_SOUL, from.x, from.y, from.z, 0, v.x, v.y, v.z, 1.0);
            }
            if (t % Math.max(4, 14 - t / 4) == 0) {
                this.playSound(SoundEvents.WARDEN_HEARTBEAT, 4.0F, 0.6F + t * 0.012F);
            }
            if (t % 8 == 0) {
                level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, this.getX(), this.floor() + 0.2, this.getZ(), 14, 3.0, 0.1, 3.0, 0.02);
                level.sendParticles(ModParticles.RESONANCE_RING.get(), this.getX(), this.floor() + 0.1, this.getZ(), 0, 2.0 + t * 0.15, 0.0, 0.0, 1.0);
            }
            if (t == 6) {
                this.playSound(ModSounds.CONDUCTOR_MASK_TRANSFORM.get(), 5.0F, 0.45F);
            }
            if (t == 24) {
                this.playSound(SoundEvents.WARDEN_SONIC_CHARGE, 5.0F, 0.5F);
            }
        } else if (t == KAIJU_BURST) {
            this.playSound(SoundEvents.GENERIC_EXPLODE.value(), 5.0F, 0.55F);
            this.playSound(SoundEvents.WARDEN_ROAR, 6.0F, 0.5F);
            this.playSound(ModSounds.CONDUCTOR_MASK_TRANSFORM.get(), 5.0F, 0.35F);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 1, 0.0, 0.0, 0.0, 0.0);
            level.sendParticles(ParticleTypes.SCULK_SOUL, c.x, c.y, c.z, 120, 2.0, 3.0, 2.0, 0.15);
            level.sendParticles(ModParticles.SIFT_NOTE.get(), c.x, c.y + 2.0, c.z, 40, 3.0, 3.0, 3.0, 1.0);
            for (int i = 0; i < 5; i++) {
                level.sendParticles(ModParticles.RESONANCE_RING.get(), this.getX(), this.floor() + 0.1, this.getZ(), 0, 3.0 + i * 3.0, 0.0, 0.0, 1.0);
            }
            for (Player p : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(8.0))) {
                Vec3 away = p.position().subtract(this.position()).multiply(1, 0, 1);
                away = away.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : away.normalize();
                p.push(away.x * 2.0, 0.7, away.z * 2.0);
            }
        } else {
            if (t % 6 == 0) {
                Vec3 pipes = this.pipes();
                level.sendParticles(ModParticles.SIFT_NOTE.get(), pipes.x, pipes.y, pipes.z, 3, 1.5, 0.5, 1.5, 1.0);
            }
            if (t == KAIJU_BURST + 22) {
                this.level().broadcastEntityEvent(this, EVENT_ROAR);
                this.playSound(ModSounds.DICTATOR_ROAR.get(), 6.0F, 0.5F);
                this.playSound(SoundEvents.WARDEN_SONIC_BOOM, 4.0F, 0.5F);
                Vec3 m = this.mouth();
                level.sendParticles(ParticleTypes.SONIC_BOOM, m.x, m.y, m.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
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
            if (phase == 2) {
                this.hover(null);
            }
            return;
        }
        this.actionTicks++;
        if (this.summonCooldown > 0) {
            this.summonCooldown--;
        }
        if (phase == 2) {
            this.hover(target);
        } else if (phase == 3) {
            this.stride(level, target);
        }
        if (this.getAction() != NONE) {
            this.tickAction(level, target);
        } else {
            if (this.actionCooldown > 0) {
                this.actionCooldown--;
            } else {
                this.chooseAction(level, target, phase);
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

    private void chooseAction(ServerLevel level, LivingEntity target, int phase) {
        double d = this.distanceTo(target);
        int roll = this.random.nextInt(10);
        if (this.summonCooldown <= 0 && this.canSummon(level)) {
            // C3: a conducting gesture, and his band crawls up out of the stage
            this.summonCooldown = phase == 3 ? 340 : 420;
            this.setAction(SUMMON);
            this.getNavigation().stop();
            this.level().broadcastEntityEvent(this, EVENT_SUMMON);
            this.playSound(ModSounds.DICTATOR_CRESCENDO.get(), 3.0F, 0.75F);
            return;
        }
        switch (phase) {
            case 1 -> {
                if (d > 4.0 && d < 16.0 && this.hasLineOfSight(target) && roll < 5) {
                    this.setAction(LUNGE_WINDUP);
                    this.playSound(ModSounds.DICTATOR_CRESCENDO.get(), 2.0F, 1.4F);
                }
            }
            case 2 -> this.setAction(roll < 5 ? BARRAGE : CHORD);
            default -> {
                // the colossus
                boolean sees = this.hasLineOfSight(target);
                if (d < this.getBbWidth() * 0.5 + 6.5 && roll < 5) {
                    this.setAction(SLAM);
                    this.playSound(SoundEvents.WARDEN_ROAR, 3.0F, 0.8F);
                } else if (sees && d > 5.0 && roll < 4) {
                    this.setAction(BEAM);
                } else if (roll < 6) {
                    this.setAction(QUAKE);
                    this.playSound(ModSounds.DICTATOR_CRESCENDO.get(), 4.0F, 0.6F);
                } else if (roll < 8) {
                    this.setAction(RAIN);
                } else if (roll < 9) {
                    this.beginFinale();
                } else {
                    this.setAction(sees ? BEAM : QUAKE);
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
                    this.startRings(null);
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
                if (this.isKaiju() && t % 3 == 0 && t < 70) {
                    // the colossus plays it on his organ pipes: notes shoot up out of them
                    Vec3 pipes = this.pipes();
                    level.sendParticles(ModParticles.SIFT_NOTE.get(), pipes.x, pipes.y, pipes.z, 2, 1.2, 0.3, 1.2, 1.0);
                    level.sendParticles(ParticleTypes.END_ROD, pipes.x, pipes.y, pipes.z, 0, 0.0, 0.6, 0.0, 1.0);
                }
                if (t >= 90) {
                    this.endAction(30);
                }
            }
            case SUMMON -> {
                // baton (or claw) raised, a long sweep, and the stage gives up his band
                this.getNavigation().stop();
                this.getLookControl().setLookAt(target, 30.0F, 30.0F);
                if (t < SUMMON_AT && t % 3 == 0) {
                    double ring = this.getBbWidth() * 0.5 + 2.5;
                    for (int k = 0; k < 6; k++) {
                        double ang = this.random.nextDouble() * Math.PI * 2.0;
                        level.sendParticles(ParticleTypes.SCULK_SOUL, this.getX() + Math.cos(ang) * ring, this.getY() + 0.2, this.getZ() + Math.sin(ang) * ring, 1, 0.2, 0.0, 0.2, 0.03);
                    }
                    this.playSound(SoundEvents.SCULK_CLICKING, 2.0F, 0.6F + t * 0.03F);
                }
                if (t == SUMMON_AT) {
                    this.callBand(level, target);
                }
                if (t >= SUMMON_TICKS) {
                    this.endAction(30);
                }
            }
            case SLAM -> this.tickSlam(level, target, t);
            case BEAM -> this.tickBeam(level, target, t);
            case QUAKE -> {
                this.getNavigation().stop();
                if (t == QUAKE_HIT) {
                    this.level().broadcastEntityEvent(this, EVENT_SLAM);
                    this.playSound(SoundEvents.GENERIC_EXPLODE.value(), 4.0F, 0.5F);
                    this.playSound(ModSounds.THUMPER_SLAM.get(), 4.0F, 0.6F);
                    for (int semis : new int[]{0, 3, 7}) {
                        this.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 4.0F, (float) Math.pow(2.0, (semis - 12) / 12.0));
                    }
                    level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, this.getX(), this.floor() + 0.2, this.getZ(), 40, 3.0, 0.1, 3.0, 0.05);
                    this.startRings(new Vec3(this.getX(), this.floor(), this.getZ()));
                }
                if (t >= QUAKE_TICKS) {
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
        Vec3 from = this.isKaiju() ? this.pipes() : this.position().add(0.0, this.getBbHeight() * 0.62, 0.0);
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

    private void startRings(@Nullable Vec3 centre) {
        this.ringTicks = 0;
        this.ringCentre = centre;
        if (!this.isKaiju()) {
            this.level().broadcastEntityEvent(this, EVENT_CRESCENDO);
        }
        this.playSound(ModSounds.DICTATOR_CRESCENDO.get(), 4.0F, this.isKaiju() ? 0.6F : 1.0F);
    }

    /** Rings of sculk sound rolling out across the stage; anyone standing on the ring is struck. */
    private void rollRings(ServerLevel level) {
        int t = this.ringTicks++;
        boolean big = this.isKaiju();
        int waves = big ? 4 : 3;
        int gap = big ? 9 : 10;
        int life = big ? 24 : 26;
        if (t > (waves - 1) * gap + life) {
            this.ringTicks = -1;
            return;
        }
        Vec3 c = this.ringCentre != null ? this.ringCentre : this.getPhase() == 1 ? this.position() : new Vec3(this.getX(), this.floor(), this.getZ());
        for (int wave = 0; wave < waves; wave++) {
            int wk = t - wave * gap;
            if (wk < 0 || wk > life) {
                continue;
            }
            double r = (big ? this.getBbWidth() * 0.5 + 1.0 : 1.5) + wk * (big ? 0.85 : 0.6);
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
                if (Math.abs(d - r) < 0.7 && e.hurtServer(level, this.damageSources().sonicBoom(this), big ? 9.0F : 7.0F)) {
                    e.push(0.0, 0.6, 0.0);
                    this.corrupt(e);
                }
            }
        }
    }

    // ------------------------------------------------------------------ C3: the colossus

    /** The maw behind the split mask, where his roar comes out. */
    private Vec3 mouth() {
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        double fwd = this.getBbWidth() * 0.5 + 1.2;
        return new Vec3(this.getX() - Mth.sin(yaw) * fwd, this.getY() + this.getBbHeight() * 0.66, this.getZ() + Mth.cos(yaw) * fwd);
    }

    /** The tops of the organ pipes on his back. */
    private Vec3 pipes() {
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        double back = this.getBbWidth() * 0.2;
        return new Vec3(this.getX() + Mth.sin(yaw) * back, this.getY() + this.getBbHeight() * 0.95, this.getZ() - Mth.cos(yaw) * back);
    }

    /** Stalks after his target, turning his whole bulk slowly; the stage shakes under each step. */
    private void stride(ServerLevel level, LivingEntity target) {
        int a = this.getAction();
        double reach = this.getBbWidth() * 0.5 + 5.0;
        if (a == NONE && this.distanceTo(target) > reach) {
            if (this.tickCount % 10 == 0) {
                this.getNavigation().moveTo(target, 0.6);
            }
        } else if (a != NONE) {
            this.getNavigation().stop();
        }
        if (a != BEAM && (a != NONE || this.getNavigation().isDone())) {
            double dx = target.getX() - this.getX();
            double dz = target.getZ() - this.getZ();
            float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
            this.setYRot(Mth.approachDegrees(this.getYRot(), yaw, a == SLAM && this.actionTicks > SLAM_HIT - 6 ? 0.0F : 4.0F));
            this.yBodyRot = this.getYRot();
        }
        if (this.onGround() && this.getDeltaMovement().horizontalDistanceSqr() > 0.002 && this.tickCount % 16 == 0) {
            this.playSound(SoundEvents.WARDEN_STEP, 3.0F, 0.5F);
            level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, this.getX(), this.getY() + 0.1, this.getZ(), 6, this.getBbWidth() * 0.4, 0.05,
                    this.getBbWidth() * 0.4, 0.01);
        }
    }

    /** Both fists raised high, then brought down on the floor in front of him. */
    private void tickSlam(ServerLevel level, LivingEntity target, int t) {
        this.getNavigation().stop();
        if (t == SLAM_HIT) {
            float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
            Vec3 fwd = new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
            double ahead = this.getBbWidth() * 0.5 + 3.5;
            Vec3 at = new Vec3(this.getX() + fwd.x * ahead, this.floor(), this.getZ() + fwd.z * ahead);
            this.level().broadcastEntityEvent(this, EVENT_SLAM);
            this.playSound(SoundEvents.GENERIC_EXPLODE.value(), 4.0F, 0.6F);
            this.playSound(ModSounds.THUMPER_SLAM.get(), 4.0F, 0.7F);
            level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.5, at.z, 3, 1.2, 0.2, 1.2, 0.0);
            level.sendParticles(ParticleTypes.SCULK_SOUL, at.x, at.y + 0.3, at.z, 30, 2.0, 0.2, 2.0, 0.08);
            level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, at.x, at.y + 0.2, at.z, 30, 2.5, 0.1, 2.5, 0.05);
            for (int i = 0; i < 3; i++) {
                level.sendParticles(ModParticles.RESONANCE_RING.get(), at.x, at.y + 0.1, at.z, 0, 2.0 + i * 2.5, 0.0, 0.0, 1.0);
            }
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(4.5, 3.0, 4.5))) {
                if (e == this || MiniBoss.isBandmate(e)) {
                    continue;
                }
                double d = e.position().multiply(1, 0, 1).distanceTo(at.multiply(1, 0, 1));
                if (d < 4.5 && e.hurtServer(level, this.damageSources().mobAttack(this), d < 2.0 ? 18.0F : 12.0F)) {
                    Vec3 away = e.position().subtract(at).multiply(1, 0, 1);
                    away = away.lengthSqr() < 1.0E-4 ? fwd : away.normalize();
                    e.push(away.x * 1.2, 0.8, away.z * 1.2);
                    this.corrupt(e);
                }
            }
        }
        if (t >= SLAM_TICKS) {
            this.endAction(25);
        }
    }

    /**
     * The roar: the mask swings open and souls pour into the maw, then a beam of sound bursts out
     * of it and sweeps after the target - slowly enough to outrun sideways.
     */
    private void tickBeam(ServerLevel level, LivingEntity target, int t) {
        this.getNavigation().stop();
        Vec3 mouth = this.mouth();
        Vec3 want = target.getEyePosition().subtract(mouth).normalize();
        if (t <= 1 || this.beamDir.lengthSqr() < 1.0E-4) {
            this.beamDir = want;
            this.playSound(SoundEvents.WARDEN_SONIC_CHARGE, 5.0F, 0.6F);
        }
        if (t < BEAM_CHARGE) {
            this.beamDir = this.beamDir.lerp(want, 0.2).normalize();
            for (int i = 0; i < 3; i++) {
                Vec3 from = mouth.add((this.random.nextDouble() - 0.5) * 8.0, (this.random.nextDouble() - 0.5) * 6.0, (this.random.nextDouble() - 0.5) * 8.0);
                Vec3 v = mouth.subtract(from).scale(0.1);
                level.sendParticles(ParticleTypes.SCULK_SOUL, from.x, from.y, from.z, 0, v.x, v.y, v.z, 1.0);
            }
        } else if (t < BEAM_TICKS - 8) {
            if (t == BEAM_CHARGE) {
                this.level().broadcastEntityEvent(this, EVENT_ROAR);
                this.playSound(SoundEvents.WARDEN_SONIC_BOOM, 6.0F, 0.5F);
                this.playSound(ModSounds.DICTATOR_ROAR.get(), 5.0F, 0.6F);
            }
            this.beamDir = this.beamDir.lerp(want, 0.045).normalize();
            this.fireBeam(level, mouth, t);
            if (t % 6 == 0) {
                this.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 3.0F, 0.5F + this.random.nextFloat() * 0.2F);
            }
        }
        float yaw = (float) (Mth.atan2(this.beamDir.z, this.beamDir.x) * Mth.RAD_TO_DEG) - 90.0F;
        this.setYRot(Mth.approachDegrees(this.getYRot(), yaw, 6.0F));
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();
        if (t >= BEAM_TICKS) {
            this.endAction(40);
        }
    }

    private void fireBeam(ServerLevel level, Vec3 from, int t) {
        Vec3 to = from.add(this.beamDir.scale(30.0));
        HitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 end = hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
        Vec3 seg = end.subtract(from);
        double len = seg.length();
        for (double step = 1.5; step < len; step += 1.5) {
            Vec3 p = from.add(seg.scale(step / len));
            if (t % 3 == 0 && ((int) (step / 1.5)) % 2 == 0) {
                level.sendParticles(ParticleTypes.SONIC_BOOM, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
            level.sendParticles(ModParticles.SIFT_NOTE.get(), p.x, p.y, p.z, 1, 0.25, 0.25, 0.25, 1.0);
        }
        level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, end.x, end.y, end.z, 4, 0.5, 0.3, 0.5, 0.05);
        if (t % 6 == 0) {
            level.sendParticles(ModParticles.RESONANCE_RING.get(), end.x, end.y + 0.1, end.z, 0, 2.0, 0.0, 0.0, 1.0);
        }
        double len2 = Math.max(1.0E-6, seg.lengthSqr());
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, end).inflate(1.5))) {
            if (e == this || MiniBoss.isBandmate(e)) {
                continue;
            }
            Vec3 c = e.getBoundingBox().getCenter();
            double k = Mth.clamp(c.subtract(from).dot(seg) / len2, 0.0, 1.0);
            if (c.distanceTo(from.add(seg.scale(k))) > 1.1 + e.getBbWidth() * 0.5) {
                continue;
            }
            if (e.hurtServer(level, this.damageSources().sonicBoom(this), 6.0F)) {
                e.push(this.beamDir.x * 0.6, 0.15, this.beamDir.z * 0.6);
                this.corrupt(e);
            }
        }
    }

    // ------------------------------------------------------------------ C3: his band

    private boolean canSummon(ServerLevel level) {
        int phase = this.getPhase();
        return this.bandCount(level, ModEntities.SCULK_PARASITE.get()) < phase + 2
                || (phase > 1 && this.bandCount(level, ModEntities.STRUMLING.get()) < (phase == 2 ? 2 : 3));
    }

    private <T extends Entity> int bandCount(ServerLevel level, EntityType<T> type) {
        return level.getEntities(type, this.getBoundingBox().inflate(48.0), e -> e.isAlive() && e.entityTags().contains(SUMMON_TAG)).size();
    }

    /** At the sweep of his baton his band crawls up out of the stage round him. */
    private void callBand(ServerLevel level, LivingEntity target) {
        int phase = this.getPhase();
        int made = this.summonKind(level, ModEntities.SCULK_PARASITE.get(), phase == 3 ? 3 : 2, phase + 2, target);
        if (phase > 1) {
            made += this.summonKind(level, ModEntities.STRUMLING.get(), phase == 3 ? 2 : 1, phase == 2 ? 2 : 3, target);
        }
        if (made > 0) {
            for (int n : new int[]{0, 3, 7, 10}) {
                this.playSound(SoundEvents.NOTE_BLOCK_BELL.value(), 3.0F, (float) Math.pow(2.0, (n - 12) / 12.0));
            }
            this.playSound(SoundEvents.SCULK_BLOCK_BREAK, 3.0F, 0.5F);
        }
    }

    private <T extends Mob> int summonKind(ServerLevel level, EntityType<T> type, int count, int max, LivingEntity target) {
        int n = Math.min(count, max - this.bandCount(level, type));
        int made = 0;
        double r0 = this.getBbWidth() * 0.5 + 2.0;
        for (int i = 0; i < n; i++) {
            double a = this.random.nextDouble() * Math.PI * 2.0;
            double r = r0 + this.random.nextDouble() * 3.0;
            BlockPos at = BlockPos.containing(this.getX() + Math.cos(a) * r, this.getY() + 1.0, this.getZ() + Math.sin(a) * r);
            var spawned = SpawnUtil.trySpawnMob(type, EntitySpawnReason.MOB_SUMMONED, level, at, 10, 3, 3, SpawnUtil.Strategy.ON_TOP_OF_COLLIDER, false);
            if (spawned.isPresent()) {
                T m = spawned.get();
                m.addTag(SUMMON_TAG);
                m.setTarget(target);
                level.sendParticles(ParticleTypes.SCULK_SOUL, m.getX(), m.getY() + 0.3, m.getZ(), 14, 0.4, 0.3, 0.4, 0.04);
                level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, m.getX(), m.getY() + 0.1, m.getZ(), 10, 0.5, 0.1, 0.5, 0.03);
                level.sendParticles(ModParticles.RESONANCE_RING.get(), m.getX(), m.getY() + 0.1, m.getZ(), 0, 1.2, 0.0, 0.0, 1.0);
                made++;
            }
        }
        return made;
    }

    /** When the music ends, so does his band. */
    private void dismissBand(ServerLevel level) {
        for (Mob m : level.getEntitiesOfClass(Mob.class, this.getBoundingBox().inflate(64.0), e -> e.isAlive() && e.entityTags().contains(SUMMON_TAG))) {
            level.sendParticles(ParticleTypes.SCULK_SOUL, m.getX(), m.getY() + 0.4, m.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
            m.kill(level);
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
            case EVENT_SUMMON -> this.summonAnimation.start(this.tickCount);
            case EVENT_SLAM -> {
                this.slamAnimation.start(this.tickCount);
                for (int i = 0; i < 24; i++) {
                    this.level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getRandomX(2.5), this.getY() + 0.2, this.getRandomZ(2.5),
                            (this.random.nextDouble() - 0.5) * 0.2, 0.02, (this.random.nextDouble() - 0.5) * 0.2);
                }
            }
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
            this.setNoGravity(this.getPhase() == 2 || (this.transformTicks() > 0 && !this.isKaiju()));
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
            level.addParticle(ModParticles.GLOW_DUST.get(), this.getRandomX(0.6), this.getY() + this.random.nextDouble() * this.getBbHeight(), this.getRandomZ(0.6), 0, -0.01, 0);
        }
        if (this.deathTime > 2) {
            for (int i = 0; i < 3; i++) {
                level.addParticle(ParticleTypes.SCULK_SOUL, this.getRandomX(0.8), this.getY() + this.random.nextDouble() * this.getBbHeight(), this.getRandomZ(0.8), 0, 0.05, 0);
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
            double r = this.getBbWidth() * 0.5 + 0.8;
            level.addParticle(ModParticles.SIFT_NOTE.get(), this.getX() + Math.cos(a) * r, this.getY() + this.getBbHeight() * (0.2 + this.random.nextDouble() * 0.5),
                    this.getZ() + Math.sin(a) * r, this.random.nextDouble(), 0, 0);
        }
        if (phase == 3 && this.deathTime == 0 && (this.transformTicks() == 0 || this.transformTicks() < KAIJU_TICKS - KAIJU_BURST)) {
            // the colossus: souls breathing out of his organ pipes
            Vec3 pipes = this.pipes();
            if (this.random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.SCULK_SOUL, pipes.x + (this.random.nextDouble() - 0.5) * 3.0, pipes.y, pipes.z + (this.random.nextDouble() - 0.5) * 3.0,
                        0, 0.06, 0);
            }
        }
    }

    // ------------------------------------------------------------------ the end of the performance

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.level() instanceof ServerLevel level) {
            BossStages.cleared(level, this, BossStages.CONDUCTOR, BossStages.DEFEATED); // B2 Thumper & cutscenes: the defeat cutscene
            this.dismissBand(level); // C3: his band falls with him
            this.spawnAtLocation(level, new ItemStack(ModItems.CONDUCTORS_STAFF.get()));
            if (this.podium != null && level.getBlockEntity(this.podium) instanceof BossDenBlockEntity p) {
                p.setDefeated();
            }
            level.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY() + this.getBbHeight() * 0.4, this.getZ(), 80,
                    this.getBbWidth() * 0.8, this.getBbHeight() * 0.3, this.getBbWidth() * 0.8, 0.08);
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
