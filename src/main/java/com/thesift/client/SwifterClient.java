package com.thesift.client;

import com.thesift.TheSift;
import com.thesift.client.model.ModelGeometry;
import com.thesift.client.particle.SiftParticle;
import com.thesift.client.renderer.SwifterRenderer;
import com.thesift.registry.ModSwifter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

/** A2 Swifter &amp; White Forest, client side: the Swifter's model and renderer, drifting fluff, and the forest's soft white mist. */
public final class SwifterClient {
    public static final ModelLayerLocation SWIFTER = new ModelLayerLocation(TheSift.id("swifter"), "main");
    /** How far into the White Forest the camera is (eases in and out so the mist never snaps). */
    private static float forest;

    private SwifterClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(SwifterClient::layers);
        modBus.addListener(SwifterClient::renderers);
        modBus.addListener(SwifterClient::particles);
        NeoForge.EVENT_BUS.addListener(SwifterClient::fogColour);
        NeoForge.EVENT_BUS.addListener(SwifterClient::fog);
    }

    private static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SWIFTER, ModelGeometry::swifter);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModSwifter.SWIFTER.get(), SwifterRenderer::new);
    }

    private static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModSwifter.WHITE_FLUFF.get(), sprites -> new SiftParticle.Provider(SiftParticle.Kind.LEAF, sprites));
    }

    /** Under the white trees the fog turns soft pearl-white. */
    private static void fogColour(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        boolean inside = mc.level != null && mc.player != null && mc.level.getBiome(mc.player.blockPosition()).is(ModSwifter.WHITE_FOREST);
        forest = Mth.clamp(forest + (inside ? 0.01F : -0.01F), 0.0F, 1.0F);
        if (forest <= 0.0F) {
            return;
        }
        float k = forest * 0.6F;
        event.setRed(Mth.lerp(k, event.getRed(), 0.92F));
        event.setGreen(Mth.lerp(k, event.getGreen(), 0.95F));
        event.setBlue(Mth.lerp(k, event.getBlue(), 1.0F));
    }

    /** ...and closes in a little, so the far trees fade into mist. */
    private static void fog(ViewportEvent.RenderFog event) {
        if (forest <= 0.0F) {
            return;
        }
        event.setFarPlaneDistance(Mth.lerp(forest, event.getFarPlaneDistance(), Math.min(event.getFarPlaneDistance(), 72.0F)));
        event.setNearPlaneDistance(Mth.lerp(forest, event.getNearPlaneDistance(), 4.0F));
    }
}
