package com.thesift.entity.caravan;

import com.thesift.block.CrystalColor;
import com.thesift.entity.KillBurst;
import com.thesift.registry.ModCaravans;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * CR2: a Caravan larva - what hatches from the eggs a Caravan leaves in an ore socket (Egg-laden
 * Ore). A grub of five shelled segments in its caravan's colour, gem nubs on its back, a pair of
 * glowing pinprick eyes and snapping mandibles.
 * <p>
 * Mining egg-laden ore lets one or two out, and they go for whoever disturbed them: small, but
 * quick. Left alone, a larva grows into a young Caravan within a few minutes. The Crystal Hymn
 * calms them like any Caravan, and a Queen's scream brings them to her defence.
 */
public class CaravanLarva extends Monster {
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(CaravanLarva.class, EntityDataSerializers.INT);
    private static final byte EVENT_BITE = 100;
    private static final byte EVENT_EMERGE = 101;
    /** How long a larva takes to grow into a Caravan (ticks). */
    private static final int GROW_TICKS = 6000;

    public final AnimationState biteAnimation = new AnimationState();
    public final AnimationState emergeAnimation = new AnimationState();
    private int age;
    private int calmTicks;

    public CaravanLarva(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 2;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.ARMOR, 1.0)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    /** S1 never freeze: like every Sift creature it keeps crawling about out of a player's reach. */
    @Override
    public int getNoActionTime() {
        return 0;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, (target, level) -> this.calmTicks <= 0));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, 0);
    }

    public int getVariant() {
        return this.entityData.get(VARIANT);
    }

    public CrystalColor colour() {
        return CrystalColor.byId(this.getVariant());
    }

    private void setColour(CrystalColor colour) {
        this.entityData.set(VARIANT, colour.ordinal());
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
            @Nullable SpawnGroupData data) {
        CrystalColor colour = data instanceof Caravan.Group group ? group.colour() : Caravan.caravanColour(level, this.blockPosition(), this.random);
        this.setColour(colour);
        return super.finalizeSpawn(level, difficulty, reason, new Caravan.Group(colour));
    }

    /**
     * Lets {@code count} larvae out of the egg-laden ore that was at {@code pos}: they wriggle out of
     * the broken socket in their caravan's colour, already angry.
     */
    public static void emerge(ServerLevel level, BlockPos pos, BlockState ore, int count) {
        CrystalColor colour = Caravan.caravanColour(level, pos, level.getRandom());
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ore), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 24, 0.3, 0.3, 0.3, 0.15);
        level.sendParticles(new DustParticleOptions(colour.rgb(), 1.0F), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.35, 0.35, 0.35, 0.0);
        level.playSound(null, pos, ModSounds.CARAVAN_LARVA_EMERGE.get(), SoundSource.HOSTILE, 1.0F, 0.9F + level.getRandom().nextFloat() * 0.2F);
        for (int i = 0; i < count; i++) {
            CaravanLarva larva = ModCaravans.CARAVAN_LARVA.get().create(level, EntitySpawnReason.TRIGGERED);
            if (larva == null) {
                continue;
            }
            larva.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
            larva.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.TRIGGERED, new Caravan.Group(colour));
            larva.setDeltaMovement((level.getRandom().nextDouble() - 0.5) * 0.25, 0.25, (level.getRandom().nextDouble() - 0.5) * 0.25);
            level.addFreshEntity(larva);
            level.broadcastEntityEvent(larva, EVENT_EMERGE);
        }
    }

    /** The Crystal Hymn: forget every grudge for a while. */
    public void calm(int ticks) {
        this.calmTicks = ticks;
        this.setTarget(null);
        this.setAggressive(false);
    }

    /** Their Queen was attacked: go for the attacker. */
    public void defend(LivingEntity attacker) {
        if (attacker instanceof Player p && (p.isCreative() || p.isSpectator())) {
            return;
        }
        this.calmTicks = 0;
        this.setTarget(attacker);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        this.calmTicks = 0;
        return super.hurtServer(level, source, damage);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        level.broadcastEntityEvent(this, EVENT_BITE);
        return super.doHurtTarget(level, target);
    }

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EVENT_BITE -> this.biteAnimation.start(this.tickCount);
            case EVENT_EMERGE -> this.emergeAnimation.start(this.tickCount);
            default -> super.handleEntityEvent(id);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level() instanceof ServerLevel server) {
            if (this.calmTicks > 0) {
                this.calmTicks--;
            }
            if (++this.age >= GROW_TICKS && this.isAlive()) {
                this.growUp(server);
            }
        } else if (this.random.nextInt(24) == 0) {
            this.level().addParticle(ModParticles.GLOW_DUST.get(), this.getRandomX(0.4), this.getY() + 0.3, this.getRandomZ(0.4), 0.0, 0.01, 0.0);
        }
    }

    /** It sheds its grub skin: a young, bare-shelled Caravan of the same colour crawls out. */
    private void growUp(ServerLevel level) {
        Caravan young = ModCaravans.CARAVAN.get().create(level, EntitySpawnReason.CONVERSION);
        if (young == null) {
            return;
        }
        young.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
        young.finalizeSpawn(level, level.getCurrentDifficultyAt(this.blockPosition()), EntitySpawnReason.CONVERSION, new Caravan.Group(this.colour()));
        young.hatch(this.colour());
        if (this.hasCustomName()) {
            young.setCustomName(this.getCustomName());
        }
        if (this.isPersistenceRequired()) {
            young.setPersistenceRequired();
        }
        level.addFreshEntity(young);
        level.sendParticles(new DustParticleOptions(this.colour().rgb(), 1.2F), this.getX(), this.getY() + 0.3, this.getZ(), 16, 0.3, 0.2, 0.3, 0.0);
        level.sendParticles(ModParticles.STAR_SPARKLE.get(), this.getX(), this.getY() + 0.3, this.getZ(), 8, 0.3, 0.2, 0.3, 0.02);
        this.playSound(ModSounds.CARAVAN_LARVA_EMERGE.get(), 1.0F, 1.3F);
        this.discard();
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ModSounds.CARAVAN_LARVA_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.CARAVAN_LARVA_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.CARAVAN_LARVA_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(ModSounds.CARAVAN_LARVA_STEP.get(), 0.15F, 1.0F);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Variant", this.getVariant());
        output.putInt("Age", this.age);
        output.putInt("CalmTicks", this.calmTicks);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(VARIANT, input.getIntOr("Variant", 0));
        this.age = input.getIntOr("Age", 0);
        this.calmTicks = input.getIntOr("CalmTicks", 0);
    }

    /** A puff of shell grit in its caravan's colour. */
    @Override
    public void makePoofParticles() {
        KillBurst.pop(this, this.colour().rgb(), 0xFFFFFF, KillBurst.STAR, ModParticles.GLOW_DUST.get());
    }
}
