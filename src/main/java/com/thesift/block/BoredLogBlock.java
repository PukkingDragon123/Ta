package com.thesift.block;

import com.thesift.entity.jungle.PonderTadpole;
import com.thesift.registry.ModCaveJungle;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;

/**
 * P4 Cave Jungle: a log Ponder Tadpoles have bored into - pocked with round, chewed holes. Up to three tadpoles
 * live in it; now and then one wriggles out again. Break it and they all burst out, teeth first, at whoever did it.
 */
public class BoredLogBlock extends RotatedPillarBlock {
    public static final IntegerProperty TADPOLES = IntegerProperty.create("tadpoles", 0, 3);

    public BoredLogBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.Y).setValue(TADPOLES, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(TADPOLES);
    }

    /** A tadpole bores into the log at {@code pos} (any log): it becomes a bored log, one more tadpole inside. */
    public static boolean boreInto(Level level, BlockPos pos, BlockState log) {
        int n = log.getBlock() instanceof BoredLogBlock ? log.getValue(TADPOLES) : 0;
        if (n >= 3) {
            return false;
        }
        BlockState bored = com.thesift.registry.ModBlocks.BORED_LOG.get().defaultBlockState().setValue(TADPOLES, n + 1);
        if (log.hasProperty(AXIS)) {
            bored = bored.setValue(AXIS, log.getValue(AXIS));
        }
        level.setBlock(pos, bored, Block.UPDATE_ALL);
        return true;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(TADPOLES) > 0;
    }

    /** Now and then a tadpole wriggles back out. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(40) != 0) {
            return;
        }
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos out = pos.relative(d);
            if (level.isEmptyBlock(out) && !level.isEmptyBlock(out.below())) {
                level.setBlock(pos, state.setValue(TADPOLES, state.getValue(TADPOLES) - 1), Block.UPDATE_CLIENTS);
                release(level, out, null);
                return;
            }
        }
    }

    /** Broken open: every tadpole inside bursts out at the nearest player. */
    @Override
    protected void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropExperience) {
        super.spawnAfterBreak(state, level, pos, tool, dropExperience);
        int n = Math.max(1, state.getValue(TADPOLES));
        Player near = null;
        double best = Double.MAX_VALUE;
        List<Player> players = level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(10.0), p -> !p.isSpectator() && !p.isCreative());
        for (Player p : players) {
            double d = p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (d < best) {
                best = d;
                near = p;
            }
        }
        for (int i = 0; i < n; i++) {
            release(level, pos, near);
        }
    }

    private static void release(ServerLevel level, BlockPos at, Player angryAt) {
        PonderTadpole t = ModCaveJungle.PONDER_TADPOLE.get().create(level, EntitySpawnReason.TRIGGERED);
        if (t == null) {
            return;
        }
        RandomSource r = level.getRandom();
        t.snapTo(at.getX() + 0.3 + r.nextDouble() * 0.4, at.getY() + 0.1, at.getZ() + 0.3 + r.nextDouble() * 0.4, r.nextFloat() * 360.0F, 0.0F);
        t.setDeltaMovement((r.nextDouble() - 0.5) * 0.4, 0.3, (r.nextDouble() - 0.5) * 0.4);
        if (angryAt != null) {
            t.setTarget(angryAt);
        }
        level.addFreshEntity(t);
        level.playSound(null, at, ModCaveJungle.TADPOLE_BURROW.get(), SoundSource.BLOCKS, 1.0F, 1.3F);
        level.sendParticles(ParticleTypes.SPLASH, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.1);
    }
}
