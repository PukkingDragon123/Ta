package com.thesift.entity.dunes;

import com.thesift.entity.KillBurst;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModDunes;
import com.thesift.registry.ModItems;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Grub: a Nib-like gem creature crusted in a shell of sand and dune rock. Shelled, it skitters out of the dunes
 * and bites, and gathers round Reservoirs to drink their Chrome. Music cracks its shell: every note played (or sung)
 * nearby adds a crack (the shell's pieces fall away one by one - the model hides them by {@link #crackStage}),
 * and at {@link #CRACK_MAX} the shell bursts, freeing a shining gem creature - amethyst, emerald, diamond or prism -
 * that is calm, flutters its crystal wings, and drops its gem.
 */
public class Grub extends Monster implements DunesNative {
    public static final double HEARING = 10.0;
    public static final int CRACK_MAX = 14;
    public static final int GEMS = 4;
    private static final EntityDataAccessor<Integer> GEM = SynchedEntityData.defineId(Grub.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CRACKS = SynchedEntityData.defineId(Grub.class, EntityDataSerializers.INT);
    private static final byte EVENT_CREAK = 101;
    private static final byte EVENT_BURST = 102;

    public final AnimationState creakAnimation = new AnimationState();
    public final AnimationState burstAnimation = new AnimationState();
    public final AnimationState drinkAnimation = new AnimationState();
    private int lastNote;
    private int healIn;

    public Grub(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(GEM, 0);
        builder.define(CRACKS, 0);
    }

    @Override
    public int getNoActionTime() {
        return 0;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.3F) {
            @Override
            public boolean canUse() {
                return !Grub.this.isCracked() && super.canUse();
            }
        });
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15, false) {
            @Override
            public boolean canUse() {
                return !Grub.this.isCracked() && super.canUse();
            }
        });
        this.goalSelector.addGoal(4, new DrinkGoal());
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, (target, level) -> !this.isCracked()));
    }

    // ------------------------------------------------------------------ state

    /** 0 amethyst, 1 emerald, 2 diamond, 3 prism. */
    public int getGem() {
        return Mth.clamp(this.entityData.get(GEM), 0, GEMS - 1);
    }

    public int cracks() {
        return this.entityData.get(CRACKS);
    }

    public boolean isCracked() {
        return this.cracks() >= CRACK_MAX;
    }

    /** How much of the shell is left: 0 whole, 1 the loose rock gone, 2 the head cap too, 3 the sides too, 4 free. */
    public int crackStage() {
        return Math.min(4, this.cracks() * 4 / CRACK_MAX);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
            @Nullable SpawnGroupData data) {
        int r = this.random.nextInt(20);
        this.entityData.set(GEM, r < 9 ? 0 : r < 15 ? 1 : r < 17 ? 2 : 3);
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return ModDunes.dunesGround(level.getBlockState(pos.below())) ? 10.0F : 0.0F;
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return !this.isCracked() && super.removeWhenFarAway(distSqr);
    }

    // ------------------------------------------------------------------ music cracks it

    /** A note was played (or sung) nearby: another crack. */
    public void hearNote(ServerLevel level, Vec3 at) {
        if (this.isCracked() || this.tickCount - this.lastNote < 3) {
            return;
        }
        this.lastNote = this.tickCount;
        int before = this.crackStage();
        this.entityData.set(CRACKS, this.cracks() + 1);
        if (this.isCracked()) {
            this.breakFree(level);
            return;
        }
        level.broadcastEntityEvent(this, EVENT_CREAK);
        this.playSound(ModDunes.GRUB_CREAK.get(), 0.8F, 0.9F + this.cracks() * 0.05F);
        if (this.crackStage() != before) {
            this.shellParticles(level, 10);
        }
    }

    private void shellParticles(ServerLevel level, int n) {
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ModBlocks.CHIME_SAND.get().defaultBlockState()), this.getX(), this.getY() + 0.35,
                this.getZ(), n, 0.25, 0.15, 0.25, 0.1);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ModBlocks.DUNESTONE.get().defaultBlockState()), this.getX(), this.getY() + 0.35,
                this.getZ(), n, 0.25, 0.15, 0.25, 0.1);
    }

    private void breakFree(ServerLevel level) {
        this.playSound(ModDunes.GRUB_CRACK.get(), 1.3F, 1.0F);
        level.broadcastEntityEvent(this, EVENT_BURST);
        this.shellParticles(level, 30);
        level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 0.4, this.getZ(), 14, 0.3, 0.3, 0.3, 0.06);
        this.setTarget(null);
        this.setPersistenceRequired();
        this.heal(this.getMaxHealth());
    }

    // ------------------------------------------------------------------ the server tick

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        // cracks close again slowly in the silence
        if (!this.isCracked() && this.cracks() > 0 && this.tickCount - this.lastNote > 200 && this.tickCount % 100 == 0) {
            this.entityData.set(CRACKS, this.cracks() - 1);
        }
        if (this.isCracked() && this.tickCount % 80 == 0 && this.random.nextInt(3) == 0) {
            this.playSound(ModDunes.GRUB_SHINE.get(), 0.6F, 1.2F + this.random.nextFloat() * 0.4F);
        }
        if (this.healIn > 0) {
            this.healIn--;
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, net.minecraft.world.entity.Entity target) {
        return !this.isCracked() && super.doHurtTarget(level, target);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        if (this.isCracked()) {
            Item gem = switch (this.getGem()) {
                case 1 -> Items.EMERALD;
                case 2 -> Items.DIAMOND;
                case 3 -> ModItems.PRISM_GEM.get();
                default -> Items.AMETHYST_SHARD;
            };
            int n = this.getGem() == 0 ? 2 + this.random.nextInt(3) : this.getGem() == 2 ? 1 : 1 + this.random.nextInt(2);
            this.spawnAtLocation(level, new ItemStack(gem, n));
        }
    }

    /** Shelled and thirsty, it goes to the nearest Reservoir with Chrome in it and drinks. */
    private class DrinkGoal extends Goal {
        private @Nullable Reservoir well;
        private int ticks;

        DrinkGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Grub g = Grub.this;
            if (g.getTarget() != null || g.random.nextInt(g.getHealth() < g.getMaxHealth() ? 20 : 200) != 0) {
                return false;
            }
            List<Reservoir> rs = g.level().getEntitiesOfClass(Reservoir.class, g.getBoundingBox().inflate(14.0), r -> r.isAlive() && r.chrome() > 4);
            this.well = rs.isEmpty() ? null : rs.get(0);
            return this.well != null;
        }

        @Override
        public boolean canContinueToUse() {
            Reservoir r = this.well;
            return r != null && r.isAlive() && r.chrome() > 0 && this.ticks < 400 && Grub.this.getTarget() == null;
        }

        @Override
        public void start() {
            this.ticks = 0;
        }

        @Override
        public void stop() {
            this.well = null;
            Grub.this.getNavigation().stop();
        }

        @Override
        public void tick() {
            Grub g = Grub.this;
            Reservoir r = this.well;
            if (r == null) {
                return;
            }
            this.ticks++;
            g.getLookControl().setLookAt(r, 30.0F, 30.0F);
            double reach = r.getBbWidth() * 0.5 + 0.8;
            if (g.distanceToSqr(r) > reach * reach) {
                if (this.ticks % 10 == 1) {
                    g.getNavigation().moveTo(r, 1.0);
                }
                return;
            }
            g.getNavigation().stop();
            if (this.ticks % 25 == 0 && g.level() instanceof ServerLevel server) {
                int got = r.drain(3);
                if (got > 0) {
                    g.heal(got);
                    g.playSound(ModDunes.GRUB_DRINK.get(), 0.6F, 1.2F);
                    server.broadcastEntityEvent(g, (byte) 103);
                    server.sendParticles(com.thesift.registry.ModParticles.CHROME_DROPLET.get(), g.getX(), g.getY() + 0.4, g.getZ(), 4, 0.15, 0.1, 0.15, 0.02);
                }
            }
        }
    }

    // ------------------------------------------------------------------ client

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EVENT_CREAK -> this.creakAnimation.start(this.tickCount);
            case EVENT_BURST -> this.burstAnimation.start(this.tickCount);
            case 103 -> this.drinkAnimation.start(this.tickCount);
            default -> super.handleEntityEvent(id);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() && this.isCracked() && this.random.nextInt(10) == 0) {
            this.level().addParticle(com.thesift.registry.ModParticles.STAR_SPARKLE.get(), this.getRandomX(0.5), this.getY() + 0.2 + this.random.nextDouble() * 0.4,
                    this.getRandomZ(0.5), 0.0, 0.01, 0.0);
        }
    }

    /** The Codex page: the shell cracks piece by piece and bursts, then it starts over (its colour changes each time). */
    public void codexPose(int t) {
        int c = (t / 12) % (CRACK_MAX + 10);
        this.entityData.set(CRACKS, Math.min(CRACK_MAX, c));
        if (c == CRACK_MAX) {
            this.burstAnimation.start(this.tickCount);
        }
        if (c == 0 && t % 12 == 0) {
            this.entityData.set(GEM, (this.getGem() + 1) % GEMS);
        }
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.isCracked() ? null : ModDunes.GRUB_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModDunes.GRUB_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModDunes.GRUB_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(ModDunes.GRUB_STEP.get(), 0.3F, 1.0F);
    }

    @Override
    public void makePoofParticles() {
        int[] cols = {0x9468CC, 0x36A860, 0x4CB6BE, 0xDA6C98};
        KillBurst.pop(this, cols[this.getGem()], 0xDAD2E9, KillBurst.STAR, ParticleTypes.END_ROD);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Gem", this.getGem());
        output.putInt("Cracks", this.cracks());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(GEM, input.getIntOr("Gem", 0));
        this.entityData.set(CRACKS, input.getIntOr("Cracks", 0));
    }
}
