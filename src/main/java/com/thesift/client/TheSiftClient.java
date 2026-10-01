package com.thesift.client;

import com.thesift.TheSift;
import com.thesift.client.dev.ClientSmokeTest;
import com.thesift.client.model.ModModelLayers;
import com.thesift.client.model.ModelGeometry;
import com.thesift.client.particle.SiftParticle;
import com.thesift.client.renderer.BulbRenderer;
import com.thesift.client.renderer.EnchoerRenderer;
import com.thesift.client.renderer.EuphoryAltarRenderer;
import com.thesift.client.renderer.RiveterRenderer;
import com.thesift.client.renderer.SifterRenderer;
import com.thesift.client.renderer.SlumblerRenderer;
import com.thesift.client.sky.SiftSkyRenderer;
import com.thesift.registry.ModBlockEntities;
import com.thesift.registry.ModEntities;
import com.thesift.registry.ModFluids;
import com.thesift.registry.ModParticles;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterCustomEnvironmentEffectRendererEvent;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

@Mod(value = TheSift.MODID, dist = Dist.CLIENT)
public class TheSiftClient {
    public TheSiftClient(IEventBus modBus, ModContainer container) {
        modBus.addListener(TheSiftClient::registerLayers);
        modBus.addListener(TheSiftClient::registerRenderers);
        modBus.addListener(TheSiftClient::registerParticles);
        modBus.addListener(TheSiftClient::registerFluidModels);
        modBus.addListener(TheSiftClient::registerClientExtensions);
        modBus.addListener(TheSiftClient::registerEnvironmentRenderers);
        if (Boolean.getBoolean("thesift.clientsmoke")) {
            ClientSmokeTest.registerIfEnabled(); // CI only; never loaded in normal play
        }
    }

    private static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayers.BULB, ModelGeometry::bulb);
        event.registerLayerDefinition(ModModelLayers.SLUMBLER, ModelGeometry::slumbler);
        event.registerLayerDefinition(ModModelLayers.SIFTER, ModelGeometry::sifter);
        event.registerLayerDefinition(ModModelLayers.ENCHOER, ModelGeometry::enchoer);
        event.registerLayerDefinition(ModModelLayers.RIVETER, ModelGeometry::riveter);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.BULB.get(), BulbRenderer::new);
        event.registerEntityRenderer(ModEntities.SLUMBLER.get(), SlumblerRenderer::new);
        event.registerEntityRenderer(ModEntities.SIFTER.get(), SifterRenderer::new);
        event.registerEntityRenderer(ModEntities.ENCHOER.get(), EnchoerRenderer::new);
        event.registerEntityRenderer(ModEntities.RIVETER.get(), RiveterRenderer::new);
        event.registerEntityRenderer(ModEntities.GLOWBALL.get(), ctx -> new ThrownItemRenderer<>(ctx, 1.0F, true));
        event.registerBlockEntityRenderer(ModBlockEntities.EUPHORY_ALTAR.get(), EuphoryAltarRenderer::new);
    }

    private static void particle(RegisterParticleProvidersEvent event, DeferredHolder<ParticleType<?>, SimpleParticleType> type, SiftParticle.Kind kind) {
        event.registerSpriteSet(type.get(), sprites -> new SiftParticle.Provider(kind, sprites));
    }

    private static void registerParticles(RegisterParticleProvidersEvent event) {
        particle(event, ModParticles.DRIFTING_SOUL, SiftParticle.Kind.DRIFTING_SOUL);
        particle(event, ModParticles.CHROME_DROPLET, SiftParticle.Kind.CHROME_DROPLET);
        particle(event, ModParticles.CHROME_BUBBLE, SiftParticle.Kind.CHROME_BUBBLE);
        particle(event, ModParticles.DREAM_POLLEN, SiftParticle.Kind.DREAM_POLLEN);
        particle(event, ModParticles.SIFT_NOTE, SiftParticle.Kind.SIFT_NOTE);
        particle(event, ModParticles.RESONANCE_RING, SiftParticle.Kind.RESONANCE_RING);
        particle(event, ModParticles.GLOW_DUST, SiftParticle.Kind.GLOW_DUST);
        particle(event, ModParticles.LULLWOOD_LEAF, SiftParticle.Kind.LEAF);
        particle(event, ModParticles.WISHWOOD_LEAF, SiftParticle.Kind.LEAF);
        particle(event, ModParticles.SIFT_MIST, SiftParticle.Kind.SIFT_MIST);
        particle(event, ModParticles.STAR_SPARKLE, SiftParticle.Kind.STAR_SPARKLE);
        particle(event, ModParticles.PORTAL_SOUL, SiftParticle.Kind.PORTAL_SOUL);
        particle(event, ModParticles.FOOTSTEP_PUFF, SiftParticle.Kind.FOOTSTEP_PUFF);
        particle(event, ModParticles.GLOW_SPLAT, SiftParticle.Kind.GLOW_SPLAT);
        particle(event, ModParticles.SLIME_TRAIL, SiftParticle.Kind.SLIME_TRAIL);
        particle(event, ModParticles.WISHING_STAR, SiftParticle.Kind.WISHING_STAR);
        particle(event, ModParticles.SLEEP_SPORE, SiftParticle.Kind.SLEEP_SPORE);
    }

    private static void registerFluidModels(RegisterFluidModelsEvent event) {
        event.register(new FluidModel.Unbaked(new Material(TheSift.id("block/chrome_still")), new Material(TheSift.id("block/chrome_flow")),
                new Material(TheSift.id("block/chrome_overlay")), null), ModFluids.CHROME, ModFluids.FLOWING_CHROME);
    }

    private static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            private static final Identifier OVERLAY = TheSift.id("textures/misc/in_chrome.png");

            @Override
            public Identifier getRenderOverlayTexture(Minecraft mc) {
                return OVERLAY;
            }

            @Override
            public void modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount,
                    Vector4f fluidFogColor) {
                // Chrome shimmers between cyan and pink as you sink through it.
                float t = (Mth.sin((level.getGameTime() + partialTick) * 0.03F) + 1.0F) * 0.5F;
                fluidFogColor.set(Mth.lerp(t, 0.45F, 0.95F), Mth.lerp(t, 0.85F, 0.6F), Mth.lerp(t, 0.95F, 0.85F), 1.0F);
            }

            @Override
            public void modifyFogRender(Camera camera, @Nullable FogEnvironment environment, float renderDistance, float partialTick, FogData fogData) {
                fogData.environmentalStart = 0.0F;
                fogData.environmentalEnd = 12.0F;
            }
        }, ModFluids.CHROME_TYPE.get());
    }

    private static void registerEnvironmentRenderers(RegisterCustomEnvironmentEffectRendererEvent event) {
        event.registerSkyboxRenderer(TheSift.id("nebula"), new SiftSkyRenderer());
    }
}
