package com.thesift.item;

import com.thesift.entity.BubbleEntity;
import com.thesift.registry.ModItems;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Bubble Gun: a brass blower built around a Tuba Bubble. Each squeeze blows a big wobbly bubble
 * that pops on whatever it touches, lifting it into the air for a moment. It needs no ammo - just
 * a breath between shots - and wears out slowly.
 */
public class BubbleGunItem extends Item {
    public BubbleGunItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            BubbleEntity bubble = new BubbleEntity(server, player, new ItemStack(ModItems.TUBA_BUBBLE.get()));
            bubble.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 0.9F, 1.5F);
            server.addFreshEntity(bubble);
            Vec3 muzzle = player.getEyePosition().add(player.getLookAngle().scale(0.8)).add(0.0, -0.15, 0.0);
            server.sendParticles(ModParticles.CHROME_BUBBLE.get(), muzzle.x, muzzle.y, muzzle.z, 8, 0.12, 0.12, 0.12, 0.05);
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.BUBBLE_POP, muzzle.x, muzzle.y, muzzle.z, 5, 0.1, 0.1, 0.1, 0.03);
            server.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.BUBBLE_GUN_SHOOT.get(), SoundSource.PLAYERS, 1.0F,
                    0.9F + server.getRandom().nextFloat() * 0.3F);
            player.getCooldowns().addCooldown(stack, 12);
            stack.hurtAndBreak(1, player, hand);
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        return InteractionResult.SUCCESS;
    }
}
