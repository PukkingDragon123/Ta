package com.thesift.world.sky;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * W-sky: the Sky Vine a swinging player holds, seen by everyone: it sits where the rope hangs from and its renderer
 * draws a liana from there to the player's hands (SkyRopeRenderer). It lives only while the player swings and is
 * never saved.
 */
public class SkyRope extends Entity {
    private static final EntityDataAccessor<Integer> HOLDER = SynchedEntityData.defineId(SkyRope.class, EntityDataSerializers.INT);

    public SkyRope(EntityType<? extends SkyRope> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    static SkyRope create(ServerLevel level, Player holder, Vec3 pivot) {
        SkyRope rope = new SkyRope(SkyIslands.SKY_ROPE.get(), level);
        rope.entityData.set(HOLDER, holder.getId());
        rope.setPos(pivot.x, pivot.y, pivot.z);
        level.addFreshEntity(rope);
        return rope;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(HOLDER, -1);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }

    /** The player holding this rope, if still around. */
    public @Nullable Player holder() {
        int id = this.entityData.get(HOLDER);
        return id >= 0 && this.level().getEntity(id) instanceof Player player ? player : null;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel && this.tickCount > 2) {
            Player holder = this.holder();
            if (holder == null || !holder.isAlive() || !SkySwing.holds(holder, this)) {
                this.discard();
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }
}
