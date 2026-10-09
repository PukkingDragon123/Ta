package com.thesift.entity;

import com.thesift.entity.slumbler.ChromeSpit;
import com.thesift.entity.slumbler.SlumblerEggs;
import com.thesift.music.MusicListener;
import com.thesift.registry.ModFluids;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSlumbler;
import com.thesift.registry.ModSounds;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Slumbler: a huge, wide-mouthed Chrome salamander whose back is a carved wooden instrument, in
 * rainbow scales. Sleepy and peaceful unless you hit it - then it lumbers after you with a crushing
 * bite and spits gobs of Chrome that leave you dizzy (Rainbow Daze). It glides through Chrome, yawns
 * enormous yawns, gulps Chrome plankton from the shallows, nuzzles other Slumblers, hums along to
 * music, shakes itself dry when it climbs out - and it sleeps a great deal, napping half-submerged in
 * shallow Chrome.
 * <p>
 * CR2: feed two Slumblers fish and they lay a clutch of eggs ({@link SlumblerEggs}) that hatch into
 * stingray-like tadpoles. Drops Thick Hide, its gills and, sometimes, a Chrome Pearl.
 */
public class Slumbler extends PathfinderMob implements MusicListener, Resting {
    private static final EntityDataAccessor<Boolean> SLEEPING = SynchedEntityData.defineId(Slumbler.class, EntityDataSerializers.BOOLEAN);
    private static final byte EVENT_YAWN = 110;
    /** Ids 60-67 are vanilla's. */
    private static final byte EVENT_BITE = 111;
    private static final byte EVENT_GULP = 113;
    private static final byte EVENT_NUZZLE = 114;
    private static final byte EVENT_HUM = 115;
    // CR2: the Chrome spit, the wet shake and laying eggs
    private static final byte EVENT_SPIT = 116;
    private static final byte EVENT_SHAKE = 117;
    private static final byte EVENT_LAY = 118;
    /** The gob of Chrome leaves its mouth this many ticks into the spit. */
    private static final int SPIT_AT = 12;
    private static final int SPIT_END = 22;
    /** How long a fed Slumbler stays in the mood, and how long before it can lay again. */
    private static final int LOVE_TICKS = 600;
    private static final int BREED_COOLDOWN = 6000;

    public final AnimationState yawnAnimation = new AnimationState();
    public final AnimationState biteAnimation = new AnimationState();
    public final AnimationState sleepAnimation = new AnimationState();
    public final AnimationState gulpAnimation = new AnimationState();
    public final AnimationState nuzzleAnimation = new AnimationState();
    public final AnimationState humAnimation = new AnimationState();
    public final AnimationState spitAnimation = new AnimationState();
    public final AnimationState shakeAnimation = new AnimationState();
    public final AnimationState layAnimation = new AnimationState();
    private int humCooldown;
    private int socialCooldown = 400;
    private int yawnCooldown = 200;
    private int sleepTimer;
    private int wakeTimer = 400;
    private int spitCooldown;
    private int inLove;
    private int breedCooldown;
    /** How long it has been in the Chrome (up to 200), and the shake that follows it out. */
    private int wetTicks;
    private int dryTicks;
    private int shakeTicks;

    public Slumbler(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.17)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.FOLLOW_RANGE, 20.0)
                .add(Attributes.TEMPT_RANGE, 10.0); // its TemptGoal (fish) reads this; monsters don't get it by default
    }

    /**
     * S1 never freeze: vanilla stops a mob's random strolls once it has been 100 ticks out of
     * a player's 32-block reach, so Sift creatures seen across a valley stood frozen. The field
     * itself (which drives despawning) is left alone.
     */
    @Override
    public int getNoActionTime() {
        return 0;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SleepGoal());
        this.goalSelector.addGoal(2, new SpitGoal());
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.25, true));
        this.goalSelector.addGoal(3, new BreedGoal());
        this.goalSelector.addGoal(4, new TemptGoal(this, 0.8, stack -> stack.is(ItemTags.FISHES), false));
        this.goalSelector.addGoal(4, new ForageGoal());
        this.goalSelector.addGoal(4, new NuzzleGoal());
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7, 0.002F));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SLEEPING, false);
    }

    public boolean isSlumbering() {
        return this.entityData.get(SLEEPING);
    }

    /** A napping Slumbler lies still on purpose (see {@link Resting}). */
    @Override
    public boolean isResting() {
        return this.isSlumbering();
    }

    private void setSlumbering(boolean sleeping) {
        this.entityData.set(SLEEPING, sleeping);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (SLEEPING.equals(accessor)) {
            this.sleepAnimation.animateWhen(this.isSlumbering(), this.tickCount);
        }
    }

    private void wake(int ticks) {
        if (this.isSlumbering()) {
            this.setSlumbering(false);
        }
        this.wakeTimer = Math.max(this.wakeTimer, ticks);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (this.isSlumbering()) {
            this.wake(600);
        }
        this.inLove = 0;
        return super.hurtServer(level, source, damage);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        this.level().broadcastEntityEvent(this, EVENT_BITE);
        this.playSound(ModSounds.SLUMBLER_BITE.get(), 1.0F, 0.9F);
        return super.doHurtTarget(level, target);
    }

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EVENT_YAWN -> this.yawnAnimation.start(this.tickCount);
            case EVENT_BITE -> this.biteAnimation.start(this.tickCount);
            case EVENT_GULP -> this.gulpAnimation.start(this.tickCount);
            case EVENT_NUZZLE -> this.nuzzleAnimation.start(this.tickCount);
            case EVENT_HUM -> this.humAnimation.start(this.tickCount);
            case EVENT_SPIT -> this.spitAnimation.start(this.tickCount);
            case EVENT_SHAKE -> this.shakeAnimation.start(this.tickCount);
            case EVENT_LAY -> this.layAnimation.start(this.tickCount);
            default -> super.handleEntityEvent(id);
        }
    }

    // ------------------------------------------------------------------ feeding and eggs

    /** CR2: fish wins a Slumbler over - feed two of them and they lay eggs (it heals one that cannot). */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(ItemTags.FISHES) || this.getTarget() != null) {
            return super.mobInteract(player, hand);
        }
        boolean love = this.breedCooldown <= 0 && this.inLove <= 0;
        if (!love && this.getHealth() >= this.getMaxHealth()) {
            return InteractionResult.PASS;
        }
        if (this.level() instanceof ServerLevel server) {
            this.wake(400);
            stack.consume(1, player);
            server.broadcastEntityEvent(this, EVENT_GULP);
            this.playSound(ModSounds.SLUMBLER_EAT.get(), 1.0F, 0.9F + this.random.nextFloat() * 0.2F);
            this.heal(6.0F);
            if (love) {
                this.inLove = LOVE_TICKS;
                server.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + this.getBbHeight() + 0.2, this.getZ(), 5, 0.6, 0.3, 0.6, 0.0);
            }
        }
        return InteractionResult.SUCCESS;
    }

    public boolean isInLove() {
        return this.inLove > 0;
    }

    /** Lays a clutch of eggs - in the Chrome if there is shallow Chrome near, else where it stands. */
    private void layEggs(ServerLevel level, Slumbler partner) {
        BlockPos spot = this.findShallowChrome();
        Vec3 at = spot != null && spot.distSqr(this.blockPosition()) < 64.0 ? Vec3.atCenterOf(spot)
                : this.position().add(Vec3.directionFromRotation(0.0F, this.yBodyRot).scale(-1.6));
        SlumblerEggs eggs = ModSlumbler.SLUMBLER_EGGS.get().create(level, EntitySpawnReason.BREEDING);
        if (eggs != null) {
            eggs.snapTo(at.x, at.y, at.z, this.random.nextFloat() * 360.0F, 0.0F);
            eggs.laid();
            level.addFreshEntity(eggs);
        }
        level.broadcastEntityEvent(this, EVENT_LAY);
        this.playSound(ModSounds.SLUMBLER_LAY.get(), 1.2F, 0.9F + this.random.nextFloat() * 0.2F);
        level.sendParticles(ParticleTypes.HEART, (this.getX() + partner.getX()) * 0.5, this.getY() + 1.4, (this.getZ() + partner.getZ()) * 0.5, 6,
                0.8, 0.3, 0.8, 0.0);
        level.sendParticles(ModParticles.CHROME_DROPLET.get(), at.x, at.y + 0.3, at.z, 12, 0.4, 0.2, 0.4, 0.05);
        ExperienceOrb.award(level, this.position(), 1 + this.random.nextInt(7));
        for (Slumbler s : new Slumbler[]{this, partner}) {
            s.inLove = 0;
            s.breedCooldown = BREED_COOLDOWN;
        }
    }

    // ------------------------------------------------------------------ the Chrome spit

    private Vec3 mouth() {
        return this.position().add(Vec3.directionFromRotation(0.0F, this.yBodyRot).scale(1.6)).add(0.0, 0.7, 0.0);
    }

    /** A gob of Chrome lobbed at the target: it splashes, hurts a little and leaves it dizzy. */
    private void spitAt(ServerLevel level, LivingEntity target) {
        Vec3 from = this.mouth();
        ChromeSpit spit = new ChromeSpit(level, this);
        spit.setPos(from.x, from.y, from.z);
        double dx = target.getX() - from.x;
        double dz = target.getZ() - from.z;
        double dy = target.getY(0.6) - from.y;
        double flat = Math.sqrt(dx * dx + dz * dz);
        spit.shoot(dx, dy + flat * 0.2, dz, 1.0F, 4.0F);
        level.addFreshEntity(spit);
        this.playSound(ModSounds.SLUMBLER_SPIT.get(), 1.2F, 0.9F + this.random.nextFloat() * 0.2F);
        level.sendParticles(ModParticles.CHROME_DROPLET.get(), from.x, from.y, from.z, 8, 0.2, 0.1, 0.2, 0.06);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level() instanceof ServerLevel server) {
            if (this.wakeTimer > 0) {
                this.wakeTimer--;
            }
            // S1 never freeze: every nap ends, even one music started while the SleepGoal cannot run (afloat in water)
            if (this.isSlumbering() && --this.sleepTimer <= 0) {
                this.setSlumbering(false);
                this.wakeTimer = 300 + this.random.nextInt(500);
            }
            if (this.humCooldown > 0) {
                this.humCooldown--;
            }
            if (this.socialCooldown > 0) {
                this.socialCooldown--;
            }
            if (this.spitCooldown > 0) {
                this.spitCooldown--;
            }
            if (this.breedCooldown > 0) {
                this.breedCooldown--;
            }
            if (this.inLove > 0 && --this.inLove % 20 == 0) {
                server.sendParticles(ParticleTypes.HEART, this.getRandomX(0.8), this.getY() + this.getBbHeight() + 0.2, this.getRandomZ(0.8), 1, 0.0, 0.0,
                        0.0, 0.0);
            }
            this.dryOff(server);
            // napping in the Chrome, bubbles rise from its nostrils
            if (this.isSlumbering() && this.isInFluidType() && this.tickCount % 12 == 0) {
                Vec3 nose = this.position().add(Vec3.directionFromRotation(0.0F, this.yBodyRot).scale(1.4));
                server.sendParticles(ModParticles.CHROME_BUBBLE.get(), nose.x, this.getY() + 0.6, nose.z, 2, 0.1, 0.05, 0.1, 0.01);
            }
            if (!this.isSlumbering() && this.getTarget() == null && --this.yawnCooldown <= 0) {
                this.yawnCooldown = 300 + this.random.nextInt(500);
                server.broadcastEntityEvent(this, EVENT_YAWN);
                this.playSound(ModSounds.SLUMBLER_YAWN.get(), 1.2F, 0.8F + this.random.nextFloat() * 0.2F);
            }
            if (this.isSlumbering() && this.tickCount % 40 == 0) {
                server.sendParticles(ModParticles.SIFT_MIST.get(), this.getX(), this.getY() + 0.8, this.getZ(), 1, 0.2, 0.1, 0.2, 0.0);
            }
        } else if (this.isInFluidType() && this.random.nextInt(8) == 0) {
            this.level().addParticle(ModParticles.CHROME_BUBBLE.get(), this.getRandomX(0.8), this.getY() + 0.5, this.getRandomZ(0.8), 0, 0.03, 0);
        }
    }

    /** CR2: out of the Chrome after a good soak, it stops and shakes itself dry, flinging droplets. */
    private void dryOff(ServerLevel level) {
        if (this.isInFluidType()) {
            this.wetTicks = Math.min(200, this.wetTicks + 1);
            this.dryTicks = 0;
        } else if (this.wetTicks > 40 && this.onGround() && !this.isSlumbering() && this.getTarget() == null && ++this.dryTicks >= 10) {
            this.wetTicks = 0;
            this.dryTicks = 0;
            this.shakeTicks = 24;
            level.broadcastEntityEvent(this, EVENT_SHAKE);
            this.playSound(ModSounds.SLUMBLER_SHAKE.get(), 1.0F, 0.8F + this.random.nextFloat() * 0.2F);
        }
        if (this.shakeTicks > 0) {
            this.shakeTicks--;
            if (this.shakeTicks % 4 == 0) {
                level.sendParticles(ModParticles.CHROME_DROPLET.get(), this.getX(), this.getY() + 0.8, this.getZ(), 10, 0.9, 0.3, 0.9, 0.15);
            }
            this.getNavigation().stop();
        }
    }

    /**
     * A note was played nearby (see {@link CreatureLife}): awake, it hums along in its deep voice;
     * asleep, it only smiles and murmurs.
     */
    public void hearNote(ServerLevel level, int pitch) {
        if (this.humCooldown > 0 || this.getTarget() != null) {
            return;
        }
        this.humCooldown = 30 + this.random.nextInt(30);
        level.broadcastEntityEvent(this, EVENT_HUM);
        this.playSound(ModSounds.SLUMBLER_AMBIENT.get(), this.isSlumbering() ? 0.4F : 0.9F, 0.5F + com.thesift.music.Notes.soundPitch(pitch) * 0.35F);
        level.sendParticles(ModParticles.SIFT_NOTE.get(), this.getX(), this.getY() + this.getBbHeight() + 0.3, this.getZ(), 0, pitch / 24.0, 0.0, 0.0, 1.0);
    }

    /** Shallow Chrome to wade into for a nap: Chrome with ground just under it and air above. */
    private @Nullable BlockPos findShallowChrome() {
        BlockPos here = this.blockPosition();
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(here.offset(-10, -3, -10), here.offset(10, 2, 10))) {
            FluidState fs = this.level().getFluidState(p);
            if (fs.isEmpty() || !fs.getType().isSame(ModFluids.CHROME.get()) || !this.level().getBlockState(p.above()).isAir()
                    || !this.level().getFluidState(p.below()).isEmpty()) {
                continue;
            }
            double d = p.distSqr(here);
            if (d < bestD) {
                bestD = d;
                best = p.immutable();
            }
        }
        return best;
    }

    @Override
    public void hearMusic(BlockPos source, float strength) {
        // Music is the only thing that reliably sends a Slumbler to sleep.
        if (!this.level().isClientSide() && this.getTarget() == null && !this.isSlumbering() && this.inLove <= 0
                && this.random.nextFloat() < 0.3F * strength) {
            this.setSlumbering(true);
            this.sleepTimer = 600 + this.random.nextInt(600);
        }
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.isSlumbering() ? null : ModSounds.SLUMBLER_AMBIENT.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.SLUMBLER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SLUMBLER_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(ModSounds.SLUMBLER_STEP.get(), 0.5F, 0.8F);
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Slumbering", this.isSlumbering());
        output.putInt("SleepTimer", this.sleepTimer);
        output.putInt("InLove", this.inLove);
        output.putInt("BreedCooldown", this.breedCooldown);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setSlumbering(input.getBooleanOr("Slumbering", false));
        // S1: a nap saved without its timer (older worlds) still ends
        this.sleepTimer = input.getIntOr("SleepTimer", this.isSlumbering() ? 600 + this.random.nextInt(600) : 0);
        this.inLove = input.getIntOr("InLove", 0);
        this.breedCooldown = input.getIntOr("BreedCooldown", 0);
    }

    /** Naps whenever nothing is bothering it - and a Slumbler is rarely bothered: it sleeps a great deal. */
    private class SleepGoal extends Goal {
        private @Nullable BlockPos spot;
        private int walk;

        SleepGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            Slumbler s = Slumbler.this;
            if (s.isSlumbering()) {
                return true;
            }
            if (s.getTarget() != null || s.wakeTimer > 0 || s.inLove > 0 || s.random.nextInt(s.level().isDarkOutside() ? 30 : 120) != 0) {
                return false;
            }
            this.spot = s.isInFluidType() ? null : s.findShallowChrome();
            this.walk = this.spot != null ? 200 : 0;
            if (this.spot == null && !s.onGround() && !s.isInFluidType()) {
                return false;
            }
            s.sleepTimer = 1200 + s.random.nextInt(1800);
            if (this.spot == null) {
                s.setSlumbering(true);
            }
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            Slumbler s = Slumbler.this;
            return (s.isSlumbering() || this.walk > 0) && s.getTarget() == null && s.sleepTimer > 0;
        }

        @Override
        public void start() {
            Slumbler s = Slumbler.this;
            if (this.spot != null) {
                s.getNavigation().moveTo(this.spot.getX() + 0.5, this.spot.getY(), this.spot.getZ() + 0.5, 0.8);
            } else {
                s.getNavigation().stop();
            }
        }

        @Override
        public void tick() {
            Slumbler s = Slumbler.this;
            if (!s.isSlumbering()) {
                // still wading out to its napping spot
                this.walk--;
                boolean there = this.spot == null || s.isInFluidType() || s.position().distanceToSqr(Vec3.atBottomCenterOf(this.spot)) < 2.5;
                if (there || this.walk <= 0 || s.getNavigation().isDone()) {
                    this.walk = 0;
                    s.setSlumbering(true);
                    s.getNavigation().stop();
                }
                return;
            }
            s.getNavigation().stop();
        }

        @Override
        public void stop() {
            Slumbler.this.setSlumbering(false);
            Slumbler.this.wakeTimer = 300 + Slumbler.this.random.nextInt(500);
        }
    }

    /** CR2: from a few blocks off it rears back and spits a gob of Chrome at whoever angered it. */
    private final class SpitGoal extends Goal {
        private int ticks;

        SpitGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Slumbler s = Slumbler.this;
            LivingEntity t = s.getTarget();
            if (t == null || !t.isAlive() || s.spitCooldown > 0 || s.isSlumbering()) {
                return false;
            }
            double d = s.distanceToSqr(t);
            return d > 4.0 * 4.0 && d < 16.0 * 16.0 && s.hasLineOfSight(t);
        }

        @Override
        public boolean canContinueToUse() {
            return this.ticks < SPIT_END;
        }

        @Override
        public void start() {
            this.ticks = 0;
            Slumbler.this.getNavigation().stop();
            Slumbler.this.level().broadcastEntityEvent(Slumbler.this, EVENT_SPIT);
        }

        @Override
        public void stop() {
            Slumbler.this.spitCooldown = 50 + Slumbler.this.random.nextInt(50);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Slumbler s = Slumbler.this;
            LivingEntity t = s.getTarget();
            this.ticks++;
            s.getNavigation().stop();
            if (t != null) {
                s.getLookControl().setLookAt(t, 30.0F, 30.0F);
                if (this.ticks < SPIT_AT) {
                    float yaw = (float) (Mth.atan2(t.getZ() - s.getZ(), t.getX() - s.getX()) * Mth.RAD_TO_DEG) - 90.0F;
                    float turned = Mth.approachDegrees(s.getYRot(), yaw, 15.0F);
                    s.setYRot(turned);
                    s.setYBodyRot(turned);
                }
                if (this.ticks == SPIT_AT && t.isAlive() && s.level() instanceof ServerLevel level) {
                    s.spitAt(level, t);
                }
            }
        }
    }

    /** CR2: a Slumbler in the mood finds another and they lay a clutch of eggs together. */
    private final class BreedGoal extends Goal {
        private @Nullable Slumbler partner;
        private int together;

        BreedGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        private @Nullable Slumbler findPartner() {
            Slumbler s = Slumbler.this;
            Slumbler best = null;
            double bestD = Double.MAX_VALUE;
            for (Slumbler o : s.level().getEntitiesOfClass(Slumbler.class, s.getBoundingBox().inflate(10.0),
                    other -> other != s && other.isAlive() && other.isInLove())) {
                double d = o.distanceToSqr(s);
                if (d < bestD) {
                    best = o;
                    bestD = d;
                }
            }
            return best;
        }

        @Override
        public boolean canUse() {
            Slumbler s = Slumbler.this;
            if (!s.isInLove() || s.getTarget() != null) {
                return false;
            }
            this.partner = this.findPartner();
            return this.partner != null;
        }

        @Override
        public boolean canContinueToUse() {
            Slumbler p = this.partner;
            return p != null && p.isAlive() && p.isInLove() && Slumbler.this.isInLove() && this.together < 60;
        }

        @Override
        public void start() {
            this.together = 0;
        }

        @Override
        public void stop() {
            this.partner = null;
            this.together = 0;
        }

        @Override
        public void tick() {
            Slumbler s = Slumbler.this;
            Slumbler p = this.partner;
            if (p == null) {
                return;
            }
            s.getLookControl().setLookAt(p, 10.0F, 10.0F);
            if (s.distanceToSqr(p) > 3.5 * 3.5) {
                if (s.tickCount % 10 == 0) {
                    s.getNavigation().moveTo(p, 0.8);
                }
                return;
            }
            s.getNavigation().stop();
            if (++this.together >= 60 && s.getId() < p.getId() && s.level() instanceof ServerLevel level) {
                s.layEggs(level, p);
            }
        }
    }

    /** Awake in or beside the Chrome, it dips its huge mouth in and gulps a mouthful of plankton. */
    private final class ForageGoal extends Goal {
        private int ticks;

        ForageGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Slumbler s = Slumbler.this;
            return !s.isSlumbering() && s.getTarget() == null && s.random.nextInt(300) == 0
                    && (s.isInFluidType() || !s.level().getFluidState(s.blockPosition().relative(s.getDirection())).isEmpty());
        }

        @Override
        public boolean canContinueToUse() {
            return this.ticks > 0;
        }

        @Override
        public void start() {
            this.ticks = 45;
            Slumbler.this.getNavigation().stop();
            Slumbler.this.level().broadcastEntityEvent(Slumbler.this, EVENT_GULP);
        }

        @Override
        public void tick() {
            Slumbler s = Slumbler.this;
            this.ticks--;
            s.getNavigation().stop();
            if (this.ticks == 22 && s.level() instanceof ServerLevel server) {
                Vec3 mouth = s.position().add(Vec3.directionFromRotation(0.0F, s.yBodyRot).scale(1.5));
                server.sendParticles(ModParticles.CHROME_DROPLET.get(), mouth.x, s.getY() + 0.4, mouth.z, 8, 0.3, 0.1, 0.3, 0.08);
                s.playSound(ModSounds.SLUMBLER_BITE.get(), 0.5F, 1.3F);
                s.heal(1.0F);
            }
        }
    }

    /** Two Slumblers meeting rub their broad snouts together. */
    private final class NuzzleGoal extends Goal {
        private @Nullable Slumbler friend;
        private int ticks;

        NuzzleGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Slumbler s = Slumbler.this;
            if (s.isSlumbering() || s.getTarget() != null || s.socialCooldown > 0 || s.random.nextInt(100) != 0) {
                return false;
            }
            for (Slumbler other : s.level().getEntitiesOfClass(Slumbler.class, s.getBoundingBox().inflate(8.0),
                    o -> o != s && o.isAlive() && !o.isSlumbering() && o.getTarget() == null)) {
                this.friend = other;
                return true;
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return this.friend != null && this.friend.isAlive() && this.ticks > 0;
        }

        @Override
        public void start() {
            this.ticks = 120;
        }

        @Override
        public void tick() {
            Slumbler s = Slumbler.this;
            Slumbler f = this.friend;
            if (f == null) {
                return;
            }
            this.ticks--;
            s.getLookControl().setLookAt(f, 20.0F, 20.0F);
            if (s.distanceToSqr(f) > 9.0) {
                s.getNavigation().moveTo(f, 0.7);
            } else {
                s.getNavigation().stop();
                s.level().broadcastEntityEvent(s, EVENT_NUZZLE);
                s.playSound(ModSounds.SLUMBLER_AMBIENT.get(), 0.7F, 1.2F);
                if (s.level() instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.HEART, (s.getX() + f.getX()) / 2.0, s.getY() + 1.4, (s.getZ() + f.getZ()) / 2.0, 1, 0.2, 0.1, 0.2,
                            0.0);
                }
                s.socialCooldown = 1200 + s.random.nextInt(1200);
                f.socialCooldown = s.socialCooldown;
                this.ticks = 0;
            }
        }
    }

    /** Bubbles and droplets, like a popped soap bubble. */
    @Override
    public void makePoofParticles() {
        KillBurst.pop(this, 0x8FD0DC, 0xF59AD0, KillBurst.DROP, ModParticles.CHROME_BUBBLE.get());
    }
}
