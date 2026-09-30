package com.thesift.block;

import net.minecraft.world.level.block.BonemealSource;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Deep Sift cave vines with glowing bell flowers. Ringing a bell (using it) makes it chime and
 * pulse light; the bells are what light up Warden territory.
 */
public class GlowbellVineBlock extends GrowingPlantHeadBlock {
    public static final BooleanProperty BELL = BooleanProperty.create("bell");
    protected static final VoxelShape SHAPE = Block.column(14.0, 0.0, 16.0);

    public GlowbellVineBlock(BlockBehaviour.Properties properties) {
        super(properties.lightLevel(s -> s.getValue(BELL) ? 13 : 2), Direction.DOWN, SHAPE, false, 0.1);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0).setValue(BELL, false));
    }

    @Override
    protected int getBlocksToGrowWhenBonemealed(RandomSource random) {
        return 1;
    }

    @Override
    protected boolean canGrowInto(BlockState state) {
        return state.isAir();
    }

    @Override
    protected Block getBodyBlock() {
        return ModBlocks.GLOWBELL_VINE_PLANT.get();
    }

    @Override
    protected BlockState updateBodyAfterConvertedFromHead(BlockState headState, BlockState bodyState) {
        return bodyState.setValue(BELL, headState.getValue(BELL));
    }

    @Override
    protected BlockState getGrowIntoState(BlockState growFromState, RandomSource random) {
        return super.getGrowIntoState(growFromState, random).setValue(BELL, random.nextFloat() < 0.2F);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BELL);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return ring(state, level, pos);
    }

    static InteractionResult ring(BlockState state, Level level, BlockPos pos) {
        if (!state.getValue(BELL)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            float pitch = 1.2F + server.getRandom().nextFloat() * 0.6F;
            server.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, pitch);
            server.sendParticles(ModParticles.GLOW_DUST.get(), pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 8, 0.25, 0.2, 0.25, 0.02);
            com.thesift.music.Resonance.pulse(server, pos, 0.3F, 4);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        spawnBellParticles(state, level, pos, random);
    }

    static void spawnBellParticles(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(BELL) && random.nextInt(8) == 0) {
            level.addParticle(ModParticles.GLOW_DUST.get(), pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.2,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, -0.01, 0.0);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
        return !state.getValue(BELL);
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        level.setBlock(pos, state.setValue(BELL, true), Block.UPDATE_CLIENTS);
    }
}
