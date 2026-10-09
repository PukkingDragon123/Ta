package com.thesift.block;

import com.thesift.registry.ModCaves;
import com.thesift.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SpeleothemBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SpeleothemThickness;
import net.minecraft.world.phys.Vec3;

/**
 * W-deep caves: a pointed crystal (rose, azure or amber) - vanilla's speleothem (the pointed dripstone family) grown in
 * crystal. Stalactites and stalagmites stack and merge exactly like dripstone, a stalactite cut loose falls point first
 * and hurts what it lands on, and landing on an upright tip hurts like a stalagmite. They grow, faster than dripstone,
 * under any block of crystal dripstone ({@code #thesift:crystal_dripstone}), ring a glassy note when struck and glint in
 * the dark.
 */
public class PointedCrystalBlock extends SpeleothemBlock {
    private static final float GROWTH_CHANCE = 0.045F;

    public PointedCrystalBlock(BlockBehaviour.Properties properties) {
        super(Blocks.AMETHYST_BLOCK.defaultBlockState(), properties);
    }

    @Override
    protected int getStalactiteLandingSound() {
        return 1045; // the pointed dripstone's landing crack
    }

    @Override
    protected boolean canGrow(LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.above()).is(ModCaves.CRYSTAL_DRIPSTONE);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() < GROWTH_CHANCE && isStalactiteStartPos(state, level, pos)) {
            this.growStalactiteOrStalagmiteIfPossible(state, level, pos, random);
        }
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        if (state.getValue(TIP_DIRECTION) == Direction.UP && state.getValue(THICKNESS) == SpeleothemThickness.TIP) {
            entity.causeFallDamage(fallDistance + 2.5, 2.0F, level.damageSources().stalagmite());
        } else {
            super.fallOn(level, state, pos, entity, fallDistance);
        }
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (level instanceof ServerLevel server) {
            float pitch = 0.8F + (pos.getY() & 7) * 0.1F;
            server.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.9F, pitch);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Vec3 off = state.getOffset(pos);
        double x = pos.getX() + 0.5 + off.x;
        double z = pos.getZ() + 0.5 + off.z;
        if (random.nextInt(16) == 0) {
            level.addParticle(ModParticles.STAR_SPARKLE.get(), x + (random.nextDouble() - 0.5) * 0.35, pos.getY() + random.nextDouble(),
                    z + (random.nextDouble() - 0.5) * 0.35, 0.0, 0.0, 0.0);
        }
        // a hanging tip sheds a glittering dust of its own colour
        if (state.getValue(TIP_DIRECTION) == Direction.DOWN && state.getValue(THICKNESS) == SpeleothemThickness.TIP && random.nextInt(24) == 0) {
            int rgb = state.getMapColor(level, pos).col;
            level.addParticle(new DustParticleOptions(rgb, 0.7F), x, pos.getY() + 0.25, z, 0.0, -0.03, 0.0);
        }
    }
}
