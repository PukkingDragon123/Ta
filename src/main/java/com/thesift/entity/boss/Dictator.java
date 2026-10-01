package com.thesift.entity.boss;

import com.thesift.block.entity.ConductorsPodiumBlockEntity;
import com.thesift.registry.ModEntities;
import com.thesift.registry.ModItems;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import com.thesift.entity.KillBurst;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.SpawnUtil;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
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
 * The Dictator: conductor of the Sift's sculk orchestra, waiting at the top of his spiral castle.
 * Tall, thin and terribly fast.
 *
 * <ol>
 *   <li>Overture (full health to 60%): slashes with his baton, blinks behind you, and calls up his
 *   percussion, strings and wind - Enforcers, Resonators and Howlers.</li>
 *   <li>Crescendo (60% to 25%): adds rings of sound that roll across the floor (jump over them),
 *   and summons the vocals: a Warden, once.</li>
 *   <li>Finale (below 25%): faster still, blinking constantly, drowning the arena in darkness.</li>
 * </ol>
 */
public class Dictator extends Monster {
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(Dictator.class, EntityDataSerializers.INT);
    private static final byte EVENT_BLINK = 70;
    private static final byte EVENT_SUMMON = 71;
    private static final byte EVENT_CRESCENDO = 72;
    private static final byte EVENT_SLASH = 73;
    private static final byte EVENT_ROAR = 74;
    private static final int MAX_MINIONS = 4;

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
    private int crescendoCooldown = 120;
    private int darknessCooldown = 100;
    private int ringTicks = -1;
    private boolean vocalsCalled;

    public Dictator(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 250;
        this.bossEvent.setDarkenScreen(true);
        this.bossEvent.setCreateWorldFog(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 320.0).add(Attributes.ARMOR, 10.0).add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.FOLLOW_RANGE, 48.0).add(Attributes.ATTACK_DAMAGE, 11.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.85)
                .add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, true));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, OrchestraMinion.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASE, 1);
    }

    public int getPhase() {
        return this.entityData.get(PHASE);
    }

    public void setPodium(BlockPos pos) {
        this.podium = pos.immutable();
    }

    // ------------------------------------------------------------------ the performance

    /** Keeps the boss bar and the phase in step with his health, every tick, AI or not. */
    private void trackHealth(ServerLevel level) {
        float hp = this.getHealth() / this.getMaxHealth();
        this.bossEvent.setProgress(hp);
        int phase = hp > 0.6F ? 1 : hp > 0.25F ? 2 : 3;
        if (phase != this.getPhase() && this.isAlive()) {
            this.enterPhase(level, phase);
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        int phase = this.getPhase();
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        if (--this.blinkCooldown <= 0 && (this.distanceTo(target) > 5.0 || this.random.nextInt(3) == 0)) {
            this.blinkBehind(level, target);
            this.blinkCooldown = phase == 3 ? 50 : phase == 2 ? 90 : 120;
        }
        if (--this.summonCooldown <= 0) {
            this.summonSection(level, phase);
            this.summonCooldown = phase == 3 ? 220 : phase == 2 ? 260 : 320;
        }
        if (phase >= 2 && --this.crescendoCooldown <= 0) {
            this.ringTicks = 0;
            this.level().broadcastEntityEvent(this, EVENT_CRESCENDO);
            this.playSound(ModSounds.DICTATOR_CRESCENDO.get(), 4.0F, 1.0F);
            this.crescendoCooldown = phase == 3 ? 150 : 200;
        }
        if (this.ringTicks >= 0) {
            this.rollRings(level);
        }
        if (phase == 3 && --this.darknessCooldown <= 0) {
            this.darknessCooldown = 300;
            for (Player p : level.getEntitiesOfClass(Player.class, new AABB(this.blockPosition()).inflate(32.0))) {
                p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 140, 0), this);
            }
        }
    }

    private void enterPhase(ServerLevel level, int phase) {
        this.entityData.set(PHASE, phase);
        this.level().broadcastEntityEvent(this, EVENT_ROAR);
        this.playSound(ModSounds.DICTATOR_ROAR.get(), 5.0F, phase == 3 ? 0.8F : 1.0F);
        level.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY() + 2.0, this.getZ(), 40, 1.5, 1.5, 1.5, 0.05);
        if (phase == 2 && !this.vocalsCalled && this.getTarget() != null) {
            // the vocals: a Warden claws its way up through the floor
            this.vocalsCalled = true;
            LivingEntity target = this.getTarget();
            SpawnUtil.trySpawnMob(EntityTypes.WARDEN, EntitySpawnReason.TRIGGERED, level, this.blockPosition(), 20, 6, 4,
                    SpawnUtil.Strategy.ON_TOP_OF_COLLIDER, false).ifPresent(w -> w.increaseAngerAt(target, 80, true));
        }
        if (phase == 3) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.42);
        }
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

    /** Calls up a section of the orchestra around the arena. */
    private void summonSection(ServerLevel level, int phase) {
        List<OrchestraMinion> band = level.getEntitiesOfClass(OrchestraMinion.class, new AABB(this.blockPosition()).inflate(40.0));
        int room = MAX_MINIONS - band.size();
        if (room <= 0) {
            return;
        }
        this.level().broadcastEntityEvent(this, EVENT_SUMMON);
        this.playSound(ModSounds.DICTATOR_SUMMON.get(), 3.0F, 1.0F);
        int count = Math.min(room, phase == 1 ? 2 : 3);
        for (int i = 0; i < count; i++) {
            EntityType<? extends OrchestraMinion> type = switch (this.random.nextInt(3)) {
                case 0 -> ModEntities.ENFORCER.get();
                case 1 -> ModEntities.RESONATOR.get();
                default -> ModEntities.HOWLER.get();
            };
            double a = this.random.nextDouble() * Math.PI * 2;
            BlockPos at = BlockPos.containing(this.getX() + Math.cos(a) * 6, this.getY() + 1, this.getZ() + Math.sin(a) * 6);
            SpawnUtil.trySpawnMob(type, EntitySpawnReason.MOB_SUMMONED, level, at, 10, 3, 3, SpawnUtil.Strategy.ON_TOP_OF_COLLIDER, false)
                    .ifPresent(m -> {
                        m.setTarget(this.getTarget());
                        level.sendParticles(ParticleTypes.SCULK_SOUL, m.getX(), m.getY() + 0.5, m.getZ(), 12, 0.4, 0.4, 0.4, 0.05);
                    });
        }
    }

    /** Rings of sound rolling out across the floor; anyone standing on the ring is struck. */
    private void rollRings(ServerLevel level) {
        int t = this.ringTicks++;
        if (t < 16) {
            return; // wind-up: baton raised
        }
        int k = t - 16;
        if (k > 54) {
            this.ringTicks = -1;
            return;
        }
        for (int wave = 0; wave < 3; wave++) {
            int wk = k - wave * 14;
            if (wk < 0 || wk > 26) {
                continue;
            }
            double r = 1.5 + wk * 0.55;
            if (wk % 2 == 0) {
                level.sendParticles(ModParticles.RESONANCE_RING.get(), this.getX(), this.getY() + 0.1, this.getZ(), 0, r, 0.0, 0.0, 1.0);
            }
            if (wk == 0) {
                this.playSound(ModSounds.ENFORCER_SLAM.get(), 3.0F, 1.2F + wave * 0.15F);
            }
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(this.blockPosition()).inflate(r + 1.0, 2.0, r + 1.0))) {
                if (e == this || OrchestraMinion.isBandmate(e) || !e.onGround()) {
                    continue;
                }
                double d = Math.sqrt(e.distanceToSqr(this.getX(), e.getY(), this.getZ()));
                if (Math.abs(d - r) < 0.7 && Math.abs(e.getY() - this.getY()) < 2.0) {
                    if (e.hurtServer(level, this.damageSources().sonicBoom(this), 8.0F)) {
                        e.push(0.0, 0.6, 0.0);
                    }
                }
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        // every blow moves the performance on, even before his next tick
        this.trackHealth(level);
        return hurt;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        this.level().broadcastEntityEvent(this, EVENT_SLASH);
        return super.doHurtTarget(level, target);
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
        }
        if (this.level().isClientSide()) {
            if (this.random.nextInt(4) == 0) {
                this.level().addParticle(ModParticles.GLOW_DUST.get(), this.getRandomX(0.6), this.getY() + this.random.nextDouble() * 3.0,
                        this.getRandomZ(0.6), 0, -0.01, 0);
            }
            if (this.deathTime > 2) {
                for (int i = 0; i < 3; i++) {
                    this.level().addParticle(ParticleTypes.SCULK_SOUL, this.getRandomX(0.8), this.getY() + this.random.nextDouble() * 3.2,
                            this.getRandomZ(0.8), 0, 0.05, 0);
                }
            }
        }
    }

    // ------------------------------------------------------------------ the end of the performance

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.level() instanceof ServerLevel level) {
            this.spawnAtLocation(level, new ItemStack(ModItems.CONDUCTORS_BATON.get()));
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
        output.putBoolean("VocalsCalled", this.vocalsCalled);
        if (this.podium != null) {
            output.putInt("PodiumX", this.podium.getX());
            output.putInt("PodiumY", this.podium.getY());
            output.putInt("PodiumZ", this.podium.getZ());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.vocalsCalled = input.getBooleanOr("VocalsCalled", false);
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
