package com.thesift.block;

import com.thesift.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.MudBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * W1 Sculk Mud: the floor of the Sculk Swamp and the Sculk Ocean. Like mud you sink into it a
 * little (MudBlock's lowered collision) and it drags at your feet (its speed factor, set in
 * tools/sculk_world.py); sculk pulses in it, popping tiny charges and breathing out glow dust.
 */
public class SculkMudBlock extends MudBlock {
    public SculkMudBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(48) != 0 || !level.getBlockState(pos.above()).isAir()) {
            return;
        }
        double x = pos.getX() + 0.15 + random.nextDouble() * 0.7;
        double z = pos.getZ() + 0.15 + random.nextDouble() * 0.7;
        if (random.nextInt(3) == 0) {
            level.addParticle(ModParticles.GLOW_DUST.get(), x, pos.getY() + 0.95, z, 0.0, 0.008, 0.0);
        } else {
            level.addParticle(ParticleTypes.SCULK_CHARGE_POP, x, pos.getY() + 0.9, z, 0.0, 0.02, 0.0);
        }
    }
}
