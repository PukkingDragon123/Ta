package com.thesift.entity;

import com.thesift.music.band.BandPlayer;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModItems;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSiftSniffer;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * E1 The Sift Sniffer: what a Sniffer becomes in the Sift - a great fluffy pink-and-white beast
 * with a little flower garden growing on its back. It grazes the Sift's grass (a full meal regrows
 * the flowers it has lost), and every few minutes it stops, sniffs along the ground and digs up
 * something rare: Torchflower seeds, Pitcher Pods, Echo Seeds, even a Sculk Bloom. It flees from
 * Swifters, but hit it and it lowers its head, paws the ground and rams you like a goat. It hums
 * and trumpets through its long nose (its band voice), and two well-fed Sniffers lay a fluffy egg.
 * Taken out of the Sift it slowly rots back into a plain, grey-mossed Zombified Sniffer (see
 * {@link SiftRot}).
 */
public class SiftSniffer extends Animal implements BandPlayer {
    public static final byte IDLE = 0;
    public static final byte GRAZING = 1;
    public static final byte SNIFFING = 2;
    public static final byte DIGGING = 3;
    public static final byte WINDUP = 4;
    public static final byte CHARGING = 5;
    /** How many flowers its back garden can hold. */
    public static final int MAX_GARDEN = 5;

    // E1 entity events (-21..-25)
    private static final byte EVENT_TRUMPET = -21;
    private static final byte EVENT_FIND = -22;
    private static final byte EVENT_LAY = -23;
    private static final byte EVENT_RAM_HIT = -24;
    private static final byte EVENT_PETALS = -25;

    private static final EntityDataAccessor<Byte> STATE = SynchedEntityData.defineId(SiftSniffer.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> GARDEN = SynchedEntityData.defineId(SiftSniffer.class, EntityDataSerializers.INT);

    public final AnimationState grazeAnimation = new AnimationState();
    public final AnimationState sniffAnimation = new AnimationState();
    public final AnimationState digAnimation = new AnimationState();
    public final AnimationState trumpetAnimation = new AnimationState();
    public final AnimationState windupAnimation = new AnimationState();
    public final AnimationState layAnimation = new AnimationState();

    private int sniffCooldown = 600;
    private int ramCooldown;
    private int angerTicks;

    public SiftSniffer(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.13)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.FOLLOW_RANGE, 20.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 2.0) {
            @Override
            public boolean canUse() {
                return SiftSniffer.this.isBaby() && super.canUse();
            }
        });
        this.goalSelector.addGoal(2, new RamGoal());
        this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Swifter.class, 14.0F, 1.6, 2.2,
                e -> SiftSniffer.this.getTarget() == null && !SiftRot.isRotten(SiftSniffer.this)));
        this.goalSelector.addGoal(4, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(5, new TemptGoal(this, 1.25, this::isFood, false));
        this.goalSelector.addGoal(6, new FollowParentGoal(this, 1.25));
        this.goalSelector.addGoal(7, new SniffGoal());
        this.goalSelector.addGoal(8, new GrazeGoal());
        this.goalSelector.addGoal(9, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(11, new RandomLookAroundGoal(this));
        // neutral: only whoever hits it (or its herd) gets rammed
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, IDLE);
        builder.define(GARDEN, MAX_GARDEN);
    }

    public byte getState() {
        return this.entityData.get(STATE);
    }

    private void setState(byte state) {
        this.entityData.set(STATE, state);
    }

    /** Flowers on its back (0 - {@link #MAX_GARDEN}). */
    public int getGarden() {
        return this.entityData.get(GARDEN);
    }

    public void setGarden(int n) {
        this.entityData.set(GARDEN, Mth.clamp(n, 0, MAX_GARDEN));
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (STATE.equals(accessor)) {
            byte s = this.getState();
            this.grazeAnimation.animateWhen(s == GRAZING, this.tickCount);
            this.sniffAnimation.animateWhen(s == SNIFFING, this.tickCount);
            this.digAnimation.animateWhen(s == DIGGING, this.tickCount);
            this.windupAnimation.animateWhen(s == WINDUP, this.tickCount);
        }
    }

    @Override
    public int getNoActionTime() {
        return 0;
    }

    // ------------------------------------------------------------------ food & breeding

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.TORCHFLOWER_SEEDS);
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ModSiftSniffer.SIFT_SNIFFER.get().create(level, EntitySpawnReason.BREEDING);
    }

    /** Like their Overworld cousins they lay an egg - a fluffy one - instead of having a live baby. */
    @Override
    public void spawnChildFromBreeding(ServerLevel level, Animal partner) {
        this.finalizeSpawnChildFromBreeding(level, partner, null);
        BlockState egg = ModBlocks.SIFT_SNIFFER_EGG.get().defaultBlockState();
        BlockPos at = null;
        for (BlockPos p : List.of(this.blockPosition(), partner.blockPosition(), this.blockPosition().relative(this.getDirection().getOpposite(), 2))) {
            if (level.getBlockState(p).canBeReplaced() && egg.canSurvive(level, p)) {
                at = p;
                break;
            }
        }
        if (at != null) {
            level.setBlock(at, egg, Block.UPDATE_ALL);
        } else {
            this.spawnAtLocation(level, new ItemStack(ModItems.SIFT_SNIFFER_EGG.get()));
        }
        level.broadcastEntityEvent(this, EVENT_LAY);
        this.playSound(ModSiftSniffer.EGG_PLOP.get(), 1.0F, 1.0F);
        this.playSound(ModSiftSniffer.HAPPY.get(), 1.0F, 1.0F);
    }

    // ------------------------------------------------------------------ life

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.sniffCooldown > 0) {
            this.sniffCooldown--;
        }
        if (this.ramCooldown > 0) {
            this.ramCooldown--;
        }
        // neutral: the grudge fades after half a minute, or once the culprit is far away
        LivingEntity target = this.getTarget();
        if (target != null) {
            if (++this.angerTicks > 600 || !target.isAlive() || this.distanceToSqr(target) > 32 * 32 || SiftRot.isRotten(this)) {
                this.setTarget(null);
                this.angerTicks = 0;
            }
        } else {
            this.angerTicks = 0;
        }
        // now and then, when all is calm, a happy toot through the nose
        if (this.getState() == IDLE && target == null && !SiftRot.isRotten(this) && this.random.nextInt(1600) == 0) {
            this.trumpet(1.0F);
        }
    }

    /** A blast on the nose-trumpet (the band calls this for every note too). */
    public void trumpet(float volume) {
        this.level().broadcastEntityEvent(this, EVENT_TRUMPET);
        this.playSound(ModSiftSniffer.TRUMPET.get(), volume, (this.isBaby() ? 1.5F : 1.0F) + (this.random.nextFloat() - 0.5F) * 0.1F);
    }

    @Override
    public void playedBandNote(int pitch, float loudness) {
        this.level().broadcastEntityEvent(this, EVENT_TRUMPET);
    }

    public boolean isAngry() {
        return this.getTarget() != null;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && this.getGarden() > 0 && this.random.nextInt(3) == 0) {
            // a hard knock shakes a flower off its back
            this.setGarden(this.getGarden() - 1);
            level.broadcastEntityEvent(this, EVENT_PETALS);
        }
        return hurt;
    }

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EVENT_TRUMPET -> {
                this.trumpetAnimation.start(this.tickCount);
                Vec3 nose = this.nose();
                this.level().addParticle(ParticleTypes.NOTE, nose.x, nose.y + 0.3, nose.z, this.random.nextInt(25) / 24.0, 0.0, 0.0);
            }
            case EVENT_FIND -> {
                Vec3 nose = this.nose();
                for (int i = 0; i < 10; i++) {
                    this.level().addParticle(ParticleTypes.HAPPY_VILLAGER, nose.x + (this.random.nextDouble() - 0.5), nose.y,
                            nose.z + (this.random.nextDouble() - 0.5), 0.0, 0.05, 0.0);
                }
            }
            case EVENT_LAY -> {
                this.layAnimation.start(this.tickCount);
                for (int i = 0; i < 14; i++) {
                    this.level().addParticle(ParticleTypes.HEART, this.getRandomX(1.0), this.getY() + 1.0 + this.random.nextDouble(),
                            this.getRandomZ(1.0), 0.0, 0.05, 0.0);
                }
            }
            case EVENT_RAM_HIT -> {
                for (int i = 0; i < 16; i++) {
                    this.level().addParticle(ParticleTypes.POOF, this.nose().x, this.getY() + 0.6, this.nose().z,
                            (this.random.nextDouble() - 0.5) * 0.3, 0.1, (this.random.nextDouble() - 0.5) * 0.3);
                }
            }
            case EVENT_PETALS -> this.shedPetals(12);
            default -> super.handleEntityEvent(id);
        }
    }

    /** Client: petals and pollen shaken off the back garden. */
    private void shedPetals(int n) {
        double top = this.getY() + this.getBbHeight() * 0.95;
        for (int i = 0; i < n; i++) {
            this.level().addParticle(i % 3 == 0 ? ModParticles.DREAM_POLLEN.get() : ParticleTypes.CHERRY_LEAVES, this.getRandomX(0.8),
                    top + this.random.nextDouble() * 0.3, this.getRandomZ(0.8), (this.random.nextDouble() - 0.5) * 0.2, 0.1,
                    (this.random.nextDouble() - 0.5) * 0.2);
        }
    }

    /** Where the tip of its nose is. */
    public Vec3 nose() {
        float s = this.getAgeScale();
        Vec3 look = Vec3.directionFromRotation(0.0F, this.yBodyRot);
        return this.position().add(look.scale(1.25 * s)).add(0.0, 0.55 * s, 0.0);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) {
            if (this.getState() == DIGGING && this.tickCount % 3 == 0) {
                Vec3 nose = this.nose();
                BlockState ground = this.level().getBlockState(BlockPos.containing(nose.x, this.getY() - 0.5, nose.z));
                if (!ground.isAir()) {
                    for (int i = 0; i < 3; i++) {
                        this.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, ground), nose.x + (this.random.nextDouble() - 0.5) * 0.6,
                                this.getY() + 0.1, nose.z + (this.random.nextDouble() - 0.5) * 0.6, 0.0, 0.2, 0.0);
                    }
                }
            } else if (this.getState() == SNIFFING && this.tickCount % 8 == 0) {
                Vec3 nose = this.nose();
                this.level().addParticle(ParticleTypes.CLOUD, nose.x, nose.y - 0.3, nose.z, 0.0, 0.01, 0.0);
            }
        }
    }

    // ------------------------------------------------------------------ grazing

    private static boolean isGrass(BlockState state) {
        return state.getBlock() instanceof com.thesift.block.SiftGrassBlock || state.is(Blocks.GRASS_BLOCK);
    }

    private static boolean isTuft(BlockState state) {
        return state.is(ModBlocks.BLUSHGRASS.get()) || state.is(Blocks.SHORT_GRASS);
    }

    /** Grazes like a sheep: head down, a good chew - a tuft of blushgrass or the turf itself - and the flowers on its back grow back. */
    private final class GrazeGoal extends Goal {
        private int t;

        GrazeGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            SiftSniffer s = SiftSniffer.this;
            if (SiftRot.isRotten(s) || s.getTarget() != null || s.random.nextInt(s.isBaby() ? 50 : (s.getGarden() < MAX_GARDEN ? 300 : 1000)) != 0) {
                return false;
            }
            BlockPos at = s.blockPosition();
            return isTuft(s.level().getBlockState(at)) || isGrass(s.level().getBlockState(at.below()));
        }

        @Override
        public void start() {
            this.t = 0;
            SiftSniffer.this.getNavigation().stop();
            SiftSniffer.this.setState(GRAZING);
        }

        @Override
        public boolean canContinueToUse() {
            return this.t < 50;
        }

        @Override
        public void stop() {
            SiftSniffer.this.setState(IDLE);
        }

        @Override
        public void tick() {
            SiftSniffer s = SiftSniffer.this;
            if (++this.t % 12 == 6) {
                s.playSound(ModSiftSniffer.EAT.get(), 0.8F, 0.9F + s.random.nextFloat() * 0.2F);
            }
            if (this.t != 40 || !(s.level() instanceof ServerLevel level)) {
                return;
            }
            BlockPos at = s.blockPosition();
            boolean griefing = Boolean.TRUE.equals(level.getGameRules().get(GameRules.MOB_GRIEFING));
            BlockState tuft = level.getBlockState(at);
            BlockState below = level.getBlockState(at.below());
            if (isTuft(tuft)) {
                if (griefing) {
                    level.destroyBlock(at, false);
                }
            } else if (isGrass(below)) {
                level.levelEvent(2001, at.below(), Block.getId(below));
                if (griefing) {
                    level.setBlock(at.below(), (below.is(Blocks.GRASS_BLOCK) ? Blocks.DIRT : ModBlocks.SIFT_SOIL.get()).defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            } else {
                return;
            }
            s.setGarden(s.getGarden() + 2);
            if (s.isBaby()) {
                s.ageUp(60);
            }
            level.sendParticles(ModParticles.DREAM_POLLEN.get(), s.getX(), s.getY() + s.getBbHeight(), s.getZ(), 8, 0.5, 0.2, 0.5, 0.01);
        }
    }

    // ------------------------------------------------------------------ sniffing out rare seeds

    /** Every few minutes: nose to the ground, a long sniff, then it digs and something rare pops out. */
    private final class SniffGoal extends Goal {
        private static final int SNIFF = 50;
        private static final int DIG = 70;
        private int t;

        SniffGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            SiftSniffer s = SiftSniffer.this;
            return s.sniffCooldown <= 0 && !s.isBaby() && s.onGround() && s.getTarget() == null && !s.isInLove() && !SiftRot.isRotten(s)
                    && s.random.nextInt(40) == 0 && diggable(s.level().getBlockState(s.blockPosition().below()));
        }

        @Override
        public boolean canContinueToUse() {
            return this.t < SNIFF + DIG && SiftSniffer.this.getTarget() == null;
        }

        @Override
        public void start() {
            this.t = 0;
            SiftSniffer.this.getNavigation().stop();
            SiftSniffer.this.setState(SNIFFING);
            SiftSniffer.this.playSound(ModSiftSniffer.SNIFF.get(), 1.0F, 1.0F);
        }

        @Override
        public void stop() {
            SiftSniffer s = SiftSniffer.this;
            s.setState(IDLE);
            s.sniffCooldown = this.t >= SNIFF + DIG ? 2400 + s.random.nextInt(2400) : 400;
        }

        @Override
        public void tick() {
            SiftSniffer s = SiftSniffer.this;
            this.t++;
            if (this.t == SNIFF) {
                s.setState(DIGGING);
                s.playSound(ModSiftSniffer.DIG.get(), 1.0F, 1.0F);
            }
            if (this.t == SNIFF + DIG && s.level() instanceof ServerLevel level) {
                s.dig(level);
            }
        }
    }

    private static boolean diggable(BlockState state) {
        return state.is(com.thesift.registry.ModTags.Blocks.SIFT_PLANTABLE) || state.is(net.minecraft.tags.BlockTags.DIRT)
                || state.is(net.minecraft.tags.BlockTags.SAND);
    }

    /** Server: the find - a rare seed (or a Pitcher Pod) pops out of the hole in front of its nose. */
    public void dig(ServerLevel level) {
        Vec3 nose = this.nose();
        this.dropFromGiftLootTable(level, ModSiftSniffer.DIGGING_LOOT, (l, stack) -> {
            net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(level, nose.x, this.getY() + 0.3, nose.z, stack);
            drop.setDefaultPickUpDelay();
            drop.setDeltaMovement((this.random.nextDouble() - 0.5) * 0.1, 0.3, (this.random.nextDouble() - 0.5) * 0.1);
            level.addFreshEntity(drop);
        });
        level.broadcastEntityEvent(this, EVENT_FIND);
        this.playSound(ModSiftSniffer.FIND.get(), 1.0F, 1.0F);
        this.trumpet(1.2F);
        if (this.getGarden() > 0) {
            this.setGarden(this.getGarden() - 1);
            level.broadcastEntityEvent(this, EVENT_PETALS);
        }
    }

    // ------------------------------------------------------------------ the ram

    /** Goat-style: head down, paw the ground, then a straight charge that sends its target flying. */
    private final class RamGoal extends Goal {
        private static final int WINDUP_TICKS = 24;
        private int t;
        private Vec3 dir = Vec3.ZERO;

        RamGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            SiftSniffer s = SiftSniffer.this;
            LivingEntity target = s.getTarget();
            return target != null && target.isAlive() && s.ramCooldown <= 0 && !s.isBaby() && !SiftRot.isRotten(s)
                    && s.distanceToSqr(target) < 18 * 18 && s.hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = SiftSniffer.this.getTarget();
            return target != null && target.isAlive() && this.t < WINDUP_TICKS + 34;
        }

        @Override
        public void start() {
            this.t = 0;
            SiftSniffer s = SiftSniffer.this;
            s.getNavigation().stop();
            s.setState(WINDUP);
            s.playSound(ModSiftSniffer.PREPARE_RAM.get(), 1.0F, 1.0F);
        }

        @Override
        public void stop() {
            SiftSniffer s = SiftSniffer.this;
            s.setState(IDLE);
            s.ramCooldown = 50 + s.random.nextInt(40);
        }

        @Override
        public void tick() {
            SiftSniffer s = SiftSniffer.this;
            LivingEntity target = s.getTarget();
            if (target == null) {
                return;
            }
            this.t++;
            if (this.t < WINDUP_TICKS) {
                s.getLookControl().setLookAt(target, 30.0F, 30.0F);
                Vec3 to = target.position().subtract(s.position());
                s.setYRot((float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90.0F);
                s.yBodyRot = s.getYRot();
                return;
            }
            if (this.t == WINDUP_TICKS) {
                Vec3 to = target.position().subtract(s.position());
                this.dir = new Vec3(to.x, 0.0, to.z).normalize();
                s.setState(CHARGING);
                s.trumpet(1.3F);
            }
            s.setDeltaMovement(this.dir.x * 0.5, s.getDeltaMovement().y, this.dir.z * 0.5);
            s.setYRot((float) (Mth.atan2(this.dir.z, this.dir.x) * Mth.RAD_TO_DEG) - 90.0F);
            s.yBodyRot = s.getYRot();
            if (!(s.level() instanceof ServerLevel level)) {
                return;
            }
            if (s.getBoundingBox().inflate(0.4).intersects(target.getBoundingBox())) {
                if (s.doHurtTarget(level, target)) {
                    target.push(this.dir.x * 1.6, 0.5, this.dir.z * 1.6); // sent flying, goat-style
                }
                s.playSound(ModSiftSniffer.RAM.get(), 1.2F, 1.0F);
                level.broadcastEntityEvent(s, EVENT_RAM_HIT);
                this.t = WINDUP_TICKS + 34;
            } else if (s.horizontalCollision && this.t > WINDUP_TICKS + 4) {
                // bonk: it ran into a wall
                s.playSound(ModSiftSniffer.RAM.get(), 0.8F, 0.8F);
                level.broadcastEntityEvent(s, EVENT_RAM_HIT);
                this.t = WINDUP_TICKS + 34;
            }
        }
    }

    // ------------------------------------------------------------------ sounds

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SiftRot.isRotten(this) ? null : ModSiftSniffer.AMBIENT.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 220;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSiftSniffer.HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSiftSniffer.DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(ModSiftSniffer.STEP.get(), 0.35F, this.isBaby() ? 1.4F : 1.0F);
    }

    @Override
    public float getVoicePitch() {
        return this.isBaby() ? 1.4F + this.random.nextFloat() * 0.2F : 0.95F + (this.random.nextFloat() - this.random.nextFloat()) * 0.1F;
    }

    // ------------------------------------------------------------------ save

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Garden", this.getGarden());
        output.putInt("SniffCooldown", this.sniffCooldown);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setGarden(input.getIntOr("Garden", MAX_GARDEN));
        this.sniffCooldown = input.getIntOr("SniffCooldown", 600);
    }

    /** Petals and fluff. */
    @Override
    public void makePoofParticles() {
        KillBurst.pop(this, 0xF6B8D2, 0xFFFFFF, KillBurst.HEART, ModParticles.FOOTSTEP_PUFF.get());
    }

    /** For tests and the Codex: start digging right away. */
    public void sniffNow() {
        this.sniffCooldown = 0;
    }

    /** Used by {@link SiftRot} when it turns into a vanilla Sniffer: its flowers wilt away. */
    public static boolean isSiftSniffer(Mob mob) {
        return mob instanceof SiftSniffer;
    }
}
