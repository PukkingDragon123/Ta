package com.thesift.entity.jungle;

import com.thesift.entity.KillBurst;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModCaveJungle;
import com.thesift.registry.ModItems;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * P4 Cave Jungle: the Crocotodo - the flightless bird of the Cave Jungle. A plump moss-teal bird with a crocodile's
 * long toothed snout and armoured scutes down its back. It forages on the moss, pecking up seeds and glow berries,
 * waddles in little flocks and flaps its stubby wings when startled. Its chicks run for it; an adult stands its
 * ground and snaps back - those jaws hurt. Breed them with Glow Berries or Pitcher Pods.
 */
public class Crocotodo extends Animal {
    private static final byte EVENT_PECK = 100;
    private static final byte EVENT_SNAP = 101;
    private static final byte EVENT_FLAP = 102;

    public final AnimationState peckAnimation = new AnimationState();
    public final AnimationState snapAnimation = new AnimationState();
    public final AnimationState flapAnimation = new AnimationState();

    public Crocotodo(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 14.0).add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.ATTACK_DAMAGE, 4.0).add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    public int getNoActionTime() {
        return 0;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.6) {
            @Override
            public boolean canUse() {
                return Crocotodo.this.isBaby() && super.canUse();
            }
        });
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.35, true));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.2, this::isFood, false));
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.15));
        this.goalSelector.addGoal(6, new PeckGoal());
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.GLOW_BERRIES) || stack.is(ModItems.PITCHER_POD.get());
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ModCaveJungle.CROCOTODO.get().create(level, EntitySpawnReason.BREEDING);
    }

    /** At home on the jungle's moss (and anywhere in the dark caves: they live there). */
    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        BlockState below = level.getBlockState(pos.below());
        return below.is(ModBlocks.LUMEN_MOSS_BLOCK.get()) || below.is(net.minecraft.world.level.block.Blocks.MOSS_BLOCK) ? 10.0F : 0.0F;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        level.broadcastEntityEvent(this, EVENT_SNAP);
        this.playSound(ModCaveJungle.CROCOTODO_SNAP.get(), 1.0F, 0.9F + this.random.nextFloat() * 0.2F);
        return super.doHurtTarget(level, target);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && this.isAlive()) {
            level.broadcastEntityEvent(this, EVENT_FLAP);
            level.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 0.7, this.getZ(), 3, 0.3, 0.2, 0.3, 0.02);
        }
        return hurt;
    }

    @Override
    public void tick() {
        super.tick();
        // a startled flap now and then as it runs
        if (!this.level().isClientSide() && this.getDeltaMovement().horizontalDistanceSqr() > 0.03 && this.random.nextInt(40) == 0) {
            this.level().broadcastEntityEvent(this, EVENT_FLAP);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EVENT_PECK -> this.peckAnimation.start(this.tickCount);
            case EVENT_SNAP -> this.snapAnimation.start(this.tickCount);
            case EVENT_FLAP -> this.flapAnimation.start(this.tickCount);
            default -> super.handleEntityEvent(id);
        }
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ModCaveJungle.CROCOTODO_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModCaveJungle.CROCOTODO_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModCaveJungle.CROCOTODO_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(ModCaveJungle.CROCOTODO_STEP.get(), 0.3F, 1.0F);
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 4;
    }

    @Override
    public void makePoofParticles() {
        KillBurst.pop(this, 0x2E6E5A, 0xE06E34, KillBurst.STAR, ParticleTypes.POOF);
    }

    /** Stops, bobs its head down and pecks at the moss a few times. */
    private class PeckGoal extends Goal {
        private int time;

        PeckGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return Crocotodo.this.onGround() && Crocotodo.this.getTarget() == null && Crocotodo.this.random.nextInt(160) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return this.time > 0 && Crocotodo.this.getTarget() == null;
        }

        @Override
        public void start() {
            this.time = 50;
            Crocotodo.this.getNavigation().stop();
        }

        @Override
        public void tick() {
            Crocotodo c = Crocotodo.this;
            this.time--;
            if (this.time % 16 == 0 && c.level() instanceof ServerLevel server) {
                server.broadcastEntityEvent(c, EVENT_PECK);
                c.playSound(ModCaveJungle.CROCOTODO_PECK.get(), 0.6F, 0.9F + c.random.nextFloat() * 0.3F);
                BlockState ground = server.getBlockState(c.blockPosition().below());
                if (!ground.isAir()) {
                    double fx = c.getX() - Math.sin(c.yBodyRot * Math.PI / 180.0) * 0.6;
                    double fz = c.getZ() + Math.cos(c.yBodyRot * Math.PI / 180.0) * 0.6;
                    server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), fx, c.getY() + 0.05, fz, 4, 0.1, 0.02, 0.1, 0.05);
                }
            }
        }
    }
}
