package com.thesift.entity.jungle;

import com.thesift.block.BoredLogBlock;
import com.thesift.entity.KillBurst;
import com.thesift.registry.ModCaveJungle;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * P4 Cave Jungle: a Ponder Tadpole - the Colossus Ponder's young: a big head, a mouth full of needle teeth, two
 * budding legs and a see-through fin of a tail. Tadpoles wriggle about near their parent and bore into logs to live
 * in them, leaving the wood pocked with holes ({@link BoredLogBlock}). Left alone they are harmless; bite back at
 * anything that hurts them - and hurting one brings every Colossus Ponder near on a rampage.
 */
public class PonderTadpole extends PathfinderMob {
    private static final byte EVENT_BITE = 100;
    private static final byte EVENT_BURROW = 101;

    public final AnimationState biteAnimation = new AnimationState();
    public final AnimationState burrowAnimation = new AnimationState();

    public PonderTadpole(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 6.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    public int getNoActionTime() {
        return 0;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.4, true));
        this.goalSelector.addGoal(4, new BurrowGoal());
        this.goalSelector.addGoal(6, new RandomStrollGoal(this, 0.9));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return 0.0F;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && source.getEntity() instanceof LivingEntity attacker) {
            ColossusPonder.alarm(level, this.position(), attacker);
        }
        return hurt;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        level.broadcastEntityEvent(this, EVENT_BITE);
        this.playSound(ModCaveJungle.TADPOLE_BITE.get(), 0.9F, 1.0F + this.random.nextFloat() * 0.3F);
        return super.doHurtTarget(level, target);
    }

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EVENT_BITE -> this.biteAnimation.start(this.tickCount);
            case EVENT_BURROW -> this.burrowAnimation.start(this.tickCount);
            default -> super.handleEntityEvent(id);
        }
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ModCaveJungle.TADPOLE_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModCaveJungle.TADPOLE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModCaveJungle.TADPOLE_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 0.6F;
    }

    @Override
    public void makePoofParticles() {
        KillBurst.pop(this, 0x353820, 0x45B4B8, KillBurst.DROP, ParticleTypes.SPLASH);
    }

    /** Wriggles to a log close by and bores into it (once it has been out a while, and only when nothing bothers it). */
    private class BurrowGoal extends Goal {
        private @Nullable BlockPos log;
        private int time;

        BurrowGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            PonderTadpole t = PonderTadpole.this;
            if (t.getTarget() != null || t.tickCount < 600 || t.random.nextInt(200) != 0) {
                return false;
            }
            BlockPos o = t.blockPosition();
            for (int i = 0; i < 60; i++) {
                BlockPos p = o.offset(t.random.nextInt(17) - 8, t.random.nextInt(7) - 2, t.random.nextInt(17) - 8);
                BlockState st = t.level().getBlockState(p);
                if (st.is(BlockTags.LOGS) && (!(st.getBlock() instanceof BoredLogBlock) || st.getValue(BoredLogBlock.TADPOLES) < 3)) {
                    this.log = p;
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return this.log != null && this.time < 300 && PonderTadpole.this.getTarget() == null;
        }

        @Override
        public void start() {
            this.time = 0;
        }

        @Override
        public void stop() {
            this.log = null;
        }

        @Override
        public void tick() {
            PonderTadpole t = PonderTadpole.this;
            BlockPos p = this.log;
            if (p == null) {
                return;
            }
            this.time++;
            Vec3 c = Vec3.atCenterOf(p);
            if (t.distanceToSqr(c) > 2.6) {
                if (t.getNavigation().isDone() || this.time % 20 == 0) {
                    // the nearest open side of the log
                    BlockPos side = p;
                    for (Direction d : Direction.Plane.HORIZONTAL) {
                        if (t.level().isEmptyBlock(p.relative(d))) {
                            side = p.relative(d);
                            break;
                        }
                    }
                    t.getNavigation().moveTo(side.getX() + 0.5, side.getY(), side.getZ() + 0.5, 1.0);
                }
                return;
            }
            if (!(t.level() instanceof ServerLevel server)) {
                return;
            }
            BlockState st = server.getBlockState(p);
            if (st.is(BlockTags.LOGS) && BoredLogBlock.boreInto(server, p, st)) {
                server.broadcastEntityEvent(t, EVENT_BURROW);
                t.playSound(ModCaveJungle.TADPOLE_BURROW.get(), 1.0F, 1.0F);
                server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, st), c.x, c.y, c.z, 20, 0.35, 0.35, 0.35, 0.1);
                t.discard();
            }
            this.log = null;
        }
    }
}
