package com.thesift.block;

import com.thesift.entity.boss.MiniBoss;
import com.thesift.music.SongEvents;
import com.thesift.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;

/**
 * A Musical Cobweb, woven by the Weaver: glowing sculk silk strung like the strings of a harp.
 * It barely holds you - a little drag and a springy twang that bounces you back out - but every
 * strand is tuned, and touching it plays its note (then it rings, brighter, for half a second
 * before it can be played again). The Weaver and its brood walk its webs as if they were not there.
 */
public class MusicalCobwebBlock extends Block {
    public static final BooleanProperty RINGING = BooleanProperty.create("ringing");
    private static final int RING_TICKS = 10;

    public MusicalCobwebBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(RINGING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RINGING);
    }

    /** The note a strand plays (0-24): neighbouring strands climb a scale, so a web plays runs. */
    public static int noteAt(BlockPos pos) {
        return Math.floorMod(pos.getX() * 2 + pos.getY() * 5 + pos.getZ() * 7, 25);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (entity instanceof LivingEntity le && MiniBoss.isBandmate(le)) {
            return;
        }
        Vec3 v = entity.getDeltaMovement();
        if (!state.getValue(RINGING)) {
            // the twang: a springy little bounce back out of the strands
            entity.setDeltaMovement(v.x * 0.6, Math.max(v.y < -0.1 ? -v.y * 0.5 : v.y, 0.3), v.z * 0.6);
            entity.resetFallDistance();
            if (level instanceof ServerLevel server) {
                this.play(server, pos, state, entity instanceof Player p ? p : null);
            }
        } else {
            // only mildly slowing: a soft drag, nothing like a cobweb's grip
            entity.setDeltaMovement(v.x * 0.8, v.y < 0.0 ? v.y * 0.6 : v.y, v.z * 0.8);
            entity.resetFallDistance();
        }
    }

    /** Plays the strand's note, lights it up and lets it ring. */
    public void play(ServerLevel level, BlockPos pos, BlockState state, @org.jspecify.annotations.Nullable Player player) {
        int note = noteAt(pos);
        float pitch = (float) Math.pow(2.0, (note - 12) / 12.0);
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.BLOCKS, 0.9F, pitch);
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.35F, pitch);
        level.sendParticles(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 0, note / 24.0, 0.0, 0.0, 1.0);
        level.sendParticles(new DustParticleOptions(0x7FF7FF, 0.8F), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.0);
        SongEvents.note(level, player, Vec3.atCenterOf(pos), note);
        level.setBlock(pos, state.setValue(RINGING, true), Block.UPDATE_ALL);
        level.scheduleTick(pos, this, RING_TICKS);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(RINGING)) {
            level.setBlock(pos, state.setValue(RINGING, false), Block.UPDATE_ALL);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        boolean ringing = state.getValue(RINGING);
        if (ringing || random.nextInt(6) == 0) {
            level.addParticle(new DustParticleOptions(ringing ? 0xD6FFFF : 0x29DFEB, ringing ? 0.9F : 0.5F), pos.getX() + random.nextDouble(),
                    pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble(), 0, 0, 0);
        }
        if (random.nextInt(ringing ? 3 : 40) == 0) {
            level.addParticle(ModParticles.SIFT_NOTE.get(), pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, random.nextDouble(), 0, 0);
        }
    }
}
