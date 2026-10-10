package com.thesift.entity.dunes;

import com.thesift.effect.DeafenedEffect;
import com.thesift.entity.HopMoveControl;
import com.thesift.entity.KillBurst;
import com.thesift.entity.Resting;
import com.thesift.entity.Sifter;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModDunes;
import com.thesift.registry.ModEffects;
import com.thesift.registry.ModParticles;
import com.thesift.block.TuningCactusBudBlock;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Jaberora: a jerboa of the Rocky Dunes with big dark eyes, no nose, a mouth made for opera and a shining tail. It bounds
 * about in noisy groups, eats Tuning Fruit off the cacti, sings arias (louder at dusk) and lives with the Kerkorers:
 * it rides on their backs, sleeps curled against them at night, and its singing draws curious creatures into a
 * hiding Kerkorer's reach - and when a Kerkorer strikes, the Jaberoras round it stun its prey. Threatened, it lets
 * out one piercing note that stuns everything round it and Deafens them. Sifters hunt it. Tame it with Tuning
 * Fruit: it follows you, sings in your band and stuns your foes. It drops nothing.
 */
public class Jaberora extends TamableAnimal implements DunesNative, Resting, HopMoveControl.Hopper {
    public static final double PULSE_RADIUS = 6.0;
    public static final int PULSE_COOLDOWN = 200;
    /** The scale of an aria, in semitones above its own note (a major arpeggio with a run at the top). */
    private static final int[] ARIA = {0, 4, 7, 12, 11, 12, 16, 14, 12, 7, 12};
    private static final EntityDataAccessor<Boolean> SINGING = SynchedEntityData.defineId(Jaberora.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> NAPPING = SynchedEntityData.defineId(Jaberora.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CROUCH = SynchedEntityData.defineId(Jaberora.class, EntityDataSerializers.BOOLEAN);
    private static final byte EVENT_PULSE = 101;
    private static final byte EVENT_EAT = 102;
    /** Its colour: 0 pink (the commonest), 1 sand, 2 peach, 3 lilac, 4 snow (JaberoraRenderer picks the texture). */
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(Jaberora.class, EntityDataSerializers.INT);
    public static final int VARIANTS = 5;
    /** How often each colour turns up in the wild, out of 100. */
    private static final int[] VARIANT_WEIGHTS = {40, 18, 16, 14, 12};

    public final AnimationState pulseAnimation = new AnimationState();
    public final AnimationState eatAnimation = new AnimationState();
    public final AnimationState singAnimation = new AnimationState();
    /** Client: crouching before a hop / in the air (eased). */
    public float crouch;
    public float crouchO;
    public float air;
    public float airO;
    public int blinkTicks;
    private int pulseCooldown;
    private int ariaStep = -1;
    private int ariaIn = 400;
    private int voice;
    private int rideTicks;
    private int fed;

    public Jaberora(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.moveControl = new HopMoveControl<>(this);
        this.voice = level.getRandom().nextInt(5);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.JUMP_STRENGTH, 0.55)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SINGING, false);
        builder.define(NAPPING, false);
        builder.define(CROUCH, false);
        builder.define(VARIANT, 0);
    }

    @Override
    public int getNoActionTime() {
        return 0;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PulseGoal());
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Sifter.class, 10.0F, 1.2, 1.5, e -> !Jaberora.this.isTame()));
        this.goalSelector.addGoal(4, new NapGoal());
        this.goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.15, 8.0F, 3.0F));
        this.goalSelector.addGoal(6, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(6, new TemptGoal(this, 1.15, this::isFood, false));
        this.goalSelector.addGoal(7, new FruitGoal());
        this.goalSelector.addGoal(7, new KerkorerGoal());
        this.goalSelector.addGoal(8, new GroupGoal());
        this.goalSelector.addGoal(9, new WaterAvoidingRandomStrollGoal(this, 0.9) {
            @Override
            public boolean canUse() {
                return !Jaberora.this.isSinging() && !Jaberora.this.isNapping() && super.canUse();
            }
        });
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(11, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers());
    }

    // ------------------------------------------------------------------ state

    public boolean isSinging() {
        return this.entityData.get(SINGING);
    }

    /** Asleep (curled up, mostly against a Kerkorer). Not vanilla's bed sleep. */
    public boolean isNapping() {
        return this.entityData.get(NAPPING);
    }

    private void setNapping(boolean b) {
        this.entityData.set(NAPPING, b);
    }

    public int getVariant() {
        return Mth.clamp(this.entityData.get(VARIANT), 0, VARIANTS - 1);
    }

    public void setVariant(int variant) {
        this.entityData.set(VARIANT, Mth.clamp(variant, 0, VARIANTS - 1));
    }

    /** A wild one's colour: pink most often, then sand, peach, lilac and (rarest) snow. */
    private int rollVariant() {
        int r = this.random.nextInt(100);
        for (int i = 0; i < VARIANTS; i++) {
            r -= VARIANT_WEIGHTS[i];
            if (r < 0) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
            @Nullable SpawnGroupData data) {
        if (reason != EntitySpawnReason.BREEDING) {
            this.setVariant(this.rollVariant());
        }
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    public boolean isWindingUp() {
        return this.entityData.get(CROUCH);
    }

    @Override
    public boolean isResting() {
        return this.isNapping() || this.isPassenger() || this.isOrderedToSit() || this.isSinging();
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ModDunes.JABERORA_FOOD);
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        Jaberora baby = ModDunes.JABERORA.get().create(level, EntitySpawnReason.BREEDING);
        if (baby != null) {
            // a baby takes after one of its parents
            baby.setVariant(partner instanceof Jaberora other && this.random.nextBoolean() ? other.getVariant() : this.getVariant());
        }
        return baby;
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return ModDunes.dunesGround(level.getBlockState(pos.below())) ? 10.0F : 0.0F;
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 5;
    }

    // ------------------------------------------------------------------ hopping

    @Override
    public int hopDelay() {
        return this.getTarget() != null || this.isTame() && this.getOwner() != null && this.distanceToSqr(this.getOwner()) > 36.0
                ? 1 + this.random.nextInt(2) : 3 + this.random.nextInt(6);
    }

    @Override
    public int windup() {
        return 2;
    }

    @Override
    public void onWindup() {
        this.entityData.set(CROUCH, true);
    }

    @Override
    public void onHop() {
        this.entityData.set(CROUCH, false);
        this.playSound(ModDunes.JABERORA_HOP.get(), 0.4F, 1.0F + this.random.nextFloat() * 0.3F);
    }

    @Override
    protected int calculateFallDamage(double fallDistance, float damageModifier) {
        return Math.max(0, super.calculateFallDamage(fallDistance, damageModifier) - 5);
    }

    // ------------------------------------------------------------------ taming and feeding

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (this.isFood(stack) && !this.isTame() && !this.isBaby()) {
            if (this.level() instanceof ServerLevel server) {
                this.usePlayerItem(player, hand, stack);
                this.eat(server);
                if (++this.fed >= 2 && this.random.nextInt(3) == 0) {
                    this.tame(player);
                    this.setOrderedToSit(false);
                    this.setPersistenceRequired();
                    server.broadcastEntityEvent(this, (byte) 7);
                    player.sendOverlayMessage(Component.translatable("message.thesift.jaberora.tamed"));
                    this.ariaIn = 10; // it sings for joy
                } else {
                    server.broadcastEntityEvent(this, (byte) 6);
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (this.isTame() && this.isOwnedBy(player)) {
            if (this.isFood(stack) && this.getHealth() < this.getMaxHealth()) {
                if (this.level() instanceof ServerLevel server) {
                    this.usePlayerItem(player, hand, stack);
                    this.eat(server);
                    this.heal(4.0F);
                }
                return InteractionResult.SUCCESS;
            }
            if (player.isSecondaryUseActive() && !this.isFood(stack)) {
                if (!this.level().isClientSide()) {
                    boolean sit = !this.isOrderedToSit();
                    this.setOrderedToSit(sit);
                    this.getNavigation().stop();
                    this.setTarget(null);
                    player.sendOverlayMessage(Component.translatable(sit ? "message.thesift.jaberora.sit" : "message.thesift.jaberora.follow"));
                }
                return InteractionResult.SUCCESS;
            }
        }
        return super.mobInteract(player, hand);
    }

    private void eat(ServerLevel level) {
        this.playSound(ModDunes.JABERORA_EAT.get(), 0.8F, 1.0F + this.random.nextFloat() * 0.2F);
        level.broadcastEntityEvent(this, EVENT_EAT);
    }

    // ------------------------------------------------------------------ the server tick

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.pulseCooldown > 0) {
            this.pulseCooldown--;
        }
        if (this.isNapping() && (!level.isDarkOutside() || this.getTarget() != null || this.isPassenger()) && this.random.nextInt(20) == 0) {
            this.setNapping(false);
        }
        if (this.isNapping() && this.tickCount % 60 == 0) {
            this.playSound(ModDunes.JABERORA_SNORE.get(), 0.4F, 1.4F + this.random.nextFloat() * 0.3F);
        }
        this.tickAria(level);
        this.tickRide(level);
        // when its Kerkorer strikes, it helps: one stunning note at the prey
        if (!this.isTame() && this.pulseCooldown <= 0 && this.tickCount % 10 == 0) {
            for (Kerkorer k : level.getEntitiesOfClass(Kerkorer.class, this.getBoundingBox().inflate(12.0), Kerkorer::isAlive)) {
                LivingEntity p = k.prey();
                if (p != null && p.isAlive() && p.distanceToSqr(this) < PULSE_RADIUS * PULSE_RADIUS) {
                    this.pulse(level);
                    break;
                }
            }
        }
    }

    /** An aria: a phrase of sung notes; near a hiding Kerkorer it draws curious creatures in to listen. */
    private void tickAria(ServerLevel level) {
        if (this.ariaStep < 0) {
            int every = level.isDarkOutside() ? 500 : 900;
            if (!this.isNapping() && --this.ariaIn <= 0) {
                this.ariaStep = 0;
                this.entityData.set(SINGING, true);
                this.getNavigation().stop();
            } else if (this.ariaIn > every) {
                this.ariaIn = every;
            }
            return;
        }
        if (this.ariaStep % 4 == 0) {
            int n = this.ariaStep / 4;
            if (n < ARIA.length) {
                int semis = ARIA[n] + this.voice - 6;
                float pitch = (float) Math.pow(2.0, semis / 12.0);
                this.playSound(ModDunes.JABERORA_SING.get(), 1.2F, Mth.clamp(pitch, 0.5F, 2.0F));
                level.sendParticles(ModParticles.SIFT_NOTE.get(), this.getX(), this.getY() + 0.9, this.getZ(), 0, (ARIA[n] % 12) / 24.0, 0.0, 0.0, 1.0);
                // its singing is music too: it cracks the Grubs' shells
                for (Grub g : level.getEntitiesOfClass(Grub.class, this.getBoundingBox().inflate(Grub.HEARING), Grub::isAlive)) {
                    g.hearNote(level, this.position());
                }
            }
        }
        if (this.ariaStep == 4 && !this.isTame()) {
            this.lure(level);
        }
        if (++this.ariaStep >= ARIA.length * 4 + 6) {
            this.ariaStep = -1;
            this.ariaIn = 300 + this.random.nextInt(600);
            this.entityData.set(SINGING, false);
        }
    }

    /** Curious creatures come to listen - straight into a hiding Kerkorer's reach. */
    private void lure(ServerLevel level) {
        List<Kerkorer> hunters = level.getEntitiesOfClass(Kerkorer.class, this.getBoundingBox().inflate(10.0), Kerkorer::isHiding);
        if (hunters.isEmpty()) {
            return;
        }
        Kerkorer k = hunters.get(0);
        Vec3 bait = k.mouth();
        for (PathfinderMob m : level.getEntitiesOfClass(PathfinderMob.class, this.getBoundingBox().inflate(16.0),
                x -> x.isAlive() && !(x instanceof DunesNative) && x.getTarget() == null && !(x instanceof net.minecraft.world.entity.monster.Enemy))) {
            if (m instanceof TamableAnimal t && t.isTame()) {
                continue;
            }
            m.getNavigation().moveTo(bait.x, bait.y, bait.z, 1.0);
        }
    }

    private void tickRide(ServerLevel level) {
        if (this.getVehicle() instanceof Kerkorer k) {
            if (++this.rideTicks > 900 || !k.isResting() || this.isTame()) {
                this.stopRiding();
                this.rideTicks = 0;
            }
        }
    }

    /** One piercing note: everything round it (but its own kind, its Kerkorer and its owner) is stunned and Deafened. */
    public void pulse(ServerLevel level) {
        if (this.pulseCooldown > 0) {
            return;
        }
        this.pulseCooldown = PULSE_COOLDOWN + this.random.nextInt(60);
        this.playSound(ModDunes.JABERORA_PULSE.get(), 2.0F, 1.6F + this.random.nextFloat() * 0.3F);
        level.broadcastEntityEvent(this, EVENT_PULSE);
        level.sendParticles(ModParticles.RESONANCE_RING.get(), this.getX(), this.getY() + 0.5, this.getZ(), 0, 0.0, 0.0, 0.0, 1.0);
        level.sendParticles(ParticleTypes.NOTE, this.getX(), this.getY() + 0.9, this.getZ(), 8, 0.6, 0.3, 0.6, 1.0);
        LivingEntity owner = this.getOwner();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(PULSE_RADIUS), LivingEntity::isAlive)) {
            if (e == this || e instanceof Jaberora || e instanceof Kerkorer || e == owner || e instanceof Reservoir) {
                continue;
            }
            if (e instanceof TamableAnimal t && owner != null && t.isOwnedBy(owner)) {
                continue;
            }
            if (e instanceof Player p && (p.isCreative() || p.isSpectator() || this.isTame() && this.getTarget() != p)) {
                continue;
            }
            DeafenedEffect.stun(e, this, 60, 200);
        }
        if (this.isSinging()) {
            this.ariaStep = ARIA.length * 4;
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && this.isAlive()) {
            this.setNapping(false);
            if (this.isPassenger()) {
                this.stopRiding();
            }
            if (source.getEntity() instanceof LivingEntity attacker && attacker != this.getOwner()) {
                this.pulse(level);
            }
        }
        return hurt;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, net.minecraft.world.entity.Entity target) {
        // it fights with its voice, not its teeth
        this.pulse(level);
        return super.doHurtTarget(level, target);
    }

    // ------------------------------------------------------------------ goals

    /** Its target in earshot: the stunning note (it keeps its distance otherwise). */
    private class PulseGoal extends Goal {
        PulseGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = Jaberora.this.getTarget();
            return t != null && t.isAlive() && !Jaberora.this.isOrderedToSit();
        }

        @Override
        public void stop() {
            Jaberora.this.getNavigation().stop();
        }

        @Override
        public void tick() {
            Jaberora j = Jaberora.this;
            LivingEntity t = j.getTarget();
            if (t == null) {
                return;
            }
            j.getLookControl().setLookAt(t, 30.0F, 30.0F);
            double d = j.distanceToSqr(t);
            if (d < (PULSE_RADIUS - 1.5) * (PULSE_RADIUS - 1.5)) {
                if (j.pulseCooldown <= 0 && j.level() instanceof ServerLevel server) {
                    j.pulse(server);
                } else if (j.tickCount % 20 == 0) {
                    // it keeps out of reach while its voice recovers
                    Vec3 away = j.position().add(j.position().subtract(t.position()).normalize().scale(4.0));
                    j.getNavigation().moveTo(away.x, away.y, away.z, 1.2);
                }
            } else if (j.tickCount % 10 == 0) {
                j.getNavigation().moveTo(t, 1.1);
            }
        }
    }

    /** At night a wild Jaberora curls up against its Kerkorer (or anywhere, if it has none); tamed ones nap when told to sit. */
    private class NapGoal extends Goal {
        private @Nullable Kerkorer bed;

        NapGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            Jaberora j = Jaberora.this;
            if (!j.level().isDarkOutside() || j.getTarget() != null || j.isPassenger() || j.random.nextInt(60) != 0) {
                return false;
            }
            if (j.isTame()) {
                return j.isOrderedToSit();
            }
            List<Kerkorer> ks = j.level().getEntitiesOfClass(Kerkorer.class, j.getBoundingBox().inflate(24.0), Kerkorer::isAlive);
            this.bed = ks.isEmpty() ? null : ks.get(0);
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            Jaberora j = Jaberora.this;
            return j.level().isDarkOutside() && j.getTarget() == null && !j.isPassenger() && (!j.isTame() || j.isOrderedToSit());
        }

        @Override
        public void stop() {
            Jaberora.this.setNapping(false);
        }

        @Override
        public void tick() {
            Jaberora j = Jaberora.this;
            if (j.isNapping()) {
                return;
            }
            Kerkorer k = this.bed;
            if (k != null && k.isAlive() && j.distanceToSqr(k) > 2.5 * 2.5) {
                if (j.tickCount % 20 == 0) {
                    j.getNavigation().moveTo(k, 1.0);
                }
                return;
            }
            j.getNavigation().stop();
            j.setNapping(true);
        }
    }

    /** Hungry, it hops to a Tuning Cactus crown in fruit and eats the fruit off it. */
    private class FruitGoal extends Goal {
        private @Nullable BlockPos crown;
        private int ticks;

        FruitGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Jaberora j = Jaberora.this;
            if (j.isOrderedToSit() || j.isNapping() || j.random.nextInt(j.getHealth() < j.getMaxHealth() ? 40 : 300) != 0) {
                return false;
            }
            BlockPos at = j.blockPosition();
            for (BlockPos p : BlockPos.betweenClosed(at.offset(-8, -2, -8), at.offset(8, 6, 8))) {
                BlockState s = j.level().getBlockState(p);
                if (s.is(ModBlocks.TUNING_CACTUS_BUD.get()) && s.getValue(TuningCactusBudBlock.FRUIT)) {
                    this.crown = p.immutable();
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return this.crown != null && this.ticks < 300;
        }

        @Override
        public void start() {
            this.ticks = 0;
        }

        @Override
        public void stop() {
            this.crown = null;
        }

        @Override
        public void tick() {
            Jaberora j = Jaberora.this;
            BlockPos c = this.crown;
            if (c == null) {
                return;
            }
            this.ticks++;
            if (j.distanceToSqr(c.getX() + 0.5, j.getY(), c.getZ() + 0.5) > 2.0 * 2.0) {
                if (this.ticks % 20 == 1) {
                    j.getNavigation().moveTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 1.0);
                }
                return;
            }
            BlockState s = j.level().getBlockState(c);
            if (j.level() instanceof ServerLevel server && s.is(ModBlocks.TUNING_CACTUS_BUD.get()) && s.getValue(TuningCactusBudBlock.FRUIT)) {
                server.setBlock(c, s.setValue(TuningCactusBudBlock.FRUIT, false), 3);
                j.eat(server);
                j.heal(4.0F);
                if (!j.isBaby() && j.canFallInLove() && j.random.nextInt(3) == 0) {
                    j.setInLove(null);
                }
            }
            this.crown = null;
        }
    }

    /** A wild one keeps close to a Kerkorer it lives with, and now and then climbs onto its back while it hides. */
    private class KerkorerGoal extends Goal {
        private @Nullable Kerkorer friend;

        KerkorerGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Jaberora j = Jaberora.this;
            if (j.isTame() || j.isPassenger() || j.isNapping() || j.random.nextInt(200) != 0) {
                return false;
            }
            List<Kerkorer> ks = j.level().getEntitiesOfClass(Kerkorer.class, j.getBoundingBox().inflate(16.0), Kerkorer::isResting);
            this.friend = ks.isEmpty() ? null : ks.get(0);
            return this.friend != null;
        }

        @Override
        public boolean canContinueToUse() {
            Jaberora j = Jaberora.this;
            return this.friend != null && this.friend.isAlive() && !j.isPassenger() && j.getNavigation().isInProgress();
        }

        @Override
        public void start() {
            if (this.friend != null) {
                Jaberora.this.getNavigation().moveTo(this.friend, 1.0);
            }
        }

        @Override
        public void tick() {
            Jaberora j = Jaberora.this;
            Kerkorer k = this.friend;
            if (k != null && j.distanceToSqr(k) < 2.2 * 2.2 && k.isResting() && j.random.nextInt(3) == 0) {
                j.startRiding(k);
                j.rideTicks = 0;
                j.getNavigation().stop();
            }
        }
    }

    /** It never strays far from its group: back towards the others when it wanders off. */
    private class GroupGoal extends Goal {
        private @Nullable Vec3 centre;

        GroupGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Jaberora j = Jaberora.this;
            if (j.isTame() || j.tickCount % 40 != 0) {
                return false;
            }
            List<Jaberora> group = j.level().getEntitiesOfClass(Jaberora.class, j.getBoundingBox().inflate(24.0), o -> o != j && !o.isTame());
            if (group.isEmpty()) {
                return false;
            }
            double x = 0;
            double z = 0;
            for (Jaberora o : group) {
                x += o.getX();
                z += o.getZ();
            }
            Vec3 c = new Vec3(x / group.size(), j.getY(), z / group.size());
            if (c.distanceToSqr(j.position()) < 8.0 * 8.0) {
                return false;
            }
            this.centre = c;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return this.centre != null && Jaberora.this.getNavigation().isInProgress();
        }

        @Override
        public void start() {
            Vec3 c = this.centre;
            if (c != null) {
                Jaberora.this.getNavigation().moveTo(c.x, c.y, c.z, 1.0);
            }
        }
    }

    // ------------------------------------------------------------------ client

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EVENT_PULSE -> {
                this.pulseAnimation.start(this.tickCount);
                for (int i = 0; i < 16; i++) {
                    double a = i * Math.PI / 8.0;
                    this.level().addParticle(ParticleTypes.END_ROD, this.getX(), this.getY() + 0.6, this.getZ(), Math.cos(a) * 0.35, 0.0,
                            Math.sin(a) * 0.35);
                }
            }
            case EVENT_EAT -> this.eatAnimation.start(this.tickCount);
            default -> super.handleEntityEvent(id);
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (SINGING.equals(accessor) && this.level().isClientSide()) {
            this.singAnimation.animateWhen(this.isSinging(), this.tickCount);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.crouchO = this.crouch;
            this.crouch += ((this.isWindingUp() ? 1.0F : 0.0F) - this.crouch) * 0.5F;
            this.airO = this.air;
            this.air += ((this.onGround() || this.isPassenger() ? 0.0F : 1.0F) - this.air) * 0.4F;
            if (this.blinkTicks > 0) {
                this.blinkTicks--;
            } else if (this.random.nextInt(70) == 0) {
                this.blinkTicks = 5;
            }
            if (this.isNapping() && this.random.nextInt(50) == 0) {
                this.level().addParticle(ModParticles.SLEEP_SPORE.get(), this.getX(), this.getY() + 0.6, this.getZ(), 0.0, 0.02, 0.0);
            }
        }
    }

    /** The Codex page: it sings, then pulses. */
    public void codexPose(int t) {
        if (t % 60 == 0) {
            this.setVariant((t / 60) % VARIANTS); // the page shows every colour in turn
        }
        if (t % 100 == 5) {
            this.singAnimation.start(this.tickCount);
        }
        if (t % 100 == 60) {
            this.singAnimation.stop();
            this.pulseAnimation.start(this.tickCount);
        }
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.isNapping() || this.isSinging() ? null : ModDunes.JABERORA_CHIRP.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModDunes.JABERORA_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModDunes.JABERORA_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }

    @Override
    public void makePoofParticles() {
        KillBurst.pop(this, 0xC78E7C, 0xF08F98, KillBurst.NOTE, ParticleTypes.NOTE);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Fed", this.fed);
        output.putInt("Voice", this.voice);
        output.putInt("Variant", this.getVariant());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.fed = input.getIntOr("Fed", 0);
        this.voice = input.getIntOr("Voice", this.voice);
        this.setVariant(input.getIntOr("Variant", 0));
    }
}
