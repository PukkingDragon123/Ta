package com.thesift.block;

import com.thesift.music.Notes;
import com.thesift.music.SongEvents;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSeaReefs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * W-sea: Trumpet Coral, the living brass of the Brass Coral Reef. It comes in four metals - Brass (gold), Silver, Copper
 * (orange) and Verdigris (oxidized green) - as a full block, a branching tube ({@link TrumpetCoralPipeBlock}) and the
 * flared bell at the end of each branch ({@link TrumpetCoralBellBlock}), which is the part that grows.
 *
 * <p>Music: every piece sounds one note of the reef's chord (G major pentatonic, note-block pitches), chosen by where it
 * stands; the metal picks the register and the voice (Mojang's copper trumpet notes, bright brass to muffled verdigris).
 * Bells toot on their own under water, and every piece sounds when tapped or when something swims through a bell; a
 * player's taps reach the song tracker, so a reef can be played.
 */
public final class TrumpetCoral {
    /** A bell stops growing at this age. */
    public static final int MAX_AGE = 7;
    /** The reef's chord: G major pentatonic in note-block pitches (0 = F#3). */
    private static final int[] SCALE = {1, 3, 5, 8, 10, 13, 15, 17, 20, 22};

    public enum Metal {
        BRASS(4), SILVER(5), COPPER(2), VERDIGRIS(0);

        /** The lowest of the five scale steps this metal sings. */
        final int low;

        Metal(int low) {
            this.low = low;
        }

        public SoundEvent sound() {
            return switch (this) {
                case BRASS -> ModSeaReefs.TRUMPET_BRASS.get();
                case SILVER -> ModSeaReefs.TRUMPET_SILVER.get();
                case COPPER -> ModSeaReefs.TRUMPET_COPPER.get();
                case VERDIGRIS -> ModSeaReefs.TRUMPET_VERDIGRIS.get();
            };
        }

        public Block pipe() {
            return switch (this) {
                case BRASS -> ModBlocks.BRASS_TRUMPET_CORAL.get();
                case SILVER -> ModBlocks.SILVER_TRUMPET_CORAL.get();
                case COPPER -> ModBlocks.COPPER_TRUMPET_CORAL.get();
                case VERDIGRIS -> ModBlocks.VERDIGRIS_TRUMPET_CORAL.get();
            };
        }

        /** The metal a Trumpet Coral block is made of (from its id: brass_..., silver_..., copper_..., verdigris_...). */
        public static Metal of(Block block) {
            String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
            for (Metal m : values()) {
                if (path.startsWith(m.name().toLowerCase(java.util.Locale.ROOT) + "_")) {
                    return m;
                }
            }
            return BRASS;
        }
    }

    private TrumpetCoral() {
    }

    /** True for every piece of Trumpet Coral: block, tube or bell. */
    public static boolean isFamily(BlockState state) {
        Block b = state.getBlock();
        return b instanceof TrumpetCoralPipeBlock || b instanceof TrumpetCoralBellBlock || b instanceof TrumpetCoralBlock;
    }

    /** The note (0-24) this piece of coral sounds. */
    public static int note(Metal metal, BlockPos pos) {
        long h = pos.getX() * 73428767L ^ pos.getY() * 912931L ^ pos.getZ() * 4382371L;
        h ^= h >>> 17;
        return SCALE[metal.low + (int) Math.floorMod(h, 5L)];
    }

    /**
     * Sounds a piece of coral: its note in its metal's voice, a rising note and a puff of bubbles from {@code mouth}.
     * A player's touch also counts as a played note (songs, listeners).
     */
    public static void toot(ServerLevel level, BlockPos pos, Metal metal, Vec3 mouth, @Nullable Player player, float volume) {
        int pitch = note(metal, pos);
        level.playSound(null, mouth.x, mouth.y, mouth.z, metal.sound(), SoundSource.BLOCKS, volume, Notes.soundPitch(pitch));
        level.sendParticles(ModParticles.SIFT_NOTE.get(), mouth.x, mouth.y + 0.3, mouth.z, 0, pitch / 24.0, 0.0, 0.0, 1.0);
        level.sendParticles(ParticleTypes.NOTE, mouth.x, mouth.y + 0.5, mouth.z, 0, pitch / 24.0, 0.0, 0.0, 1.0);
        if (level.getFluidState(BlockPos.containing(mouth)).is(net.minecraft.tags.FluidTags.WATER)) {
            level.sendParticles(ParticleTypes.BUBBLE, mouth.x, mouth.y, mouth.z, 6, 0.15, 0.15, 0.15, 0.06);
        }
        if (player != null) {
            SongEvents.note(level, player, mouth, pitch);
        }
    }

    /** Client: a bell blows its note by itself, for the players near it only. */
    public static void tootLocally(Level level, BlockPos pos, Metal metal, Vec3 mouth, float volume) {
        int pitch = note(metal, pos);
        level.playLocalSound(mouth.x, mouth.y, mouth.z, metal.sound(), SoundSource.BLOCKS, volume, Notes.soundPitch(pitch), false);
        level.addParticle(ModParticles.SIFT_NOTE.get(), mouth.x, mouth.y + 0.3, mouth.z, pitch / 24.0, 0.0, 0.0);
    }

    /** Where a bell's mouth is. */
    public static Vec3 mouth(BlockPos pos, Direction facing) {
        return Vec3.atCenterOf(pos).add(facing.getStepX() * 0.45, facing.getStepY() * 0.45, facing.getStepZ() * 0.45);
    }

    /** A right-angle turn away from {@code facing}: brass tubing bends, it never doubles straight back. */
    public static Direction turn(Direction facing, RandomSource random) {
        if (facing.getAxis() == Direction.Axis.Y) {
            return Direction.Plane.HORIZONTAL.getRandomDirection(random);
        }
        int k = random.nextInt(4);
        return k < 2 ? Direction.UP : k == 2 ? facing.getClockWise() : facing.getCounterClockWise();
    }

    /**
     * One growth step of a bell (a random tick, under water or in Chrome): the bell moves one block on - usually straight,
     * sometimes round a bend - leaving a length of tube behind it, and now and then the tube forks into a second bell.
     * A bell that has nowhere left to go, or is old enough, stops.
     */
    public static void grow(ServerLevel level, BlockPos pos, BlockState bell, RandomSource random) {
        int age = bell.getValue(TrumpetCoralBellBlock.AGE);
        if (age >= MAX_AGE || !SeaLogging.wet(bell) || !(bell.getBlock() instanceof TrumpetCoralBellBlock block)) {
            return;
        }
        Direction facing = bell.getValue(TrumpetCoralBellBlock.FACING);
        Direction dir = random.nextInt(10) < 3 ? turn(facing, random) : facing;
        BlockPos to = pos.relative(dir);
        if (!SeaLogging.openSea(level, to)) {
            dir = turn(facing, random);
            to = pos.relative(dir);
            if (!SeaLogging.openSea(level, to)) {
                level.setBlock(pos, bell.setValue(TrumpetCoralBellBlock.AGE, MAX_AGE), Block.UPDATE_CLIENTS);
                return;
            }
        }
        Metal metal = block.metal();
        BlockState tube = SeaLogging.inFluid(metal.pipe().defaultBlockState(), SeaLogging.fluid(bell));
        level.setBlock(pos, TrumpetCoralPipeBlock.withConnections(level, pos, tube), Block.UPDATE_ALL);
        BlockState moved = bell.setValue(TrumpetCoralBellBlock.FACING, dir).setValue(TrumpetCoralBellBlock.AGE, age + 1);
        level.setBlock(to, SeaLogging.inFluidAt(moved, level, to), Block.UPDATE_ALL);
        if (age + 2 < MAX_AGE && random.nextInt(4) == 0) {
            Direction side = turn(dir, random);
            BlockPos fork = pos.relative(side);
            if (side != facing.getOpposite() && SeaLogging.openSea(level, fork)) {
                BlockState branch = bell.setValue(TrumpetCoralBellBlock.FACING, side).setValue(TrumpetCoralBellBlock.AGE, age + 2);
                level.setBlock(fork, SeaLogging.inFluidAt(branch, level, fork), Block.UPDATE_ALL);
            }
        }
        level.playSound(null, to, ModSeaReefs.TRUMPET_GROW.get(), SoundSource.BLOCKS, 0.6F, 0.9F + random.nextFloat() * 0.2F);
        toot(level, to, metal, mouth(to, dir), null, 0.8F);
    }
}
