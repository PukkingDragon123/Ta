package com.thesift.block;

import com.thesift.registry.ModSwifter;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Swifter den: a low nest of woven twigs lined with white fluff. Its loot table holds the
 * family's treasures (glowing slime balls, chrome pearls, gold, emeralds, now and then Siftite);
 * breaking it makes the cubs cry and the parents attack (see registry/ModSwifter and
 * {@link com.thesift.entity.Swifter#denBroken}).
 */
public class SwifterDenBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 5.0, 16.0);

    public SwifterDenBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Now and then a wisp of fluff lifts off the nest and drifts away. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(10) == 0) {
            level.addParticle(ModSwifter.WHITE_FLUFF.get(), pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.45,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, (random.nextDouble() - 0.5) * 0.02, 0.04, (random.nextDouble() - 0.5) * 0.02);
        }
    }
}
