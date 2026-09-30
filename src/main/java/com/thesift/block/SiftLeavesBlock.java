package com.thesift.block;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FallingParticlesLeavesBlock;
import net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Untinted leaves that shed their own drifting leaf particles (particle looked up lazily). */
public class SiftLeavesBlock extends FallingParticlesLeavesBlock {
    private final Supplier<? extends ParticleOptions> leafParticle;

    public SiftLeavesBlock(float leafParticleChance, Supplier<? extends ParticleOptions> leafParticle, BlockBehaviour.Properties properties) {
        super(leafParticleChance, AmbientLeavesBlockSoundPlayer.noAmbientSound(), properties);
        this.leafParticle = leafParticle;
    }

    @Override
    protected void spawnFallingLeavesParticle(Level level, BlockPos pos, RandomSource random) {
        ParticleUtils.spawnParticleBelow(level, pos, random, this.leafParticle.get());
    }
}
