package com.thesift.entity.cave;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The inside of the Jailer's cell: an invisible box the trapped player stands in (as its passenger),
 * itself carried by the Jailer. The Jailer's model draws the bars around it. Every hit the
 * prisoner lands goes to the bars - their eyes are inside this box, so it is what they strike,
 * whichever way they look - and so does every hit a friend lands on the cell from outside.
 * Sneaking does not get you out (see ModCaveCreatures#onDismount); only breaking the bars does.
 */
public class JailCell extends Entity {
    private boolean released;
    private int lonely;

    public JailCell(EntityType<? extends JailCell> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }

    public @Nullable LivingEntity prisoner() {
        return this.getFirstPassenger() instanceof LivingEntity prisoner ? prisoner : null;
    }

    public @Nullable Jailer jailer() {
        return this.getVehicle() instanceof Jailer jailer ? jailer : null;
    }

    /** True while the cell still holds this player against a sneak-to-dismount. */
    public boolean holdsFast(Player player) {
        return !this.released && !this.isRemoved() && player.isAlive() && player.isShiftKeyDown() && this.hasPassenger(player)
                && this.jailer() != null;
    }

    public void struggle(Player player) {
        Jailer jailer = this.jailer();
        if (jailer != null) {
            jailer.struggle(player);
        }
    }

    /** Opens the cell: lets the prisoner out where they stand and goes away. */
    public void release() {
        this.released = true;
        this.ejectPassengers();
        this.discard();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel && (this.jailer() == null || this.prisoner() == null)) {
            // nobody inside, or nobody carrying it (after a reload, say): it crumbles away
            if (++this.lonely > 2) {
                this.release();
            }
        } else {
            this.lonely = 0;
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        Jailer jailer = this.jailer();
        return !this.released && jailer != null && jailer.hitBars(level, this, source, amount);
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean canBePickedFromInside() {
        return true;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().isEmpty() && passenger instanceof LivingEntity;
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        moveFunction.accept(passenger, this.getX(), this.getY() + 0.05, this.getZ());
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    public boolean dismountsUnderwater() {
        return false;
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        return new Vec3(this.getX(), this.getY(), this.getZ());
    }
}
