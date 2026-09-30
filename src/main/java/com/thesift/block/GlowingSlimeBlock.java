package com.thesift.block;

import com.thesift.registry.ModParticles;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Bouncy, glowing Bulb slime. Every bounce throws off a little shower of sparkles. */
public class GlowingSlimeBlock extends SlimeBlock {
    public GlowingSlimeBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        if (fallDistance > 0.5 && level.isClientSide()) {
            for (int i = 0; i < 6; i++) {
                level.addParticle(ModParticles.GLOW_SPLAT.get(), entity.getX() + (level.getRandom().nextDouble() - 0.5), entity.getY() + 0.1,
                        entity.getZ() + (level.getRandom().nextDouble() - 0.5), 0, 0.1, 0);
            }
        }
        super.fallOn(level, state, pos, entity, fallDistance);
    }

    @Override
    public boolean isSlimeBlock(BlockState state) {
        return true;
    }

    @Override
    public boolean isStickyBlock(BlockState state) {
        return true;
    }

    @Override
    public boolean canStickTo(BlockState state, BlockState other) {
        return !other.is(net.minecraft.world.level.block.Blocks.HONEY_BLOCK);
    }
}
