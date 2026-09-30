package com.thesift.block;

import com.thesift.registry.ModParticles;
import com.thesift.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Base for short Sift ground cover: rooted on Sift soils, rustles and sheds pollen when brushed. */
public class SiftPlantBlock extends VegetationBlock {
    protected static final VoxelShape SHAPE = Block.column(12.0, 0.0, 13.0);

    public SiftPlantBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public static boolean isSiftSoil(BlockState state) {
        return state.is(ModTags.Blocks.SIFT_PLANTABLE) || state.is(BlockTags.DIRT);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return isSiftSoil(state) || super.mayPlaceOn(state, level, pos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE.move(state.getOffset(pos));
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (level.isClientSide() && entity instanceof LivingEntity && entity.getKnownMovement().horizontalDistanceSqr() > 1.0E-4
                && level.getRandom().nextInt(6) == 0) {
            RandomSource r = level.getRandom();
            level.addParticle(ModParticles.DREAM_POLLEN.get(), pos.getX() + r.nextDouble(), pos.getY() + 0.3 + r.nextDouble() * 0.4,
                    pos.getZ() + r.nextDouble(), (r.nextDouble() - 0.5) * 0.02, 0.02, (r.nextDouble() - 0.5) * 0.02);
        }
    }
}
