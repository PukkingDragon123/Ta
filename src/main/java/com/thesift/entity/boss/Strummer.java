package com.thesift.entity.boss;

import com.thesift.block.MusicalCobwebBlock;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModEntities;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import com.thesift.world.Rumble;
import com.thesift.world.TemporaryBlocks;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Weaver (entity id {@code strummer}): a great sculk spider with a bone mantis riding its back,
 * playing the glowing strings that run from the spider's spinnerets up to its scythe hands. It
 * crawls up out of the ground like the Warden when it is woken, and fights in three movements:
 *
 * <ol>
 *   <li>Slash, spit silk, pounce, snap a string to drag you in, strum the brood stronger, and lay
 *   Sculk Spiders.</li>
 *   <li>Two thirds down it roars and rings itself in webs; now it also climbs the walls, shoots a
 *   silk thread up to the ceiling and swings across the arena on it to drop on you, and weaves an
 *   orb of Musical Cobwebs around you in a heartbeat.</li>
 *   <li>One third left: it roars again, everything comes faster, and every strum makes every web
 *   it has woven sing - hurting whoever stands in one.</li>
 * </ol>
 * Every attack is telegraphed: a hiss, a crouch, a ring of light where the webs will fall.
 */
public class Strummer extends MiniBoss {
    public static final int SLASH = 1;
    public static final int WEB = 2;
    public static final int POUNCE_WINDUP = 3;
    public static final int POUNCE = 4;
    public static final int BROOD = 5;
    public static final int STRUM = 6;
    public static final int SNAP = 7;
    /** Crawling up out of the ground (~4 s). */
    public static final int EMERGE = 8;
    /** Thread up to the ceiling, reel up, swing across, let go. */
    public static final int SWING = 9;
    /** Weaving an orb of Musical Cobwebs around its target. */
    public static final int WEAVE = 10;
    /** The roar between movements. */
    public static final int AWAKEN = 11;
    public static final float SCALE = 1.6F;

    public static final int EMERGE_TICKS = 80;
    public static final int SWING_AIM = 12;
    public static final int SWING_REEL = 12;
    public static final int SWING_ARC = 22;
    private static final int WEAVE_AIM = 16;

    private static final EntityDataAccessor<Boolean> CLIMBING = SynchedEntityData.defineId(Strummer.class, EntityDataSerializers.BOOLEAN);
    /** Where the silk thread is anchored (while it swings). */
    private static final EntityDataAccessor<BlockPos> THREAD = SynchedEntityData.defineId(Strummer.class, EntityDataSerializers.BLOCK_POS);

    private int strumCooldown = 120;
    private int swingCooldown = 60;
    private int weaveCooldown = 100;
    private boolean emerged;
    private int lastPhase = 1;
    // the swing, worked out when it starts
    private Vec3 swingFrom = Vec3.ZERO;
    private Vec3 swingAnchor = Vec3.ZERO;
    private Vec3 swingDir = Vec3.ZERO;
    private double swingRope;
    private double swingAngle;
    private final Set<Integer> swingHits = new HashSet<>();
    private BlockPos weaveCentre = BlockPos.ZERO;

    public Strummer(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.PURPLE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 260.0).add(Attributes.ARMOR, 6.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 8.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.8).add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CLIMBING, false);
        builder.define(THREAD, BlockPos.ZERO);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WallClimberNavigation(this, level);
    }

    /** Which movement of the fight it is in: 1, 2 or 3 (from its health, so both sides know). */
    public int phase() {
        float f = this.getHealth() / this.getMaxHealth();
        return f > 2.0F / 3.0F ? 1 : (f > 1.0F / 3.0F ? 2 : 3);
    }

    public boolean enraged() {
        return this.phase() == 3;
    }

    /** Cooldowns shrink as the fight goes on. */
    private int paced(int ticks) {
        return (int) (ticks * new float[]{1.0F, 0.8F, 0.55F}[this.phase() - 1]);
    }

    public boolean isClimbing() {
        return this.entityData.get(CLIMBING);
    }

    /** The silk thread's anchor, or null while it hangs on none. */
    public @org.jspecify.annotations.Nullable Vec3 threadAnchor() {
        if (this.getState() != SWING) {
            return null;
        }
        BlockPos p = this.entityData.get(THREAD);
        return new Vec3(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
    }

    @Override
    protected double chaseSpeed() {
        return new double[]{1.05, 1.15, 1.3}[this.phase() - 1];
    }

    @Override
    protected double meleeReach() {
        return 1.3;
    }

    @Override
    protected boolean canChase(int state) {
        return state == IDLE || state == STRUM;
    }

    @Override
    protected boolean holdsState(int state) {
        return state == EMERGE || state == AWAKEN || state == SWING;
    }

    // ------------------------------------------------------------------ the entrance and the movements

    @Override
    protected void tickAlways(ServerLevel level, int state) {
        if (!this.emerged && state != EMERGE) {
            this.setState(EMERGE);
            return;
        }
        if (state == EMERGE) {
            this.tickEmerge(level);
            return;
        }
        int p = this.phase();
        if (p > this.lastPhase && state != SWING && state != POUNCE) {
            this.lastPhase = p;
            this.setState(AWAKEN);
            return;
        }
        if (state == AWAKEN) {
            this.tickAwaken(level);
        } else if (state == SWING) {
            this.tickSwing(level);
        }
    }

    /** Crawling up out of the ground, the earth heaving, and a roar. */
    private void tickEmerge(ServerLevel level) {
        int t = this.stateTicks;
        this.getNavigation().stop();
        this.setDeltaMovement(0.0, Math.min(0.0, this.getDeltaMovement().y), 0.0);
        BlockState ground = this.groundState();
        if (t == 1) {
            this.playSound(SoundEvents.WARDEN_EMERGE, 5.0F, 0.8F);
        }
        if (t < 62 && t % 7 == 0) {
            level.playSound(null, this.getX(), this.getY(), this.getZ(), ground.getSoundType().getBreakSound(), SoundSource.HOSTILE, 2.0F, 0.6F);
            this.playSound(SoundEvents.SPIDER_STEP, 2.0F, 0.5F);
        }
        if (t == 40) {
            this.playSound(ModSounds.STRUMMER_HISS.get(), 3.0F, 0.6F);
        }
        if (t == 62) {
            this.playSound(SoundEvents.WARDEN_ROAR, 4.0F, 1.25F);
            this.playSound(ModSounds.STRUMMER_CHORD.get(), 3.0F, 0.7F);
            level.sendParticles(ModParticles.RESONANCE_RING.get(), this.getX(), this.getY() + 0.2, this.getZ(), 0, 7.0, 0.0, 0.0, 1.0);
            level.sendParticles(ModParticles.SIFT_NOTE.get(), this.getX(), this.getY() + 2.5, this.getZ(), 24, 2.5, 1.0, 2.5, 1.0);
            TemporaryBlocks.webRing(level, this.blockPosition(), 4.5, 240);
        }
        if (t >= EMERGE_TICKS) {
            this.emerged = true;
            this.lastPhase = this.phase();
            this.setState(IDLE);
            this.cooldown = 20;
        }
    }

    /** Between movements: it rears up and roars, and webs burst up in a ring around it. */
    private void tickAwaken(ServerLevel level) {
        int t = this.stateTicks;
        this.getNavigation().stop();
        double r = 5.5;
        if (t == 1) {
            this.playSound(ModSounds.STRUMMER_HISS.get(), 3.0F, 0.7F);
        }
        if (t < 22 && t % 2 == 0) {
            // the telegraph: a ring of light where the webs will fall
            for (int i = 0; i < 12; i++) {
                double a = (i + t * 0.1) * Math.PI / 6.0;
                level.sendParticles(new DustParticleOptions(0x29DFEB, 1.2F), this.getX() + Math.cos(a) * r, this.getY() + 0.2, this.getZ() + Math.sin(a) * r,
                        1, 0, 0, 0, 0);
            }
        }
        if (t == 20) {
            this.playSound(SoundEvents.WARDEN_ROAR, 4.0F, 1.4F);
            this.playSound(ModSounds.STRUMMER_CHORD.get(), 3.0F, 0.6F);
            level.sendParticles(ModParticles.RESONANCE_RING.get(), this.getX(), this.getY() + 0.2, this.getZ(), 0, r + 2.0, 0.0, 0.0, 1.0);
            TemporaryBlocks.webRing(level, this.blockPosition(), r, 260);
            this.hitAround(level, 3.0, 5.0F, 1.2, 0.5, false);
        }
        if (t == 30) {
            this.summon(level, ModEntities.STRUMLING.get(), 2, this.phase() == 3 ? 8 : 6, 3.0);
        }
        if (t >= 46) {
            this.endAttack(10);
        }
    }

    private BlockState groundState() {
        BlockState s = this.level().getBlockState(this.blockPosition().below());
        return s.isAir() ? Blocks.SCULK.defaultBlockState() : s;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.getState() == EMERGE) {
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean isPushable() {
        return this.getState() != EMERGE && this.getState() != SWING && super.isPushable();
    }

    // ------------------------------------------------------------------ attacks

    @Override
    protected void tickAttacks(ServerLevel level, LivingEntity target, int state) {
        int t = this.stateTicks;
        double dist = this.distanceTo(target);
        int phase = this.phase();
        boolean rage = phase == 3;
        if (this.strumCooldown > 0) {
            this.strumCooldown--;
        }
        if (this.swingCooldown > 0) {
            this.swingCooldown--;
        }
        if (this.weaveCooldown > 0) {
            this.weaveCooldown--;
        }
        switch (state) {
            case IDLE -> {
                if (this.cooldown > 0) {
                    return;
                }
                int roll = this.random.nextInt(10);
                if (this.strumCooldown <= 0) {
                    this.setState(STRUM);
                } else if (this.summonCooldown <= 0 && roll < 3) {
                    this.setState(BROOD);
                    this.summonCooldown = this.paced(280);
                } else if (phase >= 2 && this.swingCooldown <= 0 && dist > 5.0 && dist < 22.0 && roll < 6 && this.prepareSwing(level, target)) {
                    this.setState(SWING);
                    this.swingCooldown = this.paced(220);
                } else if (phase >= 2 && this.weaveCooldown <= 0 && dist < 18.0 && roll < 6) {
                    this.weaveCentre = target.blockPosition();
                    this.setState(WEAVE);
                    this.weaveCooldown = this.paced(200);
                } else if (dist < 4.0) {
                    this.setState(roll < 7 ? SLASH : WEB);
                } else if (dist < 11.0 && roll < 4) {
                    this.setState(POUNCE_WINDUP);
                } else if (dist < 16.0 && roll < 7) {
                    this.setState(SNAP);
                } else if (dist < 22.0) {
                    this.setState(WEB);
                }
                if (this.getState() != IDLE && this.getState() != STRUM) {
                    this.getNavigation().stop();
                }
            }
            case SLASH -> {
                this.getNavigation().stop();
                this.getLookControl().setLookAt(target, 30.0F, 30.0F);
                if (t == 8 || t == 15) {
                    this.playSound(ModSounds.STRUMMER_SLASH.get(), 2.0F, t == 8 ? 1.0F : 1.2F);
                    Vec3 f = this.getLookAngle().multiply(1, 0, 1).normalize();
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(2.6, 1.0, 2.6))) {
                        Vec3 to = e.position().subtract(this.position()).multiply(1, 0, 1);
                        if (e != this && !isBandmate(e) && to.length() < 4.2 && to.normalize().dot(f) > 0.2
                                && e.hurtServer(level, this.damageSources().mobAttack(this), 7.0F)) {
                            e.push(f.x * 0.5, 0.25, f.z * 0.5);
                        }
                    }
                    level.sendParticles(ParticleTypes.SWEEP_ATTACK, this.getX() + f.x * 2.0, this.getY() + 1.6, this.getZ() + f.z * 2.0, 2, 0.5, 0.2, 0.5, 0);
                }
                if (t >= 24) {
                    this.endAttack(this.paced(16));
                }
            }
            case WEB -> {
                this.getNavigation().stop();
                this.getLookControl().setLookAt(target, 30.0F, 30.0F);
                if (t == 14) {
                    this.playSound(ModSounds.STRUMMER_SPIT.get(), 2.0F, 1.0F);
                    int shots = 2 + phase;
                    for (int i = 0; i < shots; i++) {
                        WebShot w = new WebShot(level, this);
                        w.setPos(this.getX(), this.getY() + 1.2, this.getZ());
                        Vec3 d = target.position().add(0, target.getBbHeight() * 0.4, 0).subtract(w.position());
                        double spread = (i - (shots - 1) / 2.0) * 0.18;
                        Vec3 side = new Vec3(-d.z, 0, d.x).normalize().scale(d.length() * spread);
                        Vec3 aim = d.add(side).add(0, d.horizontalDistance() * 0.1, 0);
                        w.shoot(aim.x, aim.y, aim.z, 1.1F, 2.0F);
                        level.addFreshEntity(w);
                    }
                }
                if (t >= 24) {
                    this.endAttack(this.paced(25));
                }
            }
            case POUNCE_WINDUP -> {
                this.getNavigation().stop();
                this.getLookControl().setLookAt(target, 30.0F, 30.0F);
                if (t == 1) {
                    this.playSound(ModSounds.STRUMMER_HISS.get(), 2.0F, 1.0F);
                }
                if (t >= 14) {
                    Vec3 d = target.position().subtract(this.position());
                    Vec3 h = d.multiply(1, 0, 1);
                    double len = Math.max(1.0, h.length());
                    this.setDeltaMovement(h.normalize().scale(Math.min(1.5, len * 0.16)).add(0, 0.55 + Math.max(0.0, d.y) * 0.08, 0));
                    this.setState(POUNCE);
                }
            }
            case POUNCE -> {
                if (t > 3 && this.onGround()) {
                    this.land(level);
                    this.endAttack(this.paced(25));
                } else if (t > 40) {
                    this.endAttack(20);
                }
            }
            case BROOD -> {
                this.getNavigation().stop();
                if (t % 4 == 0) {
                    level.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY() + 0.5, this.getZ(), 4, 1.2, 0.3, 1.2, 0.02);
                    level.sendParticles(ParticleTypes.WHITE_ASH, this.getX(), this.getY() + 0.5, this.getZ(), 10, 1.2, 0.3, 1.2, 0.02);
                }
                if (t == 20) {
                    this.summon(level, ModEntities.STRUMLING.get(), 1 + phase, 4 + phase * 2, 2.5);
                    this.playSound(ModSounds.STRUMMER_HISS.get(), 2.0F, 1.5F);
                }
                if (t >= 30) {
                    this.endAttack(20);
                }
            }
            case STRUM -> {
                // it keeps scuttling after you while the mantis plays
                if (t % 5 == 0 && t <= 30) {
                    int[] chord = {0, 4, 7, 12, 7, 4, 0};
                    float pitch = (float) Math.pow(2.0, (chord[(t / 5) % chord.length] - 6) / 12.0);
                    this.playSound(SoundEvents.NOTE_BLOCK_GUITAR.value(), 2.5F, pitch);
                    this.playSound(SoundEvents.NOTE_BLOCK_HARP.value(), 1.5F, pitch * 2.0F);
                }
                if (t == 30) {
                    this.buffBand(level, phase);
                    this.ringWebs(level, phase);
                }
                if (t >= 40) {
                    this.strumCooldown = this.paced(240);
                    this.endAttack(10);
                }
            }
            case SNAP -> {
                this.getNavigation().stop();
                this.getLookControl().setLookAt(target, 30.0F, 30.0F);
                if (t == 1) {
                    this.playSound(ModSounds.STRUMMER_DRAW.get(), 2.0F, 1.0F);
                }
                if (t == 16 && this.hasLineOfSight(target) && dist < 18.0) {
                    this.playSound(ModSounds.STRUMMER_PLUCK.get(), 2.5F, 1.0F);
                    Vec3 from = this.position().add(0, 2.6, 0);
                    Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0);
                    Vec3 d = to.subtract(from);
                    this.silkLine(level, from, to, 4.0);
                    if (target.hurtServer(level, this.damageSources().mobAttack(this), 4.0F)) {
                        Vec3 pull = d.normalize().scale(-1.3);
                        target.push(pull.x, 0.45, pull.z);
                    }
                }
                if (t >= 26) {
                    this.endAttack(this.paced(25));
                }
            }
            case WEAVE -> this.tickWeave(level, target, t, phase);
            case EMERGE, AWAKEN, SWING -> {
                // run from tickAlways
            }
            default -> this.endAttack(20);
        }
    }

    /** Lands from a pounce or a swing: a blow all around, and webs where it came down. */
    private void land(ServerLevel level) {
        this.playSound(ModSounds.STRUMMER_LAND.get(), 3.0F, 0.8F);
        level.sendParticles(ModParticles.RESONANCE_RING.get(), this.getX(), this.getY() + 0.1, this.getZ(), 0, 3.5, 0.0, 0.0, 1.0);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, this.groundState()), this.getX(), this.getY() + 0.1, this.getZ(), 30, 1.5, 0.1, 1.5, 0.15);
        this.hitAround(level, 2.5, 9.0F, 0.8, 0.4, false);
        TemporaryBlocks.webs(level, this.blockPosition(), 3, 0.12F, 140);
    }

    /** A strand of glowing silk drawn from a to b. */
    private void silkLine(ServerLevel level, Vec3 from, Vec3 to, double perBlock) {
        Vec3 d = to.subtract(from);
        DustParticleOptions dust = new DustParticleOptions(0x7FF7FF, 1.0F);
        int n = (int) Math.max(2, d.length() * perBlock);
        for (int i = 0; i <= n; i++) {
            Vec3 p = from.add(d.scale(i / (double) n));
            level.sendParticles(dust, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    // ------------------------------------------------------------------ the swing

    /** Finds something to hang a thread from above the middle of the way, and plans the arc. */
    private boolean prepareSwing(ServerLevel level, LivingEntity target) {
        Vec3 from = this.position();
        Vec3 to = target.position();
        Vec3 h = to.subtract(from).multiply(1, 0, 1);
        double span = h.length();
        if (span < 4.0) {
            return false;
        }
        Vec3 mid = from.add(h.scale(0.5));
        double floor = Math.max(from.y, to.y);
        double top = floor + 12.0;
        // the ceiling if there is one, else the dark above
        for (int dy = 3; dy <= 16; dy++) {
            BlockPos p = BlockPos.containing(mid.x, floor + dy, mid.z);
            if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty()) {
                top = p.getY();
                break;
            }
        }
        double rope = top - floor - 2.0;
        if (rope < 4.0) {
            return false;
        }
        this.swingFrom = from;
        this.swingAnchor = new Vec3(Mth.floor(mid.x) + 0.5, top, Mth.floor(mid.z) + 0.5);
        this.swingDir = h.normalize();
        this.swingRope = rope;
        this.swingAngle = Math.asin(Mth.clamp(span * 0.5 / rope, 0.35, 0.9));
        this.swingHits.clear();
        this.entityData.set(THREAD, BlockPos.containing(this.swingAnchor));
        return true;
    }

    /** Where it hangs on the thread at swing angle a (radians from straight down). */
    private Vec3 onThread(double a) {
        return this.swingAnchor.add(this.swingDir.scale(Math.sin(a) * this.swingRope)).add(0, -Math.cos(a) * this.swingRope, 0);
    }

    private void tickSwing(ServerLevel level) {
        int t = this.stateTicks;
        this.getNavigation().stop();
        if (t == 1) {
            this.playSound(ModSounds.STRUMMER_SPIT.get(), 2.0F, 1.4F);
            this.playSound(ModSounds.STRUMMER_DRAW.get(), 2.0F, 0.8F);
        }
        if (t < SWING_AIM) {
            // the telegraph: it rears and fires the thread up
            this.setDeltaMovement(0.0, Math.min(0.0, this.getDeltaMovement().y), 0.0);
            if (t == 6) {
                level.sendParticles(ParticleTypes.WHITE_ASH, this.swingAnchor.x, this.swingAnchor.y - 0.2, this.swingAnchor.z, 12, 0.4, 0.1, 0.4, 0.02);
            }
            return;
        }
        Vec3 want;
        if (t < SWING_AIM + SWING_REEL) {
            // reeling itself up the thread to the top of the swing
            float u = (t - SWING_AIM + 1) / (float) SWING_REEL;
            float s = u * u * (3.0F - 2.0F * u);
            want = this.swingFrom.lerp(this.onThread(-this.swingAngle), s);
            if (t % 4 == 0) {
                this.playSound(ModSounds.STRUMMER_DRAW.get(), 1.0F, 1.2F + u * 0.4F);
            }
        } else {
            float u = Math.min(1.0F, (t - SWING_AIM - SWING_REEL + 1) / (float) SWING_ARC);
            // a pendulum: slow at the top, fastest at the bottom
            want = this.onThread(-this.swingAngle * Math.cos(Math.PI * u));
            if (t == SWING_AIM + SWING_REEL) {
                this.playSound(ModSounds.STRUMMER_HISS.get(), 2.5F, 0.9F);
            }
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.8))) {
                if (e != this && !isBandmate(e) && this.swingHits.add(e.getId()) && e.hurtServer(level, this.damageSources().mobAttack(this), 8.0F)) {
                    e.push(this.swingDir.x * 1.2, 0.5, this.swingDir.z * 1.2);
                    this.playSound(ModSounds.STRUMMER_SLASH.get(), 2.0F, 0.8F);
                }
            }
            if (u >= 1.0F || (u > 0.15F && this.horizontalCollision)) {
                // let go and drop on them
                LivingEntity target = this.getTarget();
                Vec3 h = target != null ? target.position().subtract(this.position()).multiply(1, 0, 1) : Vec3.ZERO;
                this.setDeltaMovement(h.scale(0.1).add(this.swingDir.scale(0.2)).add(0, 0.2, 0));
                this.playSound(ModSounds.STRUMMER_PLUCK.get(), 2.5F, 0.7F);
                this.setState(POUNCE);
                return;
            }
        }
        this.setDeltaMovement(want.subtract(this.position()));
        this.resetFallDistance();
    }

    // ------------------------------------------------------------------ weaving

    private void tickWeave(ServerLevel level, LivingEntity target, int t, int phase) {
        this.getNavigation().stop();
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        double radius = phase == 3 ? 5.0 : 4.0;
        if (t == 1) {
            this.playSound(ModSounds.STRUMMER_HISS.get(), 2.0F, 1.2F);
        }
        if (t < WEAVE_AIM && t % 2 == 0) {
            // the telegraph: the orb's outline glows on the ground before the silk flies
            for (int i = 0; i < 16; i++) {
                double a = i * Math.PI / 8.0 + t * 0.05;
                level.sendParticles(new DustParticleOptions(0x7FF7FF, 1.0F), this.weaveCentre.getX() + 0.5 + Math.cos(a) * radius, this.weaveCentre.getY() + 0.15,
                        this.weaveCentre.getZ() + 0.5 + Math.sin(a) * radius, 1, 0, 0, 0, 0);
            }
        }
        int strands = phase == 3 ? 18 : 13;
        int k = t - WEAVE_AIM;
        if (k >= 0 && k < strands) {
            // a spiral of strands from the hub outwards, two a tick: an orb web, woven in a blink
            for (int j = 0; j < 2; j++) {
                int n = k * 2 + j;
                double a = n * 2.39996;
                double r = 0.8 + (radius - 0.8) * n / (strands * 2.0);
                BlockPos at = this.weaveAt(level, this.weaveCentre.getX() + 0.5 + Math.cos(a) * r, this.weaveCentre.getZ() + 0.5 + Math.sin(a) * r,
                        this.weaveCentre.getY() + (n % 4 == 0 ? 1 : 0), 160 + this.random.nextInt(60));
                if (at != null) {
                    this.silkLine(level, this.position().add(0, 1.4, 0), Vec3.atCenterOf(at), 1.5);
                    if (j == 0) {
                        float pitch = (float) Math.pow(2.0, (MusicalCobwebBlock.noteAt(at) - 12) / 12.0);
                        level.playSound(null, at, SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.HOSTILE, 1.2F, pitch);
                    }
                }
            }
        }
        if (t == WEAVE_AIM + strands + 2 && phase == 3) {
            this.playSound(ModSounds.STRUMMER_PLUCK.get(), 2.5F, 1.3F);
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1), this);
        }
        if (t >= WEAVE_AIM + strands + 10) {
            this.endAttack(this.paced(20));
        }
    }

    /** One strand on the ground (or just above it) near x, z. */
    private @org.jspecify.annotations.Nullable BlockPos weaveAt(ServerLevel level, double x, double z, int y, int ticks) {
        for (int dy = 1; dy >= -2; dy--) {
            BlockPos p = BlockPos.containing(x, y + dy, z);
            if (!level.getBlockState(p.below()).isAir() && TemporaryBlocks.web(level, p, ticks)) {
                return p;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ the strum

    /** The mantis's chord: every bandmate around it is buffed, and so is the spider. */
    private void buffBand(ServerLevel level, int phase) {
        level.sendParticles(ModParticles.RESONANCE_RING.get(), this.getX(), this.getY() + 0.2, this.getZ(), 0, 6.0, 0.0, 0.0, 1.0);
        level.sendParticles(ModParticles.RESONANCE_RING.get(), this.getX(), this.getY() + 0.2, this.getZ(), 0, 10.0, 0.0, 0.0, 1.0);
        level.sendParticles(ModParticles.SIFT_NOTE.get(), this.getX(), this.getY() + 3.0, this.getZ(), 30, 2.5, 1.0, 2.5, 1.0);
        this.playSound(ModSounds.STRUMMER_CHORD.get(), 3.0F, 1.0F);
        int amp = phase == 3 ? 1 : 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(this.blockPosition()).inflate(16.0))) {
            if (isBandmate(e)) {
                e.addEffect(new MobEffectInstance(MobEffects.SPEED, 160, amp), this);
                e.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 160, amp), this);
                e.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 160, 0), this);
                level.sendParticles(ModParticles.SIFT_NOTE.get(), e.getX(), e.getY() + e.getBbHeight() + 0.3, e.getZ(), 4, 0.3, 0.2, 0.3, 1.0);
            }
        }
        for (Player p : level.getEntitiesOfClass(Player.class, new AABB(this.blockPosition()).inflate(8.0))) {
            // the chord is deafening up close
            p.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 80, 0), this);
        }
    }

    /**
     * From the second movement on, its webs are part of the instrument: the chord makes every
     * Musical Cobweb nearby ring out, and from the third, the ringing hurts whoever stands in one.
     */
    private void ringWebs(ServerLevel level, int phase) {
        if (phase < 2) {
            return;
        }
        int rung = 0;
        BlockPos c = this.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-12, -4, -12), c.offset(12, 5, 12))) {
            BlockState s = level.getBlockState(p);
            if (!s.is(ModBlocks.MUSICAL_COBWEB.get()) || rung++ > 48) {
                continue;
            }
            if (s.getBlock() instanceof MusicalCobwebBlock web && !s.getValue(MusicalCobwebBlock.RINGING)) {
                web.play(level, p.immutable(), s, null);
            }
            if (phase == 3) {
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(p).inflate(0.4))) {
                    if (!isBandmate(e) && e.hurtServer(level, this.damageSources().indirectMagic(this, this), 3.0F)) {
                        e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1), this);
                    }
                }
            }
        }
    }

    private void endAttack(int cooldown) {
        this.setState(IDLE);
        this.cooldown = cooldown + this.random.nextInt(10);
    }

    // ------------------------------------------------------------------ both sides

    @Override
    public void tick() {
        super.tick();
        int s = this.getState();
        if (!this.level().isClientSide()) {
            this.entityData.set(CLIMBING, this.horizontalCollision && s != SWING && s != EMERGE);
            return;
        }
        int et = this.tickCount - this.stateStart;
        if (s == EMERGE && this.deathTime == 0) {
            // the ground heaves and breaks as it claws its way out
            BlockParticleOption dirt = new BlockParticleOption(ParticleTypes.BLOCK, this.groundState());
            int n = et < 62 ? 6 : 2;
            for (int i = 0; i < n; i++) {
                double a = this.random.nextDouble() * Math.PI * 2.0;
                double r = this.random.nextDouble() * 2.2;
                this.level().addParticle(dirt, this.getX() + Math.cos(a) * r, this.getY() + 0.1, this.getZ() + Math.sin(a) * r, 0.0, 0.25, 0.0);
            }
            if (this.random.nextInt(3) == 0) {
                this.level().addParticle(ParticleTypes.SCULK_SOUL, this.getRandomX(1.0), this.getY() + 0.2, this.getRandomZ(1.0), 0.0, 0.06, 0.0);
            }
            if (et < 60 && et % 6 == 0) {
                Rumble.at(this.position(), 1.4F, 28.0F, 10);
            }
            if (et == 62) {
                Rumble.at(this.position(), 3.5F, 48.0F, 36);
            }
        } else if (s == AWAKEN && et == 20) {
            Rumble.at(this.position(), 2.5F, 40.0F, 24);
        }
        // the strings shimmer; while it strums, notes spill from them
        if (s != EMERGE && this.random.nextInt(s == STRUM ? 1 : 6) == 0) {
            float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
            for (int side = -1; side <= 1; side += 2) {
                double x = this.getX() - Mth.sin(yaw) * 0.6 + Mth.cos(yaw) * 0.7 * side;
                double z = this.getZ() + Mth.cos(yaw) * 0.6 + Mth.sin(yaw) * 0.7 * side;
                double y = this.getY() + 1.6 + this.random.nextDouble() * 1.2;
                this.level().addParticle(new DustParticleOptions(0x7FF7FF, 0.7F), x, y, z, 0, 0, 0);
                if (s == STRUM && this.random.nextInt(3) == 0) {
                    this.level().addParticle(ModParticles.SIFT_NOTE.get(), x, y + 0.5, z, this.random.nextDouble(), 0, 0);
                }
            }
        }
    }

    @Override
    public boolean onClimbable() {
        return this.isClimbing();
    }

    @Override
    public void makeStuckInBlock(BlockState state, Vec3 speedMultiplier) {
        if (!state.is(Blocks.COBWEB)) {
            super.makeStuckInBlock(state, speedMultiplier);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Emerged", this.emerged);
        output.putInt("Movement", this.lastPhase);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.emerged = input.getBooleanOr("Emerged", true);
        this.lastPhase = input.getIntOr("Movement", 1);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.STRUMMER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.STRUMMER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.STRUMMER_DEATH.get();
    }

    @Override
    protected int burstColorA() {
        return 0x29DFEB;
    }

    @Override
    protected int burstColorB() {
        return 0xC46CFF;
    }
}
