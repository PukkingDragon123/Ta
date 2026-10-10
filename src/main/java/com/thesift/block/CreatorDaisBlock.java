package com.thesift.block;

import com.thesift.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * S1 land: the Creator's Dais, in the middle of every Creator's Ruin (tools/creators_ruin.py). The Creator's Hymn is
 * carved into it: use it to copy the hymn onto a Music Sheet. Played beside it (any instrument), the hymn summons the
 * Mini Creator, who rises out of the dais in a column of light ({@link com.thesift.knowledge.CreatorShrine}).
 */
public class CreatorDaisBlock extends Block {
    public CreatorDaisBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server) {
            boolean has = false;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                has |= player.getInventory().getItem(i).is(ModItems.MUSIC_SHEET_HYMN.get());
            }
            if (!has) {
                ItemStack sheet = new ItemStack(ModItems.MUSIC_SHEET_HYMN.get());
                if (!player.getInventory().add(sheet)) {
                    if (player.level() instanceof net.minecraft.server.level.ServerLevel dropLevel) player.spawnAtLocation(dropLevel, sheet); // 26.3: Player.drop needs a Prediction
                }
                player.sendOverlayMessage(Component.translatable("message.thesift.dais.sheet").withStyle(ChatFormatting.GOLD));
                server.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 1.0F);
            } else {
                player.sendOverlayMessage(Component.translatable("message.thesift.dais.known").withStyle(ChatFormatting.GOLD));
            }
            server.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 1.3F);
            server.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.4, pos.getZ() + 0.5, 30, 0.4, 0.4, 0.4, 0.6);
        }
        return InteractionResult.SUCCESS;
    }

    /** A soft glow: motes of light rising off the glyph circle, and magic drawn down into it. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.25 + random.nextDouble() * 0.5, pos.getY() + 1.05,
                    pos.getZ() + 0.25 + random.nextDouble() * 0.5, 0.0, 0.02 + random.nextDouble() * 0.02, 0.0);
        }
        if (random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.6, pos.getZ() + 0.5, (random.nextDouble() - 0.5) * 2.0,
                    -0.6 - random.nextDouble() * 0.4, (random.nextDouble() - 0.5) * 2.0);
        }
    }
}
