package com.thesift.world.sky;

import com.thesift.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * W-sky: the Driftfruit - a giant glowing gourd that grows at the end of a hanging Sky Vine (the Sky Whales graze on
 * them: block tag {@code thesift:sky_whale_food}). Break it for Driftfruit Slices; Silk Touch keeps it whole. It hangs
 * from its vine (or anything above it) or sits on solid ground - cut the vine and it drops.
 */
public class DriftfruitBlock extends Block {
    private static final VoxelShape SHAPE = Shapes.or(Block.box(2.0, 1.0, 2.0, 14.0, 13.0, 14.0), Block.box(4.0, 13.0, 4.0, 12.0, 14.0, 12.0),
            Block.box(7.0, 14.0, 7.0, 9.0, 16.0, 9.0), Block.box(5.0, 0.0, 5.0, 11.0, 1.0, 11.0));

    public DriftfruitBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        if (above.getBlock() instanceof SkyVineBlock || above.is(BlockTags.LEAVES) || above.is(BlockTags.LOGS)
                || above.isFaceSturdy(level, pos.above(), Direction.DOWN)) {
            return true;
        }
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (directionToNeighbour.getAxis() == Direction.Axis.Y && !state.canSurvive(level, pos)) {
            ticks.scheduleTick(pos, this, 2);
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    /** Its vine is gone: the fruit falls (and lands as a block, or breaks into an item on anything soft). */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            FallingBlockEntity.fall(level, pos, state);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(14) == 0) {
            level.addParticle(ModParticles.GLOW_DUST.get(), pos.getX() + 0.25 + random.nextDouble() * 0.5, pos.getY() + 0.05,
                    pos.getZ() + 0.25 + random.nextDouble() * 0.5, 0.0, -0.015, 0.0);
        }
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
