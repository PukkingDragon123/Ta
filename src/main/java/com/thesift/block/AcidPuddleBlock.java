package com.thesift.block;

import com.thesift.registry.ModCaves;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * W-deep caves: a film of acid pooled on the Cave Jungle's floor under an Acid Weeper. It fizzes; anything standing in
 * it is burned and has its armour eaten ({@link AcidWeeperBlock#corrode}). Left alone it slowly dries up - unless the
 * Weeper above keeps it topped up.
 */
public class AcidPuddleBlock extends Block {
    private static final VoxelShape SHAPE = Block.column(16.0, 0.0, 1.0);

    public AcidPuddleBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighbourPos,
            BlockState neighbourState, RandomSource random) {
        return direction == Direction.DOWN && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (level instanceof ServerLevel server && entity instanceof LivingEntity living && living.tickCount % 10 == 0) {
            AcidWeeperBlock.corrode(server, living, 1.0F);
        }
    }

    /** Dries up, slowly, unless a weeper overhead keeps dripping into it. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) != 0) {
            return;
        }
        BlockPos.MutableBlockPos p = pos.mutable();
        for (int i = 0; i < 12; i++) {
            p.move(Direction.UP);
            BlockState above = level.getBlockState(p);
            if (above.getBlock() instanceof AcidWeeperBlock) {
                return;
            }
            if (!above.isAir()) {
                break;
            }
        }
        level.removeBlock(pos, false);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            level.addParticle(ModCaves.ACID_FIZZ.get(), pos.getX() + 0.1 + random.nextDouble() * 0.8, pos.getY() + 0.06,
                    pos.getZ() + 0.1 + random.nextDouble() * 0.8, 0.0, 0.0, 0.0);
        }
    }
}
