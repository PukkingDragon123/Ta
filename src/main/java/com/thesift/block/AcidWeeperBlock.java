package com.thesift.block;

import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModCaves;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * W-deep caves: the Acid Weeper, a drooping, glowing growth on the Cave Jungle's ceilings. It weeps acid: drops fall from
 * its tips (and splash where they land), every few seconds a drop lands on whatever stands beneath it, and the acid pools
 * on the floor below as an {@link AcidPuddleBlock}. Acid burns and eats at armour (see {@link #corrode}); touching the
 * Weeper itself does too.
 */
public class AcidWeeperBlock extends Block {
    private static final VoxelShape SHAPE = Block.column(12.0, 2.0, 16.0);
    private static final EquipmentSlot[] ARMOUR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final int REACH = 12;

    public AcidWeeperBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE.move(state.getOffset(pos));
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos up = pos.above();
        return level.getBlockState(up).isFaceSturdy(level, up, Direction.DOWN);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighbourPos,
            BlockState neighbourState, RandomSource random) {
        return direction == Direction.UP && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (level instanceof ServerLevel server && entity instanceof LivingEntity living && living.tickCount % 10 == 0) {
            corrode(server, living, 1.5F);
        }
    }

    /** Wakes the weeper up: from then on it drips on its own schedule. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, 10 + random.nextInt(30));
        }
    }

    /** A drop falls: onto whatever stands in the column below, or onto the floor, where the acid pools. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos.MutableBlockPos p = pos.mutable();
        int floor = Integer.MIN_VALUE;
        for (int i = 1; i <= REACH; i++) {
            p.move(Direction.DOWN);
            BlockState below = level.getBlockState(p);
            if (!below.getCollisionShape(level, p).isEmpty() || !below.getFluidState().isEmpty()) {
                floor = p.getY();
                break;
            }
        }
        double bottom = floor == Integer.MIN_VALUE ? pos.getY() - REACH : floor + 1.0;
        AABB column = new AABB(pos.getX() + 0.25, bottom, pos.getZ() + 0.25, pos.getX() + 0.75, pos.getY(), pos.getZ() + 0.75);
        List<LivingEntity> under = level.getEntitiesOfClass(LivingEntity.class, column, e -> e.isAlive() && !(e instanceof Player pl && pl.isSpectator()));
        if (!under.isEmpty()) {
            corrode(level, under.get(0), 2.0F);
        } else if (floor != Integer.MIN_VALUE && random.nextInt(4) == 0) {
            BlockPos at = new BlockPos(pos.getX(), floor + 1, pos.getZ());
            BlockState puddle = ModBlocks.ACID_PUDDLE.get().defaultBlockState();
            if (level.getBlockState(at).isAir() && puddle.canSurvive(level, at)) {
                level.setBlock(at, puddle, Block.UPDATE_ALL);
                level.playSound(null, at, SoundEvents.BEEHIVE_DRIP, SoundSource.BLOCKS, 0.5F, 0.7F);
            }
        }
        level.scheduleTick(pos, this, 30 + random.nextInt(40));
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) == 0) {
            Vec3 off = state.getOffset(pos);
            level.addParticle(ModCaves.ACID_DRIP.get(), pos.getX() + 0.5 + off.x + (random.nextDouble() - 0.5) * 0.5, pos.getY() + 0.05,
                    pos.getZ() + 0.5 + off.z + (random.nextDouble() - 0.5) * 0.5, 0.0, 0.0, 0.0);
        }
    }

    /** Acid on a living thing: it burns, fizzes, and eats into every piece of armour it wears. */
    public static void corrode(ServerLevel level, LivingEntity e, float damage) {
        e.hurtServer(level, level.damageSources().source(ModCaves.ACID), damage);
        for (EquipmentSlot slot : ARMOUR) {
            ItemStack stack = e.getItemBySlot(slot);
            if (!stack.isEmpty() && stack.isDamageableItem()) {
                stack.hurtAndBreak(2, e, slot);
            }
        }
        level.playSound(null, e.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.35F, 1.7F + level.getRandom().nextFloat() * 0.3F);
        level.sendParticles(ModCaves.ACID_FIZZ.get(), e.getX(), e.getY() + e.getBbHeight() * 0.4, e.getZ(), 8, e.getBbWidth() * 0.5, e.getBbHeight() * 0.3,
                e.getBbWidth() * 0.5, 0.01);
    }
}
