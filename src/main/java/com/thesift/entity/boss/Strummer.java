package com.thesift.entity.boss;

import com.thesift.registry.ModEntities;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import com.thesift.world.TemporaryBlocks;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Strummer: a giant mantis riding a giant spider, with glowing strings running from its two
 * scythe hands down to the spider like the strings of a guitar. The spider does the fighting; the
 * mantis plays. It never stops moving and it does everything at once:
 *
 * <ul>
 *   <li>Slash: a double scythe stroke from the mantis.</li>
 *   <li>Web: the spider spits three balls of web that burst into cobwebs where they land.</li>
 *   <li>Pounce: the spider leaps on you and lays a web around where it lands.</li>
 *   <li>Brood: spiderlings - Strumlings - pour out from under the spider.</li>
 *   <li>Strum: the mantis plays a chord and every bandmate nearby is buffed (speed, strength,
 *   resistance), in a storm of notes.</li>
 *   <li>Snap: it plucks one string at you: the string catches and drags you in.</li>
 * </ul>
 * Below half health it plays twice as fast.
 */
public class Strummer extends MiniBoss {
    public static final int SLASH = 1;
    public static final int WEB = 2;
    public static final int POUNCE_WINDUP = 3;
    public static final int POUNCE = 4;
    public static final int BROOD = 5;
    public static final int STRUM = 6;
    public static final int SNAP = 7;
    public static final float SCALE = 1.6F;

    private int strumCooldown = 120;

    public Strummer(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.PURPLE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 220.0).add(Attributes.ARMOR, 6.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 8.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.8).add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.STEP_HEIGHT, 1.5);
    }

    public boolean enraged() {
        return this.getHealth() < this.getMaxHealth() * 0.5F;
    }

    @Override
    protected double chaseSpeed() {
        return this.enraged() ? 1.25 : 1.05;
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
    protected void tickAttacks(ServerLevel level, LivingEntity target, int state) {
        int t = this.stateTicks;
        double dist = this.distanceTo(target);
        boolean rage = this.enraged();
        if (this.strumCooldown > 0) {
            this.strumCooldown--;
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
                    this.summonCooldown = rage ? 160 : 260;
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
                    this.endAttack(rage ? 6 : 16);
                }
            }
            case WEB -> {
                this.getNavigation().stop();
                this.getLookControl().setLookAt(target, 30.0F, 30.0F);
                if (t == 14) {
                    this.playSound(ModSounds.STRUMMER_SPIT.get(), 2.0F, 1.0F);
                    int shots = rage ? 5 : 3;
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
                    this.endAttack(rage ? 10 : 25);
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
                    this.playSound(ModSounds.STRUMMER_LAND.get(), 3.0F, 0.8F);
                    level.sendParticles(ModParticles.RESONANCE_RING.get(), this.getX(), this.getY() + 0.1, this.getZ(), 0, 3.5, 0.0, 0.0, 1.0);
                    this.hitAround(level, 2.5, 9.0F, 0.8, 0.4, false);
                    TemporaryBlocks.webs(level, this.blockPosition(), 3, 0.12F, 140);
                    this.endAttack(rage ? 10 : 25);
                } else if (t > 40) {
                    this.endAttack(20);
                }
            }
            case BROOD -> {
                this.getNavigation().stop();
                if (t % 4 == 0) {
                    level.sendParticles(ParticleTypes.WHITE_ASH, this.getX(), this.getY() + 0.5, this.getZ(), 10, 1.2, 0.3, 1.2, 0.02);
                }
                if (t == 20) {
                    this.summon(level, ModEntities.STRUMLING.get(), rage ? 4 : 3, rage ? 10 : 7, 2.5);
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
                    this.buffBand(level, rage);
                }
                if (t >= 40) {
                    this.strumCooldown = rage ? 140 : 240;
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
                    DustParticleOptions dust = new DustParticleOptions(0x7FF7FF, 1.0F);
                    for (int i = 0; i < d.length() * 4; i++) {
                        Vec3 p = from.add(d.scale(i / (d.length() * 4)));
                        level.sendParticles(dust, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                    }
                    if (target.hurtServer(level, this.damageSources().mobAttack(this), 4.0F)) {
                        Vec3 pull = d.normalize().scale(-1.3);
                        target.push(pull.x, 0.45, pull.z);
                    }
                }
                if (t >= 26) {
                    this.endAttack(rage ? 10 : 25);
                }
            }
            default -> this.endAttack(20);
        }
    }

    /** The mantis's chord: every bandmate around it is buffed, and so is the spider. */
    private void buffBand(ServerLevel level, boolean rage) {
        level.sendParticles(ModParticles.RESONANCE_RING.get(), this.getX(), this.getY() + 0.2, this.getZ(), 0, 6.0, 0.0, 0.0, 1.0);
        level.sendParticles(ModParticles.RESONANCE_RING.get(), this.getX(), this.getY() + 0.2, this.getZ(), 0, 10.0, 0.0, 0.0, 1.0);
        level.sendParticles(ModParticles.SIFT_NOTE.get(), this.getX(), this.getY() + 3.0, this.getZ(), 30, 2.5, 1.0, 2.5, 1.0);
        this.playSound(ModSounds.STRUMMER_CHORD.get(), 3.0F, 1.0F);
        int amp = rage ? 1 : 0;
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

    private void endAttack(int cooldown) {
        this.setState(IDLE);
        this.cooldown = cooldown + this.random.nextInt(10);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            // the strings shimmer; while it strums, notes spill from them
            int s = this.getState();
            if (this.random.nextInt(s == STRUM ? 1 : 6) == 0) {
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
    }

    @Override
    public boolean onClimbable() {
        return this.horizontalCollision;
    }

    @Override
    public void makeStuckInBlock(net.minecraft.world.level.block.state.BlockState state, Vec3 speedMultiplier) {
        if (!state.is(net.minecraft.world.level.block.Blocks.COBWEB)) {
            super.makeStuckInBlock(state, speedMultiplier);
        }
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
        return 0xC46CFF;
    }

    @Override
    protected int burstColorB() {
        return 0x7FC04A;
    }
}
