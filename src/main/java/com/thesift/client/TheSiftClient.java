package com.thesift.client;

import com.thesift.TheSift;
import com.thesift.client.dev.ClientSmokeTest;
import com.thesift.client.model.ModModelLayers;
import com.thesift.client.model.ModelGeometry;
import com.thesift.client.particle.SiftParticle;
import com.thesift.client.renderer.BulbRenderer;
import com.thesift.client.renderer.HarmonerRenderer;
import com.thesift.client.model.boss.StrumlingModel;
import com.thesift.client.model.boss.StrummerModel;
import com.thesift.client.model.boss.ThumperModel;
import com.thesift.client.model.boss.ThumplingModel;
import com.thesift.client.model.boss.WhistlerModel;
import com.thesift.client.model.boss.WhistlingModel;
import com.thesift.client.renderer.boss.MiniBossRenderer;
import com.thesift.entity.boss.Thumper;
import com.thesift.entity.boss.Whistler;
import com.thesift.entity.boss.Strummer;
import com.thesift.client.renderer.boss.DictatorRenderer;
import com.thesift.client.renderer.boss.MinionRenderer;
import com.thesift.client.renderer.SiftSnifferRenderer;
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
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.thesift.client.gui.ConductorBossBar::onBossBar);
        modBus.addListener(TheSiftClient::registerFluidModels);
        modBus.addListener(ClientEffects::registerOverlays);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ClientEffects::onClientTick);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ClientEffects::onPlaySound);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ClientEffects::onFogColor);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ClientEffects::onRenderFog);
        modBus.addListener(TheSiftClient::registerClientExtensions);
        modBus.addListener(TheSiftClient::registerEnvironmentRenderers);
        if (Boolean.getBoolean("thesift.clientsmoke")) {
            ClientSmokeTest.registerIfEnabled(); // CI only; never loaded in normal play
        }
    }

    private static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayers.BULB, ModelGeometry::bulb);
        event.registerLayerDefinition(ModModelLayers.HARMONER, ModelGeometry::harmoner);
        event.registerLayerDefinition(ModModelLayers.DICTATOR, ModelGeometry::dictator);
        event.registerLayerDefinition(ModModelLayers.THUMPER, ModelGeometry::thumper);
        event.registerLayerDefinition(ModModelLayers.WHISTLER, ModelGeometry::whistler);
        event.registerLayerDefinition(ModModelLayers.STRUMMER, ModelGeometry::strummer);
        event.registerLayerDefinition(ModModelLayers.THUMPLING, ModelGeometry::thumpling);
        event.registerLayerDefinition(ModModelLayers.WHISTLING, ModelGeometry::whistling);
        event.registerLayerDefinition(ModModelLayers.STRUMLING, ModelGeometry::strumling);
        event.registerLayerDefinition(ModModelLayers.CONDUCTOR_MASK, ModelGeometry::conductor_mask);
        event.registerLayerDefinition(ModModelLayers.SLUMBLER, ModelGeometry::slumbler);
        event.registerLayerDefinition(ModModelLayers.SIFTER, ModelGeometry::sifter);
        event.registerLayerDefinition(ModModelLayers.ENCHOER, ModelGeometry::enchoer);
        event.registerLayerDefinition(ModModelLayers.RIVETER, ModelGeometry::riveter);
        // the wild creatures
        event.registerLayerDefinition(ModModelLayers.STOMPER, ModelGeometry::stomper);
        event.registerLayerDefinition(ModModelLayers.FANFARE_EEL, ModelGeometry::fanfare_eel);
        event.registerLayerDefinition(ModModelLayers.KAZOO_FISH, ModelGeometry::kazoo_fish);
        event.registerLayerDefinition(ModModelLayers.TUBAFISH, ModelGeometry::tubafish);
        event.registerLayerDefinition(ModModelLayers.SKY_WHALE, ModelGeometry::sky_whale);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.BULB.get(), BulbRenderer::new);
        event.registerEntityRenderer(ModEntities.HARMONER.get(), HarmonerRenderer::new);
        event.registerEntityRenderer(ModEntities.SCULK_HARMONER.get(), com.thesift.client.renderer.SculkHarmonerRenderer::new);
        event.registerEntityRenderer(ModEntities.SIFT_SNIFFER.get(), SiftSnifferRenderer::new);
        event.registerEntityRenderer(ModEntities.DICTATOR.get(), DictatorRenderer::new);
        event.registerEntityRenderer(ModEntities.THUMPER.get(), c -> new MiniBossRenderer<Thumper, ThumperModel>(c, new ThumperModel(c.bakeLayer(ModModelLayers.THUMPER)),
                "thumper", Thumper.SCALE, 0.8F, (e, s) -> s.bossState == Thumper.DAZED && e.deathTime == 0 ? com.thesift.client.Expression.HURT : null));
        event.registerEntityRenderer(ModEntities.WHISTLER.get(), c -> new MiniBossRenderer<Whistler, WhistlerModel>(c, new WhistlerModel(c.bakeLayer(ModModelLayers.WHISTLER)),
                "whistler", Whistler.SCALE, 0.6F, (e, s) -> s.bossState == Whistler.STUNNED && e.deathTime == 0 ? com.thesift.client.Expression.HURT : null));
        event.registerEntityRenderer(ModEntities.STRUMMER.get(), c -> new MiniBossRenderer<Strummer, StrummerModel>(c, new StrummerModel(c.bakeLayer(ModModelLayers.STRUMMER)),
                "strummer", Strummer.SCALE, 0.9F, (e, s) -> null));
        event.registerEntityRenderer(ModEntities.THUMPLING.get(),
                c -> new MinionRenderer<>(c, new ThumplingModel(c.bakeLayer(ModModelLayers.THUMPLING)), "thumpling", 0.4F));
        event.registerEntityRenderer(ModEntities.WHISTLING.get(),
                c -> new MinionRenderer<>(c, new WhistlingModel(c.bakeLayer(ModModelLayers.WHISTLING)), "whistling", 0.35F));
        event.registerEntityRenderer(ModEntities.STRUMLING.get(),
                c -> new MinionRenderer<>(c, new StrumlingModel(c.bakeLayer(ModModelLayers.STRUMLING)), "strumling", 0.4F));
        event.registerEntityRenderer(ModEntities.CONDUCTOR_MASK.get(), com.thesift.client.renderer.boss.ConductorMaskRenderer::new);
        event.registerEntityRenderer(ModEntities.WEB_SHOT.get(), ctx -> new ThrownItemRenderer<>(ctx, 1.2F, false));
        event.registerEntityRenderer(ModEntities.SLUMBLER.get(), SlumblerRenderer::new);
        event.registerEntityRenderer(ModEntities.SIFTER.get(), SifterRenderer::new);
        event.registerEntityRenderer(ModEntities.ENCHOER.get(), EnchoerRenderer::new);
        event.registerEntityRenderer(ModEntities.RIVETER.get(), RiveterRenderer::new);
        event.registerEntityRenderer(ModEntities.GLOWBALL.get(), ctx -> new ThrownItemRenderer<>(ctx, 1.0F, true));
        event.registerBlockEntityRenderer(ModBlockEntities.EUPHORY_ALTAR.get(), EuphoryAltarRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.INSTRUMENT_ALTAR.get(), com.thesift.client.renderer.InstrumentAltarRenderer::new);
        // the wild creatures
        event.registerEntityRenderer(ModEntities.STOMPER.get(), com.thesift.client.renderer.StomperRenderer::new);
        event.registerEntityRenderer(ModEntities.SKY_WHALE.get(), com.thesift.client.renderer.SkyWhaleRenderer::new);
        event.registerEntityRenderer(ModEntities.FANFARE_EEL.get(), c -> new com.thesift.client.renderer.SiftFishRenderer<>(c,
                new com.thesift.client.model.FanfareEelModel(c.bakeLayer(ModModelLayers.FANFARE_EEL)), "fanfare_eel", 0.35F, 1.0F,
                Expression.BLINK, Expression.ANGRY, Expression.HURT, Expression.DEAD));
        event.registerEntityRenderer(ModEntities.KAZOO_FISH.get(), c -> new com.thesift.client.renderer.SiftFishRenderer<>(c,
                new com.thesift.client.model.KazooFishModel(c.bakeLayer(ModModelLayers.KAZOO_FISH)), "kazoo_fish", 0.2F, 1.0F,
                Expression.BLINK, Expression.HURT, Expression.DEAD));
        event.registerEntityRenderer(ModEntities.TUBAFISH.get(), c -> new com.thesift.client.renderer.SiftFishRenderer<>(c,
                new com.thesift.client.model.TubafishModel(c.bakeLayer(ModModelLayers.TUBAFISH)), "tubafish", 0.6F, 1.25F,
                Expression.BLINK, Expression.HAPPY, Expression.ANGRY, Expression.HURT, Expression.DEAD));
        event.registerEntityRenderer(ModEntities.BUBBLE.get(), ctx -> new ThrownItemRenderer<>(ctx, 1.4F, true));
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
        particle(event, ModParticles.GUIDE_NOTE, SiftParticle.Kind.GUIDE_NOTE);
        particle(event, ModParticles.WISHING_STAR, SiftParticle.Kind.WISHING_STAR);
        particle(event, ModParticles.SLEEP_SPORE, SiftParticle.Kind.SLEEP_SPORE);
        particle(event, ModParticles.KILL_STAR, SiftParticle.Kind.KILL_STAR);
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
