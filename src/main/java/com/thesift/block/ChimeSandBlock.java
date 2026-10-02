package com.thesift.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Chime Sand: what Chrome settles into where it meets water. It falls like sand, tinkles its note
 * under every footstep (sneak to walk quietly) and as it lands, and smelts into Chime Glass.
 */
public class ChimeSandBlock extends ColoredFallingBlock {
    public ChimeSandBlock(BlockBehaviour.Properties properties) {
        super(new ColorRGBA(0xD8D1EE), properties);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        if (level instanceof ServerLevel server && Chimes.steppedOnto(entity, pos)) {
            Chimes.ring(server, pos, entity instanceof Player p ? p : null, 0.35F, false);
        }
    }

    @Override
    public void onLand(Level level, BlockPos pos, BlockState state, BlockState replacedBlock, FallingBlockEntity entity) {
        if (level instanceof ServerLevel server) {
            server.playSound(null, pos, net.minecraft.sounds.SoundEvents.NOTE_BLOCK_CHIME.value(), net.minecraft.sounds.SoundSource.BLOCKS, 0.25F,
                    Chimes.pitchOf(Chimes.noteAt(pos)));
        }
    }
}
