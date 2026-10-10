package com.thesift.client.dev;

import com.thesift.client.music.FreePlay;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * INS free play, client smoke test: a player raising an instrument and playing it (the client
 * smoke test's {@code play_*} scenes) - seen from the front in third person, or through their own
 * eyes - so CI renders the stances, the moving parts and the notes in flight. The stance is let go
 * and the camera put back a few seconds after the last scene.
 */
public final class InstrumentSmoke {
    private static int resetIn;
    private static boolean listening;

    private InstrumentSmoke() {
    }

    /** Client: raise the instrument in hand and play a phrase, the camera in front of the player or behind their eyes. */
    public static void play(boolean thirdPerson) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) {
            return;
        }
        if (!listening) {
            listening = true;
            NeoForge.EVENT_BUS.addListener(InstrumentSmoke::onTick);
        }
        mc.options.setCameraType(thirdPerson ? CameraType.THIRD_PERSON_FRONT : CameraType.FIRST_PERSON);
        mc.options.keyUse.setDown(true);
        if (!mc.player.isUsingItem()) {
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        }
        FreePlay.demo(160);
        resetIn = 90;
    }

    private static void onTick(ClientTickEvent.Post event) {
        if (resetIn > 0 && --resetIn == 0) {
            Minecraft mc = Minecraft.getInstance();
            mc.options.keyUse.setDown(false);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
    }
}
