package com.thesift.world.sky;

import com.thesift.block.SiftPlantBlock;
import com.thesift.registry.ModSwifter;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * W-sky: the Fluffbush - a soft puff of lavender-white fluff on a few thin stems. You sink into it rather than bump into
 * it, and it catches you: a fall into a Fluffbush does no harm at all (a good place to land a swing).
 */
public class FluffbushBlock extends VegetationBlock {
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0.0, 6.0, 0.0, 16.0, 16.0, 16.0), Block.box(6.0, 0.0, 6.0, 10.0, 6.0, 10.0));

    public FluffbushBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return SiftPlantBlock.isSiftSoil(state) || super.mayPlaceOn(state, level, pos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (!(entity instanceof LivingEntity)) {
            return;
        }
        entity.resetFallDistance();
        entity.makeStuckInBlock(state, new Vec3(0.85, 0.55, 0.85));
        if (level.isClientSide() && entity.getKnownMovement().lengthSqr() > 0.004 && level.getRandom().nextInt(3) == 0) {
            RandomSource r = level.getRandom();
            level.addParticle(ModSwifter.WHITE_FLUFF.get(), pos.getX() + r.nextDouble(), pos.getY() + 0.5 + r.nextDouble() * 0.5,
                    pos.getZ() + r.nextDouble(), (r.nextDouble() - 0.5) * 0.04, 0.02, (r.nextDouble() - 0.5) * 0.04);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(40) == 0) {
            level.addParticle(ModSwifter.WHITE_FLUFF.get(), pos.getX() + random.nextDouble(), pos.getY() + 1.0, pos.getZ() + random.nextDouble(),
                    0.0, 0.01, 0.0);
        }
    }
}
