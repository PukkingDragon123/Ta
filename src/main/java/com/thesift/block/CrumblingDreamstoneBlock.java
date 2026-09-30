package com.thesift.block;

import com.thesift.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Weak, cracked flooring in ruined towers and temples. It shudders when stepped on and gives way
 * a moment later, dropping the unwary into whatever lies below (often Chrome).
 */
public class CrumblingDreamstoneBlock extends Block {
    public CrumblingDreamstoneBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level instanceof ServerLevel server && entity instanceof Player player && !player.isCreative() && !server.getBlockTicks().hasScheduledTick(pos, this)) {
            server.scheduleTick(pos, this, 12);
            server.playSound(null, pos, ModSounds.CRUMBLE.get(), SoundSource.BLOCKS, 0.9F, 0.8F);
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    12, 0.4, 0.05, 0.4, 0.05);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.playSound(null, pos, ModSounds.CRUMBLE.get(), SoundSource.BLOCKS, 1.2F, 0.6F);
        FallingBlockEntity.fall(level, pos, state);
        for (BlockPos n : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 0, 1))) {
            if (!n.equals(pos) && level.getBlockState(n).is(this) && random.nextInt(3) == 0) {
                level.scheduleTick(n.immutable(), this, 4 + random.nextInt(8));
            }
        }
    }
}
