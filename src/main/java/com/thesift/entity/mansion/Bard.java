package com.thesift.entity.mansion;

import com.thesift.registry.ModItems;
import com.thesift.registry.ModMansion;
import com.thesift.registry.ModParticles;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
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
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * MANSION: the Bard - a mansion minstrel in a plumed cap with a guitar. He keeps away from you and plays for his kin: when
 * an illager near him (or he himself) is hurt, he stops and strums a song ({@link #SONG} ticks, the guitar held across
 * his body like a player's), green notes drifting out; every illager within {@link #RANGE} blocks regenerates while he
 * plays, and the last chord heals them all at once. Lives in Woodland Mansions and marches with raids.
 */
public class Bard extends AbstractIllager {
    public static final byte EVENT_HEAL = 111;
    public static final int SONG = 60;
    public static final double RANGE = 10.0;
    /** A G-major progression on the guitar: the notes he strums (note-block pitches). */
    private static final int[][] CHORDS = {{1, 5, 8}, {6, 10, 13}, {8, 11, 15}, {1, 5, 8}};
    private int cooldown = 40;
    /** Client: when the last healing chord rang (for the flourish). */
    public int healTick = -1000;

    public Bard(EntityType<? extends Bard> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.33F)
                .add(Attributes.FOLLOW_RANGE, 20.0)
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.ATTACK_DAMAGE, 2.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new AbstractIllager.RaiderOpenDoorGoal(this));
        this.goalSelector.addGoal(3, new SongGoal());
        this.goalSelector.addGoal(4, new AvoidEntityGoal<>(this, Player.class, 5.0F, 0.9, 1.1));
        this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 0.9, false));
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

    /** True while he plays (the guitar is "in use": raised into its playing stance). */
    public boolean isPlaying() {
        return this.isUsingItem();
    }

    @Override
    public AbstractIllager.IllagerArmPose getArmPose() {
        return this.isCelebrating() ? AbstractIllager.IllagerArmPose.CELEBRATING : AbstractIllager.IllagerArmPose.NEUTRAL;
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
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.GUITAR.get()));
    }

    @Override
    public void applyRaidBuffs(ServerLevel level, int wave, boolean isCaptain) {
        if (this.getMainHandItem().isEmpty()) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.GUITAR.get()));
        }
    }

    @Override
    public SoundEvent getCelebrateSound() {
        return SoundEvents.NOTE_BLOCK_GUITAR.value();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModMansion.BARD_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModMansion.BARD_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModMansion.BARD_DEATH.get();
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_HEAL) {
            this.healTick = this.tickCount;
        } else {
            super.handleEntityEvent(id);
        }
    }

    /** The illagers (and their beasts) his song reaches, himself included. */
    private List<Raider> kin(ServerLevel level) {
        return level.getEntitiesOfClass(Raider.class, new AABB(this.blockPosition()).inflate(RANGE), r -> r.isAlive() && (r == this || r.isAlliedTo(this)));
    }

    private boolean anyoneHurt(ServerLevel level) {
        for (Raider r : this.kin(level)) {
            if (r.getHealth() < r.getMaxHealth() - 1.0F) {
                return true;
            }
        }
        return false;
    }

    /** One strum: a chord on the guitar and green notes drifting out; everyone near regenerates. */
    private void strum(ServerLevel level, int beat) {
        int[] chord = CHORDS[(beat / 2) % CHORDS.length];
        for (int n : chord) {
            level.playSound(null, this.getX(), this.getY() + 1.0, this.getZ(), SoundEvents.NOTE_BLOCK_GUITAR.value(), SoundSource.HOSTILE, 1.0F,
                    (float) Math.pow(2.0, (n - 12) / 12.0));
        }
        double a = this.getYRot() * Math.PI / 180.0;
        level.sendParticles(ParticleTypes.NOTE, this.getX() - Math.sin(a) * 0.4, this.getY() + 1.3, this.getZ() + Math.cos(a) * 0.4, 0,
                chord[0] / 24.0, 0.0, 0.0, 1.0);
        for (Raider r : this.kin(level)) {
            if (r.getHealth() < r.getMaxHealth()) {
                r.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 50, 0), this);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, r.getX(), r.getY() + r.getBbHeight() + 0.2, r.getZ(), 2, 0.3, 0.2, 0.3, 0.0);
            }
        }
    }

    /** The song's last chord: every illager near him heals at once. */
    public void playHealingChord(ServerLevel level) {
        level.playSound(null, this.getX(), this.getY() + 1.0, this.getZ(), ModMansion.BARD_HEAL.get(), SoundSource.HOSTILE, 1.4F, 1.0F);
        for (int n : new int[]{1, 5, 8, 13}) {
            level.playSound(null, this.getX(), this.getY() + 1.0, this.getZ(), SoundEvents.NOTE_BLOCK_GUITAR.value(), SoundSource.HOSTILE, 1.2F,
                    (float) Math.pow(2.0, (n - 12) / 12.0));
        }
        level.sendParticles(ModParticles.RESONANCE_RING.get(), this.getX(), this.getY() + 0.1, this.getZ(), 0, RANGE * 0.6, 0.0, 0.0, 1.0);
        for (Raider r : this.kin(level)) {
            r.heal(6.0F);
            r.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1), this);
            level.sendParticles(ParticleTypes.HEART, r.getX(), r.getY() + r.getBbHeight() + 0.4, r.getZ(), 3, 0.3, 0.2, 0.3, 0.0);
        }
        level.broadcastEntityEvent(this, EVENT_HEAL);
    }

    /**
     * The song: when a kinsman near him (or he himself) is hurt, he stops, raises the guitar and
     * strums for {@link #SONG} ticks (a chord every five), then lets the last chord ring out and heal everyone.
     */
    private class SongGoal extends Goal {
        private int time;

        SongGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Bard b = Bard.this;
            return b.cooldown == 0 && !b.getMainHandItem().isEmpty() && b.level() instanceof ServerLevel level && b.anyoneHurt(level);
        }

        @Override
        public boolean canContinueToUse() {
            return this.time <= SONG && Bard.this.isAlive();
        }

        @Override
        public void start() {
            this.time = 0;
            Bard.this.getNavigation().stop();
            Bard.this.startUsingItem(InteractionHand.MAIN_HAND);
        }

        @Override
        public void stop() {
            Bard.this.stopUsingItem();
            Bard.this.cooldown = 80 + Bard.this.random.nextInt(60);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (!(Bard.this.level() instanceof ServerLevel level)) {
                return;
            }
            this.time++;
            if (this.time % 5 == 0 && this.time < SONG) {
                Bard.this.strum(level, this.time / 5);
            } else if (this.time == SONG) {
                Bard.this.playHealingChord(level);
            }
        }
    }
}
