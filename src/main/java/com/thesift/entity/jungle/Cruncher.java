package com.thesift.entity.jungle;

import com.thesift.entity.KillBurst;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModCaveJungle;
import com.thesift.registry.ModItems;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * P4 Cave Jungle: the Cruncher - a small, bad-tempered cave raptor plated with crusts of Magnesite and dusted with
 * glittering Magnesium. It eats rock: it hunts out Magnesite (seams in the cave walls, or blocks and stones left
 * lying about), crunches it with a jaw of fangs and molars and, a little later, leaves a pile of Magnesium behind -
 * the only way to get any. It goes for anything that walks into its cave. Its teeth are worth having.
 */
public class Cruncher extends Monster {
    private static final byte EVENT_BITE = 100;
    private static final byte EVENT_CRUNCH = 101;
    private static final byte EVENT_WASTE = 102;

    public final AnimationState biteAnimation = new AnimationState();
    public final AnimationState crunchAnimation = new AnimationState();
    public final AnimationState wasteAnimation = new AnimationState();

    /** Mouthfuls of Magnesite waiting to come out as Magnesium. */
    private int meals;
    private int digest;

    public Cruncher(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 6;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 22.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.ARMOR, 4.0).add(Attributes.FOLLOW_RANGE, 20.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
    }

    @Override
    public int getNoActionTime() {
        return 0;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, false));
        this.goalSelector.addGoal(4, new EatMagnesiteGoal());
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.9));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return level.getBlockState(pos.below()).is(ModBlocks.MAGNESITE.get()) ? 10.0F : 0.0F;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        level.broadcastEntityEvent(this, EVENT_BITE);
        this.playSound(ModCaveJungle.CRUNCHER_BITE.get(), 1.0F, 0.9F + this.random.nextFloat() * 0.3F);
        return super.doHurtTarget(level, target);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.meals > 0 && --this.digest <= 0) {
            // what goes in as Magnesite comes out as Magnesium
            this.meals--;
            this.digest = 200 + this.random.nextInt(200);
            this.spawnAtLocation(level, new ItemStack(ModItems.MAGNESIUM.get(), 1 + this.random.nextInt(2)));
            level.broadcastEntityEvent(this, EVENT_WASTE);
            this.playSound(ModCaveJungle.CRUNCHER_WASTE.get(), 0.8F, 1.0F);
            Vec3 tail = this.position().add(Vec3.directionFromRotation(0.0F, this.yBodyRot).scale(-0.7));
            level.sendParticles(ParticleTypes.WHITE_ASH, tail.x, this.getY() + 0.3, tail.z, 16, 0.25, 0.15, 0.25, 0.02);
            level.sendParticles(ParticleTypes.POOF, tail.x, this.getY() + 0.2, tail.z, 4, 0.15, 0.05, 0.15, 0.02);
        }
    }

    /** A mouthful of Magnesite: crunched, swallowed, and on its way to becoming Magnesium. */
    void ate(ServerLevel level, Vec3 at, BlockState crumbs) {
        level.broadcastEntityEvent(this, EVENT_CRUNCH);
        this.playSound(ModCaveJungle.CRUNCHER_CRUNCH.get(), 1.0F, 0.9F + this.random.nextFloat() * 0.2F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, crumbs), at.x, at.y, at.z, 16, 0.25, 0.25, 0.25, 0.1);
        this.heal(4.0F);
        if (this.meals == 0) {
            this.digest = 160 + this.random.nextInt(120);
        }
        this.meals = Math.min(this.meals + 1, 6);
    }

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EVENT_BITE -> this.biteAnimation.start(this.tickCount);
            case EVENT_CRUNCH -> this.crunchAnimation.start(this.tickCount);
            case EVENT_WASTE -> this.wasteAnimation.start(this.tickCount);
            default -> super.handleEntityEvent(id);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() && this.random.nextInt(14) == 0) {
            // Magnesium dust glittering off its plates
            this.level().addParticle(ParticleTypes.WHITE_ASH, this.getRandomX(0.5), this.getY() + 0.6 + this.random.nextDouble() * 0.4,
                    this.getRandomZ(0.5), 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Meals", this.meals);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.meals = input.getIntOr("Meals", 0);
        this.digest = 200;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ModCaveJungle.CRUNCHER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModCaveJungle.CRUNCHER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModCaveJungle.CRUNCHER_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(ModCaveJungle.CRUNCHER_STEP.get(), 0.3F, 1.0F);
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 3;
    }

    @Override
    public void makePoofParticles() {
        KillBurst.pop(this, 0x55585C, 0xD3C7B8, KillBurst.STAR, ParticleTypes.WHITE_ASH);
    }

    /** Sniffs out Magnesite - a loose stone or block on the floor first, else a seam in the rock - and eats it. */
    private class EatMagnesiteGoal extends Goal {
        private @Nullable BlockPos rock;
        private @Nullable ItemEntity loose;
        private int time;
        private int cooldown;

        EatMagnesiteGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Cruncher c = Cruncher.this;
            if (c.getTarget() != null || c.meals >= 6 || --this.cooldown > 0) {
                return false;
            }
            this.cooldown = 80;
            List<ItemEntity> items = c.level().getEntitiesOfClass(ItemEntity.class, c.getBoundingBox().inflate(8.0),
                    e -> e.isAlive() && e.getItem().is(ModItems.MAGNESITE.get()));
            if (!items.isEmpty()) {
                this.loose = items.get(0);
                return true;
            }
            if (c.random.nextInt(3) != 0) {
                return false;
            }
            BlockPos o = c.blockPosition();
            for (int i = 0; i < 80; i++) {
                BlockPos p = o.offset(c.random.nextInt(17) - 8, c.random.nextInt(5) - 2, c.random.nextInt(17) - 8);
                if (c.level().getBlockState(p).is(ModBlocks.MAGNESITE.get()) && c.level().isEmptyBlock(p.above())) {
                    this.rock = p;
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return (this.rock != null || this.loose != null && this.loose.isAlive()) && this.time < 240 && Cruncher.this.getTarget() == null;
        }

        @Override
        public void start() {
            this.time = 0;
        }

        @Override
        public void stop() {
            this.rock = null;
            this.loose = null;
        }

        @Override
        public void tick() {
            Cruncher c = Cruncher.this;
            this.time++;
            Vec3 at = this.loose != null ? this.loose.position() : this.rock != null ? Vec3.atCenterOf(this.rock) : null;
            if (at == null) {
                return;
            }
            c.getLookControl().setLookAt(at.x, at.y, at.z);
            if (c.distanceToSqr(at) > 3.2) {
                if (c.getNavigation().isDone() || this.time % 20 == 0) {
                    c.getNavigation().moveTo(at.x, at.y, at.z, 1.0);
                }
                return;
            }
            c.getNavigation().stop();
            if (this.time % 15 != 0 || !(c.level() instanceof ServerLevel server)) {
                return;
            }
            if (this.loose != null) {
                ItemStack stack = this.loose.getItem().copy();
                stack.shrink(1);
                if (stack.isEmpty()) {
                    this.loose.discard();
                } else {
                    this.loose.setItem(stack);
                }
                c.ate(server, at, ModBlocks.MAGNESITE.get().defaultBlockState());
                this.loose = null;
                return;
            }
            BlockPos p = this.rock;
            if (p == null) {
                return;
            }
            BlockState st = server.getBlockState(p);
            if (!st.is(ModBlocks.MAGNESITE.get())) {
                this.rock = null;
                return;
            }
            c.ate(server, at, st);
            if (c.random.nextInt(3) == 0) {
                if (Boolean.TRUE.equals(server.getGameRules().get(GameRules.MOB_GRIEFING))) {
                    server.destroyBlock(p, false, c);
                }
                this.rock = null;
            }
        }
    }
}
