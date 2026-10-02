package com.thesift.item;

import com.thesift.music.SongEvents;
import com.thesift.registry.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A Guitar, strung with Sculk String. Every strum plays one note, and where you look picks it:
 * look up for the high notes, down for the low ones (two octaves, 0-24, like a note block).
 */
public class GuitarItem extends Item {
    public GuitarItem(Item.Properties properties) {
        super(properties);
    }

    /** The note a player is fretting: from their look pitch, 0 (straight down) to 24 (straight up). */
    public static int noteFor(Player player) {
        return Mth.clamp(Math.round((90.0F - player.getXRot()) / 180.0F * 24.0F), 0, 24);
    }

    /** Plays one note of the guitar at the player and tells the song system about it. */
    public static void strum(ServerLevel level, Player player, int note) {
        float pitch = (float) Math.pow(2.0, (note - 12) / 12.0);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_GUITAR.value(), SoundSource.PLAYERS, 1.0F, pitch);
        Vec3 at = player.getEyePosition().add(player.getLookAngle().scale(0.6)).add(0.0, -0.5, 0.0);
        level.sendParticles(ParticleTypes.NOTE, at.x, at.y + 0.4, at.z, 0, note / 24.0, 0.0, 0.0, 1.0);
        level.sendParticles(ModParticles.SIFT_NOTE.get(), at.x, at.y, at.z, 1, 0.2, 0.2, 0.2, 1.0);
        SongEvents.note(level, player, player.position().add(0.0, 1.0, 0.0), note);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            strum(server, player, noteFor(player));
            player.getCooldowns().addCooldown(stack, 5);
        }
        return InteractionResult.SUCCESS;
    }
}
