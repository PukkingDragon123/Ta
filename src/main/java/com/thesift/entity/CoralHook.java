package com.thesift.entity;

import com.thesift.registry.ModSculkSea;
import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * CR3 Fish &amp; Coral Organs: the hooked line a {@link CoralOrgan} fires. It flies straight from the organ's
 * horn; whatever it strikes (a swimmer, a player at the surface, a fish) is caught and reeled steadily down to
 * the organ's pipes, and held there. Once it has caught someone it sits round its catch's head, so every blow
 * the catch strikes, whichever way they look, lands on the line (friends can cut it from outside too): three
 * blows, or two heavy ones, and it snaps. It also lets go if the organ dies, the catch gets too far away, or
 * after half a minute. Lines are never saved: a reloaded one is simply gone.
 */
public class CoralHook extends Entity {
    private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(CoralHook.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HOOKED = SynchedEntityData.defineId(CoralHook.class, EntityDataSerializers.INT);
    private static final double REEL_SPEED = 0.17;
    private static final int MAX_HOLD = 600;
    private static final int MAX_FLIGHT = 40;
    private int strength = 3;
    private int life;
    private int clampTimer = 10;

    public CoralHook(EntityType<? extends CoralHook> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER, -1);
        builder.define(HOOKED, -1);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }

    /** Sets the hook flying from the organ's mouth. */
    void launch(CoralOrgan owner, Vec3 from, Vec3 velocity) {
        this.entityData.set(OWNER, owner.getId());
        this.setDeltaMovement(velocity);
        this.setCentre(from);
    }

    public @Nullable CoralOrgan owner() {
        int id = this.entityData.get(OWNER);
        return id >= 0 && this.level().getEntity(id) instanceof CoralOrgan organ ? organ : null;
    }

    public @Nullable LivingEntity hooked() {
        int id = this.entityData.get(HOOKED);
        return id >= 0 && this.level().getEntity(id) instanceof LivingEntity caught ? caught : null;
    }

    /** True while some line holds this creature. */
    public static boolean isHooked(LivingEntity e) {
        return !e.level().getEntitiesOfClass(CoralHook.class, e.getBoundingBox().inflate(2.0), h -> h.hooked() == e).isEmpty();
    }

    private Vec3 centre() {
        return this.position().add(0.0, this.getBbHeight() * 0.5, 0.0);
    }

    private void setCentre(Vec3 c) {
        this.setPos(c.x, c.y - this.getBbHeight() * 0.5, c.z);
    }

    /** Sits round the catch's head: its eyes are inside the line, so the line is what it strikes. */
    private void hold(LivingEntity caught) {
        this.setCentre(caught.getEyePosition());
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity caught = this.hooked();
        if (!(this.level() instanceof ServerLevel level)) {
            if (caught != null) {
                this.hold(caught);
            }
            return;
        }
        CoralOrgan owner = this.owner();
        this.life++;
        if (owner == null || !owner.isAlive()) {
            this.snap(level);
            return;
        }
        if (this.entityData.get(HOOKED) >= 0) {
            if (caught == null) {
                this.discard(); // the catch is gone (a fish swallowed whole)
            } else {
                this.reel(level, owner, caught);
            }
            return;
        }
        this.fly(level, owner);
    }

    private boolean catchable(LivingEntity e, CoralOrgan owner) {
        if (e == owner || !e.isAlive() || e instanceof CoralOrgan || e instanceof SculkFish || isHooked(e)) {
            return false;
        }
        return !(e instanceof Player p) || !p.isCreative() && !p.isSpectator();
    }

    private void fly(ServerLevel level, CoralOrgan owner) {
        Vec3 pos = this.centre();
        Vec3 next = pos.add(this.getDeltaMovement());
        LivingEntity hit = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos, next).inflate(0.6), e -> this.catchable(e, owner))) {
            AABB box = e.getBoundingBox().inflate(0.3);
            Optional<Vec3> at = box.clip(pos, next);
            if (at.isPresent() || box.contains(pos)) {
                double d = pos.distanceToSqr(e.position());
                if (d < best) {
                    best = d;
                    hit = e;
                }
            }
        }
        if (hit != null) {
            this.entityData.set(HOOKED, hit.getId());
            this.hold(hit);
            hit.stopRiding();
            this.playSound(ModSculkSea.HOOK_HIT.get(), 1.4F, 0.9F + this.random.nextFloat() * 0.2F);
            level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, hit.getX(), hit.getY() + hit.getBbHeight() * 0.5, hit.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
            return;
        }
        HitResult wall = level.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (wall.getType() != HitResult.Type.MISS || this.life > MAX_FLIGHT || pos.distanceTo(owner.mouth()) > CoralOrgan.RANGE + 6.0) {
            // missed: it clatters off the rock and is drawn back in
            level.sendParticles(ParticleTypes.BUBBLE, pos.x, pos.y, pos.z, 8, 0.15, 0.15, 0.15, 0.08);
            this.playSound(ModSculkSea.HOOK_STRAIN.get(), 0.8F, 1.4F);
            this.discard();
            return;
        }
        this.setCentre(next);
        if (this.tickCount % 2 == 0) {
            level.sendParticles(ParticleTypes.BUBBLE, pos.x, pos.y, pos.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    private void reel(ServerLevel level, CoralOrgan owner, LivingEntity caught) {
        if (!caught.isAlive() || caught instanceof Player p && (p.isCreative() || p.isSpectator()) || caught.distanceToSqr(owner) > 44.0 * 44.0
                || this.life > MAX_HOLD) {
            this.snap(level);
            return;
        }
        if (caught.isPassenger()) {
            caught.stopRiding(); // dragged out of the boat
        }
        this.hold(caught);
        Vec3 mouth = owner.mouth();
        Vec3 body = caught.position().add(0.0, caught.getBbHeight() * 0.5, 0.0);
        Vec3 to = mouth.subtract(body);
        double d = to.length();
        // a steady drag down the line, then held fast at the pipes
        Vec3 want = d > 1.4 ? to.scale(REEL_SPEED / d) : to.scale(0.25);
        Vec3 delta = want.subtract(caught.getDeltaMovement());
        caught.push(delta.x, delta.y, delta.z);
        caught.resetFallDistance();
        if (d <= 1.8) {
            if (--this.clampTimer <= 0) {
                this.clampTimer = 30;
                owner.clamp(level, caught);
            }
        } else {
            this.clampTimer = Math.min(this.clampTimer, 10);
            if (this.tickCount % 12 == 0) {
                owner.playSound(ModSculkSea.ORGAN_REEL.get(), 1.4F, 0.8F + this.random.nextFloat() * 0.25F);
            }
        }
        if (this.tickCount % 3 == 0) {
            Vec3 p = body.add(to.scale(this.random.nextDouble()));
            level.sendParticles(ParticleTypes.BUBBLE, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.02);
        }
    }

    /** The line breaks: a twang, a burst of bubbles and sculk, and it is gone. */
    private void snap(ServerLevel level) {
        LivingEntity caught = this.hooked();
        Vec3 at = caught != null ? caught.getEyePosition() : this.centre();
        this.playSound(ModSculkSea.HOOK_SNAP.get(), 1.4F, 0.9F + this.random.nextFloat() * 0.3F);
        level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, at.x, at.y - 0.3, at.z, 10, 0.3, 0.3, 0.3, 0.04);
        level.sendParticles(ParticleTypes.BUBBLE, at.x, at.y - 0.3, at.z, 16, 0.4, 0.4, 0.4, 0.1);
        this.discard();
    }

    /** Break the line: every hit weakens it, heavy ones twice as much. */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.isRemoved() || source.getEntity() instanceof CoralOrgan) {
            return false;
        }
        this.strength -= amount >= 6.0F ? 2 : 1;
        Vec3 at = this.centre();
        this.playSound(ModSculkSea.HOOK_STRAIN.get(), 1.2F, 0.8F + this.strength * 0.15F);
        level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, at.x, at.y - 0.3, at.z, 4, 0.2, 0.2, 0.2, 0.02);
        if (this.strength <= 0) {
            this.snap(level);
        }
        return true;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean canBePickedFromInside() {
        return true;
    }
}
