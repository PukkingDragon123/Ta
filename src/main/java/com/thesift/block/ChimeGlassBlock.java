package com.thesift.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Chime Glass: opal glass smelted from Chime Sand. It rings its note when you touch it - tap or
 * punch it, walk on it, land on it or hit it with a projectile - and breaks with a falling chord.
 * Every block has its own note, so a wall of it plays like a xylophone.
 */
public class ChimeGlassBlock extends TransparentBlock {
    /** Glass that rings like amethyst. */
    public static final SoundType SOUND = new SoundType(1.0F, 1.2F, SoundEvents.GLASS_BREAK, SoundEvents.AMETHYST_BLOCK_STEP, SoundEvents.GLASS_PLACE,
            SoundEvents.AMETHYST_BLOCK_HIT, SoundEvents.AMETHYST_BLOCK_FALL);

    public ChimeGlassBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level instanceof ServerLevel server) {
            Chimes.ring(server, pos, player, 0.8F, true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (level instanceof ServerLevel server) {
            Chimes.ring(server, pos, player, 0.9F, true);
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        if (level instanceof ServerLevel server && Chimes.steppedOnto(entity, pos)) {
            Chimes.ring(server, pos, entity instanceof Player p ? p : null, 0.45F, true);
        }
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        super.fallOn(level, state, pos, entity, fallDistance);
        if (level instanceof ServerLevel server && fallDistance > 0.5) {
            Chimes.ring(server, pos, entity instanceof Player p ? p : null, 1.0F, true);
        }
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (level instanceof ServerLevel server) {
            Chimes.ring(server, hit.getBlockPos(), projectile.getOwner() instanceof Player p ? p : null, 1.0F, true);
        }
    }

    @Override
    protected void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropExperience) {
        super.spawnAfterBreak(state, level, pos, tool, dropExperience);
        Chimes.shatter(level, pos);
    }
}
