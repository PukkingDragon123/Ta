package com.thesift.entity.boss;

import com.thesift.effect.SculkCorruptionEffect;
import com.thesift.registry.ModEntities;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import com.thesift.world.Rumble;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Thumper: a giant snapping turtle with a war drum strapped to its shell. Nothing gets
 * through that shell - every blow anywhere else clanks off - but the drum on top is its weak
 * point. Its attacks:
 *
 * <ul>
 *   <li>Slam: rears up on its hind legs and crashes down; shockwave rings roll out across the
 *   ground (jump them).</li>
 *   <li>Charge: drums itself into a fury, lowers its head and thunders forward in a straight
 *   line. If it runs into a wall it is dazed: its drum hangs low and every hit on it counts
 *   double.</li>
 *   <li>Spin: tucks into its shell and spins like a top after you, throwing everything away.</li>
 * </ul>
 *
 * <p>Beaten down to half its health it does not fall: it wakes the sculk in its shell and grows
 * into a titan three times its size, plated down its back like something out of a monster film.
 * Nothing on the ground can hurt it any more. Its back is solid, though - a deck of living sculk
 * you can walk on - and from up there every blow lands (on the drum, sunk into the middle of it,
 * half again as hard). Climb one of the towers of its arena and leap on. It stands still while it
 * is ridden, but it will not stand for it long: it shakes itself (crouch to hold on) and Sculk
 * Parasites crawl out of its shell to bite at whoever is up there. On the ground it stomps
 * (shockwaves - jump them) and breathes a sweeping beam of sculk song; if you climb a tower it
 * walks up to it and waits, glaring, right below you.
 */
public class Thumper extends MiniBoss {
    public static final int SLAM = 1;
    public static final int CHARGE_WINDUP = 2;
    public static final int CHARGE = 3;
    public static final int SPIN = 4;
    public static final int DAZED = 5;
    /** Phase two begins: it swells into the titan (cannot be hurt meanwhile). */
    public static final int AWAKEN = 7;
    public static final int T_STOMP = 8;
    public static final int T_BREATH = 9;
    public static final int T_SHAKE = 10;
    /** Walking up to the tower its target climbed... */
    public static final int T_BRACE = 11;
    /** ...and standing right below them, glaring up: the moment to leap on. */
    public static final int T_HOLD = 12;
    /** Rendered at twice its model size. */
    public static final float SCALE = 2.0F;
    /** And the titan three times that again. */
    public static final float TITAN_SCALE = 3.0F;
    public static final int GROW_START = 30;
    public static final int GROW_END = 120;
    public static final int AWAKEN_TICKS = 150;
    public static final int STOMP_IMPACT = 25;
    public static final int BREATH_CHARGE = 44;
    public static final int BREATH_END = 100;
    public static final String TITAN_NAME = "entity.thesift.thumper.titan";
    private static final float STOMP_RADIUS = 16.0F;
    private static final int MAX_PARASITES = 6;
    private static final byte EVENT_DUST = 102;
    private static final byte EVENT_STOMP = 105;
    private static final byte EVENT_ROAR = 106;
    private static final byte EVENT_SHAKE = 107;
    private static final byte EVENT_STEP = 108;
    private static final EntityDataAccessor<Float> GROWTH = SynchedEntityData.defineId(Thumper.class, EntityDataSerializers.FLOAT);

    private Vec3 chargeDir = Vec3.ZERO;
    private final Set<Integer> chargeHits = new HashSet<>();
    private final Set<Integer> ringHits = new HashSet<>();
    private final Set<Integer> beamCorrupted = new HashSet<>();
    private @Nullable BlockPos home;
    private @Nullable Vec3 beamAim;
    private @Nullable Vec3 braceAt;
    private float growthO;
    /** Set while a blow that ignores invulnerability (/kill) is landing: then it may die early. */
    private boolean mortal;
    private int parasiteCooldown = 40;
    private int shakeCooldown = 120;
    private int riddenTicks;
    private int hintCooldown;
    private boolean leapHinted;
    private boolean parasitesHinted;
    private double walked;

    public Thumper(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.GREEN);
        this.moveControl = new TitanMoveControl(this);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 240.0).add(Attributes.ARMOR, 8.0).add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ATTACK_DAMAGE, 10.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.STEP_HEIGHT, 1.5).add(Attributes.ATTACK_KNOCKBACK, 1.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(GROWTH, 0.0F);
    }

    @Override
    protected double meleeReach() {
        return this.isTitan() ? 2.5 : 1.4;
    }

    // ------------------------------------------------------------------ the titan's size

    public float getGrowth() {
        return this.entityData.get(GROWTH);
    }

    /** 0 as the turtle, 1 as the titan (with the partial tick, for rendering). */
    public float growth(float partialTicks) {
        return Mth.lerp(partialTicks, this.growthO, this.getGrowth());
    }

    public boolean isTitan() {
        return this.getGrowth() > 0.0F;
    }

    /** How many times its turtle size it is right now. */
    public float titanScale() {
        return 1.0F + (TITAN_SCALE - 1.0F) * this.getGrowth();
    }

    private void setGrowth(float g) {
        this.entityData.set(GROWTH, Mth.clamp(g, 0.0F, 1.0F));
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (GROWTH.equals(accessor)) {
            this.refreshDimensions();
        }
    }

    /** As the titan it is lower and wider for its size: its box ends at the top of the deck. */
    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        EntityDimensions base = super.getDefaultDimensions(pose);
        float g = this.entityData == null ? 0.0F : this.getGrowth();
        if (g <= 0.0F) {
            return base;
        }
        float s = 1.0F + (TITAN_SCALE - 1.0F) * g;
        float h = Mth.lerp(g, base.height(), 2.75F) * s;
        return EntityDimensions.scalable(Mth.lerp(g, base.width(), 2.5F) * s, h).withEyeHeight(h * 0.62F);
    }

    /** Its back is solid ground once it is a titan. */
    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return this.isTitan() && this.isAlive() && !(other instanceof Thumper);
    }

    @Override
    public boolean isPushable() {
        return !this.isTitan() && super.isPushable();
    }

    @Override
    public boolean isPushedByFluid() {
        return !this.isTitan();
    }

    /** Is this standing (or jumping) on the titan's back? */
    public boolean isOnBack(@Nullable Entity e) {
        if (e == null || e == this || !this.isTitan()) {
            return false;
        }
        AABB box = this.getBoundingBox();
        double feet = e.getY();
        return feet > box.maxY - 0.6 && feet < box.maxY + 3.0 && e.getX() > box.minX - 0.5 && e.getX() < box.maxX + 0.5
                && e.getZ() > box.minZ - 0.5 && e.getZ() < box.maxZ + 0.5;
    }

    /** The players on its back. */
    public List<Player> riders() {
        if (!this.isTitan()) {
            return List.of();
        }
        AABB box = this.getBoundingBox();
        return this.level().getEntitiesOfClass(Player.class, new AABB(box.minX - 0.5, box.maxY - 0.6, box.minZ - 0.5, box.maxX + 0.5, box.maxY + 3.0,
                box.maxZ + 0.5), p -> p.isAlive() && !p.isSpectator() && this.isOnBack(p));
    }

    /** Standing on the drum skin, sunk into the middle of the deck. */
    private boolean onDrum(Entity e) {
        Vec3 c = this.getBoundingBox().getCenter();
        double dx = e.getX() - c.x;
        double dz = e.getZ() - c.z;
        double r = 6.5 / 16.0 * SCALE * this.titanScale();
        return dx * dx + dz * dz < r * r;
    }

    private Vec3 forward() {
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
    }

    /** Blocks per model unit at its current size. */
    private double unit() {
        return SCALE * this.titanScale() / 16.0;
    }

    /** Where its breath comes from: its jaws, held high. */
    public Vec3 mouth() {
        return this.position().add(this.forward().scale(24.0 * this.unit())).add(0.0, 17.0 * this.unit(), 0.0);
    }

    // ------------------------------------------------------------------ the drum, its weak point

    /** The drum on its back, in world space. */
    public AABB drumBox() {
        if (this.isTitan()) {
            AABB box = this.getBoundingBox();
            Vec3 c = box.getCenter();
            double r = 6.5 / 16.0 * SCALE * this.titanScale();
            return new AABB(c.x - r, box.maxY - 0.2, c.z - r, c.x + r, box.maxY + 0.2, c.z + r);
        }
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        double back = 0.125;
        double cx = this.getX() + Mth.sin(yaw) * back;
        double cz = this.getZ() - Mth.cos(yaw) * back;
        double low = this.getState() == DAZED ? 2.0 : 2.55;
        return new AABB(cx - 0.85, this.getY() + low, cz - 0.85, cx + 0.85, this.getY() + 3.95, cz + 0.85);
    }

    /** Did this blow land on the drum? Projectiles must touch it; melee must be aimed at it. */
    public boolean hitsDrum(DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return true;
        }
        AABB drum = this.drumBox();
        Entity direct = source.getDirectEntity();
        if (direct instanceof Projectile p) {
            return drum.inflate(0.6).intersects(p.getBoundingBox()) || drum.inflate(0.6).contains(p.position());
        }
        if (direct instanceof LivingEntity le) {
            Vec3 eye = le.getEyePosition();
            Vec3 end = eye.add(le.getLookAngle().scale(8.0));
            return drum.inflate(0.3).contains(eye) || drum.inflate(0.3).clip(eye, end).isPresent();
        }
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            this.mortal = true;
            try {
                return super.hurtServer(level, source, amount);
            } finally {
                this.mortal = false;
            }
        }
        int state = this.getState();
        if (state == AWAKEN || state == SPIN) {
            this.clank(level, source);
            return false;
        }
        if (this.isTitan()) {
            return this.hurtTitan(level, source, amount);
        }
        if (!this.hitsDrum(source)) {
            this.clank(level, source);
            return false;
        }
        boolean dazed = state == DAZED;
        boolean hurt = super.hurtServer(level, source, dazed ? amount * 2.0F : amount);
        if (hurt) {
            AABB d = this.drumBox();
            this.playSound(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 3.0F, 0.6F + this.random.nextFloat() * 0.2F);
            this.playSound(ModSounds.THUMPER_DRUM_HIT.get(), 2.0F, dazed ? 0.8F : 1.0F);
            level.sendParticles(ModParticles.RESONANCE_RING.get(), d.getCenter().x, d.maxY - 0.05, d.getCenter().z, 0, 1.4, 0.0, 0.0, 1.0);
            level.sendParticles(ModParticles.SIFT_NOTE.get(), d.getCenter().x, d.maxY + 0.3, d.getCenter().z, 6, 0.5, 0.3, 0.5, 1.0);
            level.sendParticles(ParticleTypes.SCULK_SOUL, d.getCenter().x, d.maxY, d.getCenter().z, 4, 0.4, 0.1, 0.4, 0.04);
        }
        return hurt;
    }

    /** As the titan only blows struck from its back count - and on the drum, half again as hard. */
    private boolean hurtTitan(ServerLevel level, DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (!this.isOnBack(attacker)) {
            this.clank(level, source);
            if (attacker instanceof Player p && this.hintCooldown <= 0) {
                p.sendOverlayMessage(Component.translatable("message.thesift.titan.climb"));
                this.hintCooldown = 120;
            }
            return false;
        }
        boolean drum = this.onDrum(attacker);
        boolean hurt = super.hurtServer(level, source, drum ? amount * 1.5F : amount);
        if (hurt) {
            Vec3 at = attacker.position();
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SCULK.defaultBlockState()), at.x, at.y + 0.1, at.z, 18, 0.4, 0.05, 0.4, 0.2);
            level.sendParticles(ParticleTypes.SCULK_SOUL, at.x, at.y + 0.2, at.z, 3, 0.3, 0.1, 0.3, 0.03);
            this.playSound(SoundEvents.SCULK_BLOCK_BREAK, 2.0F, 0.6F);
            if (drum) {
                AABB box = this.getBoundingBox();
                Vec3 c = box.getCenter();
                this.playSound(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 4.0F, 0.5F);
                this.playSound(ModSounds.THUMPER_DRUM_HIT.get(), 3.0F, 0.7F);
                level.sendParticles(ModParticles.RESONANCE_RING.get(), c.x, box.maxY + 0.05, c.z, 0, 3.0, 0.0, 0.0, 1.0);
                level.sendParticles(ModParticles.SIFT_NOTE.get(), c.x, box.maxY + 0.5, c.z, 6, 1.0, 0.3, 1.0, 1.0);
            }
        }
        return hurt;
    }

    /** It will not fall as a turtle: at half health it wakes as the titan instead. */
    @Override
    public void setHealth(float health) {
        if (!this.mortal && !this.isTitan() && this.getState() != AWAKEN && !this.level().isClientSide()) {
            health = Math.max(health, this.getMaxHealth() * 0.5F);
        }
        super.setHealth(health);
    }

    /** A blow on the shell: a clank and a shower of sparks, and nothing else. */
    private void clank(ServerLevel level, DamageSource source) {
        if (this.tickCount % 2 == 0 || source.getDirectEntity() instanceof Projectile) {
            Entity e = source.getDirectEntity();
            Vec3 mid = this.position().add(0, this.getBbHeight() * 0.4, 0);
            Vec3 at = e != null ? e.position().add(0, e.getBbHeight() * 0.7, 0).lerp(mid, this.isTitan() ? 0.2 : 0.6) : mid;
            level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 8, 0.2, 0.2, 0.2, 0.3);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 4, 0.2, 0.2, 0.2, 0.2);
            this.playSound(ModSounds.THUMPER_CLANK.get(), 1.5F, (this.isTitan() ? 0.6F : 0.9F) + this.random.nextFloat() * 0.3F);
        }
    }

    // ------------------------------------------------------------------ attacks

    @Override
    protected boolean canChase(int state) {
        return state == IDLE && !this.isTitan();
    }

    @Override
    protected boolean holdsState(int state) {
        return state == AWAKEN;
    }

    @Override
    protected void tickAlways(ServerLevel level, int state) {
        if (this.home == null) {
            this.home = this.blockPosition();
        }
        if (this.hintCooldown > 0) {
            this.hintCooldown--;
        }
        if (!this.isTitan() && state != AWAKEN && this.getHealth() <= this.getMaxHealth() * 0.5F + 0.01F) {
            this.beginAwaken(level);
            return;
        }
        if (state == AWAKEN) {
            this.tickAwaken(level, this.stateTicks);
            return;
        }
        if (this.isTitan()) {
            this.tickRiders(level, state);
            this.stepRumble(level);
        }
    }

    @Override
    protected void tickAttacks(ServerLevel level, LivingEntity target, int state) {
        if (state == AWAKEN) {
            return;
        }
        if (this.isTitan()) {
            this.tickTitan(level, target, state);
            return;
        }
        int t = this.stateTicks;
        double dist = this.distanceTo(target);
        switch (state) {
            case IDLE -> {
                if (this.cooldown > 0) {
                    return;
                }
                if (dist < 6.5) {
                    this.setState(this.random.nextBoolean() ? SLAM : SPIN);
                } else if (dist < 22 && this.hasLineOfSight(target)) {
                    this.setState(this.random.nextInt(4) == 0 ? SPIN : CHARGE_WINDUP);
                }
                if (this.getState() != IDLE) {
                    this.getNavigation().stop();
                    this.announce(level);
                }
            }
            case SLAM -> {
                this.getNavigation().stop();
                this.getLookControl().setLookAt(target, 10.0F, 10.0F);
                if (t == 22) {
                    this.slamImpact(level);
                }
                if (t >= 42) {
                    this.endAttack(30);
                }
            }
            case CHARGE_WINDUP -> {
                this.getNavigation().stop();
                this.getLookControl().setLookAt(target, 30.0F, 30.0F);
                this.faceTowards(target, 8.0F);
                if (t % 4 == 0) {
                    this.playSound(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 2.0F, 0.7F + t * 0.02F);
                    level.sendParticles(ModParticles.SIFT_NOTE.get(), this.getX(), this.getY() + 4.0, this.getZ(), 1, 0.4, 0.2, 0.4, 1.0);
                }
                if (t >= 26) {
                    Vec3 d = target.position().subtract(this.position()).multiply(1, 0, 1);
                    this.chargeDir = d.lengthSqr() < 1.0E-4 ? this.getLookAngle().multiply(1, 0, 1).normalize() : d.normalize();
                    this.chargeHits.clear();
                    this.playSound(ModSounds.THUMPER_ROAR.get(), 3.0F, 1.0F);
                    this.setState(CHARGE);
                }
            }
            case CHARGE -> this.tickCharge(level, t);
            case SPIN -> this.tickSpin(level, target, t);
            case DAZED -> {
                this.getNavigation().stop();
                if (t % 10 == 0) {
                    AABB d = this.drumBox();
                    level.sendParticles(ModParticles.STAR_SPARKLE.get(), d.getCenter().x, d.maxY + 0.6, d.getCenter().z, 4, 0.6, 0.2, 0.6, 0.0);
                }
                if (t >= 80) {
                    this.endAttack(20);
                }
            }
            default -> this.endAttack(20);
        }
    }

    private void announce(ServerLevel level) {
        this.level().broadcastEntityEvent(this, EVENT_DUST);
        this.playSound(ModSounds.THUMPER_WINDUP.get(), 2.0F, 0.9F + this.random.nextFloat() * 0.2F);
    }

    private void endAttack(int cooldown) {
        this.setState(IDLE);
        this.cooldown = cooldown + this.random.nextInt(20);
    }

    private void faceTowards(Entity e, float maxTurn) {
        double dx = e.getX() - this.getX();
        double dz = e.getZ() - this.getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        this.setYRot(Mth.approachDegrees(this.getYRot(), yaw, maxTurn));
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();
    }

    private void slamImpact(ServerLevel level) {
        this.playSound(ModSounds.THUMPER_SLAM.get(), 4.0F, 0.7F);
        this.playSound(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 4.0F, 0.5F);
        Vec3 c = this.position();
        for (int i = 0; i < 4; i++) {
            level.sendParticles(ModParticles.RESONANCE_RING.get(), c.x, c.y + 0.1, c.z, 0, 2.5 + i * 2.0, 0.0, 0.0, 1.0);
        }
        BlockState ground = level.getBlockState(this.blockPosition().below());
        if (!ground.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), c.x, c.y + 0.1, c.z, 80, 3.0, 0.1, 3.0, 0.3);
        }
        level.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.5, c.z, 3, 1.5, 0.2, 1.5, 0.0);
        this.hitAround(level, 7.5, 9.0F, 1.1, 0.75, true);
    }

    private void tickCharge(ServerLevel level, int t) {
        double speed = Math.min(0.85, 0.3 + t * 0.06);
        this.setDeltaMovement(this.chargeDir.x * speed, this.getDeltaMovement().y, this.chargeDir.z * speed);
        float yaw = (float) (Mth.atan2(this.chargeDir.z, this.chargeDir.x) * Mth.RAD_TO_DEG) - 90.0F;
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
        if (t % 2 == 0) {
            BlockState ground = level.getBlockState(this.blockPosition().below());
            if (!ground.isAir()) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), this.getX(), this.getY() + 0.1, this.getZ(), 12, 1.2, 0.1, 1.2, 0.15);
            }
            level.sendParticles(ParticleTypes.CLOUD, this.getX() - this.chargeDir.x * 1.5, this.getY() + 0.3, this.getZ() - this.chargeDir.z * 1.5, 3, 0.5, 0.2, 0.5, 0.02);
        }
        if (t % 6 == 0) {
            this.playSound(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 2.0F, 0.6F);
        }
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.6))) {
            if (e == this || isBandmate(e) || !this.chargeHits.add(e.getId())) {
                continue;
            }
            if (e.hurtServer(level, this.damageSources().mobAttack(this), 13.0F)) {
                e.push(this.chargeDir.x * 2.2, 0.7, this.chargeDir.z * 2.2);
                this.playSound(ModSounds.THUMPER_SLAM.get(), 2.0F, 1.3F);
            }
        }
        if (this.horizontalCollision && t > 4) {
            // smack into a wall: dazed, its drum hanging low
            this.setDeltaMovement(Vec3.ZERO);
            this.playSound(ModSounds.THUMPER_SLAM.get(), 4.0F, 0.6F);
            this.playSound(ModSounds.THUMPER_DAZED.get(), 2.0F, 1.0F);
            level.sendParticles(ParticleTypes.EXPLOSION, this.getX() + this.chargeDir.x * 1.6, this.getY() + 1.2, this.getZ() + this.chargeDir.z * 1.6,
                    2, 0.4, 0.4, 0.4, 0.0);
            this.setState(DAZED);
            return;
        }
        if (t >= 48) {
            this.endAttack(30);
        }
    }

    private void tickSpin(ServerLevel level, LivingEntity target, int t) {
        if (t < 10 || t > 74) {
            this.getNavigation().stop();
        } else {
            Vec3 d = target.position().subtract(this.position()).multiply(1, 0, 1);
            if (d.lengthSqr() > 1.0E-4) {
                d = d.normalize();
                Vec3 v = this.getDeltaMovement();
                this.setDeltaMovement(Mth.lerp(0.15, v.x, d.x * 0.55), v.y, Mth.lerp(0.15, v.z, d.z * 0.55));
            }
            if (t % 5 == 0) {
                this.playSound(ModSounds.THUMPER_SPIN.get(), 1.6F, 0.8F + (t % 20) * 0.02F);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, this.getX(), this.getY() + 1.0, this.getZ(), 3, 1.4, 0.3, 1.4, 0.0);
            }
            if (t % 8 == 0) {
                this.hitAround(level, 1.6, 6.0F, 1.6, 0.45, false);
            }
        }
        if (t >= 86) {
            this.endAttack(30);
        }
    }

    // ------------------------------------------------------------------ phase two: the titan

    private void beginAwaken(ServerLevel level) {
        this.setState(AWAKEN);
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        this.playSound(ModSounds.THUMPER_ROAR.get(), 5.0F, 0.6F);
        this.playSound(SoundEvents.WARDEN_ROAR, 4.0F, 0.7F);
        level.broadcastEntityEvent(this, EVENT_ROAR);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SCULK.defaultBlockState()), this.getX(), this.getY() + 2.5, this.getZ(),
                80, 1.5, 1.0, 1.5, 0.3);
        for (Player p : level.players()) {
            if (p.distanceToSqr(this) < 64 * 64) {
                p.sendOverlayMessage(Component.translatable("message.thesift.titan.awaken"));
            }
        }
    }

    private void tickAwaken(ServerLevel level, int t) {
        this.getNavigation().stop();
        Vec3 c = this.position();
        // first it scrambles back to the middle of its arena, where it has room to grow
        if (t < GROW_START && this.home != null) {
            Vec3 h = Vec3.atBottomCenterOf(this.home).subtract(c).multiply(1, 0, 1);
            double d = h.length();
            if (d > 1.5 && d < 32.0) {
                Vec3 v = h.scale(Math.min(0.45, d * 0.12) / d);
                this.setDeltaMovement(v.x, this.getDeltaMovement().y, v.z);
                float yaw = (float) (Mth.atan2(h.z, h.x) * Mth.RAD_TO_DEG) - 90.0F;
                this.setYRot(Mth.approachDegrees(this.getYRot(), yaw, 12.0F));
                this.yBodyRot = this.getYRot();
            }
            if (t % 4 == 0) {
                this.playSound(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 3.0F, 0.5F + t * 0.01F);
            }
        }
        if (t >= GROW_START && t <= GROW_END) {
            float k = (t - GROW_START) / (float) (GROW_END - GROW_START);
            this.setGrowth(k * k * (3.0F - 2.0F * k));
            AABB box = this.getBoundingBox();
            double r = this.getBbWidth() * 0.5;
            // sculk boils up out of the shell and spreads round its feet as it swells
            level.sendParticles(ParticleTypes.SCULK_SOUL, c.x, box.maxY, c.z, 3, r * 0.6, 0.3, r * 0.6, 0.05);
            level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, c.x, box.maxY * 0.5 + c.y * 0.5, c.z, 6, r, box.getYsize() * 0.4, r, 0.05);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SCULK.defaultBlockState()), c.x, c.y + 0.2, c.z, 10, r + 1.0, 0.1, r + 1.0, 0.2);
            if (t % 10 == 0) {
                this.playSound(SoundEvents.WARDEN_HEARTBEAT, 5.0F, 0.5F);
                this.playSound(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 4.0F, 0.5F);
                level.broadcastEntityEvent(this, EVENT_STEP);
                level.sendParticles(ModParticles.RESONANCE_RING.get(), c.x, c.y + 0.15, c.z, 0, r + 3.0, 0.0, 0.0, 1.0);
            }
            if (t % 30 == 0) {
                this.playSound(SoundEvents.SCULK_SHRIEKER_SHRIEK, 5.0F, 0.5F);
            }
        }
        if (t == GROW_END) {
            this.becomeTitan(level);
        }
        if (t >= AWAKEN_TICKS) {
            this.setState(IDLE);
            this.cooldown = 30;
        }
    }

    private void becomeTitan(ServerLevel level) {
        this.setGrowth(1.0F);
        if (!this.hasCustomName()) {
            this.setBossBarName(Component.translatable(TITAN_NAME));
        }
        var step = this.getAttribute(Attributes.STEP_HEIGHT);
        if (step != null) {
            step.setBaseValue(2.5);
        }
        this.playSound(ModSounds.THUMPER_ROAR.get(), 6.0F, 0.45F);
        this.playSound(SoundEvents.WARDEN_ROAR, 5.0F, 0.5F);
        this.playSound(SoundEvents.WARDEN_SONIC_BOOM, 4.0F, 0.5F);
        level.broadcastEntityEvent(this, EVENT_STOMP);
        Vec3 c = this.position();
        for (int i = 0; i < 4; i++) {
            level.sendParticles(ModParticles.RESONANCE_RING.get(), c.x, c.y + 0.15, c.z, 0, 8.0 + i * 4.0, 0.0, 0.0, 1.0);
        }
        level.sendParticles(ParticleTypes.SONIC_BOOM, c.x, c.y + this.getBbHeight() * 0.6, c.z, 1, 0, 0, 0, 0);
        // the blast of its waking throws everyone back
        this.hitAround(level, 5.0, 4.0F, 1.8, 0.6, false);
        for (Player p : level.players()) {
            if (p.distanceToSqr(this) < 64 * 64) {
                p.sendOverlayMessage(Component.translatable("message.thesift.titan.risen"));
            }
        }
    }

    /** While anyone rides it: it stands still for them - and its shell starts spawning parasites. */
    private void tickRiders(ServerLevel level, int state) {
        List<Player> riders = this.riders();
        if (this.shakeCooldown > 0) {
            this.shakeCooldown--;
        }
        if (riders.isEmpty()) {
            this.riddenTicks = 0;
            return;
        }
        this.riddenTicks++;
        if (state != T_STOMP) {
            this.getNavigation().stop();
            this.getMoveControl().setWantedPosition(this.getX(), this.getY(), this.getZ(), 0.0);
        }
        if (this.parasiteCooldown > 0) {
            this.parasiteCooldown--;
        } else {
            Player victim = riders.get(this.random.nextInt(riders.size()));
            if (this.spawnParasites(level, 1 + this.random.nextInt(2), victim) > 0 && !this.parasitesHinted) {
                this.parasitesHinted = true;
                for (Player p : riders) {
                    p.sendOverlayMessage(Component.translatable("message.thesift.titan.parasites"));
                }
            }
            this.parasiteCooldown = 70 + this.random.nextInt(60);
        }
        if (this.tickCount % 3 == 0) {
            // the deck seethes under their feet
            AABB box = this.getBoundingBox();
            level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, box.getCenter().x, box.maxY + 0.1, box.getCenter().z, 2, box.getXsize() * 0.35, 0.05,
                    box.getZsize() * 0.35, 0.01);
        }
    }

    /** Sculk Parasites crawl out of the shriekers on its back. */
    private int spawnParasites(ServerLevel level, int n, @Nullable LivingEntity victim) {
        AABB box = this.getBoundingBox();
        int alive = level.getEntities(ModEntities.SCULK_PARASITE.get(), box.inflate(8.0, 4.0, 8.0), Entity::isAlive).size();
        Vec3 f = this.forward();
        double unit = this.unit();
        int made = 0;
        for (int i = 0; i < n && alive + made < MAX_PARASITES; i++) {
            double along = (this.random.nextBoolean() ? 7.0 : -7.0) * unit;
            double side = (this.random.nextDouble() - 0.5) * 1.5;
            double x = this.getX() + f.x * along - f.z * side;
            double z = this.getZ() + f.z * along + f.x * side;
            SculkParasite p = ModEntities.SCULK_PARASITE.get().create(level, EntitySpawnReason.MOB_SUMMONED);
            if (p == null) {
                continue;
            }
            p.snapTo(x, box.maxY + 0.05, z, this.random.nextFloat() * 360.0F, 0.0F);
            if (victim != null) {
                p.setTarget(victim);
            }
            level.addFreshEntity(p);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SCULK.defaultBlockState()), x, box.maxY + 0.3, z, 24, 0.3, 0.2, 0.3, 0.15);
            level.sendParticles(ParticleTypes.SCULK_SOUL, x, box.maxY + 0.4, z, 5, 0.2, 0.2, 0.2, 0.04);
            level.playSound(null, x, box.maxY, z, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 1.2F, 1.5F + this.random.nextFloat() * 0.3F);
            made++;
        }
        return made;
    }

    /** The ground shakes under each of its footfalls. */
    private void stepRumble(ServerLevel level) {
        this.walked += this.getDeltaMovement().horizontalDistance();
        if (this.walked > 3.4) {
            this.walked = 0.0;
            this.playSound(ModSounds.THUMPER_SLAM.get(), 2.5F, 0.4F + this.random.nextFloat() * 0.1F);
            level.broadcastEntityEvent(this, EVENT_STEP);
            BlockState ground = level.getBlockState(this.blockPosition().below());
            if (!ground.isAir()) {
                double r = this.getBbWidth() * 0.5;
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), this.getX(), this.getY() + 0.1, this.getZ(), 30, r, 0.1, r, 0.2);
            }
        }
    }

    private void tickTitan(ServerLevel level, LivingEntity target, int state) {
        int t = this.stateTicks;
        boolean ridden = this.riddenTicks > 0;
        boolean aboard = this.isOnBack(target);
        double half = this.getBbWidth() * 0.5;
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        double hd = Math.sqrt(dx * dx + dz * dz);
        // up a tower (or anything else it cannot reach)
        boolean perched = !aboard && target.getY() > this.getY() + 4.0 && hd < 40.0;
        switch (state) {
            case IDLE -> {
                if (!aboard) {
                    this.getLookControl().setLookAt(target, 6.0F, 6.0F);
                }
                if (ridden) {
                    if (this.shakeCooldown <= 0 && this.riddenTicks > 50) {
                        this.setState(T_SHAKE);
                    } else if (!aboard && this.cooldown <= 0 && hd < 34.0 && this.hasLineOfSight(target) && this.random.nextInt(60) == 0) {
                        this.beginBreath(target);
                    }
                    return;
                }
                if (this.cooldown > 0) {
                    this.approach(target, perched ? half + 2.5 : half + 6.0);
                    return;
                }
                if (perched) {
                    if (this.random.nextInt(3) == 0 && this.hasLineOfSight(target)) {
                        this.beginBreath(target);
                    } else {
                        this.beginBrace(target);
                    }
                } else if (hd < half + 9.0) {
                    this.beginStomp(level);
                } else if (hd < 34.0 && this.hasLineOfSight(target) && this.random.nextInt(3) > 0) {
                    this.beginBreath(target);
                } else {
                    this.approach(target, half + 6.0);
                }
            }
            case T_STOMP -> this.tickStomp(level, t, ridden);
            case T_BREATH -> this.tickBreath(level, target, t);
            case T_SHAKE -> this.tickShake(level, t);
            case T_BRACE -> {
                Vec3 at = this.braceAt;
                if (ridden || at == null || t > 160) {
                    this.endAttack(10);
                    return;
                }
                double ax = at.x - this.getX();
                double az = at.z - this.getZ();
                double d = Math.sqrt(ax * ax + az * az);
                if (d < 1.0 || this.horizontalCollision && t > 20 || d < 3.0 && t > 100) {
                    this.setState(T_HOLD);
                    this.playSound(ModSounds.THUMPER_ROAR.get(), 4.0F, 0.6F);
                    if (!this.leapHinted && target instanceof Player p) {
                        this.leapHinted = true;
                        p.sendOverlayMessage(Component.translatable("message.thesift.titan.leap"));
                    }
                } else {
                    this.getMoveControl().setWantedPosition(at.x, this.getY(), at.z, 1.1);
                }
            }
            case T_HOLD -> {
                if (ridden) {
                    this.endAttack(10);
                    return;
                }
                this.getMoveControl().setWantedPosition(this.getX(), this.getY(), this.getZ(), 0.0);
                this.faceTowards(target, 3.0F);
                this.getLookControl().setLookAt(target, 10.0F, 10.0F);
                if (t % 20 == 0) {
                    this.playSound(ModSounds.THUMPER_AMBIENT.get(), 3.0F, 0.5F);
                }
                if (t == 110) {
                    // it rams the tower: everything on top of it shakes
                    this.playSound(ModSounds.THUMPER_SLAM.get(), 5.0F, 0.5F);
                    level.broadcastEntityEvent(this, EVENT_STOMP);
                    if (!aboard && target.getY() > this.getY() + 3.0 && hd < half + 5.0) {
                        target.hurtServer(level, this.damageSources().mobAttack(this), 5.0F);
                        target.push(0.0, 0.3, 0.0);
                    }
                }
                if (t >= 130) {
                    this.endAttack(40);
                }
            }
            default -> this.endAttack(20);
        }
    }

    /** Lumbers towards the target until it is `stopAt` away (its own steps shake the ground). */
    private void approach(LivingEntity target, double stopAt) {
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        if (dx * dx + dz * dz > stopAt * stopAt) {
            this.getMoveControl().setWantedPosition(target.getX(), this.getY(), target.getZ(), 1.0);
        } else {
            this.getMoveControl().setWantedPosition(this.getX(), this.getY(), this.getZ(), 0.0);
            this.faceTowards(target, 3.0F);
        }
    }

    private void beginStomp(ServerLevel level) {
        this.setState(T_STOMP);
        this.ringHits.clear();
        this.getMoveControl().setWantedPosition(this.getX(), this.getY(), this.getZ(), 0.0);
        this.playSound(ModSounds.THUMPER_WINDUP.get(), 4.0F, 0.5F);
        level.broadcastEntityEvent(this, EVENT_DUST);
    }

    /** It rears up and brings its forefeet down: a shockwave rolls out along the ground. */
    private void tickStomp(ServerLevel level, int t, boolean ridden) {
        Vec3 c = this.position();
        double half = this.getBbWidth() * 0.5;
        if (t == STOMP_IMPACT) {
            this.playSound(ModSounds.THUMPER_SLAM.get(), 6.0F, 0.45F);
            this.playSound(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 6.0F, 0.5F);
            this.playSound(SoundEvents.GENERIC_EXPLODE.value(), 3.0F, 0.6F);
            level.broadcastEntityEvent(this, EVENT_STOMP);
            level.sendParticles(ModParticles.RESONANCE_RING.get(), c.x, c.y + 0.15, c.z, 0, STOMP_RADIUS, 0.0, 0.0, 1.0);
            level.sendParticles(ModParticles.RESONANCE_RING.get(), c.x, c.y + 0.3, c.z, 0, STOMP_RADIUS * 0.7, 0.0, 0.0, 1.0);
            level.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.5, c.z, 6, half, 0.2, half, 0.0);
        }
        if (t >= STOMP_IMPACT && t < STOMP_IMPACT + 16) {
            // the wave front, growing just as the ring particle does
            float k = (t - STOMP_IMPACT + 1) / 16.0F;
            double r = half + (STOMP_RADIUS - half) * (1.0 - (1.0 - k) * (1.0 - k));
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(STOMP_RADIUS, 3.0, STOMP_RADIUS))) {
                if (e == this || isBandmate(e) || this.isOnBack(e) || !e.onGround() || Math.abs(e.getY() - this.getY()) > 2.5
                        || this.ringHits.contains(e.getId())) {
                    continue;
                }
                double ex = e.getX() - c.x;
                double ez = e.getZ() - c.z;
                double d = Math.sqrt(ex * ex + ez * ez);
                if (d > r + 0.4 || d < r - 1.8) {
                    continue;
                }
                this.ringHits.add(e.getId());
                if (e.hurtServer(level, this.damageSources().mobAttack(this), 9.0F)) {
                    double nd = Math.max(0.01, d);
                    e.push(ex / nd, 0.55, ez / nd);
                }
            }
            BlockState ground = level.getBlockState(this.blockPosition().below());
            if (!ground.isAir()) {
                for (int i = 0; i < 18; i++) {
                    double a = this.random.nextDouble() * Math.PI * 2.0;
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), c.x + Math.cos(a) * r, c.y + 0.1, c.z + Math.sin(a) * r, 2,
                            0.2, 0.05, 0.2, 0.15);
                }
            }
        }
        if (t >= STOMP_IMPACT + 30) {
            this.endAttack(40);
        }
    }

    private void beginBreath(LivingEntity target) {
        this.setState(T_BREATH);
        this.beamAim = target.getEyePosition();
        this.beamCorrupted.clear();
        this.getMoveControl().setWantedPosition(this.getX(), this.getY(), this.getZ(), 0.0);
        this.playSound(SoundEvents.WARDEN_SONIC_CHARGE, 5.0F, 0.6F);
        this.playSound(ModSounds.THUMPER_WINDUP.get(), 4.0F, 0.6F);
    }

    /**
     * Its plates light up one by one, tail to head; then it breathes a beam of sculk song that
     * sweeps after its target - slowly enough to outrun, and towers stop it. Whatever it touches
     * takes the corruption.
     */
    private void tickBreath(ServerLevel level, LivingEntity target, int t) {
        this.getLookControl().setLookAt(target, 6.0F, 6.0F);
        if (t < BREATH_CHARGE) {
            this.faceTowards(target, 3.0F);
            this.beamAim = target.getEyePosition();
            if (t % 6 == 0) {
                this.playSound(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 3.0F, 0.5F + t * 0.02F);
            }
            return;
        }
        if (t == BREATH_CHARGE) {
            this.playSound(SoundEvents.WARDEN_SONIC_BOOM, 6.0F, 0.5F);
            this.playSound(ModSounds.THUMPER_ROAR.get(), 5.0F, 0.5F);
            level.broadcastEntityEvent(this, EVENT_ROAR);
        }
        if (t < BREATH_END) {
            Vec3 want = target.getEyePosition().subtract(0.0, 0.4, 0.0);
            Vec3 aim = this.beamAim == null ? want : this.beamAim.lerp(want, 0.07);
            this.beamAim = aim;
            Vec3 from = this.mouth();
            Vec3 dir = aim.subtract(from);
            if (dir.lengthSqr() < 1.0E-4) {
                return;
            }
            Vec3 to = from.add(dir.normalize().scale(42.0));
            HitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            Vec3 end = hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
            this.beamParticles(level, from, end, t);
            if (t % 5 == 0) {
                this.beamHurt(level, from, end);
            }
            if (t % 10 == 0) {
                this.playSound(SoundEvents.WARDEN_SONIC_BOOM, 2.5F, 0.4F + this.random.nextFloat() * 0.2F);
                level.playSound(null, end.x, end.y, end.z, SoundEvents.SCULK_BLOCK_BREAK, SoundSource.HOSTILE, 2.0F, 0.6F);
            }
            return;
        }
        if (t >= BREATH_END + 14) {
            this.endAttack(50);
        }
    }

    private void beamParticles(ServerLevel level, Vec3 from, Vec3 end, int t) {
        Vec3 d = end.subtract(from);
        double len = d.length();
        if (len < 0.5) {
            return;
        }
        Vec3 u = d.scale(1.0 / len);
        if (t % 2 == 0) {
            for (double s = 0.5; s < len; s += 0.8) {
                Vec3 p = from.add(u.scale(s));
                level.sendParticles(ModParticles.GLOW_DUST.get(), p.x, p.y, p.z, 1, 0.15, 0.15, 0.15, 0.0);
            }
        }
        if (t % 4 == 0) {
            for (double s = 1.5; s < len; s += 3.0) {
                Vec3 p = from.add(u.scale(s));
                level.sendParticles(ParticleTypes.SONIC_BOOM, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
        level.sendParticles(ParticleTypes.SCULK_SOUL, end.x, end.y, end.z, 2, 0.4, 0.3, 0.4, 0.05);
        level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, end.x, end.y, end.z, 4, 0.5, 0.3, 0.5, 0.05);
        if (t % 6 == 0) {
            level.sendParticles(ModParticles.RESONANCE_RING.get(), end.x, end.y + 0.1, end.z, 0, 2.5, 0.0, 0.0, 1.0);
        }
        BlockState hit = level.getBlockState(BlockPos.containing(end.add(u.scale(0.2))));
        if (!hit.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, hit), end.x, end.y, end.z, 6, 0.3, 0.3, 0.3, 0.2);
        }
    }

    private void beamHurt(ServerLevel level, Vec3 from, Vec3 end) {
        Vec3 seg = end.subtract(from);
        double len2 = Math.max(1.0E-6, seg.lengthSqr());
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, end).inflate(1.5))) {
            if (e == this || isBandmate(e) || this.isOnBack(e)) {
                continue;
            }
            Vec3 c = e.getBoundingBox().getCenter();
            double k = Mth.clamp(c.subtract(from).dot(seg) / len2, 0.0, 1.0);
            if (c.distanceTo(from.add(seg.scale(k))) > 1.1 + e.getBbWidth() * 0.5) {
                continue;
            }
            if (e.hurtServer(level, this.damageSources().sonicBoom(this), 3.0F) && this.beamCorrupted.add(e.getId())) {
                SculkCorruptionEffect.stack(e, 0, 200, this);
            }
        }
    }

    /** Fed up with its riders, it shakes itself like a wet dog. Crouch to hold on. */
    private void tickShake(ServerLevel level, int t) {
        this.getMoveControl().setWantedPosition(this.getX(), this.getY(), this.getZ(), 0.0);
        if (t == 1) {
            this.playSound(ModSounds.THUMPER_ROAR.get(), 5.0F, 0.55F);
            this.playSound(SoundEvents.WARDEN_ANGRY, 4.0F, 0.6F);
        }
        if (t >= 10 && t <= 60 && t % 6 == 0) {
            level.broadcastEntityEvent(this, EVENT_SHAKE);
            this.playSound(ModSounds.THUMPER_SLAM.get(), 3.0F, 0.5F + this.random.nextFloat() * 0.2F);
            AABB box = this.getBoundingBox();
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SCULK.defaultBlockState()), box.getCenter().x, box.maxY + 0.1,
                    box.getCenter().z, 30, box.getXsize() * 0.4, 0.1, box.getZsize() * 0.4, 0.3);
            for (Player p : this.riders()) {
                Vec3 out = p.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
                out = out.lengthSqr() > 1.0E-3 ? out.normalize() : Vec3.ZERO;
                double a = this.random.nextDouble() * Math.PI * 2.0;
                double k = p.isShiftKeyDown() ? 0.12 : 1.0;
                p.push((Math.cos(a) * 0.35 + out.x * 0.3) * k, 0.05 + 0.28 * k, (Math.sin(a) * 0.35 + out.z * 0.3) * k);
            }
        }
        if (t == 30) {
            List<Player> riders = this.riders();
            this.spawnParasites(level, 2, riders.isEmpty() ? null : riders.get(0));
        }
        if (t >= 70) {
            this.endAttack(30);
            this.shakeCooldown = 200 + this.random.nextInt(100);
        }
    }

    private void beginBrace(LivingEntity target) {
        Vec3 to = target.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
        double d = to.length();
        Vec3 dir = d < 1.0E-3 ? this.forward() : to.scale(1.0 / d);
        // stand with its flank right under the tower's edge
        double stand = this.getBbWidth() * 0.5 + 1.4;
        this.braceAt = new Vec3(target.getX() - dir.x * stand, this.getY(), target.getZ() - dir.z * stand);
        this.setState(T_BRACE);
    }

    // ------------------------------------------------------------------ client

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EVENT_DUST -> {
                // the client hears the wind-up cue: a puff of dust
                double r = this.getBbWidth() * 0.35;
                for (int i = 0; i < 8; i++) {
                    this.level().addParticle(ParticleTypes.CLOUD, this.getRandomX(r), this.getY() + 0.2, this.getRandomZ(r), 0, 0.02, 0);
                }
            }
            case EVENT_STOMP -> Rumble.at(this.position(), 4.5F, 48.0F, 22);
            case EVENT_ROAR -> Rumble.at(this.position(), 2.5F, 64.0F, 40);
            case EVENT_SHAKE -> Rumble.at(this.position().add(0.0, this.getBbHeight(), 0.0), 3.5F, 16.0F, 10);
            case EVENT_STEP -> Rumble.at(this.position(), 1.0F, 30.0F, 8);
            default -> super.handleEntityEvent(id);
        }
    }

    @Override
    public void tick() {
        this.growthO = this.getGrowth();
        super.tick();
        if (this.level().isClientSide()) {
            this.clientTick();
        }
    }

    private void clientTick() {
        Level level = this.level();
        int s = this.getState();
        if (s == SPIN && this.random.nextInt(2) == 0) {
            double a = this.random.nextDouble() * Math.PI * 2;
            level.addParticle(ParticleTypes.CRIT, this.getX() + Math.cos(a) * 1.6, this.getY() + 0.8, this.getZ() + Math.sin(a) * 1.6,
                    -Math.sin(a) * 0.4, 0.05, Math.cos(a) * 0.4);
        }
        if (!this.isTitan() && (s == IDLE || s == CHARGE_WINDUP) && this.random.nextInt(s == IDLE ? 20 : 3) == 0) {
            AABB d = this.drumBox();
            level.addParticle(ModParticles.GLOW_DUST.get(), d.getCenter().x + (this.random.nextDouble() - 0.5), d.maxY + 0.1,
                    d.getCenter().z + (this.random.nextDouble() - 0.5), 0, 0.02, 0);
        }
        if (s == AWAKEN) {
            double r = this.getBbWidth() * 0.6 + 1.0;
            for (int i = 0; i < 3; i++) {
                level.addParticle(ParticleTypes.SCULK_SOUL, this.getRandomX(r), this.getY() + this.random.nextDouble() * this.getBbHeight(), this.getRandomZ(r),
                        0, 0.06, 0);
            }
        }
        if (!this.isTitan()) {
            return;
        }
        AABB box = this.getBoundingBox();
        if (this.random.nextInt(3) == 0) {
            // the living deck breathes out souls
            level.addParticle(ParticleTypes.SCULK_SOUL, box.minX + this.random.nextDouble() * box.getXsize(), box.maxY + 0.1,
                    box.minZ + this.random.nextDouble() * box.getZsize(), 0, 0.04, 0);
        }
        float t = this.stateTime(0.0F);
        if (s == T_BREATH && t < BREATH_CHARGE) {
            // light crawls up its spine, tail to head, and gathers in its jaws
            Vec3 f = this.forward();
            double unit = this.unit();
            double along = Mth.lerp(t / BREATH_CHARGE, -36.0, 14.0) * unit;
            for (int i = 0; i < 3; i++) {
                double side = (this.random.nextDouble() - 0.5) * box.getXsize();
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, this.getX() + f.x * along - f.z * side, box.maxY + 0.5 + this.random.nextDouble() * 2.5,
                        this.getZ() + f.z * along + f.x * side, 0, 0.05, 0);
            }
            Vec3 m = this.mouth();
            for (int i = 0; i < 2; i++) {
                Vec3 o = new Vec3(this.random.nextDouble() - 0.5, this.random.nextDouble() - 0.5, this.random.nextDouble() - 0.5).scale(4.0);
                level.addParticle(ParticleTypes.SCULK_CHARGE_POP, m.x + o.x, m.y + o.y, m.z + o.z, -o.x * 0.12, -o.y * 0.12, -o.z * 0.12);
            }
        }
    }

    // ------------------------------------------------------------------ saving

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Titan", this.isTitan());
        if (this.home != null) {
            output.store("Home", BlockPos.CODEC, this.home);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        // before the health is read, or it would be held at half
        if (input.getBooleanOr("Titan", false)) {
            this.setGrowth(1.0F);
        }
        super.readAdditionalSaveData(input);
        this.home = input.read("Home", BlockPos.CODEC).orElse(null);
        if (this.isTitan() && !this.hasCustomName()) {
            this.setBossBarName(Component.translatable(TITAN_NAME));
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.THUMPER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.THUMPER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.THUMPER_DEATH.get();
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (this.isTitan() ? 0.6F : 1.0F);
    }

    @Override
    protected float getSoundVolume() {
        return this.isTitan() ? 4.0F : 1.0F;
    }

    @Override
    protected int burstColorA() {
        return 0x73A050;
    }

    @Override
    protected int burstColorB() {
        return 0xCD5F42;
    }

    /** Turns ponderously as the titan (a few degrees a tick) and only sets off once it faces the way. */
    private static final class TitanMoveControl extends MoveControl<Thumper> {
        TitanMoveControl(Thumper thumper) {
            super(thumper);
        }

        @Override
        public void tick() {
            if (!this.mob.isTitan()) {
                super.tick();
                return;
            }
            if (this.operation != MoveControl.Operation.MOVE_TO) {
                this.mob.setZza(0.0F);
                this.mob.setSpeed(0.0F);
                return;
            }
            this.operation = MoveControl.Operation.WAIT;
            double dx = this.wantedX - this.mob.getX();
            double dz = this.wantedZ - this.mob.getZ();
            if (dx * dx + dz * dz < 0.25 || this.speedModifier <= 0.0) {
                this.mob.setZza(0.0F);
                this.mob.setSpeed(0.0F);
                return;
            }
            float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
            this.mob.setYRot(this.rotlerp(this.mob.getYRot(), yaw, 4.0F));
            this.mob.yBodyRot = this.mob.getYRot();
            float off = Math.abs(Mth.wrapDegrees(yaw - this.mob.getYRot()));
            float speed = (float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
            this.mob.setSpeed(off < 35.0F ? speed : speed * 0.2F);
        }
    }
}
