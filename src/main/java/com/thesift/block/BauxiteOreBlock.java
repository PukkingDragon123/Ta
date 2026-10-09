package com.thesift.block;

import com.thesift.registry.ModFluids;
import com.thesift.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Bauxite Ore: unstable. Broken open in the air it bursts (and the Bauxite is lost); mined while the miner or
 * the ore itself is in Chrome it comes out whole.
 */
public class BauxiteOreBlock extends DropExperienceBlock {
    public static final float BLAST = 2.0F;

    public BauxiteOreBlock(BlockBehaviour.Properties properties) {
        super(UniformInt.of(1, 3), properties);
    }

    /** True when it may be mined safely: the miner stands in Chrome, or Chrome touches the ore. */
    public static boolean isStable(Level level, BlockPos pos, @Nullable Player miner) {
        if (miner != null && miner.isInFluidType(ModFluids.CHROME_TYPE.get())) {
            return true;
        }
        if (level.getFluidState(pos).getType().isSame(ModFluids.CHROME.get())) {
            return true;
        }
        for (Direction d : Direction.values()) {
            if (level.getFluidState(pos.relative(d)).getType().isSame(ModFluids.CHROME.get())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void playerDestroy(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity,
            ItemStack destroyedWith) {
        if (isStable(level, pos, player)) {
            super.playerDestroy(level, player, pos, state, blockEntity, destroyedWith);
            return;
        }
        player.sendOverlayMessage(Component.translatable("message.thesift.bauxite.unstable"));
        detonate(level, pos);
    }

    /** The unstable ore bursts: a hiss, a puff, then a small blast. */
    public static void detonate(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, ModSounds.BAUXITE_HISS.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.0);
        level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, BLAST, Level.ExplosionInteraction.BLOCK);
    }
}
