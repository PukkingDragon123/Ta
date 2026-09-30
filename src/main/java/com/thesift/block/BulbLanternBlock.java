package com.thesift.block;

import com.thesift.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** A lantern holding a wobbling ball of glowing slime. */
public class BulbLanternBlock extends LanternBlock {
    public BulbLanternBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(12) == 0) {
            level.addParticle(ModParticles.STAR_SPARKLE.get(), pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.3,
                    pos.getY() + (state.getValue(HANGING) ? 0.3 : 0.4), pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.3, 0, 0.01, 0);
        }
    }
}
