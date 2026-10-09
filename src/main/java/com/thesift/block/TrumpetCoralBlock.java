package com.thesift.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * W-sea: a block of Trumpet Coral - fused brass tubes seen end on, the rock the reef's horns grow from. A building block;
 * tap it and it sounds its deep note, and in water a bubble rises from its pores now and then.
 */
public class TrumpetCoralBlock extends Block {
    private TrumpetCoral.Metal metal;

    public TrumpetCoralBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public TrumpetCoral.Metal metal() {
        if (this.metal == null) {
            this.metal = TrumpetCoral.Metal.of(this);
        }
        return this.metal;
    }

    private void toot(Level level, BlockPos pos, @Nullable Player player) {
        if (level instanceof ServerLevel server) {
            TrumpetCoral.toot(server, pos, this.metal(), Vec3.atCenterOf(pos).add(0.0, 0.55, 0.0), player, 1.0F);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        this.toot(level, pos, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        this.toot(level, pos, player);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(30) == 0 && level.getFluidState(pos.above()).is(FluidTags.WATER)) {
            level.addParticle(ParticleTypes.BUBBLE, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 1.05,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0.0, 0.06, 0.0);
        }
    }
}
