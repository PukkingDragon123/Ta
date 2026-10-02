package com.thesift.block.entity;

import com.thesift.block.AncientCannonBlock;
import com.thesift.entity.siege.Cannonball;
import com.thesift.registry.ModSiege;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Remembers an Ancient Cannon's heading (yaw and elevation) and who lit it last. */
public class AncientCannonBlockEntity extends BlockEntity {
    /** Muzzle speed: from a tower top it carries right across the arena. */
    private static final float SPEED = 1.75F;
    private static final float MIN_ELEVATION = 5.0F;
    private static final float MAX_ELEVATION = 60.0F;

    private float yaw = Float.NaN;
    private float elevation = 25.0F;
    private long readyAt;
    private @Nullable LivingEntity gunner;

    public AncientCannonBlockEntity(BlockPos pos, BlockState state) {
        super(ModSiege.ANCIENT_CANNON.get(), pos, state);
    }

    /** Swing round to where the player is looking. */
    public void aim(Player player) {
        this.yaw = Mth.wrapDegrees(player.getYRot());
        this.elevation = Mth.clamp(-player.getXRot() + 15.0F, MIN_ELEVATION, MAX_ELEVATION);
        this.gunner = player;
        this.setChanged();
    }

    public void load(ServerLevel level, Player player) {
        this.aim(player);
        BlockState state = this.getBlockState();
        level.setBlock(this.worldPosition, state.setValue(AncientCannonBlock.LOADED, true)
                .setValue(AncientCannonBlock.FACING, Direction.fromYRot(this.yaw)), Block.UPDATE_ALL);
        level.playSound(null, this.worldPosition, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 0.6F);
        level.playSound(null, this.worldPosition, SoundEvents.DEEPSLATE_BRICKS_PLACE, SoundSource.BLOCKS, 1.0F, 0.7F);
    }

    public void fire(ServerLevel level) {
        BlockState state = this.getBlockState();
        if (!state.getValue(AncientCannonBlock.LOADED) || level.getGameTime() < this.readyAt) {
            return;
        }
        float y = Float.isNaN(this.yaw) ? state.getValue(AncientCannonBlock.FACING).toYRot() : this.yaw;
        Vec3 dir = Vec3.directionFromRotation(-this.elevation, y);
        Vec3 c = Vec3.atBottomCenterOf(this.worldPosition).add(0.0, 0.55, 0.0);
        Vec3 muzzle = c.add(dir.scale(0.9));
        Cannonball ball = Cannonball.ball(level, muzzle, this.gunner != null && this.gunner.isAlive() ? this.gunner : null);
        ball.shoot(dir.x, dir.y, dir.z, SPEED, 0.6F);
        level.addFreshEntity(ball);
        level.setBlock(this.worldPosition, state.setValue(AncientCannonBlock.LOADED, false).setValue(AncientCannonBlock.FACING, Direction.fromYRot(y)),
                Block.UPDATE_ALL);
        this.readyAt = level.getGameTime() + 20;
        level.playSound(null, this.worldPosition, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 2.5F, 1.3F);
        level.playSound(null, this.worldPosition, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.BLOCKS, 3.0F, 0.5F);
        level.sendParticles(ParticleTypes.EXPLOSION, muzzle.x, muzzle.y, muzzle.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, muzzle.x, muzzle.y, muzzle.z, 14, 0.25, 0.25, 0.25, 0.05);
        level.sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 8, 0.1, 0.1, 0.1, 0.08);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!Float.isNaN(this.yaw)) {
            output.putFloat("Yaw", this.yaw);
        }
        output.putFloat("Elevation", this.elevation);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.yaw = input.getFloatOr("Yaw", Float.NaN);
        this.elevation = input.getFloatOr("Elevation", 25.0F);
    }
}
