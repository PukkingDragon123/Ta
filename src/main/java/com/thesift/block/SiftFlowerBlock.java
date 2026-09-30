package com.thesift.block;

import com.thesift.music.Resonant;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Sift flowers react to music: a nearby drum, jukebox or note block makes them bloom open and glow
 * for a few seconds (the RESONATING state), shedding particles that match the flower.
 */
public class SiftFlowerBlock extends FlowerBlock implements Resonant {
    public static final BooleanProperty RESONATING = BooleanProperty.create("resonating");

    public SiftFlowerBlock(Holder<MobEffect> effect, float seconds, BlockBehaviour.Properties properties) {
        this(effect, seconds, 0, properties);
    }

    public SiftFlowerBlock(Holder<MobEffect> effect, float seconds, int baseLight, BlockBehaviour.Properties properties) {
        super(effect, seconds, properties.lightLevel(s -> s.getValue(RESONATING) ? 11 : baseLight));
        this.registerDefaultState(this.stateDefinition.any().setValue(RESONATING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RESONATING);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return SiftPlantBlock.isSiftSoil(state) || super.mayPlaceOn(state, level, pos);
    }

    @Override
    public void onResonate(ServerLevel level, BlockPos pos, BlockState state, float strength) {
        if (!state.getValue(RESONATING)) {
            level.setBlock(pos, state.setValue(RESONATING, true), Block.UPDATE_ALL);
        }
        level.scheduleTick(pos, this, 80 + level.getRandom().nextInt(60));
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(RESONATING)) {
            level.setBlock(pos, state.setValue(RESONATING, false), Block.UPDATE_ALL);
        }
    }

    protected ParticleOptions ambientParticle() {
        if (this == ModBlocks.SOULPETAL.get()) return ModParticles.DRIFTING_SOUL.get();
        if (this == ModBlocks.LULLABY_BELL.get()) return ModParticles.STAR_SPARKLE.get();
        if (this == ModBlocks.NEBULA_IRIS.get()) return ModParticles.GLOW_DUST.get();
        return ModParticles.DREAM_POLLEN.get();
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        boolean res = state.getValue(RESONATING);
        if (random.nextInt(res ? 2 : 14) == 0) {
            level.addParticle(ambientParticle(), pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.5 + random.nextDouble() * 0.4,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, 0.015, 0.0);
        }
        if (res && random.nextInt(4) == 0) {
            level.addParticle(ModParticles.SIFT_NOTE.get(), pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, random.nextFloat(), 0.0, 0.0);
        }
        if (this == ModBlocks.SOULPETAL.get() && random.nextInt(40) == 0) {
            level.addParticle(ParticleTypes.SOUL, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 0.0, 0.02, 0.0);
        }
    }
}
