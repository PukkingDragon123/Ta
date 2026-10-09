package com.thesift.entity;

import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSculkSea;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * CR3 Fish &amp; Coral Organs - the Sculk Fish: a small, deep-bodied biter of the Sculk Ocean and the swamp's
 * Sculk Water, sculk-dark with glowing eyes, a row of lights down its flank and two sensor tendrils on its
 * brow. It cannot see far in the gloom; like all sculk it hunts by vibration. A school drifts behind its
 * leader until something loud swims near - a sprinting swimmer, a splashing fish, a note played on an
 * instrument - and then the whole school turns on it at once: each fish darts in, bites, peels away and
 * circles back for another pass. Sneak, swim slowly, and they may never notice you.
 */
public class SculkFish extends SiftFish implements Enemy {
    private static final byte EVENT_BITE = -96;
    /** How far a loud swimmer is heard, and how close a quiet one must come to be noticed. */
    private static final double HEAR_LOUD = 14.0;
    private static final double HEAR_QUIET = 3.5;
    public final AnimationState biteAnimation = new AnimationState();
    private @Nullable LivingEntity prey;
    private @Nullable SculkFish leader;
    private @Nullable Vec3 heardAt;
    private int heardTicks;
    private int preyCheck;
    private int leaderCheck;
    /** After a bite the fish peels off and circles for this long before it strikes again. */
    private int retreat;
    private int calmDown;

    public SculkFish(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 6.0).add(Attributes.MOVEMENT_SPEED, 0.7).add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.FOLLOW_RANGE, HEAR_LOUD);
    }

    /** CR3: sculk cyan (the classic), shrieker-pale and the rare abyss violet. */
    @Override
    public int variantCount() {
        return 3;
    }

    @Override
    protected int[] variantWeights() {
        // S2: bone-pale in the swamp's Sculk Water, sculk teal in the ocean, black-violet down deep
        if (this.inBiome("sculk_swamp")) {
            return new int[]{6, 30, 0};
        }
        return this.getY() < 30 ? new int[]{20, 2, 30} : new int[]{60, 6, 14};
    }

    @Override
    protected double cruiseSpeed() {
        return 0.085;
    }

    @Override
    protected double speedFactor() {
        if (this.prey == null) {
            return this.heardAt != null ? 1.8 : 1.0;
        }
        return this.retreat > 0 ? 1.7 : 3.2;
    }

    public boolean isHunting() {
        return this.prey != null;
    }

    /** A note was played near here: the school comes to look (SongEvents, see ModSculkSea). */
    public void hear(Vec3 at) {
        if (this.level().getDifficulty() != Difficulty.PEACEFUL && this.distanceToSqr(at) < ModSculkSea.NOTE_RANGE * ModSculkSea.NOTE_RANGE) {
            this.heardAt = at;
            this.heardTicks = 160;
        }
    }

    private boolean isPrey(LivingEntity e) {
        if (!e.isAlive() || e == this || !e.isInFluidType() || e instanceof SculkFish || e instanceof CoralOrgan) {
            return false;
        }
        if (e instanceof Player p) {
            return !p.isCreative() && !p.isSpectator();
        }
        return e instanceof KazooFish || e instanceof net.minecraft.world.entity.animal.fish.AbstractFish;
    }

    /** Sculk hears, it does not see: fast swimmers carry far, sneaking or still ones only right up close. */
    private boolean noticed(LivingEntity e) {
        double d = this.distanceToSqr(e);
        boolean loud = e.getDeltaMovement().lengthSqr() > 0.012 || e.isSprinting() || e.isSwimming() && !e.isShiftKeyDown();
        if (e.isShiftKeyDown()) {
            loud = false;
        }
        double r = loud ? HEAR_LOUD : HEAR_QUIET;
        return d < r * r;
    }

    @Override
    protected @Nullable Vec3 wantedPosition() {
        if (--this.preyCheck <= 0) {
            this.preyCheck = 8 + this.random.nextInt(5);
            this.huntCheck();
        }
        if (this.prey != null) {
            Vec3 at = this.prey.getBoundingBox().getCenter();
            if (this.retreat > 0) {
                // peel away and circle back: each fish on its own side of the prey
                double a = this.tickCount * 0.12 + this.getId() * 1.7;
                double r = 2.4 + (this.getId() % 3) * 0.6;
                return at.add(Math.cos(a) * r, Math.sin(a * 0.7) * 0.8 + 0.4, Math.sin(a) * r);
            }
            return at;
        }
        if (this.heardAt != null) {
            if (--this.heardTicks <= 0 || this.position().distanceToSqr(this.heardAt) < 2.0) {
                this.heardAt = null;
            } else {
                return this.heardAt;
            }
        }
        // drift behind the school's leader
        if (--this.leaderCheck <= 0) {
            this.leaderCheck = 25 + this.random.nextInt(20);
            this.leader = null;
            for (SculkFish f : this.level().getEntitiesOfClass(SculkFish.class, this.getBoundingBox().inflate(10.0), Entity::isAlive)) {
                if (f.getId() < this.getId() && (this.leader == null || f.getId() < this.leader.getId())) {
                    this.leader = f;
                }
            }
        }
        if (this.leader != null && this.leader.isAlive() && this.leader.inLiquid()) {
            double a = (this.getId() * 2.399) % (Math.PI * 2.0);
            double r = 1.0 + (this.getId() % 3) * 0.45;
            Vec3 spot = this.leader.position().add(Math.cos(a) * r, ((this.getId() % 5) - 2) * 0.22, Math.sin(a) * r);
            return spot.add(this.leader.getDeltaMovement().scale(5.0));
        }
        return null;
    }

    private void huntCheck() {
        if (this.level().getDifficulty() == Difficulty.PEACEFUL) {
            this.prey = null;
        } else if (this.prey != null && (!this.isPrey(this.prey) || this.distanceToSqr(this.prey) > 20.0 * 20.0)) {
            this.prey = null;
        } else if (this.prey != null && !this.noticed(this.prey)) {
            // lost track of a prey that has gone quiet: they give up after a while
            if (++this.calmDown > 12) {
                this.prey = null;
            }
        } else if (this.prey != null) {
            this.calmDown = 0;
        }
        if (this.prey == null && this.level().getDifficulty() != Difficulty.PEACEFUL) {
            double best = Double.MAX_VALUE;
            for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(HEAR_LOUD), this::isPrey)) {
                double d = this.distanceToSqr(e);
                if (d < best && this.noticed(e)) {
                    best = d;
                    this.prey = e;
                }
            }
            if (this.prey != null) {
                this.calmDown = 0;
                this.rally(this.prey);
            }
        }
        this.setAggressive(this.prey != null);
        this.setTarget(this.prey);
    }

    /** One fish finds prey and the whole school turns: every school-mate nearby takes the same target. */
    private void rally(LivingEntity target) {
        List<SculkFish> school = this.level().getEntitiesOfClass(SculkFish.class, this.getBoundingBox().inflate(9.0), Entity::isAlive);
        for (SculkFish f : school) {
            if (f != this && f.prey == null) {
                f.prey = target;
                f.calmDown = 0;
                f.retreat = f.random.nextInt(25);
            }
        }
        this.playSound(ModSculkSea.SCULK_FISH_AMBIENT.get(), 1.0F, 1.3F + this.random.nextFloat() * 0.2F);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level() instanceof ServerLevel server) {
            if (this.retreat > 0) {
                this.retreat--;
            }
            LivingEntity p = this.prey;
            double reach = 0.9 + (p != null ? p.getBbWidth() * 0.5 : 0.0);
            if (p != null && this.retreat <= 0 && this.inLiquid() && this.distanceToSqr(p.getBoundingBox().getCenter()) < reach * reach) {
                this.retreat = 45 + this.random.nextInt(30);
                this.doHurtTarget(server, p);
            }
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        level.broadcastEntityEvent(this, EVENT_BITE);
        this.playSound(ModSculkSea.SCULK_FISH_BITE.get(), 1.0F, 1.0F + this.random.nextFloat() * 0.3F);
        Vec3 dir = this.getLookAngle();
        level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, this.getX() + dir.x * 0.4, this.getY() + 0.25, this.getZ() + dir.z * 0.4, 4,
                0.15, 0.15, 0.15, 0.02);
        boolean hit = super.doHurtTarget(level, target);
        // and recoil from the bite
        this.setDeltaMovement(this.getDeltaMovement().add(dir.scale(-0.3)).add(0.0, 0.05, 0.0));
        return hit;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && source.getEntity() instanceof LivingEntity attacker && this.isPrey(attacker)) {
            // strike one and the school answers
            this.prey = attacker;
            this.rally(attacker);
        }
        return hurt;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_BITE) {
            this.biteAnimation.start(this.tickCount);
        } else {
            super.handleEntityEvent(id);
        }
    }

    /** Glowing motes off its lights, a stream of bubbles, and a sculk pop now and then while it hunts. */
    @Override
    protected void clientEffects() {
        if (this.random.nextInt(9) == 0) {
            float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
            double back = this.random.nextDouble() * 0.5;
            this.level().addParticle(ModParticles.GLOW_DUST.get(), this.getX() + Mth.sin(yaw) * back, this.getY() + 0.22, this.getZ() - Mth.cos(yaw) * back,
                    0.0, 0.004, 0.0);
        }
        if (this.isAggressive() && this.random.nextInt(14) == 0) {
            this.level().addParticle(ParticleTypes.SCULK_CHARGE_POP, this.getRandomX(0.4), this.getY() + 0.3, this.getRandomZ(0.4), 0.0, 0.01, 0.0);
        }
    }

    @Override
    protected SoundEvent getFlopSound() {
        return ModSculkSea.SCULK_FISH_FLOP.get();
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ModSculkSea.SCULK_FISH_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSculkSea.SCULK_FISH_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSculkSea.SCULK_FISH_DEATH.get();
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 6;
    }

    /** It bursts like a sculk pod: teal stars, notes, and souls. */
    @Override
    public void makePoofParticles() {
        KillBurst.pop(this, 0x3FF5E6, 0x0D2A35, KillBurst.STAR, ParticleTypes.SCULK_SOUL);
    }
}
