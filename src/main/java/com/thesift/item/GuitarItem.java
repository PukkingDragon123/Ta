package com.thesift.item;

import com.thesift.music.Instrument;
import com.thesift.music.Notes;
import com.thesift.registry.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A Guitar, strung with Sculk String. Every strum plays one note, and where you look picks it:
 * look up for the high notes, down for the low ones (two octaves, 0-24, like a note block) - the
 * same scale as every instrument and the on-screen note ladder ({@link Notes#lookPitch}).
 */
public class GuitarItem extends Item {
    public GuitarItem(Item.Properties properties) {
        super(properties);
    }

    /** The note a player is fretting: the shared look-pitch scale (straight ahead is F#4). */
    public static int noteFor(Player player) {
        return Notes.lookPitch(player);
    }

    /** Plays one note of the guitar at the player and tells the song system about it (server). */
    public static void strum(ServerLevel level, Player player, int note) {
        Notes.play(level, player, Instrument.GUITAR, note);
        Vec3 at = Notes.mouth(player).add(0.0, -0.3, 0.0);
        level.sendParticles(ModParticles.SIFT_NOTE.get(), at.x, at.y, at.z, 1, 0.2, 0.2, 0.2, 1.0);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            strum(server, player, noteFor(player));
        } else {
            Notes.play(level, player, Instrument.GUITAR, noteFor(player));
        }
        player.getCooldowns().addCooldown(stack, 5);
        return InteractionResult.SUCCESS;
    }
}
