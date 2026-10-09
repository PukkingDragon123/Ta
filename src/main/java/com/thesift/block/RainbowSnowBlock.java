package com.thesift.block;

import com.thesift.registry.ModWorldLand;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * W-land: Rainbow Snow, the White Forest's snow. Layers stack, melt and drop Snowballs like vanilla snow; its texture
 * drifts slowly round the rainbow (four phase-shifted variants, so a snowfield shimmers in patches), and now and then a
 * flake lifts off it and sparkles.
 */
public class RainbowSnowBlock extends SnowLayerBlock {
    public RainbowSnowBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(40) == 0) {
            double top = state.getValue(LAYERS) * 0.125;
            level.addParticle(ModWorldLand.RAINBOW_SNOWFLAKE.get(), pos.getX() + random.nextDouble(), pos.getY() + top + 0.05,
                    pos.getZ() + random.nextDouble(), 0.0, 0.02, 0.0);
        }
    }
}
