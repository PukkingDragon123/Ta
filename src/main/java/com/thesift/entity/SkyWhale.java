package com.thesift.entity;

import com.thesift.registry.ModItems;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Sky Whale: a vast, shaggy, gentle flying whale-bull that drifts high over the plains and groves,
 * singing deep moo-whale songs and trailing cloud. Play the crane flute towards the sky with a
 * tamed Harmoner beside you and the bird sings along: the nearest whale answers, glides down to
 * you, sings back and spits out a Skysong Gem - once per day for each whale - before rising back
 * into the sky.
 */
public class SkyWhale extends PathfinderMob {
    private static final byte EVENT_SING = 100;
    private static final byte EVENT_SPIT = 101;
    private static final long GEM_COOLDOWN = 24000L;
    private static final int ANSWER_TIMEOUT = 20 * 40;

    public final AnimationState singAnimation = new AnimationState();
    public final AnimationState spitAnimation = new AnimationState();
    /** Client: smoothed flap of the flipper-wings. */
    public float flap;
    public float flapO;

    private @Nullable Vec3 waypoint;
    private int waypointTimer;
    private @Nullable Player answering;
    private int answerTicks;
    private int songTicks = -1;
    private int riseTicks;
    private long lastGem = Long.MIN_VALUE / 2;
    private boolean gemThisVisit;

    public SkyWhale(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 150.0).add(Attributes.ARMOR, 4.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2).add(Attributes.FLYING_SPEED, 0.2).add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 24.0F));
    }

    // ------------------------------------------------------------------ the whale song

    /**
     * Called by the crane flute: when the player plays it with a tamed Harmoner within 10 blocks,
     * the bird sings along and the nearest Sky Whale within 96 blocks answers. Returns true if a
     * whale answered.
     */
    public static boolean answerSong(ServerLevel level, Player player) {
        List<Harmoner> birds = level.getEntitiesOfClass(Harmoner.class, player.getBoundingBox().inflate(10.0),
                h -> h.isAlive() && h.distanceToSqr(player) <= 100.0 && isTamed(h));
        if (birds.isEmpty()) {
            return false;
        }
        for (Harmoner h : birds) {
            h.startSong();
            level.sendParticles(ModParticles.SIFT_NOTE.get(), h.getX(), h.getY() + h.getBbHeight() + 0.3, h.getZ(), 0, 0.3, 0.0, 0.0, 1.0);
            level.sendParticles(ModParticles.STAR_SPARKLE.get(), h.getX(), h.getY() + 0.5, h.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
        }
        SkyWhale best = null;
        double bestD = 96.0 * 96.0;
        for (SkyWhale w : level.getEntitiesOfClass(SkyWhale.class, player.getBoundingBox().inflate(96.0), SkyWhale::isAlive)) {
            double d = w.distanceToSqr(player);
            if (d < bestD && (w.answering == null || w.answering == player)) {
                bestD = d;
                best = w;
            }
        }
        if (best == null) {
            return false;
        }
        best.answer(level, player);
        return true;
    }

    /** A tamed Harmoner: tamed through vanilla ownership, or through an {@code isTame()} flag. */
    private static boolean isTamed(Harmoner h) {
        return h.isTame();
    }


    public boolean isAnswering() {
        return this.answering != null;
    }

    /** Answers a player's song: the whale comes to hover and sing to them (also used by the Whale Song). */
    public void answer(ServerLevel level, Player player) {
        this.answering = player;
        this.answerTicks = ANSWER_TIMEOUT;
        this.songTicks = -1;
        this.riseTicks = 0;
        this.gemThisVisit = level.getGameTime() - this.lastGem >= GEM_COOLDOWN;
        this.setPersistenceRequired();
        level.playSound(null, this.getX(), this.getY(), this.getZ(), ModSounds.SKY_WHALE_SONG.get(), SoundSource.NEUTRAL, 6.0F, 0.9F);
        level.broadcastEntityEvent(this, EVENT_SING);
        player.sendOverlayMessage(Component.translatable("message.thesift.sky_whale.heard"));
    }

    /** Where the whale hovers while it sings to the player: in front of them, a little above. */
    private Vec3 hoverSpot(Player p) {
        Vec3 look = p.getLookAngle().multiply(1.0, 0.0, 1.0);
        if (look.lengthSqr() < 1.0E-4) {
            look = new Vec3(0.0, 0.0, 1.0);
        }
        return p.position().add(look.normalize().scale(7.0)).add(0.0, 4.0, 0.0);
    }

    private void tickAnswer(ServerLevel level) {
        Player p = this.answering;
        if (p == null) {
            return;
        }
        if (!p.isAlive() || p.level() != this.level() || --this.answerTicks <= 0) {
            this.finishAnswer();
            return;
        }
        Vec3 spot = this.hoverSpot(p);
        if (this.songTicks < 0) {
            this.waypoint = spot;
            if (this.position().distanceToSqr(spot) < 9.0) {
                this.songTicks = 0;
            }
            return;
        }
        this.waypoint = spot;
        this.getLookControl().setLookAt(p, 10.0F, 10.0F);
        int t = this.songTicks++;
        if (t == 0) {
            level.broadcastEntityEvent(this, EVENT_SING);
            this.playSound(ModSounds.SKY_WHALE_SONG.get(), 4.0F, 1.0F);
        }
        if (t % 4 == 0 && t < 60) {
            level.sendParticles(ModParticles.SIFT_NOTE.get(), this.getX(), this.getY() + 3.0, this.getZ(), 0, (t % 24) / 24.0, 0.0, 0.0, 1.0);
            level.sendParticles(ModParticles.GUIDE_NOTE.get(), this.getRandomX(1.5), this.getY() + 2.5, this.getRandomZ(1.5), 0, 0.75, 0.85, 1.0, 1.0);
        }
        if (t == 30) {
            this.playSound(ModSounds.SKY_WHALE_MOO.get(), 4.0F, 0.9F);
        }
        if (t == 60 && this.gemThisVisit) {
            level.broadcastEntityEvent(this, EVENT_SPIT);
        }
        if (t == 72) {
            if (this.gemThisVisit) {
                this.spitGem(level, p);
            } else {
                p.sendOverlayMessage(Component.translatable("message.thesift.sky_whale.no_gem"));
            }
        }
        if (t >= 110) {
            this.finishAnswer();
        }
    }

    private void spitGem(ServerLevel level, Player p) {
        Vec3 mouth = this.mouthPos();
        ItemEntity gem = new ItemEntity(level, mouth.x, mouth.y, mouth.z, new ItemStack(ModItems.SKYSONG_GEM.get()));
        Vec3 toward = p.getEyePosition().subtract(mouth);
        gem.setDeltaMovement(toward.scale(0.09).add(0.0, 0.25, 0.0));
        gem.setPickUpDelay(10);
        level.addFreshEntity(gem);
        this.lastGem = level.getGameTime();
        this.gemThisVisit = false;
        this.playSound(ModSounds.SKY_WHALE_SPIT.get(), 2.0F, 1.0F);
        level.sendParticles(ModParticles.STAR_SPARKLE.get(), mouth.x, mouth.y, mouth.z, 60, 1.2, 1.0, 1.2, 0.15);
        level.sendParticles(ModParticles.WISHING_STAR.get(), mouth.x, mouth.y + 1.0, mouth.z, 6, 0.5, 0.5, 0.5, 0.0);
        level.sendParticles(ParticleTypes.END_ROD, mouth.x, mouth.y, mouth.z, 30, 0.6, 0.6, 0.6, 0.12);
        level.sendParticles(ModParticles.RESONANCE_RING.get(), mouth.x, mouth.y, mouth.z, 0, 3.0, 0.0, 0.0, 1.0);
        level.sendParticles(ModParticles.KILL_STAR.get(), mouth.x, mouth.y, mouth.z, 0, 0xFFE08A, 0.0, 1.2, 1.0);
        level.sendParticles(ParticleTypes.CLOUD, mouth.x, mouth.y, mouth.z, 20, 0.8, 0.5, 0.8, 0.05);
        p.sendOverlayMessage(Component.translatable("message.thesift.sky_whale.gem"));
    }

    private void finishAnswer() {
        this.answering = null;
        this.songTicks = -1;
        this.riseTicks = 200;
        this.waypoint = null;
    }

    private Vec3 mouthPos() {
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        return new Vec3(this.getX() - Mth.sin(yaw) * 4.2, this.getY() + 0.9, this.getZ() + Mth.cos(yaw) * 4.2);
    }

    // ------------------------------------------------------------------ flight

    /** Server: the eased turning speed (degrees per tick), so it swings into and out of turns. */
    private float turnSpeed;
    /** Client: how hard it is banking, smoothed (degrees of turn per tick). */
    public float bank;
    public float bankO;

    @Override
    public void travel(Vec3 input) {
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.91));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level() instanceof ServerLevel server) {
            if (this.answering != null) {
                this.tickAnswer(server);
            }
            this.steer(server);
        } else {
            this.flapO = this.flap;
            this.flap = Mth.sin(this.tickCount * 0.08F + this.getId());
            this.bankO = this.bank;
            this.bank += (Mth.clamp(Mth.wrapDegrees(this.yBodyRot - this.yBodyRotO), -4.0F, 4.0F) - this.bank) * 0.08F;
            this.trail();
        }
    }

    private void steer(ServerLevel level) {
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(this.getX()), Mth.floor(this.getZ()));
        if (this.answering == null) {
            if (this.riseTicks > 0) {
                this.riseTicks--;
            }
            if (this.waypoint == null || --this.waypointTimer <= 0 || this.position().distanceToSqr(this.waypoint) < 16.0) {
                this.waypointTimer = 300 + this.random.nextInt(300);
                double a = this.random.nextDouble() * Math.PI * 2.0;
                double d = 24.0 + this.random.nextDouble() * 40.0;
                double wx = this.getX() + Math.cos(a) * d;
                double wz = this.getZ() + Math.sin(a) * d;
                int g = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(wx), Mth.floor(wz));
                double alt = Math.max(g, 63) + 32.0 + this.random.nextDouble() * 22.0;
                this.waypoint = new Vec3(wx, Math.min(alt, 290.0), wz);
            }
        }
        Vec3 target = this.waypoint;
        Vec3 v = this.getDeltaMovement();
        double speed = this.answering != null ? 0.28 : this.riseTicks > 0 ? 0.22 : 0.12;
        if (target != null) {
            Vec3 to = target.subtract(this.position());
            if (this.riseTicks > 0) {
                to = new Vec3(to.x * 0.3, Math.max(to.y, 6.0), to.z * 0.3);
            }
            double dist = to.length();
            if (dist > 0.1) {
                Vec3 desired = to.scale(Math.min(speed, dist * 0.05 + 0.02) / dist);
                v = v.add(desired.subtract(v).scale(this.answering != null ? 0.05 : 0.02));
            }
        }
        // the majestic bob
        v = v.add(0.0, Mth.sin(this.tickCount * 0.035F + this.getId()) * 0.0035, 0.0);
        // never scrape the ground
        if (this.answering == null && this.getY() < ground + 12.0) {
            v = v.add(0.0, 0.01, 0.0);
        }
        this.setDeltaMovement(v);
        if (v.horizontalDistanceSqr() > 1.0E-4 && this.songTicks < 0) {
            float yaw = (float) Math.toDegrees(Math.atan2(v.z, v.x)) - 90.0F;
            float cur = this.getYRot();
            // ease into the turn and out of it again instead of snapping round at a fixed rate
            float want = Mth.clamp(Mth.wrapDegrees(yaw - cur) * 0.06F, -1.5F, 1.5F);
            this.turnSpeed += (want - this.turnSpeed) * 0.08F;
            this.setYRot(cur + this.turnSpeed);
            this.yBodyRot = this.getYRot();
        } else if (this.answering != null) {
            double dx = this.answering.getX() - this.getX();
            double dz = this.answering.getZ() - this.getZ();
            float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
            float cur = this.getYRot();
            this.setYRot(cur + Mth.clamp(Mth.wrapDegrees(yaw - cur), -3.0F, 3.0F));
            this.yBodyRot = this.getYRot();
        }
    }

    /** Client: wisps of cloud off the tail and glowing motes along the body. */
    private void trail() {
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        double bx = Mth.sin(yaw);
        double bz = -Mth.cos(yaw);
        if (this.tickCount % 3 == 0) {
            double back = 5.5 + this.random.nextDouble();
            this.level().addParticle(ParticleTypes.CLOUD, this.getX() + bx * back + (this.random.nextDouble() - 0.5) * 2.0,
                    this.getY() + 1.0 + this.random.nextDouble(), this.getZ() + bz * back + (this.random.nextDouble() - 0.5) * 2.0, 0.0, 0.0, 0.0);
        }
        if (this.random.nextInt(3) == 0) {
            this.level().addParticle(ModParticles.DRIFTING_SOUL.get(), this.getRandomX(2.0), this.getY() + this.random.nextDouble() * 3.0,
                    this.getRandomZ(2.0), 0.0, 0.0, 0.0);
        }
        if (this.random.nextInt(5) == 0) {
            this.level().addParticle(ModParticles.STAR_SPARKLE.get(), this.getRandomX(2.5), this.getY() + this.random.nextDouble() * 3.0,
                    this.getRandomZ(2.5), 0.0, 0.0, 0.0);
        }
        // petals and pollen shaken loose from the meadow on its back drift away behind it
        if (this.random.nextInt(3) == 0) {
            this.level().addParticle(ModParticles.WISHWOOD_LEAF.get(), this.getRandomX(2.0) + bx * 2.0, this.getY() + 2.6, this.getRandomZ(2.0) + bz * 2.0,
                    bx * 0.04, -0.01, bz * 0.04);
        }
        if (this.random.nextInt(4) == 0) {
            this.level().addParticle(ModParticles.DREAM_POLLEN.get(), this.getRandomX(2.0), this.getY() + 2.4 + this.random.nextDouble(), this.getRandomZ(2.0),
                    bx * 0.02, 0.0, bz * 0.02);
        }
        if (this.random.nextInt(6) == 0) {
            this.level().addParticle(ModParticles.GLOW_DUST.get(), this.getRandomX(2.4), this.getY() + 0.2, this.getRandomZ(2.4), 0.0, -0.01, 0.0);
        }
        if (this.random.nextInt(40) == 0) {
            this.level().addParticle(ModParticles.SIFT_MIST.get(), this.getX() + bx * 4.0, this.getY() + 0.5, this.getZ() + bz * 4.0, 0.0, 0.0, 0.0);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_SING) {
            this.singAnimation.start(this.tickCount);
        } else if (id == EVENT_SPIT) {
            this.spitAnimation.start(this.tickCount);
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
            @Nullable SpawnGroupData data) {
        if (reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION) {
            // spawned on the surface: rise straight up into the sky
            int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(this.getX()), Mth.floor(this.getZ()));
            this.setPos(this.getX(), Math.max(this.getY(), top + 30.0 + this.random.nextInt(16)), this.getZ());
        }
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    // ------------------------------------------------------------------ misc

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 400;
    }

    @Override
    protected float getSoundVolume() {
        return 5.0F;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.random.nextInt(3) == 0 ? ModSounds.SKY_WHALE_MOO.get() : ModSounds.SKY_WHALE_AMBIENT.get();
    }

    @Override
    public void playAmbientSound() {
        super.playAmbientSound();
        if (this.level() instanceof ServerLevel server) {
            server.broadcastEntityEvent(this, EVENT_SING);
            for (int i = 0; i < 4; i++) {
                server.sendParticles(ModParticles.SIFT_NOTE.get(), this.getRandomX(2.0), this.getY() + 3.0, this.getRandomZ(2.0), 0, this.random.nextDouble(),
                        0.0, 0.0, 1.0);
            }
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.SKY_WHALE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SKY_WHALE_DEATH.get();
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.NEUTRAL;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putLong("LastGem", this.lastGem);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.lastGem = input.getLongOr("LastGem", Long.MIN_VALUE / 2);
    }

    /** A soft cloud-burst of fur, notes and stars. */
    @Override
    public void makePoofParticles() {
        KillBurst.pop(this, 0x78A5E3, 0xFFD0E6, KillBurst.NOTE, ModParticles.WISHWOOD_LEAF.get());
    }

    /** Sky Whales hold their place in the sky: the bounding box sweeps a wide area. */
    public AABB songArea() {
        return this.getBoundingBox().inflate(4.0);
    }
}
