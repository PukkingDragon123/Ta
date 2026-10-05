package com.thesift.entity.swamp;

import com.thesift.entity.HopMoveControl;
import com.thesift.entity.KillBurst;
import com.thesift.entity.Spring;
import com.thesift.music.Instrument;
import com.thesift.music.Song;
import com.thesift.registry.ModCaveCreatures;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * CR4 The Cypole: the one-eyed cymbal frog of the Sculk Swamp. A big squat frog with a single
 * golden eye on top of its head, brass tympana on its cheeks and two brass cymbal plates hanging
 * either side of its vocal sac.
 *
 * <p><b>Neutral, and territorial.</b> It minds its own business - hops about, floats in Sculk Water
 * with only its eye showing and croaks in rhythm with every other Cypole around (its plates tick
 * together like a hi-hat; the swamp sings in bars, the same beat for every frog nearby). Come within
 * {@link #WARN_RANGE} blocks (fewer if you sneak) and it turns to face you and rattles its plates;
 * come closer still, or linger, and it fights:
 * <ul>
 *   <li><b>the clash</b>: it rears up, swings its plates wide and crashes them together - a ring
 *   shockwave races out across the ground and water ({@link #RING_SPEED} blocks a tick, out to
 *   {@link #RING_RADIUS}), knocking back and hurting whatever stands on it. Jump it: whatever is in the
 *   air as the ring passes is spared, and walls shield you.</li>
 *   <li><b>the tongue</b>: it squats, stares, then shoots its long sticky tongue at you (it aims where
 *   you are as it locks on, so keep moving sideways) and yanks you right up to its plates. A raised
 *   shield slaps the tongue away.</li>
 * </ul>
 * Hit one and the Cypoles around it join in. Finish a song on a drum or on chimes near an
 * angry Cypole and it calms down and leaves you be for a while; they may even join your band (see
 * {@link ModCaveCreatures}). Never angry in Peaceful.
 */
public class Cypole extends PathfinderMob implements HopMoveControl.Hopper {
    /** What it is doing (synced; the client times its animations from the moment this changes). */
    public static final int NONE = 0;
    public static final int WARN = 1;
    public static final int CLASH = 2;
    public static final int TONGUE = 3;

    /** The clash: the plates come together at CRASH; it has recovered by END. */
    public static final int CLASH_CRASH = 14;
    public static final int CLASH_END = 30;
    /** The tongue: it locks its aim at LOCK, shoots at FIRE; the tongue is out FLY ticks later. */
    public static final int TONGUE_LOCK = 6;
    public static final int TONGUE_FIRE = 9;
    public static final int TONGUE_FLY = 3;
    /** The longest it hauls something in, in ticks. */
    public static final int TONGUE_YANK = 12;
    public static final double TONGUE_RANGE = 11.0;
    /** The ring shockwave: its radius when the plates meet, how fast it spreads and how far. */
    public static final float RING_START = 0.8F;
    public static final float RING_SPEED = 0.5F;
    public static final float RING_RADIUS = 7.0F;
    /** Territory: it rattles at a player this close (sneaking: SNEAK_WARN), and fights one at ANGER_RANGE. */
    public static final double WARN_RANGE = 7.5;
    public static final double SNEAK_WARN = 4.0;
    public static final double ANGER_RANGE = 4.0;
    /** The chorus: one step every BEAT ticks, eight steps a bar. */
    public static final int BEAT = 10;
    /** The mouth, from the model (tools/cypole.py MOUTH): this far forward and up from its feet. */
    public static final double MOUTH_FORWARD = 0.66;
    public static final double MOUTH_UP = 0.52;
    /** Which steps of a bar each voice croaks on (bit i = step i): on the beat, the off-beat, a tresillo... */
    private static final int[] PATTERNS = {0b00010001, 0b01000100, 0b01001001, 0b10100010, 0b00100101};

    private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(Cypole.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TONGUE_TARGET = SynchedEntityData.defineId(Cypole.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TONGUE_AIM = SynchedEntityData.defineId(Cypole.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ANGRY = SynchedEntityData.defineId(Cypole.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CROUCH = SynchedEntityData.defineId(Cypole.class, EntityDataSerializers.BOOLEAN);
    private static final byte EVENT_CROAK = -104;
    private static final byte EVENT_CRASH = -105;
    private static final float TONGUE_DAMAGE = 2.0F;

    // ---- client side
    public final AnimationState croakAnimation = new AnimationState();
    public final AnimationState clashAnimation = new AnimationState();
    public final AnimationState tongueAnimation = new AnimationState();
    public final AnimationState warnAnimation = new AnimationState();
    public final AnimationState crashAnimation = new AnimationState();
    /** Squash and stretch of the hops, and the ringing of the plates after a crash. */
    public final Spring squash = new Spring(0.3F, 0.18F);
    public final Spring ring = new Spring(0.45F, 0.08F);
    /** When the tongue began to come back in (client ticks), or -1. */
    public int tongueBack = -1;
    public int blinkTicks;
    public float crouch;
    public float crouchO;
    /** 0 sitting or floating .. 1 in mid-hop (legs flung back), eased. */
    public float air;
    public float airO;
    private int lastAction;
    private boolean wasOnGround = true;
    private double lastVelY;

    // ---- server side
    private int actionTicks;
    private int clashCooldown = 40;
    private int tongueCooldown = 40;
    private int attackRest;
    private int unseenTicks;
    private @Nullable LivingEntity tongueTarget;
    private @Nullable Vec3 tongueAim;
    private @Nullable LivingEntity yanked;
    private int yankTicks;
    private final List<Ring> rings = new ArrayList<>();
    private @Nullable UUID truce;
    private long truceUntil;

    /** One shockwave ring spreading out from where the plates crashed. */
    private static final class Ring {
        final Vec3 centre;
        float radius = RING_START;
        final Set<Integer> passed = new HashSet<>();

        Ring(Vec3 centre) {
            this.centre = centre;
        }
    }

    public Cypole(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.moveControl = new HopMoveControl<>(this);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
        this.xpReward = 7;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 22.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
                .add(Attributes.JUMP_STRENGTH, 0.46)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ACTION, NONE);
        builder.define(TONGUE_TARGET, -1);
        builder.define(TONGUE_AIM, 0);
        builder.define(ANGRY, false);
        builder.define(CROUCH, false);
    }

    /** S1 never freeze: keep strolling even far from every player. */
    @Override
    public int getNoActionTime() {
        return 0;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new FightGoal());
        this.goalSelector.addGoal(2, new WarnGoal());
        this.goalSelector.addGoal(4, new SoakGoal());
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.8, 60) {
            @Override
            public boolean canUse() {
                return Cypole.this.getAction() == NONE && super.canUse();
            }
        });
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
    }

    // ------------------------------------------------------------------ state

    public int getAction() {
        return this.entityData.get(ACTION);
    }

    private void setAction(int action) {
        this.entityData.set(ACTION, action);
        this.actionTicks = 0;
    }

    public boolean isAngry() {
        return this.entityData.get(ANGRY);
    }

    public boolean isCrouching() {
        return this.entityData.get(CROUCH);
    }

    /** The creature its tongue is stuck to (client: for drawing it), or null. */
    public @Nullable Entity tongueTarget() {
        int id = this.entityData.get(TONGUE_TARGET);
        return id < 0 ? null : this.level().getEntity(id);
    }

    /** Where the tongue was aimed (a world point), or null if it has not been aimed. */
    public @Nullable Vec3 tongueAim() {
        int packed = this.entityData.get(TONGUE_AIM);
        if (packed == 0) {
            return null;
        }
        double dx = ((packed & 0xFF) - 128) / 8.0;
        double dy = ((packed >> 8 & 0xFF) - 128) / 8.0;
        double dz = ((packed >> 16 & 0xFF) - 128) / 8.0;
        return this.position().add(dx, dy, dz);
    }

    private void setTongueAim(Vec3 at) {
        Vec3 d = at.subtract(this.position());
        int x = Mth.clamp((int) Math.round(d.x * 8.0), -127, 127) + 128;
        int y = Mth.clamp((int) Math.round(d.y * 8.0), -127, 127) + 128;
        int z = Mth.clamp((int) Math.round(d.z * 8.0), -127, 127) + 128;
        this.entityData.set(TONGUE_AIM, x | y << 8 | z << 16 | 1 << 24);
    }

    /** Where its mouth is now (the tongue comes out of here). */
    public Vec3 mouth() {
        return this.position().add(Vec3.directionFromRotation(0.0F, this.yBodyRot).scale(MOUTH_FORWARD)).add(0.0, MOUTH_UP, 0.0);
    }

    private int voice() {
        return Math.floorMod(this.getUUID().hashCode(), PATTERNS.length);
    }

    private float voicePitch() {
        return 0.82F + Math.floorMod(this.getUUID().hashCode() >> 8, 5) * 0.07F;
    }

    // ------------------------------------------------------------------ hopping

    @Override
    public int hopDelay() {
        return this.isAngry() ? 3 + this.random.nextInt(4) : 6 + this.random.nextInt(10);
    }

    @Override
    public int windup() {
        return 3;
    }

    @Override
    public void onWindup() {
        this.entityData.set(CROUCH, true);
    }

    @Override
    public void onHop() {
        this.entityData.set(CROUCH, false);
        this.playSound(ModCaveCreatures.CYPOLE_HOP.get(), 0.5F, 0.85F + this.random.nextFloat() * 0.2F);
    }

    @Override
    protected int calculateFallDamage(double fallDistance, float damageModifier) {
        return Math.max(0, super.calculateFallDamage(fallDistance, damageModifier) - 4);
    }

    /** A frog: it breathes under water as well as above it. */
    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    /** It floats low, the water up to its plates and only its eye and back showing. */
    @Override
    public double getFluidJumpThreshold() {
        return 0.55;
    }

    /** Like any creature of the swamp it stays where it was born. */
    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    // ------------------------------------------------------------------ the server tick

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.clashCooldown > 0) {
            this.clashCooldown--;
        }
        if (this.tongueCooldown > 0) {
            this.tongueCooldown--;
        }
        if (this.attackRest > 0) {
            this.attackRest--;
        }
        this.tickTemper(level);
        this.tickAction(level);
        this.tickRings(level);
        this.tickChorus(level);
    }

    /** Anger comes and goes: it calms down when its foe is gone, out of reach, or long out of sight. */
    private void tickTemper(ServerLevel level) {
        LivingEntity foe = this.getTarget();
        if (foe != null) {
            boolean gone = !foe.isAlive() || foe.level() != this.level() || this.distanceToSqr(foe) > 24.0 * 24.0
                    || foe instanceof Player p && (p.isCreative() || p.isSpectator() || level.getDifficulty() == Difficulty.PEACEFUL);
            this.unseenTicks = this.hasLineOfSight(foe) ? 0 : this.unseenTicks + 1;
            if (gone || this.unseenTicks > 200) {
                this.setTarget(null);
                foe = null;
            }
        }
        boolean angry = foe != null;
        if (angry != this.isAngry()) {
            this.entityData.set(ANGRY, angry);
            if (angry) {
                this.playSound(ModCaveCreatures.CYPOLE_ANGRY.get(), 1.2F, this.voicePitch() * 0.8F);
            }
        }
    }

    private void tickAction(ServerLevel level) {
        int action = this.getAction();
        if (action == NONE) {
            return;
        }
        this.actionTicks++;
        LivingEntity foe = this.getTarget();
        if (action == CLASH) {
            if (this.actionTicks < CLASH_CRASH && foe != null) {
                this.face(foe, 20.0F);
            } else if (this.actionTicks == CLASH_CRASH) {
                this.crash(level);
            } else if (this.actionTicks >= CLASH_END) {
                this.setAction(NONE);
            }
        } else if (action == TONGUE) {
            this.tickTongue(level);
        } else if (action == WARN && this.actionTicks > 200) {
            this.setAction(NONE); // the warning goal lost track of it
        }
    }

    private void face(Entity e, float maxTurn) {
        double dx = e.getX() - this.getX();
        double dz = e.getZ() - this.getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        float turned = Mth.approachDegrees(this.getYRot(), yaw, maxTurn);
        this.setYRot(turned);
        this.setYBodyRot(turned);
        this.setYHeadRot(turned);
    }

    // ------------------------------------------------------------------ the clash

    private void startClash(ServerLevel level) {
        this.getNavigation().stop();
        this.setAction(CLASH);
        this.clashCooldown = 70 + this.random.nextInt(40);
        this.attackRest = CLASH_END + 8;
        this.playSound(ModCaveCreatures.CYPOLE_WINDUP.get(), 1.4F, 0.9F + this.random.nextFloat() * 0.15F);
    }

    /** The plates meet: a crash, and a ring shockwave starts across the ground. */
    private void crash(ServerLevel level) {
        level.broadcastEntityEvent(this, EVENT_CRASH);
        this.playSound(ModCaveCreatures.CYPOLE_CRASH.get(), 2.2F, 0.95F + this.random.nextFloat() * 0.1F);
        this.playSound(ModCaveCreatures.CYPOLE_SHIMMER.get(), 1.6F, 1.0F);
        this.playSound(ModCaveCreatures.CYPOLE_SHOCKWAVE.get(), 1.8F, 0.9F + this.random.nextFloat() * 0.1F);
        Vec3 c = this.position();
        this.rings.add(new Ring(c));
        level.sendParticles(ModCaveCreatures.CYMBAL_RING.get(), c.x, c.y + 0.06, c.z, 0, RING_RADIUS, RING_SPEED, 0.0, 1.0);
        BlockState ground = level.getBlockState(this.blockPosition().below());
        if (!ground.isAir() && ground.getFluidState().isEmpty()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), c.x, c.y + 0.1, c.z, 30, 0.7, 0.05, 0.7, 0.18);
        } else if (this.isInFluidType()) {
            level.sendParticles(ParticleTypes.SPLASH, c.x, c.y + 0.6, c.z, 40, 0.9, 0.1, 0.9, 0.3);
        }
        Vec3 chin = this.mouth().add(0.0, -0.3, 0.0);
        level.sendParticles(ParticleTypes.WAX_ON, chin.x, chin.y, chin.z, 12, 0.3, 0.2, 0.3, 0.6);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, chin.x, chin.y, chin.z, 8, 0.25, 0.2, 0.25, 0.3);
    }

    /** Every ring spreads; whatever its front passes over that is standing (or swimming) on it is struck. */
    private void tickRings(ServerLevel level) {
        if (this.rings.isEmpty()) {
            return;
        }
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        for (Iterator<Ring> it = this.rings.iterator(); it.hasNext(); ) {
            Ring r = it.next();
            float from = r.radius;
            float to = from + RING_SPEED;
            r.radius = to;
            AABB reach = new AABB(r.centre, r.centre).inflate(to + 1.0, 2.0, to + 1.0);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, reach, e -> e.isAlive() && !(e instanceof Cypole)
                    && !r.passed.contains(e.getId()))) {
                double dx = e.getX() - r.centre.x;
                double dz = e.getZ() - r.centre.z;
                double dist = Math.sqrt(dx * dx + dz * dz);
                double half = e.getBbWidth() * 0.5;
                if (dist + half < from || dist - half > to) {
                    continue; // the front has not reached it yet
                }
                r.passed.add(e.getId());
                if (e instanceof Player p && (p.isCreative() || p.isSpectator())) {
                    continue;
                }
                double above = e.getY() - r.centre.y;
                boolean afloat = e.isInFluidType() && e.getFirstEyeInFluidType().isAir();
                boolean submerged = !e.getFirstEyeInFluidType().isAir();
                if (above > 1.1 || above < -1.6 || submerged) {
                    continue; // up on a ledge, down in a hole, or under the water: the wave rolls past
                }
                if (!e.onGround() && !afloat && above > 0.3) {
                    continue; // jumped it!
                }
                Vec3 from3 = r.centre.add(0.0, 0.5, 0.0);
                Vec3 to3 = new Vec3(e.getX(), r.centre.y + 0.5, e.getZ());
                if (level.clip(new ClipContext(from3, to3, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() != HitResult.Type.MISS) {
                    continue; // a wall takes the blow
                }
                if (e.hurtServer(level, this.damageSources().mobAttack(this), damage)) {
                    Vec3 away = dist < 1.0E-3 ? Vec3.directionFromRotation(0.0F, this.getYRot()) : new Vec3(dx / dist, 0.0, dz / dist);
                    e.push(away.x * 0.85, 0.38, away.z * 0.85);
                }
            }
            if (to >= RING_RADIUS) {
                it.remove();
            }
        }
    }

    // ------------------------------------------------------------------ the tongue

    private void startTongue(LivingEntity foe) {
        this.getNavigation().stop();
        this.setAction(TONGUE);
        this.tongueTarget = foe;
        this.tongueAim = null;
        this.yanked = null;
        this.entityData.set(TONGUE_TARGET, -1);
        this.entityData.set(TONGUE_AIM, 0);
        this.tongueCooldown = 80 + this.random.nextInt(40);
        this.attackRest = TONGUE_FIRE + TONGUE_FLY + TONGUE_YANK + 6;
    }

    private void tickTongue(ServerLevel level) {
        int t = this.actionTicks;
        LivingEntity foe = this.tongueTarget;
        if (this.yanked != null) {
            this.tickYank(level);
            return;
        }
        if (t <= TONGUE_LOCK && foe != null && foe.isAlive()) {
            this.face(foe, 30.0F);
        }
        if (t == TONGUE_LOCK) {
            // it locks on to where you are now: whoever keeps moving sideways can dodge
            this.tongueAim = foe != null && foe.isAlive() ? foe.position().add(0.0, foe.getBbHeight() * 0.55, 0.0)
                    : this.mouth().add(Vec3.directionFromRotation(0.0F, this.getYRot()).scale(5.0));
            this.setTongueAim(this.tongueAim);
        } else if (t == TONGUE_FIRE) {
            this.playSound(ModCaveCreatures.CYPOLE_TONGUE.get(), 1.4F, 0.8F + this.random.nextFloat() * 0.15F);
        } else if (t == TONGUE_FIRE + TONGUE_FLY) {
            this.tongueLands(level, foe);
        } else if (t >= TONGUE_FIRE + TONGUE_FLY + 3) {
            this.setAction(NONE);
        }
    }

    /** The tongue reaches the aim: it sticks to whoever is still there - or slaps a shield, or nothing. */
    private void tongueLands(ServerLevel level, @Nullable LivingEntity foe) {
        Vec3 mouth = this.mouth();
        Vec3 aim = this.tongueAim != null ? this.tongueAim : mouth;
        Vec3 far = aim.add(aim.subtract(mouth).normalize().scale(0.6));
        var wall = level.clip(new ClipContext(mouth, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 reach = wall.getType() == HitResult.Type.MISS ? far : wall.getLocation();
        if (foe == null || !foe.isAlive() || foe.getBoundingBox().inflate(0.3).clip(mouth, reach).isEmpty()) {
            this.playSound(ModCaveCreatures.CYPOLE_SLAP.get(), 0.8F, 1.3F); // missed: it smacks the ground and comes back
            return;
        }
        boolean shielded = foe.isBlocking();
        if (!foe.hurtServer(level, this.damageSources().mobAttack(this), TONGUE_DAMAGE) && shielded) {
            this.playSound(ModCaveCreatures.CYPOLE_SHIELDED.get(), 1.0F, 1.0F);
            return;
        }
        if (shielded || foe.isPassenger()) {
            return;
        }
        this.yanked = foe;
        this.yankTicks = 0;
        this.entityData.set(TONGUE_TARGET, foe.getId());
        this.playSound(ModCaveCreatures.CYPOLE_SLAP.get(), 1.2F, 0.9F);
    }

    /** Hauling in whatever the tongue stuck to, right up to its plates; then a quick clash follows. */
    private void tickYank(ServerLevel level) {
        LivingEntity prey = this.yanked;
        this.yankTicks++;
        Vec3 mouth = this.mouth();
        Vec3 chest = prey == null ? mouth : prey.position().add(0.0, prey.getBbHeight() * 0.4, 0.0);
        Vec3 to = mouth.subtract(chest);
        double d = to.length();
        boolean blocked = prey != null && level.clip(new ClipContext(mouth, chest, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this))
                .getType() != HitResult.Type.MISS;
        if (prey == null || !prey.isAlive() || prey.isPassenger() || d < 1.5 || d > TONGUE_RANGE + 3.0 || this.yankTicks > TONGUE_YANK || blocked
                || prey instanceof Player p && (p.isCreative() || p.isSpectator())) {
            this.yanked = null;
            this.setAction(NONE);
            this.clashCooldown = Math.min(this.clashCooldown, 6);
            this.attackRest = Math.min(this.attackRest, 6);
            return;
        }
        Vec3 pull = to.scale(Math.min(0.7, 0.22 + d * 0.07) / d);
        prey.setDeltaMovement(pull.x, Math.max(pull.y, 0.0) + (this.yankTicks == 1 ? 0.28 : 0.05), pull.z);
        prey.needsSync = true;
        prey.resetFallDistance();
        this.face(prey, 30.0F);
    }

    // ------------------------------------------------------------------ the chorus

    /** On the swamp's beat, in bars the same for every Cypole around: each voice croaks its own rhythm. */
    private void tickChorus(ServerLevel level) {
        long time = level.getGameTime();
        if (time % BEAT != 0 || this.getAction() == CLASH || this.getAction() == TONGUE || this.isDeadOrDying()) {
            return;
        }
        BlockPos at = this.blockPosition();
        long bar = time / (BEAT * 8L);
        int region = (at.getX() >> 5) * 31 + (at.getZ() >> 5) * 17;
        boolean night = level.isDarkOutside();
        if (Math.floorMod(bar + region, 7) >= (night ? 5 : 3)) {
            return; // the swamp sings in waves (longer ones at night)
        }
        int step = (int) ((time / BEAT) % 8);
        if ((PATTERNS[this.voice()] >> step & 1) == 0) {
            return;
        }
        this.croak(level, step == 0 ? 0.92F : 1.0F);
    }

    private void croak(ServerLevel level, float pitch) {
        level.broadcastEntityEvent(this, EVENT_CROAK);
        float p = this.voicePitch() * pitch;
        this.playSound(ModCaveCreatures.CYPOLE_CROAK.get(), this.isInFluidType() ? 1.1F : 0.9F, p);
        this.playSound(ModCaveCreatures.CYPOLE_TICK.get(), 0.45F, 0.9F + p * 0.2F);
    }

    // ------------------------------------------------------------------ music

    /** Whether a song played on {@code played} speaks to it: one played on its own kind of instrument, percussion (drums, chimes). */
    public static boolean likes(Song song, @Nullable Instrument played) {
        Instrument.Family family = played != null ? played.family() : song.instrument();
        return family == Instrument.Family.DRUM || family == Instrument.Family.CHIMES;
    }

    /** A song it likes, finished nearby: an angry or wary Cypole calms down and lets the player be for a while. */
    public void hearSong(ServerLevel level, Player player, Song song, @Nullable Instrument played) {
        if (!likes(song, played) || this.distanceToSqr(player) > 16.0 * 16.0) {
            return;
        }
        boolean wary = this.getTarget() == player || this.getAction() == WARN;
        this.truce = player.getUUID();
        this.truceUntil = level.getGameTime() + 1200L;
        if (wary) {
            if (this.getTarget() == player) {
                this.setTarget(null);
            }
            if (this.getAction() == WARN) {
                this.setAction(NONE);
            }
            this.playSound(ModCaveCreatures.CYPOLE_CALM.get(), 1.0F, this.voicePitch());
            level.sendParticles(ParticleTypes.NOTE, this.getX(), this.getY() + 1.2, this.getZ(), 0, 0.4, 0.0, 0.0, 1.0);
        }
    }

    private boolean inTruce(Player player) {
        return player.getUUID().equals(this.truce) && this.level().getGameTime() < this.truceUntil;
    }

    /** A player it would warn off: close, seen, and not one it has made peace with. */
    private boolean intruder(Player p) {
        if (!p.isAlive() || p.isCreative() || p.isSpectator() || this.inTruce(p) || this.level().getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        double range = p.isSteppingCarefully() ? SNEAK_WARN : WARN_RANGE;
        return this.distanceToSqr(p) < range * range && this.hasLineOfSight(p);
    }

    // ------------------------------------------------------------------ damage, sounds, client

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && source.getEntity() instanceof Player && this.getAction() == WARN) {
            this.setAction(NONE);
        }
        return hurt;
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target instanceof Player p && (this.level().getDifficulty() == Difficulty.PEACEFUL || this.inTruce(p) && this.getLastHurtByMob() != p)) {
            target = null;
        }
        super.setTarget(target);
    }

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EVENT_CROAK -> this.croakAnimation.start(this.tickCount);
            case EVENT_CRASH -> {
                this.crashAnimation.start(this.tickCount);
                this.ring.kick(1.0F);
                this.squash.kick(-0.35F);
            }
            default -> super.handleEntityEvent(id);
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (ACTION.equals(accessor) && this.level().isClientSide()) {
            int action = this.getAction();
            if (action == CLASH) {
                this.clashAnimation.start(this.tickCount);
            } else if (action == TONGUE) {
                this.tongueAnimation.start(this.tickCount);
                this.tongueBack = -1;
            }
            if (this.lastAction == TONGUE && action != TONGUE) {
                this.tongueBack = this.tickCount;
            }
            this.warnAnimation.animateWhen(action == WARN, this.tickCount);
            this.lastAction = action;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.clientTick();
        }
    }

    private void clientTick() {
        boolean onGround = this.onGround();
        double vy = this.getDeltaMovement().y;
        if (onGround && !this.wasOnGround) {
            float impact = (float) Mth.clamp(-this.lastVelY * 1.4, 0.12, 0.5);
            this.squash.kick(-impact);
            this.ring.kick(impact * 0.5F);
        } else if (!onGround && this.wasOnGround && vy > 0.0) {
            this.squash.kick(0.32F);
        }
        this.wasOnGround = onGround;
        this.lastVelY = vy;
        this.crouchO = this.crouch;
        this.crouch += ((this.isCrouching() ? 1.0F : 0.0F) - this.crouch) * 0.5F;
        this.airO = this.air;
        this.air += ((onGround || this.isInFluidType() ? 0.0F : 1.0F) - this.air) * 0.45F;
        this.squash.tick();
        this.ring.tick();
        if (this.blinkTicks > 0) {
            this.blinkTicks--;
        } else if (this.random.nextInt(this.isAngry() ? 160 : 90) == 0) {
            this.blinkTicks = 6;
        }
        // a warning: the plates fizz against each other
        if (this.getAction() == WARN && this.tickCount % 5 == 0) {
            this.ring.kick(0.25F);
        }
    }

    /** The Codex page: it croaks, crashes its plates and shoots its tongue, over and over. */
    public void codexPose(int t) {
        if (t % 20 == 5) {
            this.croakAnimation.start(this.tickCount);
        }
        if (t % 140 == 40) {
            this.clashAnimation.start(this.tickCount);
        }
        if (t % 140 == 54) {
            this.crashAnimation.start(this.tickCount);
            this.ring.kick(1.0F);
        }
        if (t % 140 == 100) {
            this.tongueAnimation.start(this.tickCount);
            this.tongueBack = -1;
        }
        if (t % 140 == 120) {
            this.tongueBack = this.tickCount;
        }
        this.squash.tick();
        this.ring.tick();
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return null; // it croaks with the chorus instead
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModCaveCreatures.CYPOLE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModCaveCreatures.CYPOLE_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 3;
    }

    @Override
    public void makePoofParticles() {
        KillBurst.pop(this, 0xE3B23C, 0x3FF5E6, KillBurst.NOTE, ParticleTypes.WAX_ON);
    }

    // ------------------------------------------------------------------ goals

    /** Angry: keep at a good distance, hopping about; crash the plates when close, the tongue when far. */
    private class FightGoal extends Goal {
        private int repath;
        private int strafe = 1;

        FightGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity foe = Cypole.this.getTarget();
            return foe != null && foe.isAlive();
        }

        @Override
        public void start() {
            this.repath = 0;
        }

        @Override
        public void stop() {
            Cypole.this.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Cypole c = Cypole.this;
            LivingEntity foe = c.getTarget();
            if (foe == null || !(c.level() instanceof ServerLevel level)) {
                return;
            }
            c.getLookControl().setLookAt(foe, 30.0F, 30.0F);
            if (c.getAction() == CLASH || c.getAction() == TONGUE) {
                c.getNavigation().stop();
                return;
            }
            double d = c.distanceTo(foe);
            boolean seen = c.getSensing().hasLineOfSight(foe);
            if (c.attackRest <= 0 && seen && Math.abs(foe.getY() - c.getY()) < 3.0) {
                boolean canClash = c.clashCooldown <= 0 && d <= 6.0;
                boolean canTongue = c.tongueCooldown <= 0 && d >= 3.5 && d <= TONGUE_RANGE;
                if (canClash && (!canTongue || d < 4.5 || c.random.nextBoolean())) {
                    c.startClash(level);
                    return;
                }
                if (canTongue) {
                    c.startTongue(foe);
                    return;
                }
            }
            if (--this.repath > 0 && !c.getNavigation().isDone()) {
                return;
            }
            this.repath = 12 + c.random.nextInt(12);
            Vec3 to;
            if (d > 8.0 || !seen) {
                to = foe.position();
            } else if (d < 3.0) {
                to = DefaultRandomPos.getPosAway(c, 6, 3, foe.position());
            } else {
                // circle round it, now this way, now that
                if (c.random.nextInt(4) == 0) {
                    this.strafe = -this.strafe;
                }
                Vec3 side = foe.position().subtract(c.position()).multiply(1.0, 0.0, 1.0).normalize().yRot(Mth.HALF_PI * this.strafe);
                to = c.position().add(side.scale(3.0));
            }
            if (to != null) {
                c.getNavigation().moveTo(to.x, to.y, to.z, 1.25);
            }
        }
    }

    /** A player has come too close: it turns to face them and rattles its plates; too close, or too long, and it fights. */
    private class WarnGoal extends Goal {
        private @Nullable Player intruder;
        private int ticks;
        private int scan;

        WarnGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Cypole c = Cypole.this;
            if (c.getTarget() != null || c.getAction() != NONE || --this.scan > 0) {
                return false;
            }
            this.scan = 5;
            Player p = c.level().getNearestPlayer(c, WARN_RANGE);
            if (p != null && c.intruder(p)) {
                this.intruder = p;
                return true;
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            Cypole c = Cypole.this;
            Player p = this.intruder;
            return p != null && c.getTarget() == null && c.getAction() == WARN && p.isAlive() && !c.inTruce(p)
                    && c.distanceToSqr(p) < (WARN_RANGE + 1.5) * (WARN_RANGE + 1.5);
        }

        @Override
        public void start() {
            Cypole c = Cypole.this;
            this.ticks = 0;
            c.getNavigation().stop();
            c.setAction(WARN);
        }

        @Override
        public void stop() {
            Cypole c = Cypole.this;
            if (c.getAction() == WARN) {
                c.setAction(NONE);
            }
            this.intruder = null;
        }

        @Override
        public void tick() {
            Cypole c = Cypole.this;
            Player p = this.intruder;
            if (p == null) {
                return;
            }
            this.ticks++;
            c.face(p, 15.0F);
            c.getLookControl().setLookAt(p, 30.0F, 30.0F);
            if (this.ticks % 12 == 1) {
                c.playSound(ModCaveCreatures.CYPOLE_RATTLE.get(), 1.0F, c.voicePitch() * (1.0F + this.ticks * 0.004F));
            }
            double close = p.isSteppingCarefully() ? ANGER_RANGE * 0.65 : ANGER_RANGE;
            if (c.distanceToSqr(p) < close * close || this.ticks > 80) {
                c.setAction(NONE);
                c.setTarget(p);
                c.clashCooldown = Math.min(c.clashCooldown, 10);
            }
        }
    }

    /** Idle: off to the nearest pool of water to float in it for a while, only its eye above the surface. */
    private class SoakGoal extends Goal {
        private @Nullable BlockPos pool;
        private int soak;
        private int travel;

        SoakGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Cypole c = Cypole.this;
            if (c.getTarget() != null || c.getAction() != NONE || c.random.nextInt(c.isInFluidType() ? 60 : 200) != 0) {
                return false;
            }
            if (c.isInFluidType()) {
                this.pool = c.blockPosition();
                return true;
            }
            this.pool = this.findPool(c);
            return this.pool != null;
        }

        private @Nullable BlockPos findPool(Cypole c) {
            BlockPos at = c.blockPosition();
            BlockPos best = null;
            double bestD = Double.MAX_VALUE;
            for (int i = 0; i < 24; i++) {
                BlockPos p = at.offset(c.random.nextInt(17) - 8, c.random.nextInt(5) - 3, c.random.nextInt(17) - 8);
                if (!c.level().getFluidState(p).isEmpty() && c.level().getFluidState(p).isSource() && c.level().getBlockState(p.above()).isAir()) {
                    double d = p.distSqr(at);
                    if (d < bestD) {
                        bestD = d;
                        best = p;
                    }
                }
            }
            return best;
        }

        @Override
        public void start() {
            Cypole c = Cypole.this;
            this.soak = 200 + c.random.nextInt(300);
            this.travel = 0;
            BlockPos p = this.pool;
            if (p != null && !c.isInFluidType()) {
                c.getNavigation().moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0.9);
            }
        }

        @Override
        public boolean canContinueToUse() {
            Cypole c = Cypole.this;
            return c.getTarget() == null && c.getAction() == NONE && this.soak > 0 && this.travel < 240;
        }

        @Override
        public void tick() {
            Cypole c = Cypole.this;
            if (c.isInFluidType()) {
                c.getNavigation().stop();
                this.soak--;
            } else {
                this.travel++;
                BlockPos p = this.pool;
                if (c.getNavigation().isDone() && p != null) {
                    c.getNavigation().moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0.9);
                }
            }
        }
    }
}
