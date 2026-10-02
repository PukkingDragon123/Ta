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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

/**
 * Organ Reed: a tall clump of hollow, pipe-like reeds in the Sound Garden. The wind hums low notes
 * through them, and pushing through a clump sounds a deep organ chord.
 */
public class OrganReedBlock extends SiftDoublePlantBlock {
    public OrganReedBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getBlock() instanceof CloudBlock || super.mayPlaceOn(state, level, pos);
    }

    /** Low notes (0-12) so that a stand of reeds sounds like the pedals of an organ. */
    public static int noteAt(BlockPos pos) {
        return new int[]{0, 5, 7, 12}[Math.floorMod(pos.getX() * 7 + pos.getZ() * 3, 4)];
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (state.getValue(HALF) != DoubleBlockHalf.LOWER || !(entity instanceof LivingEntity)
                || entity.getKnownMovement().horizontalDistanceSqr() < 1.0E-4 || (level.getGameTime() + pos.asLong()) % 14 != 0) {
            return;
        }
        if (level instanceof ServerLevel server) {
            int note = noteAt(pos);
            float pitch = (float) Math.pow(2.0, (note - 12) / 12.0);
            server.playSound(null, pos, SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.BLOCKS, 0.7F, pitch);
            server.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 0.45F, pitch * 2.0F);
            server.sendParticles(ModParticles.SIFT_NOTE.get(), pos.getX() + 0.5, pos.getY() + 1.8, pos.getZ() + 0.5, 0, note / 24.0, 0.0, 0.0, 1.0);
            SongEvents.note(server, entity instanceof Player p ? p : null, Vec3.atCenterOf(pos), note);
        }
    }

    /** The wind finds the pipes now and then: a soft, low hum and a drifting note. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(HALF) != DoubleBlockHalf.UPPER) {
            return;
        }
        if (random.nextInt(260) == 0) {
            float pitch = (float) Math.pow(2.0, (noteAt(pos) - 12) / 12.0);
            level.playLocalSound(pos, SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.AMBIENT, 0.25F, pitch, false);
            level.addParticle(ModParticles.SIFT_NOTE.get(), pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, noteAt(pos) / 24.0, 0.0, 0.0);
        }
        if (random.nextInt(40) == 0) {
            level.addParticle(ModParticles.SIFT_MIST.get(), pos.getX() + random.nextDouble(), pos.getY() + 0.9, pos.getZ() + random.nextDouble(),
                    0.0, 0.01, 0.0);
        }
    }
}
