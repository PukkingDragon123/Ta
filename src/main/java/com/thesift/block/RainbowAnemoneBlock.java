package com.thesift.block;

import com.thesift.registry.ModChrome;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * W-sea: a Rainbow Anemone of the Chrome Coral Ocean - a soft violet anemone whose tentacle tips glow through the colours;
 * it lights the coral gardens and sheds rainbow sparks into the Chrome.
 */
public class RainbowAnemoneBlock extends BubbleCoralBlock {
    public RainbowAnemoneBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (random.nextInt(5) == 0) {
            // the spark's hue turns with Chrome's own colour loop (120 ticks, ChromeClient.HUE_LOOP)
            float hue = (level.getGameTime() % 24000L) / 120.0F + random.nextFloat() * 0.3F;
            level.addParticle(ModChrome.CHROME_SPARK.get(), pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.7,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, hue, 0.02, 0.0);
        }
    }
}
