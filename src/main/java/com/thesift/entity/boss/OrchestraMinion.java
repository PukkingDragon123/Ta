package com.thesift.entity.boss;

import com.thesift.entity.KillBurst;
import java.util.EnumSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * One of the young of the Conductor's three great players - a Sculk Parasite, a Whistling or a
 * Strumling - and the rank and file of his orchestra. Every one has a signature attack: it winds up
 * (so you can see it coming), then plays its note. Subclasses say how far it reaches and what the
 * note does.
 */
public abstract class OrchestraMinion extends Monster {
    protected static final byte EVENT_ATTACK = 64;

    public final AnimationState attackAnimation = new AnimationState();
    private int cooldown = 40;
    private int windup = -1;
    /** The client only hears when a wind-up starts: it counts the wind-up out itself. */
    private int clientWindupEnd = -1;

    protected OrchestraMinion(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 12;
    }

    /** Ticks between the wind-up starting and the note landing. */
    protected abstract int windupTicks();

    protected abstract int cooldownTicks();

    protected abstract double minRange();

    protected abstract double maxRange();

    /** Plays the note at the target. */
    protected abstract void strike(ServerLevel level, LivingEntity target);

    /** Feedback while winding up (sounds, particles); called once when the wind-up starts. */
    protected void beginWindup(ServerLevel level, LivingEntity target) {
    }

    public boolean isWindingUp() {
        return this.level().isClientSide() ? this.tickCount < this.clientWindupEnd : this.windup >= 0;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SignatureAttackGoal());
        this.addMovementGoals();
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, OrchestraMinion.class, Dictator.class, MiniBoss.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Chasing or kiting goals (priority 3-5). */
    protected abstract void addMovementGoals();

    /** Members of the orchestra (and their conductor) never hurt each other. */
    protected static boolean isBandmate(LivingEntity e) {
        return MiniBoss.isBandmate(e);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_ATTACK) {
            this.attackAnimation.start(this.tickCount);
            this.clientWindupEnd = this.tickCount + this.windupTicks();
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() && this.deathTime > 2 && this.random.nextInt(2) == 0) {
            this.level().addParticle(ParticleTypes.SCULK_SOUL, this.getRandomX(0.6), this.getRandomY(), this.getRandomZ(0.6), 0, 0.04, 0);
        }
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    private final class SignatureAttackGoal extends Goal {
        SignatureAttackGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            OrchestraMinion self = OrchestraMinion.this;
            if (self.cooldown > 0) {
                self.cooldown--;
                return false;
            }
            LivingEntity target = self.getTarget();
            if (target == null || !target.isAlive() || !self.hasLineOfSight(target)) {
                return false;
            }
            double d = self.distanceTo(target);
            return d >= self.minRange() && d <= self.maxRange();
        }

        @Override
        public boolean canContinueToUse() {
            return OrchestraMinion.this.windup >= 0 && OrchestraMinion.this.getTarget() != null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            OrchestraMinion self = OrchestraMinion.this;
            self.windup = self.windupTicks();
            self.getNavigation().stop();
            self.level().broadcastEntityEvent(self, EVENT_ATTACK);
            if (self.level() instanceof ServerLevel server && self.getTarget() != null) {
                self.beginWindup(server, self.getTarget());
            }
        }

        @Override
        public void tick() {
            OrchestraMinion self = OrchestraMinion.this;
            LivingEntity target = self.getTarget();
            if (target == null) {
                return;
            }
            self.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (--self.windup < 0 && self.level() instanceof ServerLevel server) {
                self.strike(server, target);
                self.cooldown = self.cooldownTicks();
            }
        }

        @Override
        public void stop() {
            OrchestraMinion.this.windup = -1;
        }
    }

    /** Its colours for the burst of notes it leaves when it is silenced. */
    protected int burstColor() {
        return 0x2EF2E2;
    }

    /** Silenced: a burst of notes and sculk souls. */
    @Override
    public void makePoofParticles() {
        KillBurst.pop(this, this.burstColor(), 0xEAF8FF, KillBurst.NOTE, ParticleTypes.SCULK_SOUL);
    }
}
