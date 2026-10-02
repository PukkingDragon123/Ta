package com.thesift.client;

import com.thesift.entity.CreatureLife;
import com.thesift.entity.Stomper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Riding a tame Stomper, the attack key makes it stomp instead of swinging at the air (agent A1). */
public final class RiderInput {
    private RiderInput() {
    }

    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.getVehicle() instanceof Stomper stomper && stomper.getControllingPassenger() == player) {
            ClientPacketDistributor.sendToServer(CreatureLife.RiderStomp.INSTANCE);
            event.setSwingHand(true);
            event.setCanceled(true);
        }
    }
}
