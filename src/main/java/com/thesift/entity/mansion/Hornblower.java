package com.thesift.entity.mansion;

import com.thesift.registry.ModItems;
import com.thesift.registry.ModMansion;
import java.util.EnumSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * MANSION: the Hornblower - a burly illager in goat fleece with a Giant Goat Horn. He keeps a few blocks from his target,
 * raises the horn to his lips and draws a long breath (the telegraph: {@link #WINDUP} ticks of air rushing into the bell,
 * the vanilla horn-tooting pose), then blasts: everything in the cone in front of him is thrown back
 * ({@link HornBlast}). Then he needs {@link #COOLDOWN} ticks to catch his breath, and swings the horn like a club at
 * anyone who comes close. Lives in Woodland Mansions and marches with raids; when a raid is won he toots in triumph.
 */
public class Hornblower extends AbstractIllager {
    public static final byte EVENT_BLAST = 111;
    public static final int WINDUP = 30;
    public static final int COOLDOWN = 100;
    private int cooldown = 40;
    /** Client: when the last blast went off (for the recoil). */
    public int blastTick = -1000;

    public Hornblower(EntityType<? extends Hornblower> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.33F)
                .add(Attributes.FOLLOW_RANGE, 20.0)
                .add(Attributes.MAX_HEALTH, 28.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new AbstractIllager.RaiderOpenDoorGoal(this));
        this.goalSelector.addGoal(3, new HornGoal());
        this.goalSelector.addGoal(4, new Raider.HoldGroundAttackGoal(this, 10.0F));
        this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(8, new RandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 6.0F, 1.0F));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, Raider.class).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        if (!this.isNoAi() && GoalUtils.hasGroundPathNavigation(this)) {
            this.getNavigation().setCanOpenDoors(level.isRaided(this.blockPosition()));
        }
        if (this.cooldown > 0) {
            this.cooldown--;
        }
        super.customServerAiStep(level);
    }

    /** True while the horn is at his lips (winding up). */
    public boolean isBlowing() {
        return this.isUsingItem();
    }

    @Override
    public AbstractIllager.IllagerArmPose getArmPose() {
        if (this.isCelebrating()) {
            return AbstractIllager.IllagerArmPose.CELEBRATING;
        }
        return this.isAggressive() && !this.isUsingItem() ? AbstractIllager.IllagerArmPose.ATTACKING : AbstractIllager.IllagerArmPose.NEUTRAL;
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
            @Nullable SpawnGroupData groupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
        this.getNavigation().setCanOpenDoors(true);
        this.populateDefaultEquipmentSlots(level.getRandom(), difficulty);
        return data;
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        // his horn: dropped now and then (the equipment's usual drop chance)
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.GIANT_GOAT_HORN.get()));
    }

    @Override
    public void applyRaidBuffs(ServerLevel level, int wave, boolean isCaptain) {
        if (this.getMainHandItem().isEmpty()) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.GIANT_GOAT_HORN.get()));
        }
    }

    @Override
    public SoundEvent getCelebrateSound() {
        return ModMansion.HORN_BLAST.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModMansion.HORNBLOWER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModMansion.HORNBLOWER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModMansion.HORNBLOWER_DEATH.get();
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_BLAST) {
            this.blastTick = this.tickCount;
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("HornCooldown", this.cooldown);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.cooldown = input.getIntOr("HornCooldown", 40);
    }

    /** Server: the blast itself, along where he faces. */
    public void blast(ServerLevel level) {
        Vec3 dir = Vec3.directionFromRotation(this.getXRot(), this.getYHeadRot());
        LivingEntity target = this.getTarget();
        if (target != null) {
            dir = target.getEyePosition().subtract(this.getEyePosition());
        }
        HornBlast.blast(level, this, dir, 1.0F, 3.0F);
        level.broadcastEntityEvent(this, EVENT_BLAST);
        this.cooldown = COOLDOWN + this.random.nextInt(40);
    }

    /**
     * The horn: when the target is in reach of the gust (3 to 8 blocks) and he can see it, he stops, raises the horn and
     * draws breath for {@link #WINDUP} ticks, still turning to follow it, then blasts.
     */
    private class HornGoal extends Goal {
        private int time;

        HornGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = Hornblower.this.getTarget();
            if (t == null || !t.isAlive() || Hornblower.this.cooldown > 0 || Hornblower.this.getMainHandItem().isEmpty()) {
                return false;
            }
            double d = Hornblower.this.distanceTo(t);
            return d >= 2.5 && d <= HornBlast.RANGE - 1.0 && Hornblower.this.getSensing().hasLineOfSight(t);
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity t = Hornblower.this.getTarget();
            return this.time < WINDUP + 8 && t != null && t.isAlive() && Hornblower.this.isAlive();
        }

        @Override
        public void start() {
            this.time = 0;
            Hornblower.this.getNavigation().stop();
            Hornblower.this.startUsingItem(InteractionHand.MAIN_HAND);
            Hornblower.this.playSound(ModMansion.HORNBLOWER_WINDUP.get(), 1.4F, 1.0F);
        }

        @Override
        public void stop() {
            Hornblower.this.stopUsingItem();
            if (this.time < WINDUP) {
                // interrupted: he still needs a moment before trying again
                Hornblower.this.cooldown = Math.max(Hornblower.this.cooldown, 30);
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity t = Hornblower.this.getTarget();
            if (t != null) {
                Hornblower.this.getLookControl().setLookAt(t, 30.0F, 30.0F);
            }
            if (!(Hornblower.this.level() instanceof ServerLevel level)) {
                return;
            }
            this.time++;
            Vec3 dir = Vec3.directionFromRotation(Hornblower.this.getXRot(), Hornblower.this.getYHeadRot());
            if (this.time < WINDUP) {
                if (this.time % 2 == 0) {
                    HornBlast.inhale(level, Hornblower.this, dir);
                }
                if (this.time == WINDUP - 10) {
                    // the last deep breath: the bell trembles
                    level.playSound(null, Hornblower.this.getX(), Hornblower.this.getY(), Hornblower.this.getZ(), ModMansion.HORNBLOWER_WINDUP.get(),
                            SoundSource.HOSTILE, 0.8F, 1.35F);
                }
            } else if (this.time == WINDUP) {
                Hornblower.this.blast(level);
            }
        }
    }
}
