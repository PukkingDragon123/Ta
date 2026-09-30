package com.thesift.block;

import com.thesift.music.Resonance;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** A glowing sealed wall. Dissolves (with all connected seal blocks) when its harmony is solved. */
public class HarmonySealBlock extends Block {
    public HarmonySealBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public static void dissolve(ServerLevel level, BlockPos start) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && seen.size() < 256) {
            BlockPos p = queue.poll();
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (!seen.contains(n) && level.getBlockState(n).is(ModBlocks.HARMONY_SEAL.get())) {
                    seen.add(n);
                    queue.add(n);
                }
            }
        }
        for (BlockPos p : seen) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.sendParticles(ModParticles.PORTAL_SOUL.get(), p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.05);
            level.sendParticles(ModParticles.STAR_SPARKLE.get(), p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 3, 0.4, 0.4, 0.4, 0.02);
        }
        level.playSound(null, start, ModSounds.HARMONY_UNLOCK.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
        Resonance.pulse(level, start, 1.0F, 8);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(8) == 0) {
            Direction d = Direction.getRandom(random);
            BlockPos n = pos.relative(d);
            if (!level.getBlockState(n).isSolidRender()) {
                level.addParticle(ModParticles.GLOW_DUST.get(), pos.getX() + 0.5 + d.getStepX() * 0.55 + (random.nextDouble() - 0.5) * 0.6,
                        pos.getY() + 0.5 + d.getStepY() * 0.55 + (random.nextDouble() - 0.5) * 0.6,
                        pos.getZ() + 0.5 + d.getStepZ() * 0.55 + (random.nextDouble() - 0.5) * 0.6, 0, 0, 0);
            }
        }
    }
}
