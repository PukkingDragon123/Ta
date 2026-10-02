package com.thesift.block;

import com.thesift.music.SongEvents;
import com.thesift.registry.ModChrome;
import com.thesift.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The notes of Chime Sand and Chime Glass. Every block has its own note of the pentatonic scale,
 * picked from where it sits, so a path of Chime Sand plays a little tune and a wall of Chime Glass
 * can be played like a xylophone. Notes go through {@link SongEvents}, so they count as music.
 */
final class Chimes {
    private static final int[] PENTATONIC = {0, 2, 4, 7, 9};

    private Chimes() {
    }

    /** This block's note, 0-24 like a note block's. */
    static int noteAt(BlockPos pos) {
        int h = Math.floorMod(pos.getX() * 5 + pos.getZ() * 3 + pos.getY() * 7, 10);
        return 6 + PENTATONIC[h % 5] + (h >= 5 ? 12 : 0);
    }

    static float pitchOf(int note) {
        return (float) Math.pow(2.0, (note - 12) / 12.0);
    }

    /** True the tick an entity walks onto this block (not while it stands or shuffles about on it). */
    static boolean steppedOnto(Entity entity, BlockPos pos) {
        return !entity.isSteppingCarefully() && (Mth.floor(entity.xo) != pos.getX() || Mth.floor(entity.zo) != pos.getZ());
    }

    /** Rings the block at {@code pos}: a soft tinkle for sand, a clear bell for glass. */
    static void ring(ServerLevel level, BlockPos pos, @Nullable Player player, float volume, boolean glass) {
        int note = noteAt(pos);
        float pitch = pitchOf(note);
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, volume, pitch);
        if (glass) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, volume * 0.7F, pitch);
            level.sendParticles(ModParticles.SIFT_NOTE.get(), pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 0, note / 24.0, 0.0, 0.0, 1.0);
        }
        RandomSource random = level.getRandom();
        for (int i = 0; i < (glass ? 3 : 1); i++) {
            level.sendParticles(ModChrome.CHROME_SPARK.get(), pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 1.02,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, note / 24.0 + random.nextDouble() * 0.1, 0.03, 0.0, 1.0);
        }
        SongEvents.note(level, player, Vec3.atCenterOf(pos), note);
    }

    /** Shattering Chime Glass: its note, then the chord built on it, as the pieces fall. */
    static void shatter(ServerLevel level, BlockPos pos) {
        float pitch = pitchOf(noteAt(pos));
        level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 0.9F, pitch);
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.7F, pitch);
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.5F, pitch * 1.26F);
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.4F, pitch * 1.5F);
        RandomSource random = level.getRandom();
        for (int i = 0; i < 6; i++) {
            level.sendParticles(ModChrome.CHROME_SPARK.get(), pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
                    pos.getZ() + random.nextDouble(), 0, random.nextDouble(), 0.02, 0.0, 1.0);
        }
    }
}
