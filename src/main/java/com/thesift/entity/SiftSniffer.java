package com.thesift.entity;

import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import com.thesift.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Sift's Sniffers. Wild ones wander and dig up ancient seeds just like their overworld
 * cousins; feed one its favourite food until it trusts you and it becomes yours. A tamed Sniffer
 * takes a saddle and can be ridden. While ridden it ploughs straight through soft ground (soil,
 * sand, gravel, clay, moss) and every so often lifts its nose and points out buried treasure
 * nearby with a trail of glowing dust.
 */
public class SiftSniffer extends Sniffer {
    private static final EntityDataAccessor<Boolean> TAME = SynchedEntityData.defineId(SiftSniffer.class, EntityDataSerializers.BOOLEAN);
    private static final int SNIFF_INTERVAL = 160;
    private static final int SNIFF_RADIUS = 20;

    private int sniffTimer = SNIFF_INTERVAL;
    private int digTimer;

    public SiftSniffer(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TAME, false);
    }

    public boolean isTame() {
        return this.entityData.get(TAME);
    }

    public void setTame(boolean tame) {
        this.entityData.set(TAME, tame);
    }

    // ------------------------------------------------------------------ taming, saddling, riding

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!this.isTame() && !this.isBaby() && this.isFood(stack)) {
            if (this.level() instanceof ServerLevel server) {
                this.usePlayerItem(player, hand, stack);
                this.playEatingSound();
                if (this.random.nextInt(3) == 0) {
                    this.setTame(true);
                    this.setPersistenceRequired();
                    server.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + this.getBbHeight(), this.getZ(), 7, 0.6, 0.3, 0.6, 0.0);
                    this.playSound(ModSounds.SNIFFER_HAPPY.get(), 1.0F, 1.0F);
                    player.sendOverlayMessage(Component.translatable("message.thesift.sniffer.tamed"));
                } else {
                    server.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + this.getBbHeight(), this.getZ(), 6, 0.5, 0.2, 0.5, 0.01);
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (this.isTame() && !this.isBaby()) {
            if (!this.isSaddled() && this.isEquippableInSlot(stack, EquipmentSlot.SADDLE)) {
                return stack.interactLivingEntity(player, this, hand);
            }
            if (this.isSaddled() && !this.isFood(stack) && !this.isVehicle() && !player.isSecondaryUseActive()) {
                if (!this.level().isClientSide()) {
                    player.startRiding(this);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean canUseSlot(EquipmentSlot slot) {
        return slot != EquipmentSlot.SADDLE ? super.canUseSlot(slot) : this.isAlive() && !this.isBaby() && this.isTame();
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return this.isSaddled() && this.getFirstPassenger() instanceof Player player ? player : super.getControllingPassenger();
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        // the rider is in charge: no wandering off to dig while someone sits on top
        if (!this.isVehicle()) {
            super.customServerAiStep(level);
        }
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
        return (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.6F;
    }

    @Override
    protected void tickRidden(Player controller, Vec3 riddenInput) {
        super.tickRidden(controller, riddenInput);
        this.setRot(controller.getYRot(), controller.getXRot() * 0.5F);
        this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();
        if (this.level() instanceof ServerLevel server) {
            if (riddenInput.z > 0.0 && this.horizontalCollision && --this.digTimer <= 0) {
                this.digTimer = 6;
                this.ploughAhead(server);
            }
            if (--this.sniffTimer <= 0) {
                this.sniffTimer = SNIFF_INTERVAL;
                this.sniffForTreasure(server, controller);
            }
        }
    }

    /** Digs through the soft blocks blocking the way, two high and as wide as the Sniffer. */
    private void ploughAhead(ServerLevel level) {
        Direction facing = this.getDirection();
        Direction side = facing.getClockWise();
        BlockPos front = BlockPos.containing(this.getX(), this.getY() + 0.1, this.getZ()).relative(facing, 2);
        boolean dug = false;
        for (int s = -1; s <= 1; s++) {
            for (int y = 0; y <= 1; y++) {
                BlockPos pos = front.relative(side, s).above(y);
                BlockState state = level.getBlockState(pos);
                if (state.is(ModTags.Blocks.SNIFFER_MINEABLE)) {
                    level.destroyBlock(pos, true, this);
                    dug = true;
                }
            }
        }
        if (dug) {
            this.playSound(ModSounds.SNIFFER_PLOUGH.get(), 1.0F, 1.0F);
        }
    }

    /** Points out the nearest buried treasure with a trail of glowing dust. */
    private void sniffForTreasure(ServerLevel level, Player rider) {
        BlockPos here = this.blockPosition();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -SNIFF_RADIUS; dx <= SNIFF_RADIUS; dx++) {
            for (int dz = -SNIFF_RADIUS; dz <= SNIFF_RADIUS; dz++) {
                for (int dy = -8; dy <= 4; dy++) {
                    p.set(here.getX() + dx, here.getY() + dy, here.getZ() + dz);
                    double d = dx * dx + dy * dy * 2 + dz * dz;
                    if (d < bestDist && level.getBlockState(p).is(ModTags.Blocks.SNIFFER_TREASURE)) {
                        bestDist = d;
                        best = p.immutable();
                    }
                }
            }
        }
        this.playSound(ModSounds.SNIFFER_SNIFF.get(), 1.0F, 1.0F);
        if (best == null) {
            return;
        }
        Vec3 nose = this.position().add(this.getLookAngle().scale(1.6)).add(0, 0.6, 0);
        Vec3 to = Vec3.atCenterOf(best);
        Vec3 step = to.subtract(nose);
        int n = Math.max(4, (int) (step.length() * 1.5));
        for (int i = 0; i <= n; i++) {
            Vec3 at = nose.add(step.scale(i / (double) n));
            level.sendParticles(ModParticles.GLOW_DUST.get(), at.x, at.y + 0.3 * Math.sin(i * 0.6), at.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
        level.sendParticles(ModParticles.STAR_SPARKLE.get(), to.x, to.y + 0.6, to.z, 8, 0.3, 0.3, 0.3, 0.01);
        this.playSound(ModSounds.SNIFFER_HAPPY.get(), 0.8F, 1.2F);
        rider.sendOverlayMessage(Component.translatable("message.thesift.sniffer.treasure", (int) Math.sqrt(bestDist)));
    }

    // ------------------------------------------------------------------ save

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("SiftTame", this.isTame());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setTame(input.getBooleanOr("SiftTame", false));
    }
}
