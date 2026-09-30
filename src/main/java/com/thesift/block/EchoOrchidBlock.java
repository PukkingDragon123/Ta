package com.thesift.block;

import com.thesift.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** A glowing orchid whose petals ripple with echoing rings of light. */
public class EchoOrchidBlock extends SiftFlowerBlock {
    public EchoOrchidBlock(BlockBehaviour.Properties properties) {
        super(MobEffects.GLOWING, 8.0F, 9, properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (random.nextInt(30) == 0) {
            level.addParticle(ModParticles.RESONANCE_RING.get(), pos.getX() + 0.5, pos.getY() + 0.05, pos.getZ() + 0.5, 0.6, 0.0, 0.0);
        }
    }
}
