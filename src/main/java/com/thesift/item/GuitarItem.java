package com.thesift.item;

import com.thesift.music.Instrument;
import com.thesift.music.Notes;
import com.thesift.registry.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;

/**
 * A Guitar: a spruce-topped parlour guitar with four gut strings and copper frets, crafted from
 * planks, string and copper, and played freely in the world (INS free play - see
 * {@link SiftInstrumentItem}). The Weaver's Guitar is one too.
 */
public class GuitarItem extends SiftInstrumentItem {
    public GuitarItem(Item.Properties properties) {
        this(Instrument.GUITAR, properties);
    }

    protected GuitarItem(Instrument instrument, Item.Properties properties) {
        super(instrument, properties);
    }

    /** The note a player's gaze picks on the old look-angle scale (the Weaver's Guitar strums it as it weaves). */
    public static int noteFor(Player player) {
        return Notes.lookPitch(player);
    }

    /** Server: one strum of a guitar at the player, heard by the songs. */
    public static void strum(ServerLevel level, Player player, int note) {
        strum(level, player, Instrument.GUITAR, note);
    }

    /** Server: one strum of {@code instrument} at the player, heard by the songs. */
    public static void strum(ServerLevel level, Player player, Instrument instrument, int note) {
        Notes.play(level, player, instrument, note);
        Vec3 at = Notes.mouth(player).add(0.0, -0.3, 0.0);
        level.sendParticles(ModParticles.SIFT_NOTE.get(), at.x, at.y, at.z, 1, 0.2, 0.2, 0.2, 1.0);
    }
}
