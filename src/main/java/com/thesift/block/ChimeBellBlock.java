package com.thesift.block;

import com.thesift.music.SongEvents;
import com.thesift.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Chime Bell: a Sound Garden flower whose glassy bell rings when anything walks through it. Every
 * bell is tuned to a note of the pentatonic scale, so wading through a meadow of them plays a
 * melody - and a player who knows a song can walk it out note by note.
 */
public class ChimeBellBlock extends VegetationBlock {
    public static final BooleanProperty RINGING = BooleanProperty.create("ringing");
    private static final int[] PENTATONIC = {0, 2, 4, 7, 9};
    private static final VoxelShape SHAPE = Block.column(8.0, 0.0, 12.0);

    public ChimeBellBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(RINGING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RINGING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE.move(state.getOffset(pos));
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getBlock() instanceof CloudBlock || SiftPlantBlock.isSiftSoil(state) || super.mayPlaceOn(state, level, pos);
    }

    /** The bell's note (0-24), always on the pentatonic scale. */
    public static int noteAt(BlockPos pos) {
        int h = Math.floorMod(pos.getX() * 3 + pos.getZ() * 5 + pos.getY(), 10);
        return 6 + PENTATONIC[h % 5] + (h >= 5 ? 12 : 0);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (state.getValue(RINGING) || !(entity instanceof LivingEntity) || entity.getKnownMovement().horizontalDistanceSqr() < 1.0E-4) {
            return;
        }
        if (level instanceof ServerLevel server) {
            int note = noteAt(pos);
            float pitch = (float) Math.pow(2.0, (note - 12) / 12.0);
            server.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.55F, pitch);
            server.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.4F, pitch);
            server.sendParticles(ModParticles.SIFT_NOTE.get(), pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 0, note / 24.0, 0.0, 0.0, 1.0);
            SongEvents.note(server, entity instanceof Player p ? p : null, Vec3.atCenterOf(pos), note);
            server.setBlock(pos, state.setValue(RINGING, true), Block.UPDATE_ALL);
            server.scheduleTick(pos, this, 12);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(RINGING)) {
            level.setBlock(pos, state.setValue(RINGING, false), Block.UPDATE_ALL);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(state.getValue(RINGING) ? 2 : 30) == 0) {
            level.addParticle(ModParticles.STAR_SPARKLE.get(), pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.6,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, 0.01, 0.0);
        }
    }
}
