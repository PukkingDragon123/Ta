package com.thesift.item;

import com.thesift.entity.SoulGolem;
import com.thesift.registry.ModEchoer;
import com.thesift.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * A Soul Golem Core: the still-warm soul fire of an ancient construct. Set it into a block of soul
 * soil and the soil gathers itself up around it into a Soul Golem - yours.
 */
public class SoulGolemCoreItem extends Item {
    public SoulGolemCoreItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.getBlockState(pos).is(Blocks.SOUL_SOIL)) {
            Player p = context.getPlayer();
            if (p != null && !level.isClientSide()) {
                p.sendOverlayMessage(Component.translatable("message.thesift.soul_golem_core.needs_soil"));
            }
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            SoulGolem golem = ModEchoer.SOUL_GOLEM.get().create(server, EntitySpawnReason.MOB_SUMMONED);
            if (golem == null) {
                return InteractionResult.FAIL;
            }
            server.removeBlock(pos, false);
            Player player = context.getPlayer();
            float yaw = player != null ? player.getYRot() + 180.0F : 0.0F;
            golem.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0F);
            golem.setYHeadRot(yaw);
            golem.yBodyRot = yaw;
            if (player != null) {
                golem.setOwner(player);
            }
            server.addFreshEntity(golem);
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.5;
            double z = pos.getZ() + 0.5;
            server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 30, 0.35, 0.35, 0.35, 0.05);
            server.sendParticles(ParticleTypes.SOUL, x, y + 0.4, z, 10, 0.3, 0.3, 0.3, 0.02);
            server.sendParticles(ModParticles.RESONANCE_RING.get(), x, pos.getY() + 0.05, z, 0, 1.4, 0.0, 0.0, 1.0);
            server.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.NEUTRAL, 1.2F, 0.7F);
            server.playSound(null, pos, ModEchoer.GOLEM_RECHARGE.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
            context.getItemInHand().consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }
}
