package com.thesift.client;

import com.thesift.client.model.ModModelLayers;
import com.thesift.client.model.ModelGeometry;
import com.thesift.client.renderer.CaravanRenderer;
import com.thesift.client.renderer.MusicCrystalRenderer;
import com.thesift.registry.ModCaravans;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

/** C: the Caravans on the client - their model and renderer, the frozen-crystal renderer and the cavern's slowly shifting, many-coloured fog. */
public final class CaravansClient {
    /** How far into the cavern the camera is (eases in and out so the fog never snaps). */
    private static float cavern;

    private CaravansClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(CaravansClient::layers);
        modBus.addListener(CaravansClient::renderers);
        NeoForge.EVENT_BUS.addListener(CaravansClient::fogColour);
    }

    private static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayers.CARAVAN, ModelGeometry::caravan);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModCaravans.CARAVAN.get(), CaravanRenderer::new);
        event.registerBlockEntityRenderer(ModCaravans.MUSIC_CRYSTAL_ENTITY.get(), MusicCrystalRenderer::new);
    }

    /** In the Caravans Cavern the fog drifts through violet, rose and teal, like light through the crystals. */
    private static void fogColour(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        boolean inside = mc.level != null && mc.player != null && mc.level.getBiome(mc.player.blockPosition()).is(ModCaravans.CAVERN);
        cavern = Mth.clamp(cavern + (inside ? 0.01F : -0.01F), 0.0F, 1.0F);
        if (cavern <= 0.0F || mc.level == null) {
            return;
        }
        float t = (float) ((mc.level.getGameTime() + event.getPartialTick()) * 0.004);
        float r = 0.62F + 0.22F * Mth.sin(t);
        float g = 0.42F + 0.2F * Mth.sin(t + 2.1F);
        float b = 0.82F + 0.16F * Mth.sin(t + 4.2F);
        event.setRed(Mth.lerp(cavern, event.getRed(), r));
        event.setGreen(Mth.lerp(cavern, event.getGreen(), g));
        event.setBlue(Mth.lerp(cavern, event.getBlue(), b));
    }
}
