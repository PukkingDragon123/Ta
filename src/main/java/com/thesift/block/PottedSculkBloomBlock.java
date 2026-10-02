package com.thesift.block;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** A potted Sculk Bloom hums and puffs just like a planted one (see {@link SculkBloomBlock}). */
public class PottedSculkBloomBlock extends FlowerPotBlock {
    public PottedSculkBloomBlock(Supplier<FlowerPotBlock> emptyPot, Supplier<? extends Block> plant, BlockBehaviour.Properties properties) {
        super(emptyPot, plant, properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        level.scheduleTick(pos, this, SculkBloomBlock.HUM_INTERVAL);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    /** Only restarts a lost hum (the flower pot's own random tick is for eyeblossoms). */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, SculkBloomBlock.HUM_INTERVAL);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        SculkBloomBlock.hum(level, Vec3.atBottomCenterOf(pos).add(0.0, 0.7, 0.0), null, random.nextInt(4) == 0);
        level.scheduleTick(pos, this, SculkBloomBlock.HUM_INTERVAL);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) == 0) {
            level.addParticle(new DustParticleOptions(0x0F2E36, 0.9F), pos.getX() + 0.4 + random.nextDouble() * 0.2, pos.getY() + 0.75,
                    pos.getZ() + 0.4 + random.nextDouble() * 0.2, 0.0, 0.02, 0.0);
        }
        if (random.nextInt(14) == 0) {
            level.addParticle(ParticleTypes.SCULK_CHARGE_POP, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 0.0, 0.01, 0.0);
        }
    }
}
