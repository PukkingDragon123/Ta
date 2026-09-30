package com.thesift.block;

import com.thesift.music.Resonance;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * An ancient singing flower unearthed by Sniffers. It hums a soft chord when something brushes
 * past, sending a small resonance pulse through the plants around it.
 */
public class ChoirLilyBlock extends SiftDoublePlantBlock {
    public ChoirLilyBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (level instanceof ServerLevel server && entity instanceof LivingEntity && state.getValue(HALF) == DoubleBlockHalf.LOWER
                && server.getGameTime() % 30 == Math.floorMod(pos.asLong(), 30)) {
            float pitch = 0.7F + (Math.floorMod(pos.getX() * 31 + pos.getZ() * 17, 8)) * 0.1F;
            server.playSound(null, pos, ModSounds.CHOIR_LILY_SING.get(), SoundSource.BLOCKS, 0.7F, pitch);
            Resonance.pulse(server, pos, 0.4F, 5);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER && random.nextInt(6) == 0) {
            level.addParticle(ModParticles.SIFT_NOTE.get(), pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, random.nextFloat(), 0.0, 0.0);
        }
    }
}
