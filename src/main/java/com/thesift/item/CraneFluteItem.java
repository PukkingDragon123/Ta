package com.thesift.item;

import com.thesift.registry.ModEffects;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The Crane Flute, carved from the Whistler's beak.
 *
 * <ul>
 *   <li>use: a rising phrase that lifts you into the air for a moment, then lets you drift down.</li>
 *   <li>sneak + use: a Feather Shield - a whirl of feathers around you that turns most harm away
 *   (and arrows entirely) but that you can barely see through.</li>
 *   <li>use while looking up at the sky beside a tamed Harmoner: the whale song.</li>
 * </ul>
 */
public class CraneFluteItem extends Item {
    public CraneFluteItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            if (player.isSecondaryUseActive()) {
                player.addEffect(new MobEffectInstance(ModEffects.FEATHER_SHIELD, 200, 0));
                server.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.CRANE_FLUTE_SHIELD.get(), SoundSource.PLAYERS, 1.5F, 1.0F);
                server.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.8, 0.8, 0.8, 0.05);
                player.getCooldowns().addCooldown(stack, 500);
            } else if (player.getXRot() < -50.0F && whaleSong(server, player)) {
                player.getCooldowns().addCooldown(stack, 200);
            } else {
                // a rising phrase: up you go
                float[] phrase = {0.7F, 0.9F, 1.05F, 1.4F};
                for (int i = 0; i < phrase.length; i++) {
                    server.playSound(null, player.getX(), player.getY(), player.getZ(), i == 0 ? ModSounds.CRANE_FLUTE_PLAY.get() : SoundEvents.NOTE_BLOCK_FLUTE.value(),
                            SoundSource.PLAYERS, 1.2F, phrase[i]);
                }
                player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 26, 4));
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0));
                server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 0.2, player.getZ(), 20, 0.5, 0.1, 0.5, 0.02);
                server.sendParticles(ModParticles.SIFT_NOTE.get(), player.getX(), player.getY() + 2.0, player.getZ(), 6, 0.4, 0.3, 0.4, 1.0);
                server.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 12, 0.6, 0.1, 0.6, 0.03);
                player.getCooldowns().addCooldown(stack, 60);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** The whale song (see the Sky Whale); true if something in the sky answered. */
    private static boolean whaleSong(ServerLevel level, Player player) {
        return false;
    }
}
