package com.thesift.client;

import com.thesift.world.Rumble;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Shakes the camera when something huge lands nearby (see {@link Rumble}). */
public final class CameraShake {
    private CameraShake() {
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        if (!Minecraft.getInstance().isPaused()) {
            Rumble.tick();
        }
    }

    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        float k = Rumble.felt(mc.player.position()) * mc.options.screenEffectScale().get().floatValue();
        if (k <= 0.01F) {
            return;
        }
        float time = mc.player.tickCount + (float) event.getPartialTick();
        // three unrelated wobbles, so it judders rather than sways
        event.setPitch(event.getPitch() + (Mth.sin(time * 2.7F) + Mth.sin(time * 5.3F + 0.4F) * 0.5F) * k * 0.5F);
        event.setYaw(event.getYaw() + Mth.sin(time * 2.1F + 1.3F) * k * 0.35F);
        event.setRoll(event.getRoll() + (Mth.sin(time * 3.3F + 0.7F) + Mth.sin(time * 6.1F) * 0.4F) * k * 0.7F);
    }
}
