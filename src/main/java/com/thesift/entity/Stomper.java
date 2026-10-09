package com.thesift.entity;

import com.thesift.music.SongEvents;
import com.thesift.registry.ModEntities;
import com.thesift.registry.ModFluids;
import com.thesift.registry.ModItems;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import org.jspecify.annotations.Nullable;

/**
 * Stomper (S1 remake): a tall Sift elephant - a shaggy teal coat over wrinkled crimson hide, big
 * flapping ears, small tusks, a long five-part trunk and a little garden of pink Sift grass and
 * flowers growing on its domed back. Gentle and slow until it is angered:
 * <ul>
 *   <li>Trunk grab: it swings its trunk out at its foe, wraps it round them, lifts them high, holds
 *   them a moment, then slams them into the ground or flings them away. The victim rides the
 *   Stomper's trunk tip (positioned from {@link StomperRig} on both sides, so it never
 *   rubber-bands) and fights free by hitting the Stomper. A tame one grabs its owner's enemies.</li>
 *   <li>Stomp: it rears up on its hind legs and slams down - area damage, knockback, a ring of dust
 *   and broken ground, and the camera shakes for anyone near. Ridden adults stomp on the attack
 *   key; dances end in two stomps.</li>
 * </ul>
 * It plods to Chrome (or water) and drinks through the trunk, hoses monsters with it, dances to
 * drums and shakes out its garden. Fed Hummingblooms, two adults lay a Stomper Egg that hatches a
 * Stompling, which curls up and rolls about; babies (only) are tamed with Hummingblooms, grow up
 * tame, follow, sit and can be ridden, and a tame baby drums for its owner.
 */
public class Stomper extends TamableAnimal implements net.minecraft.world.entity.PlayerRideableJumping {
    private static final EntityDataAccessor<Float> CHROME = SynchedEntityData.defineId(Stomper.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DANCE = SynchedEntityData.defineId(Stomper.class, EntityDataSerializers.INT);
    /** 0: the Sift's teal coat, 1: the White Forest's frosted coat. */
    private static final EntityDataAccessor<Integer> COAT = SynchedEntityData.defineId(Stomper.class, EntityDataSerializers.INT);
    /** S1 the trunk grab's phase (StomperRig.NONE .. DROP); both sides time it from the change. */
    private static final EntityDataAccessor<Integer> GRAB = SynchedEntityData.defineId(Stomper.class, EntityDataSerializers.INT);
    public static final int NORMAL = 0;
    public static final int WHITE = 1;
    /** The egg remembers its parents' coat (custom data on the Stomper Egg). */
    public static final String EGG_COAT = "StomperCoat";
    private static final byte EVENT_DRINK = 70;
    private static final byte EVENT_SPRAY = 71;
    private static final byte EVENT_STOMP = 72;
    private static final byte EVENT_PUFF = 73;
    private static final byte EVENT_SLAP = 74;
    private static final byte EVENT_LAY = 75;
    /** A baby drums on the ground with its trunk (one event per beat). */
    private static final byte EVENT_DRUM = 76;
    /** Idle: it shakes its garden out like a wet dog, or lowers its trunk to sniff the flowers. */
    private static final byte EVENT_SHAKE = 80;
    private static final byte EVENT_SNIFF = 81;
    /** S1 the stomp lands: dust ring and camera shake on the client. */
    private static final byte EVENT_STOMP_IMPACT = 82;
    /** The baby's drum solo: one note per beat, in note-block semitones. */
    private static final int[] DRUM_PATTERN = {6, 6, 13, 6, 10, 13, 18, 13, 6, 18};
    private static final int DRUM_BEAT = 5;
    public static final int RIDER_STOMP_COOLDOWN = 30;
    public static final int DANCE_LENGTH = 120;
    /** The dance ends with two stomps, landing at these many ticks before the end. */
    private static final int STOMP_1 = 34;
    private static final int STOMP_2 = 6;
    /** Ticks from rearing up to the slam (StomperModel's stomp lands at 0.8 s). */
    public static final int STOMP_IMPACT = 16;
    public static final int SPRAY_WINDUP = 20;
    private static final int SPRAY_LENGTH = 30;
    private static final double SPRAY_RANGE = 10.0;
    /** How close (blocks) the reaching trunk tip must come to a foe's hitbox to catch it. */
    private static final double GRAB_CATCH = 1.4;
    /** A Stompling rolls on a body about this round (blocks, at half size). */
    private static final double ROLL_RADIUS = 0.375;

    public final AnimationState drinkAnimation = new AnimationState();
    public final AnimationState sprayAnimation = new AnimationState();
    public final AnimationState stompAnimation = new AnimationState();
    public final AnimationState puffAnimation = new AnimationState();
    public final AnimationState slapAnimation = new AnimationState();
    public final AnimationState drumAnimation = new AnimationState();
    public final AnimationState shakeAnimation = new AnimationState();
    public final AnimationState sniffAnimation = new AnimationState();
    private int drumLeft = -1;
    private int drumCooldown = 1200;
    private int riderStompCooldown;
    private float playerJumpPending;

    // the stomp: ticks to the slam, how hard, and whether it hits players (a wild, angry stomp)
    private int stompDelay = -1;
    private float stompPower;
    private boolean stompHostile;
    private boolean stompRider;
    private int stompCooldown = 40;

    // the grab
    private int grabTicks;
    private int grabCooldown = 60;
    private @Nullable LivingEntity grabTarget;
    private int struggle;
    private float heldDamage;
    private boolean releasing;

    // client: a Stompling curled up and rolling
    private float roll;
    private float rollO;
    private float rollAngle;
    private float rollAngleO;

    private int drinkCooldown = 200;
    private int sprayCooldown;
    private int puffTimer = 60;
    private boolean drinking;
    /** Server: ticks since it came out of water or Chrome (it shakes itself dry). */
    private int wet;

    public Stomper(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 200.0)
                .add(Attributes.ARMOR, 16.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.16)
                .add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
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
        this.goalSelector.addGoal(1, new DanceGoal());
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new SprayGoal());
        this.goalSelector.addGoal(4, new AttackGoal());
        this.goalSelector.addGoal(5, new DrinkGoal());
        this.goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.1, 10.0F, 4.0F));
        this.goalSelector.addGoal(7, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(8, new TemptGoal(this, 1.1, s -> s.is(ModItems.HUMMINGBLOOM.get()), false));
        this.goalSelector.addGoal(9, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(10, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(10, new IdleGoal());
        this.goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(12, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHROME, 0.0F);
        builder.define(DANCE, 0);
        builder.define(COAT, NORMAL);
        builder.define(GRAB, StomperRig.NONE);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (GRAB.equals(accessor)) {
            this.grabTicks = 0;
        }
    }

    /** {@link #NORMAL} or {@link #WHITE}. */
    public int getCoat() {
        return Mth.clamp(this.entityData.get(COAT), NORMAL, WHITE);
    }

    public void setCoat(int coat) {
        this.entityData.set(COAT, Mth.clamp(coat, NORMAL, WHITE));
    }

    public boolean isWhite() {
        return this.getCoat() == WHITE;
    }

    /** Born in the White Forest (natural spawns and spawn eggs alike), it grows the snowy coat. */
    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
            @Nullable SpawnGroupData data) {
        this.setCoat(SnowCoat.at(level, this.blockPosition()) ? WHITE : NORMAL);
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    /** How much Chrome is stored in the trunk and belly, 0..1. */
    public float getChrome() {
        return Mth.clamp(this.entityData.get(CHROME), 0.0F, 1.0F);
    }

    public void setChrome(float chrome) {
        this.entityData.set(CHROME, Mth.clamp(chrome, 0.0F, 1.0F));
    }

    public boolean isDancing() {
        return this.entityData.get(DANCE) > 0;
    }

    /** Ticks of dance left (counts down to 0). */
    public int getDanceTicks() {
        return this.entityData.get(DANCE);
    }

    public boolean isDrinking() {
        return this.drinking;
    }

    /** S1 the trunk grab's phase (see {@link StomperRig}). */
    public int getGrabPhase() {
        return this.entityData.get(GRAB);
    }

    /** Ticks into the grab phase, with the partial tick, capped at the phase's length. */
    public float getGrabTime(float partialTicks) {
        int phase = Mth.clamp(this.getGrabPhase(), 0, StomperRig.LENGTH.length - 1);
        return Math.min(StomperRig.LENGTH[phase], this.grabTicks + partialTicks);
    }

    public float getRoll(float partialTicks) {
        return Mth.lerp(partialTicks, this.rollO, this.roll);
    }

    public float getRollAngle(float partialTicks) {
        return Mth.lerp(partialTicks, this.rollAngleO, this.rollAngle);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ModItems.HUMMINGBLOOM.get());
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        Stomper baby = ModEntities.STOMPER.get().create(level, EntitySpawnReason.BREEDING);
        if (baby != null) {
            baby.setCoat(partner instanceof Stomper other && this.random.nextBoolean() ? other.getCoat() : this.getCoat());
        }
        return baby;
    }

    // ------------------------------------------------------------------ drums & dancing

    /**
     * A drum was played at {@code pos}: every Stomper within {@code radius} starts to dance. Called
     * by the Sift Drum and the conga drum.
     */
    public static void hearDrum(ServerLevel level, Vec3 pos, double radius) {
        AABB box = new AABB(pos, pos).inflate(radius);
        for (Stomper s : level.getEntitiesOfClass(Stomper.class, box, e -> e.isAlive() && e.distanceToSqr(pos) <= radius * radius)) {
            s.startDance();
        }
    }

    public void startDance() {
        if (this.isDancing() || this.level().isClientSide() || this.getGrabPhase() != StomperRig.NONE) {
            return;
        }
        this.entityData.set(DANCE, DANCE_LENGTH);
        this.getNavigation().stop();
        this.playSound(ModSounds.STOMPER_HAPPY.get(), 1.4F, 0.9F + this.random.nextFloat() * 0.2F);
        this.playSound(ModSounds.STOMPER_TRUMPET.get(), 1.2F, 1.1F);
    }

    private void tickDance(ServerLevel level, int left) {
        if (left % 5 == 0) {
            Vec3 head = this.trunkRoot().add(0.0, 1.0 * this.getAgeScale(), 0.0);
            level.sendParticles(ModParticles.SIFT_NOTE.get(), head.x, head.y, head.z, 0, this.random.nextDouble(), 0.0, 0.0, 1.0);
        }
        if (left % 10 == 0) {
            level.sendParticles(ModParticles.DREAM_POLLEN.get(), this.getX(), this.getY() + 2.6 * this.getAgeScale(), this.getZ(), 6, 1.0, 0.6, 1.0, 0.02);
        }
        if (left % 20 == 10 && left > STOMP_1 + STOMP_IMPACT + 10) {
            this.playSound(ModSounds.STOMPER_TRUMPET.get(), 1.0F, 1.0F + this.random.nextFloat() * 0.4F);
        }
        if (left == STOMP_1 + STOMP_IMPACT || left == STOMP_2 + STOMP_IMPACT) {
            this.startStomp(left == STOMP_2 + STOMP_IMPACT ? 1.25F : 1.0F, false, false);
        }
    }

    /** Rears up; {@link #STOMP_IMPACT} ticks later it slams down (see {@link #stomp}). */
    private void startStomp(float power, boolean hostile, boolean rider) {
        if (this.stompDelay >= 0 || !(this.level() instanceof ServerLevel server)) {
            return;
        }
        this.stompDelay = STOMP_IMPACT;
        this.stompPower = power;
        this.stompHostile = hostile;
        this.stompRider = rider;
        this.getNavigation().stop();
        server.broadcastEntityEvent(this, EVENT_STOMP);
        this.playSound(ModSounds.STOMPER_TRUMPET.get(), this.isBaby() ? 0.8F : 1.4F, this.isBaby() ? 1.6F : 0.9F + this.random.nextFloat() * 0.15F);
    }

    /** Busy rearing for a stomp or grabbing with its trunk: it stands its ground. */
    public boolean isBusy() {
        return this.stompDelay >= 0 || this.getGrabPhase() != StomperRig.NONE;
    }

    /**
     * The slam: the ground cracks in a ring of dust and broken blocks, and everything it means to
     * hit is hurt and flung away (players too when it is a wild Stomper's angry stomp).
     */
    private void stomp(ServerLevel level, float power, boolean hostile) {
        double s = this.getAgeScale();
        double r = 6.0 * power * (this.isBaby() ? 0.6 : 1.0);
        this.playSound(ModSounds.STOMPER_STOMP.get(), this.isBaby() ? 1.0F : 2.0F, (this.isBaby() ? 1.3F : 0.8F) + this.random.nextFloat() * 0.15F);
        // the forefeet land in front of the body
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        double cx = this.getX() - Mth.sin(yaw) * 0.8 * s;
        double cz = this.getZ() + Mth.cos(yaw) * 0.8 * s;
        BlockPos below = BlockPos.containing(cx, this.getY() - 0.5, cz);
        BlockState ground = level.getBlockState(below);
        if (ground.isAir()) {
            ground = level.getBlockState(below.below());
        }
        if (!ground.isAir()) {
            BlockParticleOption crack = new BlockParticleOption(ParticleTypes.BLOCK, ground);
            for (int i = 0; i < 40; i++) {
                double a = i * Mth.TWO_PI / 40.0;
                for (double d = 1.2; d <= r; d += 1.4) {
                    level.sendParticles(crack, cx + Math.cos(a) * d, this.getY() + 0.1, cz + Math.sin(a) * d, 2, 0.15, 0.05, 0.15, 0.12);
                }
            }
        }
        level.sendParticles(ModParticles.RESONANCE_RING.get(), cx, this.getY() + 0.1, cz, 0, r, 0.0, 0.0, 1.0);
        level.sendParticles(ParticleTypes.POOF, cx, this.getY() + 0.2, cz, 30, 1.6 * s, 0.1, 1.6 * s, 0.08);
        level.sendParticles(ParticleTypes.CLOUD, cx, this.getY() + 0.2, cz, 16, 1.2 * s, 0.1, 1.2 * s, 0.15);
        // its garden shakes: petals and pollen fly off its back
        level.sendParticles(ModParticles.WISHWOOD_LEAF.get(), this.getX(), this.getY() + 3.0 * s, this.getZ(), 18, 1.0, 0.3, 1.0, 0.06);
        level.sendParticles(ModParticles.DREAM_POLLEN.get(), this.getX(), this.getY() + 3.0 * s, this.getZ(), 24, 1.2, 0.4, 1.2, 0.04);
        level.broadcastEntityEvent(this, EVENT_STOMP_IMPACT);
        AABB box = new AABB(cx, this.getY(), cz, cx, this.getY(), cz).inflate(r, 2.0, r);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, v -> this.isStompable(v, hostile))) {
            double dx = e.getX() - cx;
            double dz = e.getZ() - cz;
            double d = Math.max(0.5, Math.sqrt(dx * dx + dz * dz));
            if (d > r + e.getBbWidth() || e.getY() > this.getY() + 2.0) {
                continue;
            }
            float falloff = (float) Mth.clamp(1.0 - d / (r + 1.5), 0.25, 1.0);
            e.hurtServer(level, this.damageSources().mobAttack(this), (this.isBaby() ? 4.0F : 10.0F) * power * falloff);
            e.push(dx / d * 1.4 * falloff, 0.55 * falloff + 0.2, dz / d * 1.4 * falloff);
        }
        if (this.stompRider) {
            // a ring of rising notes in every colour round a rider's stomp
            for (int i = 0; i < 12; i++) {
                double a = i * Mth.TWO_PI / 12.0;
                level.sendParticles(ModParticles.SIFT_NOTE.get(), cx + Math.cos(a) * r * 0.8, this.getY() + 0.4, cz + Math.sin(a) * r * 0.8, 0, i / 12.0, 0.0, 0.0,
                        1.0);
            }
            level.playSound(null, this.getX(), this.getY(), this.getZ(), net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BASEDRUM.value(),
                    net.minecraft.sounds.SoundSource.NEUTRAL, 2.0F, 0.7F);
            SongEvents.note(level, null, this.position(), 6);
        }
    }

    /**
     * The rider pressed attack: rear up and slam down a ring of music that flings hostile creatures
     * away (a baby's is smaller). Called from the {@link CreatureLife.RiderStomp} packet.
     */
    public void riderStomp(Player rider) {
        if (this.riderStompCooldown > 0 || this.stompDelay >= 0) {
            return;
        }
        this.riderStompCooldown = RIDER_STOMP_COOLDOWN;
        this.startStomp(this.isBaby() ? 0.55F : 0.9F, false, true);
    }

    /** A note was played nearby; a tame baby drums along when its owner plays. */
    public void hearNote(ServerLevel level, @Nullable Player player, int pitch) {
        if (player != null && this.isBaby() && this.isTame() && this.isOwnedBy(player) && !this.isVehicle() && this.drumLeft < 0
                && this.drumCooldown < 1000) {
            this.startDrumming();
        }
    }

    /** A tame baby drums on the ground with its trunk for its owner. */
    public void startDrumming() {
        this.drumLeft = DRUM_PATTERN.length * DRUM_BEAT;
        this.drumCooldown = 1600 + this.random.nextInt(1600);
        this.getNavigation().stop();
    }

    public boolean isDrumming() {
        return this.drumLeft >= 0;
    }

    private void tickDrum(ServerLevel level) {
        this.getNavigation().stop();
        if (this.drumLeft % DRUM_BEAT == 0 && this.drumLeft > 0) {
            int beat = DRUM_PATTERN.length - this.drumLeft / DRUM_BEAT;
            int pitch = DRUM_PATTERN[Mth.clamp(beat, 0, DRUM_PATTERN.length - 1)];
            Vec3 at = this.trunkRoot().add(0.0, -0.6 * this.getAgeScale(), 0.0);
            float sp = com.thesift.music.Notes.soundPitch(pitch);
            level.playSound(null, at.x, at.y, at.z, net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BASEDRUM.value(), net.minecraft.sounds.SoundSource.NEUTRAL,
                    0.9F, sp);
            level.playSound(null, at.x, at.y, at.z, net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BASS.value(), net.minecraft.sounds.SoundSource.NEUTRAL, 0.5F, sp);
            level.sendParticles(ModParticles.SIFT_NOTE.get(), at.x, at.y + 0.6, at.z, 0, pitch / 24.0, 0.0, 0.0, 1.0);
            level.broadcastEntityEvent(this, EVENT_DRUM);
            // everyone around hears the beat (no player: the song tracker ignores it)
            SongEvents.note(level, null, at, pitch);
        }
        if (this.drumLeft == 0) {
            LivingEntity owner = this.getOwner();
            if (owner != null && owner.distanceToSqr(this) < 16.0 * 16.0) {
                owner.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 30, 0), this);
                owner.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 20 * 30, 0), this);
                level.sendParticles(ParticleTypes.HEART, owner.getX(), owner.getY() + owner.getBbHeight() + 0.3, owner.getZ(), 4, 0.3, 0.2, 0.3, 0.0);
            }
            this.playSound(ModSounds.STOMPER_HAPPY.get(), 0.9F, 1.7F);
        }
        this.drumLeft--;
    }

    /**
     * Never other Stompers, never its owner or anyone's pet. A friendly stomp (dance, rider) hits
     * hostile creatures; a wild Stomper's angry stomp hits players and its foe as well.
     */
    private boolean isStompable(LivingEntity e, boolean hostile) {
        if (e == this || !e.isAlive() || e instanceof Stomper || isPet(e) || e.getVehicle() == this) {
            return false;
        }
        if (e instanceof Player player) {
            return hostile && !player.isSpectator() && !player.isCreative();
        }
        if (e instanceof Enemy || e == this.getTarget()) {
            return true;
        }
        return e instanceof Mob mob && mob.getTarget() != null && (mob.getTarget() instanceof Player || mob.getTarget() == this);
    }

    static boolean isPet(Entity e) {
        if (e instanceof TamableAnimal t && t.isTame()) {
            return true;
        }
        return e instanceof OwnableEntity o && o.getOwnerReference() != null;
    }

    // ------------------------------------------------------------------ the trunk grab

    /** Whether it would try to grab {@code e}: something it can lift, that is not riding anything. */
    private boolean canGrab(LivingEntity e) {
        if (this.isBaby() || e instanceof Stomper || e.isPassenger() || e.isVehicle() || e.getBbWidth() > 1.4F || e.getBbHeight() > 2.6F
                || isPet(e)) {
            return false;
        }
        if (e instanceof Player player) {
            return !this.isTame() && !player.isSpectator() && !player.isCreative();
        }
        return true;
    }

    private void setGrabPhase(int phase) {
        this.entityData.set(GRAB, phase);
        this.grabTicks = 0;
    }

    /** Swings the trunk out at {@code target}; it is caught if the tip reaches it (see tickGrab). */
    private void startGrab(LivingEntity target) {
        this.grabTarget = target;
        this.struggle = 0;
        this.heldDamage = 0.0F;
        this.getNavigation().stop();
        this.setGrabPhase(StomperRig.REACH);
        this.playSound(ModSounds.STOMPER_TRUMPET.get(), 1.3F, 1.2F);
    }

    /** The victim being held (any passenger that is not its rider while a grab is on). */
    private @Nullable LivingEntity held() {
        if (this.getGrabPhase() == StomperRig.NONE) {
            return null;
        }
        for (Entity e : this.getPassengers()) {
            if (e instanceof LivingEntity living && e != this.getControllingPassenger()) {
                return living;
            }
        }
        return null;
    }

    /** The trunk tip now, as the grab pose puts it (null when there is no grab). */
    private @Nullable Vec3 grabTip(float time) {
        float[] pose = new float[2 + StomperRig.SEGMENTS];
        if (!StomperRig.pose(this.getGrabPhase(), time, pose)) {
            return null;
        }
        return StomperRig.tip(this.position(), this.yBodyRot, this.getAgeScale(), pose);
    }

    private void tickGrab(ServerLevel level) {
        int phase = this.getGrabPhase();
        this.grabTicks++;
        this.getNavigation().stop();
        if (this.grabCooldown > 0 && phase == StomperRig.NONE) {
            this.grabCooldown--;
        }
        if (phase == StomperRig.NONE) {
            // nobody rides a Stomper but its owner: a victim left on it (a grab cut short by a reload) is set down
            for (Entity e : List.copyOf(this.getPassengers())) {
                if (!(this.isTame() && e instanceof Player)) {
                    e.stopRiding();
                }
            }
            return;
        }
        LivingEntity victim = this.held();
        int len = StomperRig.LENGTH[phase];
        switch (phase) {
            case StomperRig.REACH -> {
                LivingEntity t = this.grabTarget;
                if (t == null || !t.isAlive() || t.isPassenger() || this.distanceToSqr(t) > 8.0 * 8.0) {
                    this.setGrabPhase(StomperRig.MISS);
                    return;
                }
                // turn the whole body to face it, so the trunk swings straight at it
                double dx = t.getX() - this.getX();
                double dz = t.getZ() - this.getZ();
                float want = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
                this.setYRot(this.getYRot() + Mth.clamp(Mth.wrapDegrees(want - this.getYRot()), -15.0F, 15.0F));
                this.yBodyRot = this.getYRot();
                this.yHeadRot = this.getYRot();
                if (this.grabTicks >= len) {
                    Vec3 tip = this.grabTip(len);
                    if (tip != null && t.getBoundingBox().inflate(GRAB_CATCH).contains(tip) && t.startRiding(this, true, true)) {
                        this.setGrabPhase(StomperRig.LIFT);
                        t.hurtServer(level, this.damageSources().mobAttack(this), 2.0F);
                        this.playSound(ModSounds.STOMPER_HAPPY.get(), 1.2F, 0.7F);
                        level.sendParticles(ParticleTypes.CRIT, tip.x, tip.y, tip.z, 10, 0.3, 0.3, 0.3, 0.1);
                    } else {
                        this.setGrabPhase(StomperRig.MISS);
                        this.grabCooldown = 40;
                    }
                }
            }
            case StomperRig.LIFT, StomperRig.HOLD -> {
                if (victim == null || !victim.isAlive()) {
                    this.setGrabPhase(StomperRig.DROP);
                    return;
                }
                if (phase == StomperRig.HOLD && this.grabTicks % 10 == 0) {
                    // a squeeze
                    victim.hurtServer(level, this.damageSources().mobAttack(this), 1.0F);
                }
                if (this.grabTicks >= len) {
                    if (phase == StomperRig.LIFT) {
                        this.setGrabPhase(StomperRig.HOLD);
                        this.playSound(ModSounds.STOMPER_TRUMPET.get(), 1.6F, 0.8F);
                    } else {
                        this.setGrabPhase(this.random.nextBoolean() ? StomperRig.SLAM : StomperRig.FLING);
                    }
                }
            }
            case StomperRig.SLAM, StomperRig.FLING -> {
                boolean slam = phase == StomperRig.SLAM;
                if (victim != null && this.grabTicks >= (slam ? StomperRig.SLAM_RELEASE : StomperRig.FLING_RELEASE)) {
                    this.release(level, victim, slam ? 1 : 2);
                }
                if (this.grabTicks >= len) {
                    this.endGrab();
                }
            }
            default -> {
                if (victim != null) {
                    this.release(level, victim, 0);
                }
                if (this.grabTicks >= len) {
                    this.endGrab();
                }
            }
        }
    }

    private void endGrab() {
        this.setGrabPhase(StomperRig.NONE);
        this.grabTarget = null;
        this.grabCooldown = Math.max(this.grabCooldown, 100 + this.random.nextInt(60));
    }

    /** Lets go of the victim: 0 dropped (it fought free), 1 slammed down, 2 flung away. */
    private void release(ServerLevel level, LivingEntity victim, int how) {
        Vec3 tip = this.grabTip(this.grabTicks);
        this.releasing = true;
        victim.stopRiding();
        this.releasing = false;
        if (tip != null) {
            double y = how == 1 ? this.getY() + 0.05 : tip.y - victim.getBbHeight() * 0.55;
            AABB at = victim.getBoundingBox().move(tip.x - victim.getX(), y - victim.getY(), tip.z - victim.getZ());
            if (level.noCollision(victim, at)) {
                victim.teleportTo(tip.x, y, tip.z);
            }
        }
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(yaw);
        double fz = Mth.cos(yaw);
        switch (how) {
            case 1 -> {
                victim.hurtServer(level, this.damageSources().mobAttack(this), 7.0F);
                victim.push(fx * 0.3, -0.4, fz * 0.3);
                this.playSound(ModSounds.STOMPER_STOMP.get(), 1.4F, 1.1F);
                BlockState ground = level.getBlockState(victim.blockPosition().below());
                if (!ground.isAir()) {
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), victim.getX(), victim.getY() + 0.1, victim.getZ(), 30, 0.6, 0.1, 0.6,
                            0.15);
                }
                level.sendParticles(ParticleTypes.POOF, victim.getX(), victim.getY() + 0.2, victim.getZ(), 12, 0.6, 0.1, 0.6, 0.05);
                level.broadcastEntityEvent(this, EVENT_STOMP_IMPACT);
            }
            case 2 -> {
                // whipped away to its left, up and out
                double lx = Mth.cos(yaw);
                double lz = Mth.sin(yaw);
                victim.hurtServer(level, this.damageSources().mobAttack(this), 4.0F);
                victim.push(lx * 1.3 + fx * 0.6, 0.75, lz * 1.3 + fz * 0.6);
                this.playSound(ModSounds.STOMPER_TRUMPET.get(), 1.6F, 1.3F);
            }
            default -> {
                victim.push(fx * 0.4, 0.2, fz * 0.4);
                this.playSound(ModSounds.STOMPER_HURT.get(), 1.0F, 1.2F);
            }
        }
    }

    /** Whether {@code e} is held fast in its trunk right now (it cannot just get off). */
    public boolean holdsFast(Entity e) {
        return !this.releasing && this.isAlive() && e.isAlive() && e.getVehicle() == this && e != this.getControllingPassenger()
                && StomperRig.holding(this.getGrabPhase());
    }

    /** A held player cannot simply dismount: it must fight free (registered by CreatureLife). */
    public static void onDismount(EntityMountEvent event) {
        if (event.isDismounting() && !event.getLevel().isClientSide() && event.getEntityBeingMounted() instanceof Stomper stomper
                && stomper.holdsFast(event.getEntityMounting())) {
            event.setCanceled(true);
        }
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().isEmpty() && this.getGrabPhase() == StomperRig.NONE;
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (this.getGrabPhase() != StomperRig.NONE && passenger != this.getControllingPassenger()) {
            Vec3 tip = this.grabTip(this.grabTicks);
            if (tip != null) {
                moveFunction.accept(passenger, tip.x, tip.y - passenger.getBbHeight() * 0.55, tip.z);
                return;
            }
        }
        super.positionRider(passenger, moveFunction);
    }

    /** A rider sits; a victim dangles. */
    @Override
    public boolean shouldRiderSit() {
        return this.getGrabPhase() == StomperRig.NONE;
    }

    /** A victim in its trunk can hit it (that is how it fights free). */
    @Override
    public boolean canRiderInteract() {
        return this.getGrabPhase() != StomperRig.NONE;
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level() instanceof ServerLevel server) {
            int dance = this.entityData.get(DANCE);
            if (dance > 0) {
                this.tickDance(server, dance);
                this.entityData.set(DANCE, dance - 1);
            }
            if (this.riderStompCooldown > 0) {
                this.riderStompCooldown--;
            }
            if (this.stompCooldown > 0) {
                this.stompCooldown--;
            }
            if (this.stompDelay >= 0) {
                this.getNavigation().stop();
                if (this.stompDelay-- == 0) {
                    this.stomp(server, this.stompPower, this.stompHostile);
                }
            }
            this.tickGrab(server);
            if (this.drumLeft >= 0) {
                this.tickDrum(server);
            } else if (this.drumCooldown > 0) {
                this.drumCooldown--;
            } else if (this.isBaby() && this.isTame() && !this.isVehicle()) {
                LivingEntity owner = this.getOwner();
                if (owner != null && owner.distanceToSqr(this) < 8.0 * 8.0) {
                    this.startDrumming();
                }
            }
            if (this.drinkCooldown > 0) {
                this.drinkCooldown--;
            }
            if (this.sprayCooldown > 0) {
                this.sprayCooldown--;
            }
            if (this.isInFluidType()) {
                this.wet = 100;
            } else if (this.wet > 0) {
                this.wet--;
            }
            if (--this.puffTimer <= 0) {
                this.puffTimer = 90 + this.random.nextInt(200);
                if (!this.isBusy()) {
                    server.broadcastEntityEvent(this, EVENT_PUFF);
                    this.playSound(ModSounds.STOMPER_PUFF.get(), 0.6F, 0.8F + this.random.nextFloat() * 0.3F);
                }
            }
        } else {
            if (this.getGrabPhase() != StomperRig.NONE) {
                this.grabTicks++;
            }
            this.tickRoll();
            this.gardenEffects();
        }
    }

    /**
     * Client: a Stompling on the move curls up and rolls; the roll angle follows the distance it
     * covers (no skidding), and when it stops it rolls on to upright before it uncurls.
     */
    private void tickRoll() {
        this.rollO = this.roll;
        this.rollAngleO = this.rollAngle;
        double dx = this.getX() - this.xo;
        double dz = this.getZ() - this.zo;
        double moved = Math.sqrt(dx * dx + dz * dz);
        boolean rolling = this.isBaby() && moved > 0.02 && !this.isVehicle() && !this.isPassenger() && !this.isInSittingPose() && !this.isDrumming()
                && this.deathTime <= 0;
        if (rolling) {
            this.roll = Math.min(1.0F, this.roll + 0.18F);
            this.rollAngle += (float) (moved / ROLL_RADIUS);
        } else if (this.roll > 0.0F) {
            float upright = (float) (Math.ceil(this.rollAngle / Mth.TWO_PI - 1.0E-3) * Mth.TWO_PI);
            float left = upright - this.rollAngle;
            if (left > 0.05F) {
                this.rollAngle += Math.min(left, 0.35F);
            } else {
                this.rollAngle = upright;
                this.roll = Math.max(0.0F, this.roll - 0.18F);
            }
        }
        if (this.roll <= 0.0F && this.rollAngle > Mth.TWO_PI * 64.0F) {
            this.rollAngle = this.rollAngleO = 0.0F;
        }
    }

    /** Client: pollen drifts off the flowers on its back, buds twinkle, petals shake loose as it walks. */
    private void gardenEffects() {
        double s = this.getAgeScale();
        double top = this.getY() + 3.0 * s;
        if (this.random.nextInt(12) == 0) {
            this.level().addParticle(ModParticles.DREAM_POLLEN.get(), this.getRandomX(0.6), top + this.random.nextDouble() * 0.5, this.getRandomZ(0.6),
                    0.0, 0.01, 0.0);
        }
        if (this.random.nextInt(20) == 0) {
            this.level().addParticle(ModParticles.GLOW_DUST.get(), this.getRandomX(0.6), top + 0.3, this.getRandomZ(0.6), 0.0, 0.0, 0.0);
        }
        boolean moving = this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-3;
        if ((moving && this.random.nextInt(10) == 0) || (this.isDancing() && this.random.nextInt(3) == 0)) {
            this.level().addParticle(this.isWhite() ? ParticleTypes.SNOWFLAKE : ModParticles.WISHWOOD_LEAF.get(), this.getRandomX(0.8), top,
                    this.getRandomZ(0.8), (this.random.nextDouble() - 0.5) * 0.05, 0.02, (this.random.nextDouble() - 0.5) * 0.05);
        }
        if (this.isDancing() && this.random.nextInt(4) == 0) {
            this.level().addParticle(ModParticles.STAR_SPARKLE.get(), this.getRandomX(1.2), top + this.random.nextDouble(), this.getRandomZ(1.2), 0.0, 0.0,
                    0.0);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EVENT_DRINK -> this.drinkAnimation.start(this.tickCount);
            case EVENT_SPRAY -> this.sprayAnimation.start(this.tickCount);
            case EVENT_STOMP -> this.stompAnimation.start(this.tickCount);
            case EVENT_STOMP_IMPACT -> this.impactFx();
            case EVENT_SLAP -> this.slapAnimation.start(this.tickCount);
            case EVENT_DRUM -> this.drumAnimation.start(this.tickCount);
            case EVENT_SHAKE -> {
                this.shakeAnimation.start(this.tickCount);
                this.shakeGarden();
            }
            case EVENT_SNIFF -> this.sniffAnimation.start(this.tickCount);
            case EVENT_PUFF -> {
                this.puffAnimation.start(this.tickCount);
                this.puffTrunk();
            }
            case EVENT_LAY -> {
                for (int i = 0; i < 16; i++) {
                    this.level().addParticle(ParticleTypes.HEART, this.getRandomX(1.0), this.getY() + 1.0 + this.random.nextDouble(), this.getRandomZ(1.0),
                            0, 0.05, 0);
                }
            }
            default -> super.handleEntityEvent(id);
        }
    }

    /** Client: the slam shakes the ground under everyone near and throws up a ring of dust. */
    private void impactFx() {
        boolean baby = this.isBaby();
        com.thesift.world.Rumble.at(this.position(), baby ? 0.4F : 1.4F, baby ? 10.0F : 22.0F, baby ? 6 : 12);
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        double s = this.getAgeScale();
        double cx = this.getX() - Mth.sin(yaw) * 0.8 * s;
        double cz = this.getZ() + Mth.cos(yaw) * 0.8 * s;
        for (int i = 0; i < 36; i++) {
            double a = i * Mth.TWO_PI / 36.0;
            double c = Math.cos(a);
            double sn = Math.sin(a);
            this.level().addParticle(ParticleTypes.POOF, cx + c * 1.4 * s, this.getY() + 0.15, cz + sn * 1.4 * s, c * 0.25, 0.02, sn * 0.25);
        }
    }

    /** Client: the shake flings petals, leaves and pollen (snow, on a white Stomper) off its back. */
    private void shakeGarden() {
        double s = this.getAgeScale();
        double top = this.getY() + 2.9 * s;
        for (int i = 0; i < 26; i++) {
            this.level().addParticle(i % 3 == 0 ? ModParticles.DREAM_POLLEN.get() : (this.isWhite() ? ParticleTypes.SNOWFLAKE : ModParticles.WISHWOOD_LEAF.get()),
                    this.getRandomX(1.1), top + this.random.nextDouble() * 0.4, this.getRandomZ(1.1), (this.random.nextDouble() - 0.5) * 0.3,
                    0.08 + this.random.nextDouble() * 0.12, (this.random.nextDouble() - 0.5) * 0.3);
        }
        for (int i = 0; i < 10; i++) {
            this.level().addParticle(this.getChrome() > 0.1F ? ModParticles.CHROME_DROPLET.get() : ParticleTypes.SPLASH, this.getRandomX(1.2),
                    this.getY() + (0.6 + this.random.nextDouble()) * s, this.getRandomZ(1.2), (this.random.nextDouble() - 0.5) * 0.4, 0.15,
                    (this.random.nextDouble() - 0.5) * 0.4);
        }
    }

    /** Client: a snort of air out of the trunk (Chrome mist when it is full). */
    private void puffTrunk() {
        Vec3 tip = this.trunkRoot().add(0.0, -1.0 * this.getAgeScale(), 0.0).add(this.forward(0.4));
        for (int i = 0; i < 6; i++) {
            this.level().addParticle(ParticleTypes.CLOUD, tip.x, tip.y, tip.z, (this.random.nextDouble() - 0.5) * 0.06, 0.06 + this.random.nextDouble() * 0.06,
                    (this.random.nextDouble() - 0.5) * 0.06);
        }
        float chrome = this.getChrome();
        if (chrome > 0.05) {
            for (int i = 0; i < 2 + (int) (chrome * 6); i++) {
                this.level().addParticle(ModParticles.CHROME_DROPLET.get(), tip.x, tip.y, tip.z, (this.random.nextDouble() - 0.5) * 0.12,
                        0.1 + this.random.nextDouble() * 0.15, (this.random.nextDouble() - 0.5) * 0.12);
            }
            this.level().addParticle(ModParticles.SIFT_MIST.get(), tip.x, tip.y + 0.3, tip.z, 0, 0.01, 0);
        }
    }

    /** {@code blocks} ahead along the body's facing, scaled for babies. */
    private Vec3 forward(double blocks) {
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        double s = this.getAgeScale();
        return new Vec3(-Mth.sin(yaw) * blocks * s, 0.0, Mth.cos(yaw) * blocks * s);
    }

    /** Where the trunk leaves the face (tools/stomper.py: 2 blocks ahead, 2.1 up). */
    private Vec3 trunkRoot() {
        return this.position().add(this.forward(1.97)).add(0.0, 2.13 * this.getAgeScale(), 0.0);
    }

    /** Where the trunk tip is in its usual poses, {@code forward} blocks ahead of the body's centre. */
    private Vec3 headPos(double forward) {
        return this.position().add(this.forward(forward)).add(0.0, 1.4 * this.getAgeScale(), 0.0);
    }

    // ------------------------------------------------------------------ combat

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (this.isDancing()) {
            this.entityData.set(DANCE, 0);
        }
        boolean hurt = super.hurtServer(level, source, damage);
        LivingEntity victim = this.held();
        if (hurt && victim != null && StomperRig.holding(this.getGrabPhase()) && this.getGrabPhase() != StomperRig.SLAM
                && this.getGrabPhase() != StomperRig.FLING) {
            // the victim fights free by hitting it (two blows, or one hard one); friends can force it to let go
            if (source.getEntity() == victim) {
                this.struggle += damage >= 5.0F ? 2 : 1;
            } else {
                this.heldDamage += damage;
            }
            if (this.struggle >= 2 || this.heldDamage >= 8.0F) {
                this.release(level, victim, 0);
                this.setGrabPhase(StomperRig.DROP);
                this.grabCooldown = 160;
            }
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        LivingEntity victim = this.held();
        if (victim != null && this.level() instanceof ServerLevel server) {
            this.release(server, victim, 0);
        }
        super.die(source);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        level.broadcastEntityEvent(this, EVENT_SLAP);
        this.playSound(ModSounds.STOMPER_TRUMPET.get(), 0.8F, 1.3F);
        boolean hit = super.doHurtTarget(level, target);
        if (hit) {
            target.push(0.0, 0.35, 0.0);
        }
        return hit;
    }

    private @Nullable LivingEntity findSprayTarget() {
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive() && this.distanceTo(target) < SPRAY_RANGE + 2.0) {
            return target;
        }
        List<Mob> near = this.level().getEntitiesOfClass(Mob.class, this.getBoundingBox().inflate(SPRAY_RANGE),
                e -> e instanceof Enemy && e.isAlive() && this.hasLineOfSight(e));
        Mob best = null;
        double bestD = Double.MAX_VALUE;
        for (Mob m : near) {
            double d = this.distanceToSqr(m);
            if (d < bestD) {
                bestD = d;
                best = m;
            }
        }
        return best;
    }

    /** One tick of the Chrome stream from the raised trunk towards the target. */
    private void sprayTick(ServerLevel level, LivingEntity target, int sprayTick) {
        Vec3 tip = this.trunkRoot().add(this.forward(0.9)).add(0.0, 1.9 * this.getAgeScale(), 0.0);
        Vec3 aim = target.getBoundingBox().getCenter().subtract(tip);
        if (aim.lengthSqr() < 1.0E-4) {
            return;
        }
        Vec3 dir = aim.normalize();
        for (int i = 0; i < 7; i++) {
            double sp = 0.9 + this.random.nextDouble() * 0.6;
            level.sendParticles(ModParticles.CHROME_DROPLET.get(), tip.x, tip.y, tip.z, 0, dir.x * sp + (this.random.nextDouble() - 0.5) * 0.15,
                    dir.y * sp + 0.08 + (this.random.nextDouble() - 0.5) * 0.15, dir.z * sp + (this.random.nextDouble() - 0.5) * 0.15, 1.0);
        }
        if (sprayTick % 2 == 0) {
            Vec3 mid = tip.add(dir.scale(Math.min(SPRAY_RANGE, aim.length()) * this.random.nextDouble()));
            level.sendParticles(ModParticles.CHROME_BUBBLE.get(), mid.x, mid.y, mid.z, 3, 0.2, 0.2, 0.2, 0.02);
            level.sendParticles(ParticleTypes.SPLASH, mid.x, mid.y, mid.z, 4, 0.3, 0.2, 0.3, 0.1);
        }
        if (sprayTick % 4 != 0) {
            return;
        }
        AABB box = new AABB(tip, tip.add(dir.scale(SPRAY_RANGE))).inflate(2.0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, x -> x != this && x.isAlive())) {
            if (e instanceof Stomper || isPet(e) || (e instanceof Player && e != target) || (this.isTame() && this.isOwnedBy(e))) {
                continue;
            }
            Vec3 to = e.getBoundingBox().getCenter().subtract(tip);
            double along = to.dot(dir);
            if (along <= 0.0 || along > SPRAY_RANGE) {
                continue;
            }
            double off = to.subtract(dir.scale(along)).length();
            if (off < 0.6 + along * 0.18 + e.getBbWidth() * 0.5) {
                if (e.hurtServer(level, this.damageSources().mobAttack(this), 3.0F)) {
                    e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 1));
                    e.push(dir.x * 0.25, 0.08, dir.z * 0.25);
                    level.sendParticles(ModParticles.CHROME_DROPLET.get(), e.getX(), e.getY() + e.getBbHeight() * 0.6, e.getZ(), 12, 0.3, 0.3, 0.3, 0.1);
                    level.sendParticles(ParticleTypes.SPLASH, e.getX(), e.getY() + e.getBbHeight() * 0.6, e.getZ(), 10, 0.3, 0.3, 0.3, 0.1);
                }
            }
        }
    }

    // ------------------------------------------------------------------ breeding, taming, riding

    /** Two well-fed Stompers lay an egg instead of having a live baby. */
    @Override
    public void spawnChildFromBreeding(ServerLevel level, Animal partner) {
        this.finalizeSpawnChildFromBreeding(level, partner, null);
        Vec3 mid = this.position().add(partner.position()).scale(0.5);
        ItemStack egg = new ItemStack(ModItems.STOMPER_EGG.get());
        int coat = partner instanceof Stomper other && this.random.nextBoolean() ? other.getCoat() : this.getCoat();
        CustomData.update(DataComponents.CUSTOM_DATA, egg, tag -> tag.putInt(EGG_COAT, coat));
        net.minecraft.world.entity.item.ItemEntity drop = this.spawnAtLocation(level, egg, 1.0F);
        if (drop != null) {
            drop.setPos(mid.x, mid.y + 1.0, mid.z);
            drop.setDeltaMovement((this.random.nextDouble() - 0.5) * 0.2, 0.35, (this.random.nextDouble() - 0.5) * 0.2);
        }
        level.broadcastEntityEvent(this, EVENT_LAY);
        level.sendParticles(ParticleTypes.POOF, mid.x, mid.y + 0.8, mid.z, 20, 0.6, 0.4, 0.6, 0.05);
        level.sendParticles(ModParticles.STAR_SPARKLE.get(), mid.x, mid.y + 1.0, mid.z, 24, 0.8, 0.6, 0.8, 0.05);
        level.sendParticles(ModParticles.RESONANCE_RING.get(), mid.x, mid.y + 0.1, mid.z, 0, 2.5, 0.0, 0.0, 1.0);
        this.playSound(ModSounds.STOMPER_LAY.get(), 1.2F, 0.9F);
        this.playSound(ModSounds.STOMPER_HAPPY.get(), 1.2F, 1.1F);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (this.isFood(stack) && this.isBaby() && !this.isTame()) {
            if (this.level() instanceof ServerLevel server) {
                this.usePlayerItem(player, hand, stack);
                this.playSound(ModSounds.STOMPER_HAPPY.get(), 1.0F, 1.6F);
                if (this.random.nextInt(3) == 0) {
                    this.tame(player);
                    this.setOrderedToSit(false);
                    this.setPersistenceRequired();
                    server.broadcastEntityEvent(this, (byte) 7);
                    server.sendParticles(ModParticles.SIFT_NOTE.get(), this.getX(), this.getY() + 1.4, this.getZ(), 0, 0.5, 0.0, 0.0, 1.0);
                    player.sendOverlayMessage(Component.translatable("message.thesift.stomper.tamed"));
                } else {
                    server.broadcastEntityEvent(this, (byte) 6);
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (player.getVehicle() == this) {
            return InteractionResult.PASS;
        }
        if (this.isTame() && this.isOwnedBy(player) && !this.isFood(stack)) {
            if (player.isSecondaryUseActive()) {
                if (!this.level().isClientSide()) {
                    this.setOrderedToSit(!this.isOrderedToSit());
                    this.getNavigation().stop();
                    this.setTarget(null);
                }
                return InteractionResult.SUCCESS;
            }
            if (!this.isVehicle() && stack.isEmpty()) {
                if (!this.level().isClientSide()) {
                    this.setOrderedToSit(false);
                    player.startRiding(this);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        // never a victim held in the trunk (nor any mob passenger): only its owner on its back steers it
        return this.isTame() && this.getGrabPhase() == StomperRig.NONE && this.getFirstPassenger() instanceof Player player ? player : null;
    }

    @Override
    protected Vec3 getRiddenInput(Player controller, Vec3 selfInput) {
        float forward = controller.zza;
        if (forward <= 0.0F) {
            forward *= 0.3F;
        }
        return new Vec3(controller.xxa * 0.4F, 0.0, forward);
    }

    @Override
    protected float getRiddenSpeed(Player controller) {
        // a baby trots along briskly under its little rider
        return (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED) * (this.isBaby() ? 1.9F : 1.4F);
    }

    @Override
    protected void tickRidden(Player controller, Vec3 riddenInput) {
        super.tickRidden(controller, riddenInput);
        this.setRot(controller.getYRot(), controller.getXRot() * 0.5F);
        this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();
        if (this.isLocalInstanceAuthoritative() && this.onGround()) {
            if (this.playerJumpPending > 0.0F) {
                Vec3 v = this.getDeltaMovement();
                double up = (this.isBaby() ? 0.55 : 0.45) + 0.25 * this.playerJumpPending;
                this.setDeltaMovement(v.x, up, v.z);
                if (riddenInput.z > 0.0) {
                    float yaw = this.getYRot() * Mth.DEG_TO_RAD;
                    this.setDeltaMovement(this.getDeltaMovement().add(-0.3F * Mth.sin(yaw) * this.playerJumpPending, 0.0,
                            0.3F * Mth.cos(yaw) * this.playerJumpPending));
                }
                this.needsSync = true;
            }
            this.playerJumpPending = 0.0F;
        }
    }

    // ---- the rider's jump key (charges like a horse's)

    @Override
    public void onPlayerJump(int jumpAmount) {
        this.playerJumpPending = this.getPlayerJumpPendingScale(Math.max(0, jumpAmount));
    }

    @Override
    public boolean canJump() {
        return this.isTame() && this.isVehicle();
    }

    @Override
    public void handleStartJump(int jumpScale) {
        this.playSound(ModSounds.STOMPER_PUFF.get(), 0.6F, this.isBaby() ? 1.6F : 1.1F);
    }

    @Override
    public void handleStopJump() {
    }

    // ------------------------------------------------------------------ sounds, save, death

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ModSounds.STOMPER_AMBIENT.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 260;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.STOMPER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.STOMPER_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(ModSounds.STOMPER_STEP.get(), this.isBaby() ? 0.4F : 0.9F, this.isBaby() ? 1.4F : 0.8F);
    }

    @Override
    public float getVoicePitch() {
        return this.isBaby() ? 1.5F + this.random.nextFloat() * 0.2F : 0.9F + (this.random.nextFloat() - this.random.nextFloat()) * 0.1F;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Chrome", Math.round(this.getChrome() * 1000.0F));
        output.putInt("DrinkCooldown", this.drinkCooldown);
        output.putInt("Coat", this.getCoat());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setChrome(input.getIntOr("Chrome", 0) / 1000.0F);
        this.drinkCooldown = input.getIntOr("DrinkCooldown", 200);
        this.setCoat(input.getIntOr("Coat", NORMAL));
    }

    /** A big pop of fur, petals and hearts. */
    @Override
    public void makePoofParticles() {
        KillBurst.pop(this, this.isWhite() ? 0xEEF3F9 : 0x2B8F86, this.isWhite() ? 0xB6A4B6 : 0x902F3F, KillBurst.HEART,
                this.isWhite() ? ParticleTypes.SNOWFLAKE : ModParticles.WISHWOOD_LEAF.get());
    }

    // ------------------------------------------------------------------ goals

    /**
     * Standing about, it now and then shakes its whole garden out like a wet dog (always, soon after
     * a swim), or lowers its trunk to snuffle at the flowers by its feet.
     */
    private final class IdleGoal extends Goal {
        private int ticks;
        private boolean sniffing;

        IdleGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Stomper s = Stomper.this;
            boolean dripping = s.wet > 0 && !s.isInFluidType();
            return s.onGround() && !s.isVehicle() && !s.isBusy() && !s.isDancing() && !s.isDrinking() && !s.isInSittingPose() && s.getTarget() == null
                    && s.drumLeft < 0 && (dripping || s.getNavigation().isDone()) && s.random.nextInt(dripping ? 15 : 500) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return this.ticks > 0;
        }

        @Override
        public void start() {
            Stomper s = Stomper.this;
            boolean shake = s.wet > 0 || s.random.nextInt(3) == 0;
            s.wet = 0;
            this.ticks = shake ? 24 : 44;
            this.sniffing = !shake;
            s.getNavigation().stop();
            s.level().broadcastEntityEvent(s, shake ? EVENT_SHAKE : EVENT_SNIFF);
            if (shake) {
                s.playSound(ModSounds.STOMPER_SHAKE.get(), s.isBaby() ? 0.6F : 1.0F, s.isBaby() ? 1.4F : 0.9F + s.random.nextFloat() * 0.2F);
            } else {
                s.playSound(ModSounds.STOMPER_PUFF.get(), 0.5F, 1.4F);
            }
        }

        @Override
        public void tick() {
            Stomper s = Stomper.this;
            this.ticks--;
            s.getNavigation().stop();
            // the snuffle: little puffs of pollen at the trunk tip
            if (this.sniffing && this.ticks % 8 == 0 && this.ticks > 6 && this.ticks < 40 && s.level() instanceof ServerLevel server) {
                Vec3 tip = s.headPos(2.0).add(0.0, -1.0 * s.getAgeScale(), 0.0);
                server.sendParticles(ModParticles.DREAM_POLLEN.get(), tip.x, tip.y, tip.z, 3, 0.2, 0.05, 0.2, 0.01);
            }
        }
    }

    /** Dances on the spot while a drum plays: sways, waves its trunk and stomps at the end. */
    private final class DanceGoal extends Goal {
        DanceGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return Stomper.this.isDancing() && !Stomper.this.isVehicle();
        }

        @Override
        public void start() {
            Stomper.this.getNavigation().stop();
        }

        @Override
        public void tick() {
            Stomper s = Stomper.this;
            s.getNavigation().stop();
            // turn slowly on the spot in time with the beat
            int t = s.getDanceTicks();
            if (t > STOMP_1 + 8) {
                float turn = Mth.sin(t * 0.12F) * 4.0F;
                s.setYRot(s.getYRot() + turn);
                s.yBodyRot = s.getYRot();
                s.yHeadRot = s.getYRot();
            }
        }
    }

    /** Rears up, raises its trunk (the telegraph), then hoses the target with Chrome. */
    private final class SprayGoal extends Goal {
        private @Nullable LivingEntity target;
        private int ticks;

        SprayGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Stomper s = Stomper.this;
            if (s.sprayCooldown > 0 || s.getChrome() < 0.2F || s.isDancing() || s.isVehicle() || s.isBaby() || s.isBusy()) {
                return false;
            }
            this.target = s.findSprayTarget();
            return this.target != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.target != null && this.target.isAlive() && this.ticks < SPRAY_WINDUP + SPRAY_LENGTH && Stomper.this.getChrome() > 0.0F;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            Stomper s = Stomper.this;
            this.ticks = 0;
            s.getNavigation().stop();
            s.level().broadcastEntityEvent(s, EVENT_SPRAY);
            s.playSound(ModSounds.STOMPER_TRUMPET.get(), 1.6F, 0.8F);
        }

        @Override
        public void tick() {
            Stomper s = Stomper.this;
            if (this.target == null || !(s.level() instanceof ServerLevel server)) {
                return;
            }
            s.getNavigation().stop();
            s.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
            // face the target with the whole body so the trunk points at it
            double dx = this.target.getX() - s.getX();
            double dz = this.target.getZ() - s.getZ();
            float want = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
            s.setYRot(s.getYRot() + Mth.clamp(Mth.wrapDegrees(want - s.getYRot()), -8.0F, 8.0F));
            s.yBodyRot = s.getYRot();
            this.ticks++;
            if (this.ticks < SPRAY_WINDUP) {
                // the telegraph: drips from the raised trunk and a gurgle
                Vec3 tip = s.trunkRoot().add(s.forward(0.9)).add(0.0, 1.9 * s.getAgeScale(), 0.0);
                server.sendParticles(ModParticles.CHROME_DROPLET.get(), tip.x, tip.y, tip.z, 2, 0.15, 0.1, 0.15, 0.02);
                if (this.ticks == SPRAY_WINDUP - 4) {
                    s.playSound(ModSounds.STOMPER_DRINK.get(), 1.0F, 1.4F);
                }
                return;
            }
            if (this.ticks == SPRAY_WINDUP) {
                s.playSound(ModSounds.STOMPER_SPRAY.get(), 1.6F, 0.9F);
            }
            s.sprayTick(server, this.target, this.ticks - SPRAY_WINDUP);
            s.setChrome(s.getChrome() - 0.011F);
        }

        @Override
        public void stop() {
            Stomper.this.sprayCooldown = 70 + Stomper.this.random.nextInt(40);
            this.target = null;
        }
    }

    /** Plods to the nearest Chrome (or water if there is none) and drinks through the trunk. */
    private final class DrinkGoal extends Goal {
        private @Nullable BlockPos fluid;
        private boolean chrome;
        private int timeout;
        private int drinkTicks;

        DrinkGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Stomper s = Stomper.this;
            if (s.drinkCooldown > 0 || s.getChrome() > 0.9F || s.isDancing() || s.isVehicle() || s.getTarget() != null || s.isOrderedToSit()
                    || s.random.nextInt(40) != 0) {
                return false;
            }
            this.fluid = this.find(true);
            this.chrome = this.fluid != null;
            if (this.fluid == null) {
                this.fluid = this.find(false);
            }
            if (this.fluid == null) {
                s.drinkCooldown = 300;
                return false;
            }
            return true;
        }

        private @Nullable BlockPos find(boolean wantChrome) {
            Stomper s = Stomper.this;
            BlockPos here = s.blockPosition();
            BlockPos best = null;
            double bestD = Double.MAX_VALUE;
            for (BlockPos p : BlockPos.betweenClosed(here.offset(-14, -4, -14), here.offset(14, 3, 14))) {
                FluidState fs = s.level().getFluidState(p);
                if (fs.isEmpty() || !s.level().getBlockState(p.above()).isAir()) {
                    continue;
                }
                boolean ok = wantChrome ? fs.getType().isSame(ModFluids.CHROME.get()) : fs.is(FluidTags.WATER);
                if (!ok) {
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
        public boolean canContinueToUse() {
            Stomper s = Stomper.this;
            return this.fluid != null && this.timeout > 0 && s.getTarget() == null && !s.isDancing() && !s.isVehicle();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            this.timeout = 240;
            this.drinkTicks = 0;
            if (this.fluid != null) {
                Stomper.this.getNavigation().moveTo(this.fluid.getX() + 0.5, this.fluid.getY() + 1.0, this.fluid.getZ() + 0.5, 0.9);
            }
        }

        @Override
        public void tick() {
            Stomper s = Stomper.this;
            if (this.fluid == null || !(s.level() instanceof ServerLevel server)) {
                return;
            }
            this.timeout--;
            Vec3 at = Vec3.atCenterOf(this.fluid);
            s.getLookControl().setLookAt(at.x, at.y, at.z);
            double dx = at.x - s.getX();
            double dz = at.z - s.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (this.drinkTicks == 0) {
                if (dist > 3.4 * s.getAgeScale() + 0.6) {
                    if (s.getNavigation().isDone()) {
                        s.getNavigation().moveTo(at.x, at.y + 0.5, at.z, 0.9);
                    }
                    return;
                }
                // arrived at the shore: dip the trunk
                s.getNavigation().stop();
                this.drinkTicks = 60;
                this.timeout = 70;
                s.drinking = true;
                server.broadcastEntityEvent(s, EVENT_DRINK);
            }
            s.getNavigation().stop();
            this.drinkTicks--;
            Vec3 surface = new Vec3(at.x, this.fluid.getY() + 0.9, at.z);
            if (this.drinkTicks % 2 == 0 && this.drinkTicks < 52) {
                Vec3 mouth = s.headPos(1.6);
                Vec3 up = mouth.subtract(surface).scale(0.08);
                server.sendParticles(this.chrome ? ModParticles.CHROME_DROPLET.get() : ParticleTypes.SPLASH, surface.x, surface.y, surface.z, 0,
                        up.x, up.y + 0.25, up.z, 1.0);
                server.sendParticles(this.chrome ? ModParticles.CHROME_BUBBLE.get() : ParticleTypes.BUBBLE_POP, surface.x, surface.y, surface.z, 2, 0.3,
                        0.05, 0.3, 0.01);
            }
            if (this.drinkTicks % 15 == 0) {
                s.playSound(ModSounds.STOMPER_DRINK.get(), 1.0F, 0.8F + s.random.nextFloat() * 0.2F);
            }
            if (this.drinkTicks <= 0) {
                s.setChrome(s.getChrome() + (this.chrome ? 0.6F : 0.3F));
                server.sendParticles(ModParticles.STAR_SPARKLE.get(), s.getX(), s.getY() + 2.0, s.getZ(), 10, 0.8, 0.4, 0.8, 0.02);
                s.playSound(ModSounds.STOMPER_HAPPY.get(), 1.0F, 1.0F);
                this.fluid = null;
            }
        }

        @Override
        public void stop() {
            Stomper s = Stomper.this;
            s.drinking = false;
            s.drinkCooldown = 500 + s.random.nextInt(700);
            this.fluid = null;
        }
    }

    /**
     * S1 the Stomper's fight: it closes in on its foe, then grabs it with its trunk, rears up and
     * stomps, or swats it - whichever fits how close it is and what it can lift. It stands its
     * ground while a grab or a stomp is under way.
     */
    private final class AttackGoal extends Goal {
        private int repath;
        private int swing;

        AttackGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Stomper s = Stomper.this;
            LivingEntity t = s.getTarget();
            return t != null && t.isAlive() && !s.isDancing() && s.getControllingPassenger() == null && !s.isInSittingPose()
                    && !(t instanceof Player p && (p.isSpectator() || p.isCreative()));
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse() || Stomper.this.isBusy();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            this.repath = 0;
            this.swing = 10;
            Stomper.this.setAggressive(true);
        }

        @Override
        public void stop() {
            Stomper.this.setAggressive(false);
            Stomper.this.getNavigation().stop();
        }

        @Override
        public void tick() {
            Stomper s = Stomper.this;
            LivingEntity t = s.getTarget();
            if (s.isBusy() || t == null || !(s.level() instanceof ServerLevel server)) {
                s.getNavigation().stop();
                return;
            }
            s.getLookControl().setLookAt(t, 30.0F, 30.0F);
            double gap = s.distanceTo(t) - (s.getBbWidth() + t.getBbWidth()) * 0.5;
            if (!s.isBaby()) {
                if (s.grabCooldown <= 0 && gap < 2.6 && s.canGrab(t) && s.hasLineOfSight(t)) {
                    s.startGrab(t);
                    return;
                }
                if (s.stompCooldown <= 0 && gap < 2.2) {
                    s.stompCooldown = 90 + s.random.nextInt(50);
                    s.startStomp(1.0F, !s.isTame(), false);
                    return;
                }
            }
            if (this.swing > 0) {
                this.swing--;
            }
            if (gap < (s.isBaby() ? 0.8 : 1.4)) {
                s.getNavigation().stop();
                if (this.swing == 0) {
                    this.swing = 24;
                    s.doHurtTarget(server, t);
                }
            } else if (--this.repath <= 0) {
                this.repath = 10;
                s.getNavigation().moveTo(t, 1.1);
            }
        }
    }
}
