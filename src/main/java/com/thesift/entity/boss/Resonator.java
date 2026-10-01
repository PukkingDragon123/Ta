package com.thesift.entity.boss;

import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;

/**
 * Resonator - the strings. A spidery harp of bone and glowing sculk tendons that keeps its
 * distance. It draws back a string and lets it go: a taut, singing line snaps out to you, cuts, and
 * pulls you towards it.
 */
public class Resonator extends OrchestraMinion {
    public Resonator(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 24.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void addMovementGoals() {
        this.goalSelector.addGoal(4, new KeepDistanceGoal());
    }

    @Override
    protected int windupTicks() {
        return 14;
    }

    @Override
    protected int cooldownTicks() {
        return 50;
    }

    @Override
    protected double minRange() {
        return 3.0;
    }

    @Override
    protected double maxRange() {
        return 16.0;
    }

    @Override
    protected void beginWindup(ServerLevel level, LivingEntity target) {
        this.playSound(ModSounds.RESONATOR_DRAW.get(), 1.0F, 1.0F + this.random.nextFloat() * 0.3F);
    }

    @Override
    protected void strike(ServerLevel level, LivingEntity target) {
        this.playSound(ModSounds.RESONATOR_PLUCK.get(), 1.6F, 0.8F + this.random.nextFloat() * 0.6F);
        Vec3 from = this.position().add(0, this.getBbHeight() * 0.7, 0);
        Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Vec3 d = to.subtract(from);
        int n = (int) (d.length() * 3);
        for (int i = 0; i <= n; i++) {
            Vec3 p = from.add(d.scale(i / (double) n));
            // a vibrating string: the line wobbles a little around its straight path
            double w = Math.sin(i * 1.3) * 0.06;
            level.sendParticles(ModParticles.GLOW_DUST.get(), p.x + w, p.y + w, p.z - w, 1, 0.0, 0.0, 0.0, 0.0);
        }
        level.sendParticles(ModParticles.SIFT_NOTE.get(), to.x, to.y + 0.3, to.z, 0, 0.05, 0.0, 0.0, 1.0);
        if (target.hurtServer(level, this.damageSources().mobAttack(this), 5.0F)) {
            Vec3 pull = d.normalize().scale(-0.6);
            target.push(pull.x, 0.15, pull.z);
            target.hurtMarked = true;
        }
    }

    /** Backs off from anything that gets too close, and edges in when the target is too far. */
    private final class KeepDistanceGoal extends Goal {
        KeepDistanceGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return Resonator.this.getTarget() != null && !Resonator.this.isWindingUp();
        }

        @Override
        public void tick() {
            LivingEntity target = Resonator.this.getTarget();
            if (target == null || Resonator.this.tickCount % 10 != 0) {
                return;
            }
            Vec3 here = Resonator.this.position();
            Vec3 off = here.subtract(target.position());
            double dist = off.length();
            if (dist < 6.0) {
                Vec3 to = here.add(off.normalize().scale(6.0));
                Resonator.this.getNavigation().moveTo(to.x, to.y, to.z, 1.25);
            } else if (dist > 13.0) {
                Resonator.this.getNavigation().moveTo(target, 1.0);
            } else {
                Resonator.this.getNavigation().stop();
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.RESONATOR_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.RESONATOR_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.RESONATOR_DEATH.get();
    }
}
