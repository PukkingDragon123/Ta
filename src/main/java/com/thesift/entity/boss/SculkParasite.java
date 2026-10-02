package com.thesift.entity.boss;

import com.thesift.effect.SculkCorruptionEffect;
import com.thesift.entity.KillBurst;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A Sculk Parasite: a small, nasty Warden-kin centipede of the Conductor's orchestra. Fragile - a
 * couple of hits finish it - but quick, and it scuttles up walls and through cobwebs. It chases you
 * and nips, and now and then it coils up and lunges. The moment it hurts something it bursts in a
 * spray of sculk, leaving Sculk Corruption behind: II from the first, and every further parasite
 * that gets you deepens it a level.
 */
public class SculkParasite extends OrchestraMinion {
    /** It bursts: the client plays its pop (it is removed straight after, with no death bounce). */
    private static final byte EVENT_BURST = 104;
    private static final int CORRUPTION_TICKS = 300;

    /** Ticks left in a lunge, during which touching the target bites it. */
    private int lunge;
    private boolean burst;

    public SculkParasite(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 2;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 4.0).add(Attributes.MOVEMENT_SPEED, 0.34).add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    /** It chases a little faster than you walk, but not as fast as you sprint (mob speed goes with the square of the modifier). */
    @Override
    protected void addMovementGoals() {
        this.goalSelector.addGoal(3, new SkitterGoal());
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0, true));
    }

    @Override
    protected int windupTicks() {
        return 10;
    }

    @Override
    protected int cooldownTicks() {
        return 60;
    }

    @Override
    protected double minRange() {
        return 1.8;
    }

    @Override
    protected double maxRange() {
        return 6.0;
    }

    /** The tell: it coils up, clicking and hissing, its sting raised. */
    @Override
    protected void beginWindup(ServerLevel level, LivingEntity target) {
        this.playSound(ModSounds.SCULK_PARASITE_HISS.get(), 0.9F, 0.95F + this.random.nextFloat() * 0.25F);
        level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, this.getX(), this.getY() + 0.3, this.getZ(), 6, 0.3, 0.15, 0.3, 0.01);
    }

    /** The lunge: a fast, low leap straight at the target. */
    @Override
    protected void strike(ServerLevel level, LivingEntity target) {
        Vec3 h = target.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
        double len = h.length();
        Vec3 dir = len < 1.0E-4 ? this.getLookAngle().multiply(1.0, 0.0, 1.0) : h.scale(1.0 / len);
        this.setDeltaMovement(dir.scale(Math.min(1.15, 0.45 + len * 0.18)).add(0.0, 0.3, 0.0));
        this.lunge = 14;
        level.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY() + 0.2, this.getZ(), 3, 0.2, 0.1, 0.2, 0.02);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.lunge > 0 && this.isAlive() && this.level() instanceof ServerLevel server) {
            this.lunge--;
            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive() && this.getBoundingBox().inflate(0.35).intersects(target.getBoundingBox())) {
                this.lunge = 0;
                this.doHurtTarget(server, target);
                return;
            }
            if (this.lunge % 2 == 0) {
                server.sendParticles(ParticleTypes.SCULK_CHARGE_POP, this.getX(), this.getY() + 0.2, this.getZ(), 1, 0.1, 0.05, 0.1, 0.0);
            }
        }
    }

    /** A bite that lands leaves its corruption in the victim - and the parasite bursts. */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (this.burst) {
            return false;
        }
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity living) {
            SculkCorruptionEffect.stack(living, 1, CORRUPTION_TICKS, this);
            this.burst(level);
        }
        return hit;
    }

    /** Bursts in a spray of sculk and souls, and is gone (it leaves nothing behind). */
    private void burst(ServerLevel level) {
        if (this.burst) {
            return;
        }
        this.burst = true;
        double x = this.getX();
        double y = this.getY() + 0.25;
        double z = this.getZ();
        level.broadcastEntityEvent(this, EVENT_BURST);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SCULK.defaultBlockState()), x, y, z, 30, 0.35, 0.2, 0.35, 0.15);
        level.sendParticles(ParticleTypes.SCULK_SOUL, x, y, z, 12, 0.3, 0.2, 0.3, 0.05);
        level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, x, y, z, 16, 0.4, 0.2, 0.4, 0.03);
        level.sendParticles(ModParticles.RESONANCE_RING.get(), x, this.getY() + 0.05, z, 0, 1.6, 0.0, 0.0, 1.0);
        this.playSound(ModSounds.SCULK_PARASITE_BURST.get(), 1.0F, 0.9F + this.random.nextFloat() * 0.3F);
        this.playSound(this.getDeathSound(), 1.0F, this.getVoicePitch());
        this.remove(Entity.RemovalReason.KILLED);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_BURST) {
            KillBurst.pop(this, 0x29DFEB, 0xBBC39B, KillBurst.DROP, ParticleTypes.SCULK_SOUL);
            for (int i = 0; i < 10; i++) {
                this.level().addParticle(ParticleTypes.SCULK_CHARGE_POP, this.getRandomX(0.8), this.getY() + this.random.nextDouble() * 0.5,
                        this.getRandomZ(0.8), 0.0, 0.03, 0.0);
            }
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public boolean onClimbable() {
        return this.horizontalCollision;
    }

    @Override
    public void makeStuckInBlock(BlockState state, Vec3 speedMultiplier) {
        if (!state.is(Blocks.COBWEB)) {
            super.makeStuckInBlock(state, speedMultiplier);
        }
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(ModSounds.SCULK_PARASITE_STEP.get(), 0.15F, 1.0F + this.random.nextFloat() * 0.3F);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.SCULK_PARASITE_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.SCULK_PARASITE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SCULK_PARASITE_DEATH.get();
    }

    @Override
    protected int burstColor() {
        return 0x29DFEB;
    }

    /** Is it riding the titan Thumper's back? */
    private boolean aboardTitan() {
        for (Thumper th : this.level().getEntitiesOfClass(Thumper.class, this.getBoundingBox().inflate(1.0, 2.0, 1.0))) {
            if (th.isOnBack(this)) {
                return true;
            }
        }
        return false;
    }

    /**
     * On the titan's back there is no path to follow - its back is not ground the pathfinder knows -
     * so it just skitters straight at its target.
     */
    private final class SkitterGoal extends Goal {
        SkitterGoal() {
            this.setFlags(java.util.EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = SculkParasite.this.getTarget();
            return t != null && t.isAlive() && SculkParasite.this.distanceToSqr(t) < 24.0 * 24.0 && SculkParasite.this.aboardTitan();
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            SculkParasite.this.setAggressive(true);
        }

        @Override
        public void stop() {
            SculkParasite.this.setAggressive(false);
        }

        @Override
        public void tick() {
            LivingEntity t = SculkParasite.this.getTarget();
            if (t == null) {
                return;
            }
            SculkParasite.this.getLookControl().setLookAt(t, 30.0F, 30.0F);
            SculkParasite.this.getMoveControl().setWantedPosition(t.getX(), t.getY(), t.getZ(), 1.0);
        }
    }
}
